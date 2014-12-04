package no.difi.meldingsutveksling.dokumentpakking;

import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateManifest;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSBD;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSignature;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateZip;
import no.difi.meldingsutveksling.dokumentpakking.service.EncryptPayload;
import no.difi.meldingsutveksling.dokumentpakking.xml.MarshalSBD;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Mottaker;

import java.io.ByteArrayOutputStream;

public class Dokumentpakker {

	private EncryptPayload encryptPayload;
	private CreateSBD createSBD;
	private CreateAsice createAsice;

	public Dokumentpakker() {
		createSBD = new CreateSBD();
		encryptPayload = new EncryptPayload();
		createAsice = new CreateAsice(new CreateSignature(), new CreateZip(), new CreateManifest());
	}

	public byte[] pakkDokumentISbd(ByteArrayFile document, Avsender avsender, Mottaker mottaker, String conversationId,String type) {
		Payload payload = new Payload(encryptPayload.encrypt(createAsice.createAsice(document, avsender, mottaker).getBytes(), mottaker));
		no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument doc = createSBD.createSBD(avsender.getOrgNummer(), mottaker.getOrgNummer(), payload, conversationId,type);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MarshalSBD.marshal(doc, os);
		return os.toByteArray();
	}
}
