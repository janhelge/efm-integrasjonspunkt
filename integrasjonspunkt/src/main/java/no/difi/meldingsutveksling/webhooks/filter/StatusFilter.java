package no.difi.meldingsutveksling.webhooks.filter;

import no.difi.meldingsutveksling.webhooks.event.MessageStatusContent;
import no.difi.meldingsutveksling.webhooks.event.WebhookContent;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class StatusFilter implements EventFilter {

    @Override
    public String getName() {
        return "status";
    }

    @Override
    public boolean supports(WebhookContent content) {
        return content instanceof MessageStatusContent;
    }

    @Override
    public boolean supports(EventFilterOperator operator) {
        return operator == EventFilterOperator.EQUALS;
    }

    @Override
    public boolean matches(WebhookContent content, EventFilterOperator operator, String value) {
        MessageStatusContent messageStatusContent = (MessageStatusContent) content;
        String expectedStatus = messageStatusContent.getStatus();
        return Arrays.stream(value.split(","))
                .anyMatch(expectedStatus::equalsIgnoreCase);
    }
}
