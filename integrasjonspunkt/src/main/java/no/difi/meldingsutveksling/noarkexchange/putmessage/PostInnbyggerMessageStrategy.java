package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.ptp.Document;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerClient;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerException;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;

import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createErrorResponse;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createOkResponse;

public class PostInnbyggerMessageStrategy implements MessageStrategy {

    private final ServiceRegistryLookup serviceRegistry;
    private MeldingsformidlerClient.Config config;

    public PostInnbyggerMessageStrategy(MeldingsformidlerClient.Config config, ServiceRegistryLookup serviceRegistryLookup) {
        this.config = config;
        this.serviceRegistry = serviceRegistryLookup;
    }

    @Override
    public PutMessageResponseType putMessage(final EDUCore request) {
        final MeldingsformidlerClient.Config config = this.config;
        final ServiceRecord serviceRecord = serviceRegistry.getPrimaryServiceRecord(request.getReceiver().getOrgNr());

        MeldingsformidlerClient client = new MeldingsformidlerClient(config);
        try {
            client.sendMelding(new MeldingsformidlerRequest() {
                @Override
                public Document getDocument() {
                    final MeldingType meldingType = request.getPayloadAsMeldingType();
                    final DokumentType dokumentType = meldingType.getJournpost().getDokument().get(0);
                    return new Document(dokumentType.getFil().getBase64(), dokumentType.getVeMimeType(), dokumentType.getVeFilnavn(), dokumentType.getDbTittel());
                }

                @Override
                public List<Document> getAttachements() {
                    return null;
                }

                @Override
                public String getMottakerPid() {
                    return request.getReceiver().getOrgNr();
                }

                @Override
                public String getSubject() {
                    return request.getPayloadAsMeldingType().getNoarksak().getSaOfftittel(); /* TODO: er dette riktig sted og finne subject */
                }

                @Override
                public String getSenderOrgnumber() {
                    return request.getSender().getOrgNr();
                }

                @Override
                public String getConversationId() {
                    return String.valueOf(UUID.randomUUID()); /* TODO: finnes denne i EduCore? */
                }

                @Override
                public String getPostkasseAdresse() {
                    return serviceRecord.getPostkasseAdresse(); /* fra KRR via SR */
                }

                @Override
                public byte[] getCertificate() {
                    try {
                        return serviceRecord.getPemCertificate().getBytes("UTF-8"); /* fra KRR via SR */
                    } catch (UnsupportedEncodingException e) {
                        throw new MeldingsUtvekslingRuntimeException("Pem certificate from servicerecord problems", e);
                    }
                }

                @Override
                public String getOrgnrPostkasse() {
                    return serviceRecord.getOrgnrPostkasse(); /* fra KRR via SR */
                }

                @Override
                public String getSpraakKode() {
                    return null; /* TODO: hvor hentes denne fra? EduCore? */
                }

                @Override
                public String getQueueId() {
                    return "queueId"; /* TODO: hva skal denne egentlig settes til? */
                }

            });
        } catch (MeldingsformidlerException e) {
            createErrorResponse(StatusMessage.UNABLE_TO_SEND_DPI);
        }
        return createOkResponse();
    }
}
