package no.difi.meldingsutveksling.adresseregister;

import no.difi.meldingsutveksling.adresseregister.data.DataGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextStartedEvent;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Glenn Bech
 */
public class CustomApplicationListener implements org.springframework.context.ApplicationListener {

    private final Logger log = Logger.getLogger(CustomApplicationListener.class.getName());

    @Override
    public void onApplicationEvent(final ApplicationEvent event) {

    }
}


