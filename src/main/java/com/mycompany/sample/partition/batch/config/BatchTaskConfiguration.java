package com.mycompany.sample.partition.batch.config;

import com.mycompany.sample.partition.batch.domain.Transaction;
import com.mycompany.sample.partition.batch.util.AppResourceUtil;
import com.mycompany.sample.partition.batch.util.BatchProfile;
import com.mycompany.sample.partition.batch.util.SampleResourcePartitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.CompositeJobExecutionListener;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.batch.partition.CommandLineArgsProvider;
import org.springframework.cloud.task.batch.partition.DeployerPartitionHandler;
import org.springframework.cloud.task.batch.partition.DeployerStepExecutionHandler;
import org.springframework.cloud.task.batch.partition.SimpleEnvironmentVariablesProvider;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@EnableTask
@EnableBatchProcessing
@Configuration
@EnableConfigurationProperties(PartitionBatchTaskProperties.class)
public class BatchTaskConfiguration {
    private final Logger logger = LoggerFactory.getLogger(BatchTaskConfiguration.class);
    private final PartitionBatchTaskProperties properties;

    public BatchTaskConfiguration(final PartitionBatchTaskProperties properties) {
        this.properties = properties;
    }

    // ================================================================
    // Worker spring-beans definitions
    // ================================================================
    @Bean
    @Profile(BatchProfile.WORKER)
    public DeployerStepExecutionHandler stepExecutionHandler(ApplicationContext applicationContext, JobRepository jobRepository, JobExplorer jobExplorer) {
        return new DeployerStepExecutionHandler(applicationContext, jobExplorer, jobRepository);
    }

    @Bean
    @StepScope
    public FlatFileItemReader<Transaction> itemReader(@Value("#{stepExecutionContext[fileName]}") String filename) {
        logger.info("input file - {}", filename);
        return new FlatFileItemReaderBuilder<Transaction>() //
                .name("flatFileTransactionReader") //
                .resource(new ClassPathResource("/data/" + filename)) //
                .delimited() //
                .names("account", "amount", "timestamp") //
                .fieldSetMapper(fieldSet -> { //
                    Transaction transaction = new Transaction();
                    transaction.setAccount(fieldSet.readString("account"));
                    transaction.setAmount(fieldSet.readBigDecimal("amount"));
                    transaction.setTimestamp(fieldSet.readDate("timestamp", "yyyy-MM-dd HH:mm:ss"));
                    return transaction;
                }) //
                .build();
    }

    @Bean(destroyMethod = "")
    @StepScope
    public StaxEventItemWriter<Transaction> itemWriter(Marshaller marshaller, @Value("#{stepExecutionContext[opFileName]}") String filename) {
        StaxEventItemWriter<Transaction> itemWriter = new StaxEventItemWriter<>();
        itemWriter.setMarshaller(marshaller);
        itemWriter.setRootTagName("transactionRecord");
        String outFileName = System.getProperty("java.io.tmpdir") + File.separator + filename;
        logger.info("output file - {}", outFileName);
        itemWriter.setResource(new FileSystemResource(outFileName));
        return itemWriter;
    }

    @Bean
    public Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(Transaction.class);
        return marshaller;
    }

    @Bean
    public Step worker(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("worker")
                .<Transaction, Transaction>chunk(properties.getChunkSize()) //
                .reader(itemReader(null)) //
                .writer(itemWriter(marshaller(), null)) //
                .build();
    }

    // ================================================================
    // Master or non-worker spring-beans definitions
    // ================================================================
    @Value("classpath*:data/*.csv")
    private Resource[] resources;

    @Bean
    @Profile(BatchProfile.MASTER)
    public PartitionHandler partitionHandler(TaskLauncher taskLauncher, JobExplorer jobExplorer, Step worker, //
                                             TaskRepository taskRepository, //
                                             Environment environment, //
                                             @Autowired(required = false) GitProperties gitProperties) {
        final String appName = environment.getProperty("spring.application.name");
        final Resource resource = AppResourceUtil.getResource(environment, gitProperties);

        final DeployerPartitionHandler partitionHandler = new DeployerPartitionHandler(taskLauncher, jobExplorer, resource, worker.getName(), taskRepository);
        partitionHandler.setApplicationName(appName);

        // set command line arguments
        final List<String> commandLineArgs = new ArrayList<>(1);
        commandLineArgs.add("--spring.profiles.include=worker");
        final CommandLineArgsProvider commandLineArgsProvider = executionContext -> commandLineArgs;
        partitionHandler.setCommandLineArgsProvider(commandLineArgsProvider);

        // set environment variables
        final SimpleEnvironmentVariablesProvider simpleEnvironmentVariablesProvider = new SimpleEnvironmentVariablesProvider(environment);
        simpleEnvironmentVariablesProvider.setIncludeCurrentEnvironment(false);
        partitionHandler.setEnvironmentVariablesProvider(simpleEnvironmentVariablesProvider);

        return partitionHandler;
    }

    @Bean
    @Profile(BatchProfile.MASTER)
    public Partitioner partitioner() {
        return new SampleResourcePartitioner(resources);
    }

    @Bean
    @Profile(BatchProfile.MASTER)
    public Step partitionedMaster(StepBuilderFactory stepBuilderFactory, PartitionHandler partitionHandler, Partitioner partitioner, Step worker) {
        return stepBuilderFactory.get("step.master") //
                .partitioner(worker.getName(), partitioner) //
                .step(worker) //
                .partitionHandler(partitionHandler) //
                .build();
    }

    @Bean
    @Profile(BatchProfile.MASTER)
    public Job jobMaster(JobBuilderFactory jobBuilderFactory, Step partitionedMaster) {
        final CompositeJobExecutionListener compositeJobExecutionListener = new CompositeJobExecutionListener();
        List<JobExecutionListener> listeners = new ArrayList<>();
        compositeJobExecutionListener.setListeners(listeners);

        return jobBuilderFactory.get("CsvToXml") //
                .listener(compositeJobExecutionListener) //
                .incrementer(new RunIdIncrementer()) //
                .start(partitionedMaster) //
                .build();
    }
}
