package com.mycompany.sample.partition.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PartitionBatchApplication {
    private static Logger logger = LoggerFactory.getLogger(PartitionBatchApplication.class);

    public static void main(String[] args) {
        printJavaOpts();
        SpringApplication.run(PartitionBatchApplication.class, args);
    }

    protected static void printJavaOpts() {
        logger.info("JAVA_OPTS={}", System.getenv("JAVA_OPTS"));
    }
}
