//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.11.25 at 12:23:12 PM CET 
//


package no.difi.meldingsutveksling.domain.sbdh;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.Payload;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.validation.InstanceOf;
import no.difi.meldingsutveksling.validation.group.ValidationGroups.ServiceIdentifier.*;
import no.difi.meldingsutveksling.validation.group.sequenceprovider.StandardBusinessDocumentGroupSequenceProvider;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.hibernate.validator.group.GroupSequenceProvider;
import org.w3c.dom.Node;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.util.Optional;


/**
 * <p>Java class for StandardBusinessDocument complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="StandardBusinessDocument">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader}StandardBusinessDocumentHeader" minOccurs="0"/>
 *         &lt;any processContents='lax' namespace='##other'/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StandardBusinessDocument", propOrder = {
        "standardBusinessDocumentHeader",
        "any"
})
@Getter
@Setter
@ToString
@Entity
@Table(name = "document")
@JsonSerialize(using = NextMoveMessageSerializer.class)
@GroupSequenceProvider(StandardBusinessDocumentGroupSequenceProvider.class)
@ApiModel(description = "Standard Business Document")
public class StandardBusinessDocument extends AbstractEntity<Long> {

    @XmlElement(name = "StandardBusinessDocumentHeader")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @NotNull
    @Valid
    private StandardBusinessDocumentHeader standardBusinessDocumentHeader;

    @XmlAnyElement(lax = true)
    @JsonDeserialize(using = NextMoveMessageDeserializer.class)
    @JsonAlias({"dpo", "dpv", "dpe", "dpi_digital", "dpi_print"})
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = BusinessMessage.class)
    @NotNull
    @InstanceOf(value = DpoMessage.class, groups = {Dpo.class, Dpf.class})
    @InstanceOf(value = DpvMessage.class, groups = Dpv.class)
    @InstanceOf(value = DpiDigitalMessage.class, groups = DpiDigital.class)
    @InstanceOf(value = DpiPrintMessage.class, groups = DpiPrint.class)
    @InstanceOf(value = DpeMessage.class, groups = {DpeInnsyn.class, DpeData.class, DpeReceipt.class})
    @Getter(onMethod_ =
    @ApiModelProperty(name = "dpo|dpv|dpe|dpi_digital|dpi_print", value = "The payload of the document", required = true, dataType = "no.difi.meldingsutveksling.nextmove.BusinessMessage")
    )
    private Object any;

    @JsonIgnore
    public MessageInfo getMessageInfo() {
        return new MessageInfo(getReceiverIdentifier(), getSenderIdentifier(), getJournalPostId(), getConversationId(), getMessageType());
    }

    @JsonIgnore
    public String getSenderIdentifier() {
        return getStandardBusinessDocumentHeader().getFirstSender()
                .map(Partner::getIdentifier)
                .map(PartnerIdentification::getStrippedValue)
                .orElse(null);
    }

    @JsonIgnore
    public String getReceiverIdentifier() {
        return getStandardBusinessDocumentHeader().getFirstReceiver()
                .map(Partner::getIdentifier)
                .map(PartnerIdentification::getStrippedValue)
                .orElse(null);
    }

    @JsonIgnore
    public final String getJournalPostId() {
        return findScope(ScopeType.JOURNALPOST_ID)
                .map(Scope::getInstanceIdentifier)
                .orElse("");
    }

    @JsonIgnore
    public String getConversationId() {
        return getOptionalConversationId()
                .orElseThrow(MeldingsUtvekslingRuntimeException::new);
    }

    @JsonIgnore
    public Optional<String> getOptionalConversationId() {
        return findScope(ScopeType.CONVERSATION_ID)
                .map(Scope::getInstanceIdentifier);
    }

    @JsonIgnore
    public Optional<Scope> getConversationScope() {
        return findScope(ScopeType.CONVERSATION_ID);
    }

    private Optional<Scope> findScope(ScopeType scopeType) {
        return getStandardBusinessDocumentHeader().getBusinessScope()
                .getScope()
                .stream()
                .filter(scope -> scopeType.toString().equals(scope.getType()) || scopeType.name().equals(scope.getType()))
                .findAny();
    }

    @JsonIgnore
    public ServiceIdentifier getServiceIdentifier() {
        String diType = getStandardBusinessDocumentHeader().getDocumentIdentification().getType();
        return ServiceIdentifier.safeValueOf(diType).orElseThrow(() ->
                new NextMoveRuntimeException(String.format("Could not create ServiceIdentifier from documentIdentification.type=%s for message with id=%s", diType, getConversationId())));
    }

    @JsonIgnore
    public String getMessageType() {
        return getStandardBusinessDocumentHeader().getDocumentIdentification().getType();
    }

    @JsonIgnore
    public String getDocumentId() {
        return getStandardBusinessDocumentHeader().getDocumentIdentification().getInstanceIdentifier();
    }

    @JsonIgnore
    public Payload getPayload() {
        if (getAny() instanceof Payload) {
            return (Payload) getAny();
        } else if (getAny() instanceof Node) {
            return unmarshallAnyElement(getAny());
        } else {
            throw new MeldingsUtvekslingRuntimeException("Could not cast any element " + getAny() + " from " + StandardBusinessDocument.class + " to " + Payload.class);
        }
    }

    private Payload unmarshallAnyElement(Object any) {
        JAXBContext jaxbContextP;
        Unmarshaller unMarshallerP;
        Payload payload;
        try {
            jaxbContextP = JAXBContextFactory.createContext(new Class[]{Payload.class}, null);
            unMarshallerP = jaxbContextP.createUnmarshaller();
            payload = unMarshallerP.unmarshal((org.w3c.dom.Node) any, Payload.class).getValue();
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return payload;
    }

    public LogstashMarker createLogstashMarkers() {
        return getMessageInfo().createLogstashMarkers();
    }
}
