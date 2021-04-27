//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.11.25 at 12:23:12 PM CET 
//


package no.difi.meldingsutveksling.domain.sbdh;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.validation.IntegrasjonspunktOrganization;
import no.difi.meldingsutveksling.validation.group.ValidationGroups;
import org.hibernate.annotations.Parent;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.*;
import java.io.Serializable;


/**
 * Java class for PartnerIdentification complex type.
 *
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>{@code
 * <complexType name="PartnerIdentification">
 *   <simpleContent>
 *     <extension base="<http://www.w3.org/2001/XMLSchema>string">
 *       <attribute name="Authority" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PartnerIdentification", propOrder = {
        "value"
})
@EqualsAndHashCode(exclude = "partner")
@Getter
@Setter
@ToString(exclude = "partner")
@RequiredArgsConstructor
public class PartnerIdentification implements Serializable {

    @XmlTransient
    @JsonIgnore
    @Parent
    private Partner partner;

    @XmlValue
    @IntegrasjonspunktOrganization(groups = ValidationGroups.Partner.Sender.class)
    @NotNull
    protected String value;

    @XmlAttribute(name = "Authority")
    @NotNull
    protected String authority;

    @JsonIgnore
    Organisasjonsnummer getAsOrganisasjonsnummer() {
        if (value == null) {
            return null;
        }

        return Organisasjonsnummer.isIso6523(value) ? Organisasjonsnummer.fromIso6523(value) : Organisasjonsnummer.from(value);
    }

    @JsonIgnore
    String getStrippedValue() {
        if (value == null) {
            return null;
        }

        return Organisasjonsnummer.isIso6523(value) ? Organisasjonsnummer.fromIso6523(value).getOrgNummer() : value;
    }

    @JsonIgnore
    String getPaaVegneAvValue() {
        if (value == null) {
            return null;
        }
        return Organisasjonsnummer.isIso6523(value) ? Organisasjonsnummer.fromIso6523(value).getPaaVegneAvOrgnr().orElse(null) : null;
    }

}
