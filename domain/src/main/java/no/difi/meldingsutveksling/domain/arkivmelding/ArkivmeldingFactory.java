package no.difi.meldingsutveksling.domain.arkivmelding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.*;
import no.arkivverket.standarder.noark5.metadatakatalog.Korrespondanseparttype;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArkivmeldingFactory {

    private final CryptoMessagePersister cryptoMessagePersister;

    public Arkivmelding createArkivmeldingAndWriteFiles(PutMessageRequestWrapper putMessage) {
        MeldingType mt = EDUCoreConverter.payloadAsMeldingType(putMessage.getPayload());
        no.arkivverket.standarder.noark5.arkivmelding.ObjectFactory amOf = new no.arkivverket.standarder.noark5.arkivmelding.ObjectFactory();
        Arkivmelding am = amOf.createArkivmelding();

        if (mt.getNoarksak() == null) {
            throw new MeldingsUtvekslingRuntimeException(format("No Noarksak in MeldingType for message %s, aborting conversion", putMessage.getConversationId()));
        }
        Saksmappe sm = amOf.createSaksmappe();
        ofNullable(mt.getNoarksak().getSaSaar()).map(BigInteger::new).ifPresent(sm::setSaksaar);
        ofNullable(mt.getNoarksak().getSaSeknr()).map(BigInteger::new).ifPresent(sm::setSakssekvensnummer);
        ofNullable(mt.getNoarksak().getSaAnsvinit()).ifPresent(sm::setSaksansvarlig);
        ofNullable(mt.getNoarksak().getSaAdmkort()).ifPresent(sm::setAdministrativEnhet);
        ofNullable(mt.getNoarksak().getSaOfftittel()).ifPresent(sm::setOffentligTittel);

        if (mt.getJournpost() == null) {
            throw new MeldingsUtvekslingRuntimeException(format("No Journpost in MeldingType for message %s, aborting conversion", putMessage.getConversationId()));
        }
        Journalpost jp = amOf.createJournalpost();
        ofNullable(mt.getJournpost().getJpJaar()).map(BigInteger::new).ifPresent(jp::setJournalaar);
        ofNullable(mt.getJournpost().getJpSeknr()).map(BigInteger::new).ifPresent(jp::setJournalsekvensnummer);
        ofNullable(mt.getJournpost().getJpJpostnr()).map(BigInteger::new).ifPresent(jp::setJournalpostnummer);
        ofNullable(mt.getJournpost().getJpNdoktype()).map(JournalposttypeMapper::getArkivmeldingType).ifPresent(jp::setJournalposttype);
        Skjerming skjerming = amOf.createSkjerming();
        ofNullable(mt.getJournpost().getJpUoff()).ifPresent(skjerming::setSkjermingshjemmel);
        sm.setSkjerming(skjerming);

        // expecting date in format yyyy-MM-dd
        Optional<String> jpDato = ofNullable(mt.getJournpost().getJpJdato());
        if (jpDato.isPresent()) {
            jp.setJournaldato(ArkivmeldingUtil.stringAsXmlGregorianCalendar(mt.getJournpost().getJpJdato()));
        }
        Optional<String> jpDokdato = ofNullable(mt.getJournpost().getJpDokdato());
        if (jpDokdato.isPresent()) {
            jp.setDokumentetsDato(ArkivmeldingUtil.stringAsXmlGregorianCalendar(mt.getJournpost().getJpDokdato()));
        }

        mt.getJournpost().getAvsmot().forEach(a -> {
            Korrespondansepart kp = amOf.createKorrespondansepart();
            ofNullable(a.getAmNavn()).ifPresent(kp::setKorrespondansepartNavn);
            ofNullable(a.getAmAdmkort()).ifPresent(kp::setAdministrativEnhet);
            ofNullable(a.getAmSbhinit()).ifPresent(kp::setSaksbehandler);

            if ("0".equals(a.getAmIhtype())) {
                kp.setKorrespondanseparttype(Korrespondanseparttype.AVSENDER);
            }
            if ("1".equals(a.getAmIhtype())) {
                kp.setKorrespondanseparttype(Korrespondanseparttype.MOTTAKER);
            }

            Avskrivning avs = amOf.createAvskrivning();
            ofNullable(a.getAmAvskm()).filter(s -> !s.isEmpty()).map(AvskrivningsmaateMapper::getArkivmeldingType).ifPresent(avs::setAvskrivningsmaate);
            ofNullable(a.getAmAvsavdok()).ifPresent(avs::setReferanseAvskrivesAvJournalpost);
            if (!isNullOrEmpty(a.getAmAvskdato())) {
                avs.setAvskrivningsdato(ArkivmeldingUtil.stringAsXmlGregorianCalendar(a.getAmAvskdato()));
            }

            jp.getAvskrivning().add(avs);
            jp.getKorrespondansepart().add(kp);

        });

        mt.getJournpost().getDokument().forEach(d -> {
            Dokumentbeskrivelse dbeskr = amOf.createDokumentbeskrivelse();
            dbeskr.setTittel(d.getDbTittel());
            ofNullable(d.getDlRnr()).map(BigInteger::new).ifPresent(dbeskr::setDokumentnummer);
            ofNullable(d.getDlType()).map(TilknyttetRegistreringSomMapper::getArkivmeldingType).ifPresent(dbeskr::setTilknyttetRegistreringSom);

            Dokumentobjekt dobj = amOf.createDokumentobjekt();
            dobj.setReferanseDokumentfil(d.getVeFilnavn());
            ofNullable(d.getVeVariant()).map(VariantformatMapper::getArkivmeldingType).ifPresent(dobj::setVariantformat);

            dbeskr.getDokumentobjekt().add(dobj);
            jp.getDokumentbeskrivelseAndDokumentobjekt().add(dbeskr);
        });

        sm.getBasisregistrering().add(jp);
        am.getMappe().add(sm);
        return am;
    }

}