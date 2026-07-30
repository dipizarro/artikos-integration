package cl.atk.nomina.batch.batch.processor;

import cl.atk.nomina.batch.config.ArtikosOutboundProperties;
import cl.atk.nomina.batch.config.ArtikosSourceProperties;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperation;
import cl.atk.nomina.batch.domain.artikos.ArtikosFetchedNomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosGenericResponse;
import cl.atk.nomina.batch.domain.error.IntegrationErrorType;
import cl.atk.nomina.batch.service.ControlNominaService;
import cl.atk.nomina.batch.service.ControlNominaCompanyResolver;
import cl.atk.nomina.batch.service.NominaErrorPolicyService;
import cl.atk.nomina.batch.service.NominaProcessingService;
import cl.atk.nomina.batch.service.NominaReprocessingPolicyService;
import cl.atk.nomina.batch.service.artikos.ArtikosGenericSoapResponseParser;
import cl.atk.nomina.batch.service.artikos.ArtikosSoapClient;
import cl.atk.nomina.batch.shared.exception.ArtikosIntegrationException;
import cl.atk.nomina.batch.shared.logging.LoggingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

public class ArtikosNominaItemProcessor implements ItemProcessor<ArtikosFetchedNomina, ResultadoNomina> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtikosNominaItemProcessor.class);

    private final ControlNominaService controlNominaService;
    private final ControlNominaCompanyResolver companyResolver;
    private final ArtikosSoapClient soapClient;
    private final ArtikosGenericSoapResponseParser genericResponseParser;
    private final NominaProcessingService nominaProcessingService;
    private final NominaErrorPolicyService errorPolicyService;
    private final NominaReprocessingPolicyService reprocessingPolicyService;
    private final ArtikosSourceProperties sourceProperties;
    private final ArtikosOutboundProperties outboundProperties;
    private final Long jobExecutionId;
    private final boolean dryRun;

    public ArtikosNominaItemProcessor(
            ControlNominaService controlNominaService,
            ControlNominaCompanyResolver companyResolver,
            ArtikosSoapClient soapClient,
            ArtikosGenericSoapResponseParser genericResponseParser,
            NominaProcessingService nominaProcessingService,
            NominaErrorPolicyService errorPolicyService,
            NominaReprocessingPolicyService reprocessingPolicyService,
            ArtikosSourceProperties sourceProperties,
            ArtikosOutboundProperties outboundProperties,
            Long jobExecutionId,
            String dryRun) {
        this.controlNominaService = controlNominaService;
        this.companyResolver = companyResolver;
        this.soapClient = soapClient;
        this.genericResponseParser = genericResponseParser;
        this.nominaProcessingService = nominaProcessingService;
        this.errorPolicyService = errorPolicyService;
        this.reprocessingPolicyService = reprocessingPolicyService;
        this.sourceProperties = sourceProperties;
        this.outboundProperties = outboundProperties;
        this.jobExecutionId = jobExecutionId;
        this.dryRun = Boolean.parseBoolean(dryRun);
    }

    @Override
    public ResultadoNomina process(ArtikosFetchedNomina item) {
        Long numeroNomina = item.numeroNomina();
        LoggingContext.putJobExecutionId(jobExecutionId);
        LoggingContext.putProfile(item.profile().name());
        LoggingContext.putNumeroNomina(numeroNomina);
        if (dryRun) {
            try {
                LOGGER.info("Dry-run processing Artikos nomina jobExecutionId={} profile={} numeroNomina={}",
                        jobExecutionId, item.profile(), numeroNomina);
                return processNomina(item, true);
            } finally {
                LoggingContext.clearAll();
            }
        }

        try {
            if (reprocessingPolicyService.shouldSkipAlreadyOk(item.profile(), item.nomina())) {
                LOGGER.info("Nomina already processed OK, skipping Procurement reprocessing jobExecutionId={} "
                                + "profile={} numeroNomina={}",
                        jobExecutionId, item.profile(), numeroNomina);
                return processAlreadyOkNomina(item);
            }

            LOGGER.info("[CONTROL_NOMINA] PROCESSING jobExecutionId={} numeroNomina={} profile={}",
                    jobExecutionId, numeroNomina, item.profile());
            markProcessing(item);

            confirmNominaIfEnabled(item, numeroNomina);

            return processNomina(item, false);
        } catch (ArtikosIntegrationException exception) {
            if (exception.getErrorType() == IntegrationErrorType.PROCUREMENT_TECHNICAL_ERROR
                    || exception.getErrorType() == IntegrationErrorType.PROCUREMENT_MAPPING_ERROR) {
                markControlErrorIfRequired(exception);
            }
            throw exception;
        } catch (RuntimeException exception) {
            ArtikosIntegrationException integrationException = new ArtikosIntegrationException(
                    IntegrationErrorType.NOMINA_PROCESSING_ERROR,
                    item.profile().name(),
                    numeroNomina,
                    currentOperation(),
                    exception.getMessage(),
                    exception);
            markControlErrorIfRequired(integrationException);
            throw integrationException;
        } finally {
            LoggingContext.clearAll();
        }
    }

    private void markProcessing(ArtikosFetchedNomina item) {
        try {
            controlNominaService.markProcessing(
                    jobExecutionId,
                    item.numeroNomina(),
                    companyResolver.resolveCodEmpres(item.profile(), item.nomina()));
        } catch (RuntimeException exception) {
            throw new ArtikosIntegrationException(
                    IntegrationErrorType.ORACLE_CONTROL_ERROR,
                    item.profile().name(),
                    item.numeroNomina(),
                    "CONTROL_NOMINA",
                    exception.getMessage(),
                    exception);
        }
    }

    private void markControlErrorIfRequired(ArtikosIntegrationException exception) {
        if (errorPolicyService.shouldMarkControlNominaError(exception.getErrorType(), exception.getNumeroNomina())) {
            try {
                controlNominaService.markError(
                        jobExecutionId,
                        exception.getNumeroNomina(),
                        errorPolicyService.buildControlErrorMessage(exception),
                        companyResolver.resolveCodEmpres(exception.getProfile()));
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

    private String currentOperation() {
        return LoggingContext.snapshot().get("operation");
    }

    private void confirmNominaIfEnabled(ArtikosFetchedNomina item, Long numeroNomina) {
        if (sourceProperties.isLocalXmlMode() || !outboundProperties.isConfirmEnabled()) {
            LOGGER.warn("Skipping Artikos NOMFACTCONFIR because local XML mode or confirm disabled is active "
                            + "profile={} numeroNomina={} sourceMode={} confirmEnabled={}",
                    item.profile(), numeroNomina, sourceProperties.getMode(), outboundProperties.isConfirmEnabled());
            return;
        }

        LoggingContext.putOperation(ArtikosOperation.NOMFACTCONFIR.name());
        try {
            LOGGER.info("Sending Artikos confirmation profile={} numeroNomina={}", item.profile(), numeroNomina);
            String confirmationRawXml = soapClient.confirmNominaRawXml(item.profile(), numeroNomina, 0);
            ArtikosGenericResponse confirmationResponse = genericResponseParser.parseGenericResponse(confirmationRawXml);
            if (!confirmationResponse.success()) {
                String message = "Confirmacion Artikos rechazada: " + confirmationResponse.messageText();
                LOGGER.warn("Artikos confirmation error profile={} numeroNomina={} msgStatus={} message={}",
                        item.profile(), numeroNomina, confirmationResponse.msgStatus(), confirmationResponse.messageText());
                ArtikosIntegrationException exception = new ArtikosIntegrationException(
                        IntegrationErrorType.NOMINA_CONFIRM_ERROR,
                        item.profile().name(),
                        numeroNomina,
                        ArtikosOperation.NOMFACTCONFIR.name(),
                        message,
                        null);
                markControlErrorIfRequired(exception);
                throw exception;
            }
            LOGGER.info("Artikos confirmation OK profile={} numeroNomina={}", item.profile(), numeroNomina);
        } finally {
            LoggingContext.clearOperation();
        }
    }

    private ResultadoNomina processNomina(ArtikosFetchedNomina item, boolean forceSimulatedProcessing) {
        LoggingContext.clearOperation();
        LOGGER.info("Processing nomina documents profile={} numeroNomina={}", item.profile(), item.numeroNomina());
        ResultadoNomina result = forceSimulatedProcessing
                ? nominaProcessingService.processSimulated(
                        jobExecutionId,
                        item.numeroNomina(),
                        item.profile(),
                        item.nomina(),
                        soapClient.resultadoNominaConfig(item.profile()))
                : nominaProcessingService.process(
                        jobExecutionId,
                        item.numeroNomina(),
                        item.profile(),
                        item.nomina(),
                        soapClient.resultadoNominaConfig(item.profile()));
        LOGGER.info("Nomina documents processed jobExecutionId={} profile={} numeroNomina={} totalDocuments={} "
                        + "totalOk={} totalNok={}",
                jobExecutionId,
                item.profile(),
                item.numeroNomina(),
                result.totalDocuments(),
                result.totalOk(),
                result.totalNok());
        return result;
    }

    private ResultadoNomina processAlreadyOkNomina(ArtikosFetchedNomina item) {
        LoggingContext.clearOperation();
        ResultadoNomina result = nominaProcessingService.processAlreadyOk(
                jobExecutionId,
                item.numeroNomina(),
                item.profile(),
                item.nomina(),
                soapClient.resultadoNominaConfig(item.profile()));
        LOGGER.info("Nomina already OK result generated jobExecutionId={} profile={} numeroNomina={} totalDocuments={} "
                        + "totalOk={} totalNok={}",
                jobExecutionId,
                item.profile(),
                item.numeroNomina(),
                result.totalDocuments(),
                result.totalOk(),
                result.totalNok());
        return result;
    }
}
