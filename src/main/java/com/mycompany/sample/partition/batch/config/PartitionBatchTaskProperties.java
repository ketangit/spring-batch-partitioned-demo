package com.mycompany.sample.partition.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("partition")
public class PartitionBatchTaskProperties {

    /**
     * ChunkSize, Number of records to be processed in chunk or unit-of-work
     */
    private int chunkSize = 1000;

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
}
