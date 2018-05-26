package ch.systemsx.sybit.crkwebui.server.commons;

import org.glassfish.jersey.message.filtering.SelectableEntityFilteringFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class InitJerseyRestApp extends ResourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(InitJerseyRestApp.class);

    public InitJerseyRestApp() {

        logger.info("Initialising Jersey REST application");
        packages(true, "ch.systemsx.sybit.crkwebui.server.files.downloader.rest");

        register(SelectableEntityFilteringFeature.class);
        property(SelectableEntityFilteringFeature.QUERY_PARAM_NAME, "select");
    }
}