package cl.atk.nomina.batch.batch.reader;

import cl.atk.nomina.batch.artikos.source.ArtikosNominaSource;
import cl.atk.nomina.batch.config.ArtikosSourceProperties;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperation;
import cl.atk.nomina.batch.domain.artikos.ArtikosFetchedNomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.domain.error.IntegrationErrorType;
import cl.atk.nomina.batch.service.artikos.ArtikosSoapClientException;
import cl.atk.nomina.batch.shared.exception.ArtikosIntegrationException;
import cl.atk.nomina.batch.shared.exception.NominaXmlParsingException;
import cl.atk.nomina.batch.shared.logging.LoggingContext;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;

public class ArtikosNominaItemReader implements ItemReader<ArtikosFetchedNomina> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtikosNominaItemReader.class);

    private final ArtikosNominaSource nominaSource;
    private final ArtikosSourceProperties sourceProperties;
    private final ArtikosProfileType profile;
    private final long maxNominas;
    private final boolean dryRun;
    private long fetchedCount;

    public ArtikosNominaItemReader(
            ArtikosNominaSource nominaSource,
            ArtikosSourceProperties sourceProperties,
            String profile,
            Long maxNominas,
            String dryRun) {
        this.nominaSource = nominaSource;
        this.sourceProperties = sourceProperties;
        this.profile = ArtikosProfileType.from(profile);
        this.maxNominas = maxNominas == null ? Long.MAX_VALUE : maxNominas;
        this.dryRun = Boolean.parseBoolean(dryRun);
    }

    @Override
    public ArtikosFetchedNomina read() {
        LoggingContext.putProfile(profile.name());
        LoggingContext.putOperation(ArtikosOperation.NOMFACTERP.name());
        if (fetchedCount >= maxNominas) {
            LOGGER.info("Artikos reader reached operational safety limit maxNominas={} profile={} dryRun={} fetchedCount={}",
                    maxNominas, profile, dryRun, fetchedCount);
            LoggingContext.clearOperation();
            LoggingContext.clearNomina();
            return null;
        }

        try {
            LOGGER.info("Fetching Artikos nomina profile={} sourceMode={} currentCount={} maxNominas={} dryRun={}",
                    profile, sourceProperties.getMode(), fetchedCount, maxNominas, dryRun);
            Optional<Nomina> parsedNomina = nominaSource.fetchNextNomina(profile);
            if (parsedNomina.isEmpty()) {
                LOGGER.info("Artikos source returned no nomina profile={} sourceMode={} fetchedCount={}",
                        profile, sourceProperties.getMode(), fetchedCount);
                return null;
            }

            Nomina nomina = parsedNomina.get();
            fetchedCount++;
            LoggingContext.putNumeroNomina(nomina.cabecera().numeroNomina());
            LOGGER.info("Artikos nomina received profile={} numeroNomina={} tipoNomina={} cantidadDocumentos={}",
                    profile,
                    nomina.cabecera().numeroNomina(),
                    nomina.cabecera().tipoNomina(),
                    nomina.cabecera().cantidadDocumentos());
            return new ArtikosFetchedNomina(
                    profile,
                    nomina,
                    nomina.cabecera().numeroNomina(),
                    nomina.cabecera().tipoNomina(),
                    nomina.cabecera().cantidadDocumentos(),
                    "",
                    dryRun);
        } catch (ArtikosSoapClientException exception) {
            throw new ArtikosIntegrationException(
                    IntegrationErrorType.ARTIKOS_FETCH_ERROR,
                    profile.name(),
                    null,
                    ArtikosOperation.NOMFACTERP.name(),
                    exception.getMessage(),
                    exception);
        } catch (NominaXmlParsingException exception) {
            throw new ArtikosIntegrationException(
                    IntegrationErrorType.XML_PARSING_ERROR,
                    profile.name(),
                    null,
                    ArtikosOperation.NOMFACTERP.name(),
                    exception.getMessage(),
                    exception);
        } catch (RuntimeException exception) {
            throw new ArtikosIntegrationException(
                    IntegrationErrorType.ARTIKOS_FETCH_ERROR,
                    profile.name(),
                    null,
                    ArtikosOperation.NOMFACTERP.name(),
                    "No fue posible consultar nomina en Artikos QA",
                    exception);
        } finally {
            LoggingContext.clearOperation();
            LoggingContext.clearNomina();
        }
    }
}
