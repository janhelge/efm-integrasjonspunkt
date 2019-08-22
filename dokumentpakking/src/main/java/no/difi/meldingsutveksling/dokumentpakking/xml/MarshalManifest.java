package no.difi.meldingsutveksling.dokumentpakking.xml;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.persistence.jaxb.JAXBContextFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.OutputStream;

@Slf4j
@UtilityClass
public final class MarshalManifest {

    public static void marshal(Manifest doc, OutputStream os) {
        try {
            JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{Manifest.class}, null);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(doc, os);
        } catch (JAXBException e) {
            log.error("Marshalling failed", e);
        }
    }
}
