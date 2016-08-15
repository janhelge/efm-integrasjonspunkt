package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.*;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.ptv.CorrespondenceRequest;

import static no.difi.meldingsutveksling.noarkexchange.PutMessageMarker.markerFrom;

public class PostVirksomhetPutMessageStrategy implements PutMessageStrategy {

    private final CorrespondenceAgencyConfiguration config;

    public PostVirksomhetPutMessageStrategy(CorrespondenceAgencyConfiguration config) {
        this.config = config;
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType requestType) {
        final PutMessageRequestWrapper putMessageWrapper = new PutMessageRequestWrapper(requestType);
        try {
            final InsertCorrespondenceV2 message = CorrespondenceAgencyMessageFactory.create(config, putMessageWrapper);
            CorrespondenceAgencyClient client = new CorrespondenceAgencyClient(markerFrom(putMessageWrapper));
            final CorrespondenceRequest request = new CorrespondenceRequest.Builder().withUsername(config.getSystemUserCode()).withPassword(config.getPassword()).withPayload(message).build();

            client.send(request);
            return PutMessageResponseFactory.createOkResponse();
        } catch (PayloadException e) {
            Audit.error("Unable to create message for Post til virksomhet", markerFrom(putMessageWrapper), e);
            return PutMessageResponseFactory.createErrorResponse(new MessageException(StatusMessage.POST_VIRKSOMHET_REQUEST_MISSING_VALUES));
        }
    }
}
