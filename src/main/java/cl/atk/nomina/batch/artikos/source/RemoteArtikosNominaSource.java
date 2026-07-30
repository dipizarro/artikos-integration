package cl.atk.nomina.batch.artikos.source;

import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.service.artikos.ArtikosSoapClient;
import cl.atk.nomina.batch.service.artikos.ArtikosSoapResponseParser;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "artikos.source.mode", havingValue = "remote", matchIfMissing = true)
public class RemoteArtikosNominaSource implements ArtikosNominaSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteArtikosNominaSource.class);

    private final ArtikosSoapClient soapClient;
    private final ArtikosSoapResponseParser responseParser;

    public RemoteArtikosNominaSource(
            ArtikosSoapClient soapClient,
            ArtikosSoapResponseParser responseParser) {
        this.soapClient = soapClient;
        this.responseParser = responseParser;
    }

    @Override
    public Optional<Nomina> fetchNextNomina(ArtikosProfileType profile) {
        String rawXml = soapClient.fetchNominaRawXml(profile);
        if (responseParser.isNoNominasResponse(rawXml)) {
            LOGGER.info("Artikos returned no nominas profile={} message={}",
                    profile, responseParser.extractNoNominasMessage(rawXml));
            return Optional.empty();
        }
        return responseParser.extractNomina(rawXml);
    }
}
