package no.difi.meldingsutveksling;

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Getter
public enum MessageType {

    STATUS("status"),
    FEIL("feil"),

    ARKIVMELDING("arkivmelding"),
    ARKIVMELDING_KVITTERING("arkivmelding_kvittering"),

    AVTALT("avtalt"),

    FIKSIO("fiksio"),

    DIGITAL("digital"),
    DIGITAL_DPV("digital_dpv"),
    PRINT("print"),

    INNSYNSKRAV("innsynskrav"),
    PUBLISERING("publisering"),
    EINNSYN_KVITTERING("einnsyn_kvittering");

    private static final Set<MessageType> RECEIPTS = EnumSet.of(ARKIVMELDING_KVITTERING, EINNSYN_KVITTERING);

    private final String type;

    public boolean isReceipt() {
        return RECEIPTS.contains(this);
    }

    public static Optional<MessageType> valueOfType(String type) {
        if (type == null) {
            return Optional.empty();
        }
        return Arrays.stream(MessageType.values())
                .filter(p -> p.type.equalsIgnoreCase(type))
                .findAny();
    }

    public static Optional<MessageType> valueOfDocumentType(String documentType) {
        if (Strings.isNullOrEmpty(documentType)) {
            return Optional.empty();
        }
        return Arrays.stream(MessageType.values())
            .filter(d -> documentType.endsWith("::"+d.getType()))
            .findFirst();
    }

    public static Stream<MessageType> stream() {
        return Arrays.stream(MessageType.values());
    }
}
