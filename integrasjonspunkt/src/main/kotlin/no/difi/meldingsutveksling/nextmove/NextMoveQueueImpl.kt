package no.difi.meldingsutveksling.nextmove

import com.nimbusds.jose.proc.BadJWSException
import no.difi.meldingsutveksling.NextMoveConsts
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.api.ConversationService
import no.difi.meldingsutveksling.api.MessagePersister
import no.difi.meldingsutveksling.api.NextMoveQueue
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil
import no.difi.meldingsutveksling.domain.sbdh.ScopeType
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.logging.Audit
import no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository
import no.difi.meldingsutveksling.receipt.ReceiptStatus
import no.difi.meldingsutveksling.status.ConversationMarker
import no.difi.meldingsutveksling.util.logger
import no.difi.move.common.oauth.JWTDecoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream


@Component
open class NextMoveQueueImpl(private val messageRepo: NextMoveMessageInRepository,
                             private val conversationService: ConversationService,
                             private val sbdUtil: SBDUtil,
                             private val messagePersister: MessagePersister,
                             private val timeToLiveHelper: TimeToLiveHelper,
                             private val statusSender: ResponseStatusSender,
                             private val jwtDecoder: JWTDecoder) : NextMoveQueue {

    val log = logger()

    override fun enqueueIncomingStatus(sbd: StandardBusinessDocument, serviceIdentifier: ServiceIdentifier) {
        enqueueIncomingMessage(sbd, serviceIdentifier, null)
    }

    @Transactional
    override fun enqueueIncomingMessage(sbd: StandardBusinessDocument, serviceIdentifier: ServiceIdentifier, asicStream: InputStream?) {
        when {
            sbd.any !is BusinessMessage<*> -> throw MeldingsUtvekslingRuntimeException("SBD payload not of a known type")
            sbdUtil.isExpired(sbd) -> {
                timeToLiveHelper.registerErrorStatusAndMessage(sbd, serviceIdentifier, ConversationDirection.INCOMING)
                asicStream?.close()
                return
            }
            sbdUtil.isStatus(sbd) -> {
                log.debug("Message with id=${sbd.documentId} is a receipt")
                conversationService.registerStatus(sbd.documentId, (sbd.any as StatusMessage).status)
                return
            }
        }

        if (sbd.findScope(ScopeType.SIGNED_JWT).isPresent) {
            sbd.getScope(ScopeType.SIGNED_JWT).let {
                try {
                    // Will check for valid signature
                    jwtDecoder.getPayload(it.identifier)
                } catch (e: BadJWSException) {
                    asicStream?.close()
                    val c = conversationService.registerConversation(sbd, serviceIdentifier, ConversationDirection.INCOMING)
                    conversationService.registerStatus(c.messageId, ReceiptStatus.FEIL, "Signature verification failed: ${e.message}")
                    log.warn(ConversationMarker.markerFrom(c), "Error verifying signature for message[id=${sbd.messageId}], discarding", e)
                    return
                }
            }
        }

        asicStream?.use { messagePersister.writeStream(sbd.documentId, NextMoveConsts.ASIC_FILE, it, -1L) }

        val message = messageRepo.findByMessageId(sbd.documentId).orElseGet {
            messageRepo.save(NextMoveInMessage.of(sbd, serviceIdentifier))
        }

        conversationService.registerConversation(sbd, serviceIdentifier, ConversationDirection.INCOMING, ReceiptStatus.INNKOMMENDE_MOTTATT)
        statusSender.queue(message.sbd, serviceIdentifier, ReceiptStatus.MOTTATT)

        log.info(markerFrom(message), "Message [id=${message.messageId}, serviceIdentifier=$serviceIdentifier] put on local queue")
    }

}