package no.difi.meldingsutveksling.dokumentpakking.service;

import java.util.UUID;

import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;

import org.joda.time.DateTime;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.BusinessScope;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.DocumentIdentification;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.Partner;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.PartnerIdentification;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.Scope;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocumentHeader;

public class CreateSBD {
	public static final String STANDARD = "urn:no:difi:meldingsutveksling:1.0";
	public static final String HEADER_VERSION = "1.0";
	public static final String TYPE_VERSION = "1.0";
	public static final String CONVERSATIONID = "ConversationId";
	public static final String TYPE = "BEST/EDU";
	public static final String INSTANCE_IDENTIFIER = "12345678-id";
	
	public StandardBusinessDocument createSBD(Organisasjonsnummer avsender, Organisasjonsnummer mottaker, Payload payload, String conversationId) {
    	StandardBusinessDocument doc = new StandardBusinessDocument()
		.withStandardBusinessDocumentHeader(
				new StandardBusinessDocumentHeader()
						.withHeaderVersion(HEADER_VERSION)
						.withSenders(new Partner().withIdentifier(new PartnerIdentification(avsender.asIso6523(), Organisasjonsnummer.ISO6523_ACTORID)))
						.withReceivers(new Partner().withIdentifier(new PartnerIdentification(mottaker.asIso6523(), Organisasjonsnummer.ISO6523_ACTORID)))
						.withDocumentIdentification(new DocumentIdentification()
										.withStandard(STANDARD)
										.withTypeVersion(TYPE_VERSION)
										.withInstanceIdentifier(UUID.randomUUID().toString())
										.withType(TYPE)
										.withCreationDateAndTime(DateTime.now())
						)
						.withBusinessScope(new BusinessScope()
										.withScopes(new Scope()
														.withIdentifier(STANDARD)
														.withType(CONVERSATIONID)
														.withInstanceIdentifier(conversationId)
										)
						)
		)
		.withAny(payload);
    	return doc;
	}
	
}
