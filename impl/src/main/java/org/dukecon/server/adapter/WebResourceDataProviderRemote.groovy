package org.dukecon.server.adapter

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Slf4j
import org.dukecon.model.Conference
import org.dukecon.server.conference.ConferencesConfiguration

import java.nio.charset.StandardCharsets

/**
 * @author Falk Sippach, falk@jug-da.de, @sippsack
 */
@Slf4j
@TypeChecked
class WebResourceDataProviderRemote {

    // JsonOutput and JsonSlurper will encode/parse UTF characters as \\u anyway,
    // but to be sure choosing UTF-8 here.
    private final static String BACKUP_CHARSET = StandardCharsets.UTF_8.toString();

    private final RawDataResourceSupplier rawDataResourceSupplier
    private final ConferencesConfiguration.Conference config

    volatile boolean backupActive = false
    Exception staleException
    volatile Conference conference

    WebResourceDataProviderRemote(RawDataResourceSupplier rawDataResourceSupplier, ConferencesConfiguration.Conference config) {
        this.rawDataResourceSupplier = rawDataResourceSupplier
        this.config = config
    }

    private Conference createConference(rawData) {
        ConferenceDataExtractor dataExtractor = this.config.extractorClass.newInstance(this.config.id, rawData, config.startDate, config.name, config.url) as ConferenceDataExtractor
        dataExtractor.conference
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    @HystrixCommand(groupKey = "doag", commandKey = "readConferenceData", fallbackMethod = "readConferenceDataFallback", commandProperties = @HystrixProperty(name = "hystrix.command.default.execution.timeout.enabled", value = "false"))
    public Conference readConferenceData() {
        try {
            log.info("Rereading data from '{}'", config.talksUri)
            def rawData = rawDataResourceSupplier.get()
            Conference conference = createConference(rawData)
            try {
                File backupFile = new File("backup/${this.config.backupUri}");
                backupFile.write(rawDataResourceSupplier.get().text, BACKUP_CHARSET);
            } catch (IOException e) {
                log.warn("unable to write backup file '{}'", "backup/${this.config.backupUri}", e);
            }
            backupActive = false;
            staleException = null;
            return conference;
        } catch (RuntimeException e) {
            log.error("unable to read data", e);
            staleException = e;
            throw e;
        }
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    public Conference readConferenceDataFallback() {
        try {
            log.info("Rereading JSON data from backup '{}'", "backup/${this.config.backupUri}")
            Conference conference = createConference(this.class.getResourceAsStream("/backup/${this.config.backupUri}"))
            backupActive = true;
            return conference;
        } catch (RuntimeException e) {
            log.error("unable to read backup", e);
            throw e;
        }
    }
}
