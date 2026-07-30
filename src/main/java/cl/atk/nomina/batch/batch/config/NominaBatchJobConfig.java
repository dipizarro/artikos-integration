package cl.atk.nomina.batch.batch.config;

import cl.atk.nomina.batch.artikos.source.ArtikosNominaSource;
import cl.atk.nomina.batch.batch.processor.ArtikosNominaItemProcessor;
import cl.atk.nomina.batch.batch.processor.NominaItemProcessor;
import cl.atk.nomina.batch.batch.processor.NominaDocumentoItemProcessor;
import cl.atk.nomina.batch.batch.reader.ArtikosNominaItemReader;
import cl.atk.nomina.batch.batch.reader.NominaItemReader;
import cl.atk.nomina.batch.batch.reader.NominaDocumentoItemReader;
import cl.atk.nomina.batch.batch.writer.ArtikosNominaResultItemWriter;
import cl.atk.nomina.batch.batch.writer.NominaResultItemWriter;
import cl.atk.nomina.batch.config.ArtikosOutboundProperties;
import cl.atk.nomina.batch.config.ArtikosSourceProperties;
import cl.atk.nomina.batch.domain.ResultadoDocumento;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.domain.SimulatedDocumentoContable;
import cl.atk.nomina.batch.domain.SimulatedNomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosFetchedNomina;
import cl.atk.nomina.batch.service.BatchResultStore;
import cl.atk.nomina.batch.service.ControlNominaService;
import cl.atk.nomina.batch.service.ControlNominaCompanyResolver;
import cl.atk.nomina.batch.service.NominaErrorPolicyService;
import cl.atk.nomina.batch.service.NominaResultXmlService;
import cl.atk.nomina.batch.service.NominaXmlParserService;
import cl.atk.nomina.batch.service.NominaProcessingService;
import cl.atk.nomina.batch.service.NominaReprocessingPolicyService;
import cl.atk.nomina.batch.service.artikos.ArtikosGenericSoapResponseParser;
import cl.atk.nomina.batch.service.artikos.ArtikosSoapClient;
import cl.atk.nomina.batch.service.artikos.ArtikosSoapResponseParser;
import cl.atk.nomina.batch.shared.logging.LoggingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class NominaBatchJobConfig {

    public static final String JOB_NAME = "nominaDocumentosContablesJob";
    public static final String PROCESS_STEP_NAME = "processNominaDocumentosStep";

    private static final Logger LOGGER = LoggerFactory.getLogger(NominaBatchJobConfig.class);
    private static final String LOG_START_NANO = "logStartNano";

    @Bean
    public Job nominaDocumentosContablesJob(
            JobRepository jobRepository,
            Step processNominaDocumentosStep,
            BatchResultStore batchResultStore) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(operationalJobListener(batchResultStore))
                .start(processNominaDocumentosStep)
                .build();
    }

    @Bean
    public Step processNominaDocumentosStep(
            JobRepository jobRepository,
            @Qualifier("appTransactionManager") PlatformTransactionManager transactionManager,
            ItemReader<ArtikosFetchedNomina> artikosNominaItemReader,
            ItemProcessor<ArtikosFetchedNomina, ResultadoNomina> artikosNominaItemProcessor,
            ItemWriter<ResultadoNomina> artikosNominaResultItemWriter,
            @Value("${atk.batch.real.chunk-size}") int chunkSize) {
        return new StepBuilder(PROCESS_STEP_NAME, jobRepository)
                .<ArtikosFetchedNomina, ResultadoNomina>chunk(chunkSize, transactionManager)
                .reader(artikosNominaItemReader)
                .processor(artikosNominaItemProcessor)
                .writer(artikosNominaResultItemWriter)
                .listener(operationalStepListener())
                .build();
    }

    @Bean
    @StepScope
    public ArtikosNominaItemReader artikosNominaItemReader(
            ArtikosNominaSource nominaSource,
            ArtikosSourceProperties sourceProperties,
            @Value("#{jobParameters['profile']}") String profile,
            @Value("#{jobParameters['maxNominas']}") Long maxNominas,
            @Value("#{jobParameters['dryRun']}") String dryRun) {
        return new ArtikosNominaItemReader(nominaSource, sourceProperties, profile, maxNominas, dryRun);
    }

    @Bean
    @StepScope
    public ArtikosNominaItemProcessor artikosNominaItemProcessor(
            ControlNominaService controlNominaService,
            ControlNominaCompanyResolver companyResolver,
            ArtikosSoapClient soapClient,
            ArtikosGenericSoapResponseParser genericResponseParser,
            NominaProcessingService nominaProcessingService,
            NominaErrorPolicyService errorPolicyService,
            NominaReprocessingPolicyService reprocessingPolicyService,
            ArtikosSourceProperties sourceProperties,
            ArtikosOutboundProperties outboundProperties,
            @Value("#{stepExecution.jobExecutionId}") Long jobExecutionId,
            @Value("#{jobParameters['dryRun']}") String dryRun) {
        return new ArtikosNominaItemProcessor(
                controlNominaService,
                companyResolver,
                soapClient,
                genericResponseParser,
                nominaProcessingService,
                errorPolicyService,
                reprocessingPolicyService,
                sourceProperties,
                outboundProperties,
                jobExecutionId,
                dryRun);
    }

    @Bean
    @StepScope
    public ArtikosNominaResultItemWriter artikosNominaResultItemWriter(
            ArtikosSoapClient soapClient,
            ArtikosGenericSoapResponseParser genericResponseParser,
            ControlNominaService controlNominaService,
            ControlNominaCompanyResolver companyResolver,
            NominaErrorPolicyService errorPolicyService,
            BatchResultStore batchResultStore,
            ArtikosSourceProperties sourceProperties,
            ArtikosOutboundProperties outboundProperties,
            @Value("#{jobParameters['profile']}") String profile,
            @Value("#{jobParameters['dryRun']}") String dryRun,
            @Value("#{stepExecution.jobExecutionId}") Long jobExecutionId) {
        return new ArtikosNominaResultItemWriter(
                soapClient,
                genericResponseParser,
                controlNominaService,
                companyResolver,
                errorPolicyService,
                batchResultStore,
                sourceProperties,
                outboundProperties,
                profile,
                dryRun,
                jobExecutionId);
    }

    @Bean
    @StepScope
    public NominaItemReader nominaItemReader(
            NominaXmlParserService parserService,
            @Value("${atk.batch.simulation-nominas}") int simulationNominas) {
        return new NominaItemReader(parserService, simulationNominas);
    }

    @Bean
    @StepScope
    public NominaItemProcessor nominaItemProcessor(
            NominaDocumentoItemProcessor nominaDocumentoItemProcessor,
            NominaResultXmlService nominaResultXmlService,
            ControlNominaService controlNominaService,
            @Value("#{stepExecution.jobExecutionId}") Long jobExecutionId) {
        return new NominaItemProcessor(
                nominaDocumentoItemProcessor,
                nominaResultXmlService,
                controlNominaService,
                jobExecutionId);
    }

    @Bean
    @StepScope
    public NominaResultItemWriter nominaResultItemWriter(
            BatchResultStore batchResultStore,
            ControlNominaService controlNominaService,
            @Value("#{stepExecution.jobExecutionId}") Long jobExecutionId) {
        return new NominaResultItemWriter(batchResultStore, controlNominaService, jobExecutionId);
    }

    @Bean
    @StepScope
    public NominaDocumentoItemReader nominaDocumentoItemReader(
            NominaXmlParserService parserService,
            @Value("${atk.batch.simulation-iterations}") int simulationIterations) {
        return new NominaDocumentoItemReader(parserService, simulationIterations);
    }

    @Bean
    public NominaDocumentoItemProcessor nominaDocumentoItemProcessor() {
        return new NominaDocumentoItemProcessor();
    }

    private JobExecutionListener operationalJobListener(BatchResultStore batchResultStore) {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                Long jobExecutionId = jobExecution.getId();
                String profile = jobExecution.getJobParameters().getString("profile");
                Long maxNominas = jobExecution.getJobParameters().getLong("maxNominas");
                String dryRun = jobExecution.getJobParameters().getString("dryRun");
                jobExecution.getExecutionContext().putLong(LOG_START_NANO, System.nanoTime());
                LoggingContext.putJobExecutionId(jobExecutionId);
                LoggingContext.putProfile(profile);
                LOGGER.info("Job started jobExecutionId={} jobName={} profile={} maxNominas={} dryRun={}",
                        jobExecutionId, jobExecution.getJobInstance().getJobName(), profile, maxNominas, dryRun);
                LOGGER.info("Clearing in-memory batch results jobExecutionId={}", jobExecutionId);
                batchResultStore.clearResults(jobExecution.getId());
                batchResultStore.putMetadata(jobExecution.getId(), profile, Boolean.parseBoolean(dryRun));
                LoggingContext.clearAll();
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                Long jobExecutionId = jobExecution.getId();
                String profile = jobExecution.getJobParameters().getString("profile");
                Long maxNominas = jobExecution.getJobParameters().getLong("maxNominas");
                String dryRun = jobExecution.getJobParameters().getString("dryRun");
                long startedAt = jobExecution.getExecutionContext().containsKey(LOG_START_NANO)
                        ? jobExecution.getExecutionContext().getLong(LOG_START_NANO)
                        : System.nanoTime();
                LoggingContext.putJobExecutionId(jobExecutionId);
                LoggingContext.putProfile(profile);
                LOGGER.info("Job finished jobExecutionId={} jobName={} profile={} maxNominas={} dryRun={} "
                                + "status={} exitCode={} elapsedMs={} exitDescription={}",
                        jobExecutionId,
                        jobExecution.getJobInstance().getJobName(),
                        profile,
                        maxNominas,
                        dryRun,
                        jobExecution.getStatus(),
                        jobExecution.getExitStatus().getExitCode(),
                        elapsedMs(startedAt),
                        compact(jobExecution.getExitStatus().getExitDescription()));
                LoggingContext.clearAll();
            }
        };
    }

    private StepExecutionListener operationalStepListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                stepExecution.getExecutionContext().putLong(LOG_START_NANO, System.nanoTime());
                LoggingContext.putJobExecutionId(stepExecution.getJobExecutionId());
                LoggingContext.putProfile(stepExecution.getJobExecution().getJobParameters().getString("profile"));
                LOGGER.info("Step started jobExecutionId={} stepName={}",
                        stepExecution.getJobExecutionId(), stepExecution.getStepName());
                LoggingContext.clearAll();
            }

            @Override
            public org.springframework.batch.core.ExitStatus afterStep(StepExecution stepExecution) {
                long startedAt = stepExecution.getExecutionContext().containsKey(LOG_START_NANO)
                        ? stepExecution.getExecutionContext().getLong(LOG_START_NANO)
                        : System.nanoTime();
                LoggingContext.putJobExecutionId(stepExecution.getJobExecutionId());
                LoggingContext.putProfile(stepExecution.getJobExecution().getJobParameters().getString("profile"));
                LOGGER.info("Step finished jobExecutionId={} stepName={} status={} readCount={} writeCount={} "
                                + "processSkipCount={} readSkipCount={} writeSkipCount={} commitCount={} "
                                + "rollbackCount={} elapsedMs={}",
                        stepExecution.getJobExecutionId(),
                        stepExecution.getStepName(),
                        stepExecution.getStatus(),
                        stepExecution.getReadCount(),
                        stepExecution.getWriteCount(),
                        stepExecution.getProcessSkipCount(),
                        stepExecution.getReadSkipCount(),
                        stepExecution.getWriteSkipCount(),
                        stepExecution.getCommitCount(),
                        stepExecution.getRollbackCount(),
                        elapsedMs(startedAt));
                LoggingContext.clearAll();
                return stepExecution.getExitStatus();
            }
        };
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String compact(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String compactValue = value.replaceAll("\\s+", " ").trim();
        return compactValue.length() <= 500 ? compactValue : compactValue.substring(0, 500) + "...";
    }
}
