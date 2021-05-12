package no.difi.meldingsutveksling.dpi;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.move.common.cert.KeystoreProvider;
import no.difi.move.common.cert.KeystoreProviderException;
import no.difi.sdp.client2.KlientKonfigurasjon;
import no.difi.sdp.client2.SendResultat;
import no.difi.sdp.client2.SikkerDigitalPostKlient;
import no.difi.sdp.client2.domain.*;
import no.difi.sdp.client2.domain.exceptions.SendException;
import no.difi.sdp.client2.internal.Billable;
import no.difi.sdp.client2.internal.EbmsForsendelseBuilder;
import no.difi.sdp.client2.internal.TrustedCertificates;
import no.digipost.api.representations.EbmsForsendelse;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class SikkerDigitalPostKlientFactory {

    private final IntegrasjonspunktProperties props;

    public SikkerDigitalPostKlient createSikkerDigitalPostKlient(AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer) {
        KlientKonfigurasjon klientKonfigurasjon = createKlientKonfigurasjonBuilder().build();
        return createSikkerDigitalPostKlient(klientKonfigurasjon, aktoerOrganisasjonsnummer);
    }

    public SikkerDigitalPostKlient createSikkerDigitalPostKlient(AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer, ClientInterceptor clientInterceptor) {
        KlientKonfigurasjon klientKonfigurasjon = createKlientKonfigurasjonBuilder().soapInterceptors(clientInterceptor).build();
        return createSikkerDigitalPostKlient(klientKonfigurasjon, aktoerOrganisasjonsnummer);
    }

    private SikkerDigitalPostKlient createSikkerDigitalPostKlient(KlientKonfigurasjon klientKonfigurasjon, AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer) {
        Databehandler tekniskAvsender;

        KeyStore keyStore;
        try {
            keyStore = KeystoreProvider.loadKeyStore(props.getDpi().getKeystore());
        } catch (KeystoreProviderException e) {
            throw new MeldingsUtvekslingRuntimeException("Cannot load DPI keystore", e);
        }

        if (props.getDpi().getTrustStore() != null) {
            KeyStore trustStore;
            try {
                trustStore = KeystoreProvider.loadKeyStore(props.getDpi().getTrustStore());
            } catch (KeystoreProviderException e) {
                throw new MeldingsUtvekslingRuntimeException("Cannot load DPI trust store", e);
            }
            KeyStore trustedSDP = TrustedCertificates.getTrustStore();
            Enumeration<String> aliases;
            try {
                aliases = trustedSDP.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    Certificate certificate = trustedSDP.getCertificate(alias);
                    trustStore.setCertificateEntry(alias, certificate);
                }
            } catch (KeyStoreException e) {
                throw new MeldingsUtvekslingRuntimeException("Could not get SDP truststore aliases", e);
            }

            NoekkelparOverride noekkelparOverride = new NoekkelparOverride(keyStore, trustStore,
                    props.getDpi().getKeystore().getAlias(),
                    props.getDpi().getKeystore().getPassword(),
                    false);
            tekniskAvsender = Databehandler.builder(aktoerOrganisasjonsnummer.forfremTilDatabehandler(), noekkelparOverride).build();
        } else {
            tekniskAvsender = Databehandler.builder(aktoerOrganisasjonsnummer.forfremTilDatabehandler(),
                    Noekkelpar.fraKeyStoreUtenTrustStore(keyStore,
                            props.getDpi().getKeystore().getAlias(),
                            props.getDpi().getKeystore().getPassword()))
                    .build();
        }

        return new SikkerDigitalPostKlient(tekniskAvsender, klientKonfigurasjon) {

            @Override
            public SendResultat send(Forsendelse forsendelse) throws SendException {
                Billable<EbmsForsendelse> forsendelseBundleWithBillableBytes = new EbmsForsendelseBuilder().buildEbmsForsendelse(tekniskAvsender, klientKonfigurasjon.getMeldingsformidlerOrganisasjon(), forsendelse);
                EbmsForsendelse entity = forsendelseBundleWithBillableBytes.entity;
                File targetFile = new File(String.format("%s.asice", entity.instanceIdentifier));
                try(InputStream inputStream = entity.getDokumentpakke().getInputStream()) {
                    targetFile.createNewFile();
                    Files.copy(
                            inputStream,
                            targetFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new SendException(String.format("Couldn't store file %s", targetFile), SendException.AntattSkyldig.KLIENT, e);
                }
                return new SendResultat(entity.messageId, entity.refToMessageId, forsendelseBundleWithBillableBytes.billableBytes);
            }

        };
    }

    private KlientKonfigurasjon.Builder createKlientKonfigurasjonBuilder() {
        return KlientKonfigurasjon.builder(getMiljo())
                .connectionTimeout(20, TimeUnit.SECONDS);
    }

    private Miljo getMiljo() {
        return new Miljo(null, URI.create(props.getDpi().getEndpoint()));
    }
}
