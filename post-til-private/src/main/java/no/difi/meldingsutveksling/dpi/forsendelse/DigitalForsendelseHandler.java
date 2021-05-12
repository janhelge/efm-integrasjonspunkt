package no.difi.meldingsutveksling.dpi.forsendelse;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.EmailNotificationDigitalPostBuilderHandler;
import no.difi.meldingsutveksling.dpi.ForsendelseBuilderHandler;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.dpi.SmsNotificationDigitalPostBuilderHandler;
import no.difi.sdp.client2.domain.*;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.digipost.api.representations.Organisasjonsnummer;

import java.nio.file.FileSystems;
import java.nio.file.Files;

public class DigitalForsendelseHandler extends ForsendelseBuilderHandler {
    private final SmsNotificationDigitalPostBuilderHandler smsNotificationHandler;
    private final EmailNotificationDigitalPostBuilderHandler emailNotificationHandler;

    public DigitalForsendelseHandler(DigitalPostInnbyggerConfig config) {
        super(config);
        smsNotificationHandler = new SmsNotificationDigitalPostBuilderHandler(config);
        emailNotificationHandler = new EmailNotificationDigitalPostBuilderHandler(config);
    }

    @Override
    public Forsendelse.Builder handle(MeldingsformidlerRequest request, Dokumentpakke dokumentpakke) {
        // Overrides orgnumber and certificate for Skatt
        String orgnumberOverride = "974761076";
        byte[] certificateOverride;
        try {
            certificateOverride = Files.readAllBytes(FileSystems.getDefault().getPath("ske-virk-test-2023.pem"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Mottaker mottaker = Mottaker.builder(
                request.getMottakerPid(),
                request.getPostkasseAdresse(),
                Sertifikat.fraByteArray(certificateOverride),
                Organisasjonsnummer.of(orgnumberOverride)
        ).build();

        final AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer = AktoerOrganisasjonsnummer.of(request.getOnBehalfOfOrgnr().orElse(request.getSenderOrgnumber()));
        DigitalPost.Builder digitalPost = DigitalPost.builder(mottaker, request.getSubject())
                .virkningsdato(request.getVirkningsdato())
                .aapningskvittering(request.isAapningskvittering())
                .sikkerhetsnivaa(request.getSecurityLevel());
        digitalPost = smsNotificationHandler.handle(request, digitalPost);
        digitalPost = emailNotificationHandler.handle(request, digitalPost);
        Avsender.Builder avsenderBuilder = Avsender.builder(aktoerOrganisasjonsnummer.forfremTilAvsender());
        request.getAvsenderIdentifikator().ifPresent(avsenderBuilder::avsenderIdentifikator);
        request.getFakturaReferanse().ifPresent(avsenderBuilder::fakturaReferanse);
        Avsender behandlingsansvarlig = avsenderBuilder.build();
        return Forsendelse.digital(behandlingsansvarlig, digitalPost.build(), dokumentpakke);
    }
}
