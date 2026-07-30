package cl.atk.nomina.batch.batch.writer;

import cl.atk.nomina.batch.config.ArtikosOutboundProperties;
import cl.atk.nomina.batch.config.ArtikosSourceProperties;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperation;
import cl.atk.nomina.batch.domain.artikos.ArtikosGenericResponse;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.domain.error.IntegrationErrorType;
import cl.atk.nomina.batch.service.BatchResultStore;
import cl.atk.nomina.batch.service.ControlNominaCompanyResolver;
import cl.atk.nomina.batch.service.ControlNominaService;
import cl.atk.nomina.batch.service.NominaErrorPolicyService;
import cl.atk.nomina.batch.service.artikos.ArtikosGenericSoapResponseParser;
import cl.atk.nomina.batch.service.artikos.ArtikosSoapClient;
import cl.atk.nomina.batch.shared.exception.ArtikosIntegrationException;
import cl.atk.nomina.batch.shared.logging.LoggingContext;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

public class ArtikosNominaResultItemWriter implements ItemWriter<ResultadoNomina> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtikosNominaResultItemWriter.class);

    private final ArtikosSoapClient soapClient;
    private final ArtikosGenericSoapResponseParser genericResponseParser;
    private final ControlNominaService controlNominaService;
    private final ControlNominaCompanyResolver companyResolver;
    private final NominaErrorPolicyService errorPolicyService;
    private final BatchResultStore batchResultStore;
    private final ArtikosSourceProperties sourceProperties;
    private final ArtikosOutboundProperties outboundProperties;
    private final ArtikosProfileType profile;
    private final boolean dryRun;
    private final Long jobExecutionId;

    public ArtikosNominaResultItemWriter(
            ArtikosSoapClient soapClient,
            ArtikosGenericSoapResponseParser genericResponseParser,
            ControlNominaService controlNominaService,
            ControlNominaCompanyResolver companyResolver,
            NominaErrorPolicyService errorPolicyService,
            BatchResultStore batchResultStore,
            ArtikosSourceProperties sourceProperties,
            ArtikosOutboundProperties outboundProperties,
            String profile,
            String dryRun,
            Long jobExecutionId) {
        this.soapClient = soapClient;
        this.genericResponseParser = genericResponseParser;
        this.controlNominaService = controlNominaService;
        this.companyResolver = companyResolver;
        this.errorPolicyService = errorPolicyService;
        this.batchResultStore = batchResultStore;
        this.sourceProperties = sourceProperties;
        this.outboundProperties = outboundProperties;
        this.profile = ArtikosProfileType.from(profile);
        this.dryRun = Boolean.parseBoolean(dryRun);
        this.jobExecutionId = jobExecutionId;
    }

    @Override
    public void write(Chunk<? extends ResultadoNomina> chunk) {
        LoggingContext.putJobExecutionId(jobExecutionId);
        LoggingContext.putProfile(profile.name());
        LOGGER.info("Starting Artikos result writer chunk jobExecutionId={} profile={} size={} dryRun={}",
                jobExecutionId, profile, chunk.size(), dryRun);
        List<ResultadoNomina> completedResults = new ArrayList<>();
        try {
            for (ResultadoNomina result : chunk.getItems()) {
                ResultadoNomina resultWithJob = result.withJobExecutionId(jobExecutionId);
                LoggingContext.putNumeroNomina(resultWithJob.numeroNomina());
                if (dryRun) {
                    LOGGER.info("Dry-run storing local result jobExecutionId={} profile={} numeroNomina={} status={}",
                            jobExecutionId, profile, resultWithJob.numeroNomina(), resultWithJob.status());
                    completedResults.add(resultWithJob);
                    continue;
                }

                sendResultToArtikos(resultWithJob);
                completedResults.add(resultWithJob);
            }

            batchResultStore.addNominaResults(jobExecutionId, completedResults);
            LOGGER.info("BatchResultStore updated jobExecutionId={} profile={} results={}",
                    jobExecutionId, profile, completedResults.size());
        } finally {
            LoggingContext.clearAll();
        }
    }

    private void sendResultToArtikos(ResultadoNomina result) {
        if (sourceProperties.isLocalXmlMode() || !outboundProperties.isResultEnabled()) {
            LOGGER.warn("Skipping Artikos NOMFACTRES send because local XML mode or result disabled is active "
                            + "profile={} jobExecutionId={} numeroNomina={} sourceMode={} resultEnabled={}",
                    profile,
                    jobExecutionId,
                    result.numeroNomina(),
                    sourceProperties.getMode(),
                    outboundProperties.isResultEnabled());
            LOGGER.debug("Generated NOMFACTRES profile={} jobExecutionId={} numeroNomina={} xml={}",
                    profile, jobExecutionId, result.numeroNomina(), result.nomfactresXml());
            controlNominaService.markCompleted(result);
            LOGGER.info("CONTROL_NOMINA updated without Artikos result send jobExecutionId={} numeroNomina={} status={}",
                    jobExecutionId, result.numeroNomina(), result.status());
            return;
        }

        LoggingContext.putOperation(ArtikosOperation.NOMFACTRES.name());
        try {
            LOGGER.info("Sending NOMFACTRES to Artikos profile={} jobExecutionId={} numeroNomina={}",
                    profile, jobExecutionId, result.numeroNomina());
            String resultRawXml = soapClient.sendNominaResultRawXml(profile, result);
            ArtikosGenericResponse response = genericResponseParser.parseGenericResponse(resultRawXml);
            if (!response.success()) {
                String message = "NOMFACTRES Artikos rechazado: " + response.messageText();
                LOGGER.warn("Artikos NOMFACTRES error profile={} jobExecutionId={} numeroNomina={} msgStatus={} "
                                + "message={}",
                        profile, jobExecutionId, result.numeroNomina(), response.msgStatus(), response.messageText());
                ArtikosIntegrationException exception = new ArtikosIntegrationException(
                        IntegrationErrorType.NOMINA_RESULT_ERROR,
                        profile.name(),
                        result.numeroNomina(),
                        ArtikosOperation.NOMFACTRES.name(),
                        message,
                        null);
                markControlErrorIfRequired(exception, result);
                throw exception;
            }

            LOGGER.info("Artikos NOMFACTRES OK profile={} jobExecutionId={} numeroNomina={}",
                    profile, jobExecutionId, result.numeroNomina());
            controlNominaService.markCompleted(result);
            LOGGER.info("CONTROL_NOMINA updated from writer jobExecutionId={} numeroNomina={} status={}",
                    jobExecutionId, result.numeroNomina(), result.status());
        } catch (ArtikosIntegrationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            ArtikosIntegrationException integrationException = new ArtikosIntegrationException(
                    IntegrationErrorType.NOMINA_RESULT_ERROR,
                    profile.name(),
                    result.numeroNomina(),
                    ArtikosOperation.NOMFACTRES.name(),
                    exception.getMessage(),
                    exception);
            markControlErrorIfRequired(integrationException, result);
            throw integrationException;
        } finally {
            LoggingContext.clearOperation();
        }
    }

    private void markControlErrorIfRequired(ArtikosIntegrationException exception, ResultadoNomina result) {
        if (errorPolicyService.shouldMarkControlNominaError(exception.getErrorType(), exception.getNumeroNomina())) {
            try {
                controlNominaService.markError(
                        jobExecutionId,
                        exception.getNumeroNomina(),
                        errorPolicyService.buildControlErrorMessage(exception),
                        resolveControlCodEmpres(result));
            } catch (RuntimeException controlException) {
                throw new ArtikosIntegrationException(
                        IntegrationErrorType.ORACLE_CONTROL_ERROR,
                        exception.getProfile(),
                        exception.getNumeroNomina(),
                        "CONTROL_NOMINA",
                        controlException.getMessage(),
                        controlException);
            }
        }
    }

    private String resolveControlCodEmpres(ResultadoNomina result) {
        return result.codEmpres() == null || result.codEmpres().isBlank()
                ? companyResolver.resolveCodEmpres(profile)
                : result.codEmpres();
    }
}
