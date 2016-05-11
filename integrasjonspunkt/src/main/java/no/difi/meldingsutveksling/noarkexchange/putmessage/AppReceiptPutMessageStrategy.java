package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createOkResponse;

/**
 * Strategy for handling appreceipt messages.
 *
 * @author Glenn Bech
 */
class AppReceiptPutMessageStrategy implements PutMessageStrategy {

    private final PutMessageContext context;

    private static final JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext.newInstance("no.difi.meldingsutveksling.noarkexchange.schema");
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    public AppReceiptPutMessageStrategy(PutMessageContext context) {
        this.context = context;
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType request) {
        final PutMessageRequestWrapper wrapper = new PutMessageRequestWrapper(request);
        Audit.info("Received AppReceipt", markerFrom(wrapper));
        final String payload = (String) request.getPayload();
        try {
             AppReceiptType receipt = PayloadUtil.getAppReceiptType(payload);
            if (receipt.getType().equals("OK")) {
                wrapper.swapSenderAndReceiver();
                context.getMessageSender().sendMessage(wrapper.getRequest());
                Audit.info("AppReceipt sent to "+ wrapper.getRecieverPartyNumber(), markerFrom(wrapper));
            }
            return createOkResponse();
        } catch (JAXBException e) {
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
                final Marshaller marshaller = jaxbContext.createMarshaller();
                StringWriter requestAsXml = new StringWriter(4096);
                marshaller.marshal(request, requestAsXml);
                Audit.error("This request resultet in error: {}", markerFrom(new PutMessageRequestWrapper(request)), requestAsXml.toString());
            } catch (JAXBException e1) {
                throw new MeldingsUtvekslingRuntimeException(e1);
            }
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }
}
