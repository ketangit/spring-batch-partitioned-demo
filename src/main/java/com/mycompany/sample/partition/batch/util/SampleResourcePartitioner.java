package com.mycompany.sample.partition.batch.util;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

public class SampleResourcePartitioner implements Partitioner {
    private static final String DEFAULT_KEY_NAME = "fileName";
    private static final String PARTITION_KEY = "partition";
    private static final String KEY_NAME = DEFAULT_KEY_NAME;
    private final Resource[] resources;

    public SampleResourcePartitioner(Resource[] resources) {
        this.resources = resources;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> map = new HashMap<>(gridSize);
        int i = 0;
        int k = 1;
        for (Resource resource : resources) {
            ExecutionContext context = new ExecutionContext();
            Assert.state(resource.exists(), "Resource does not exist: " + resource);
            context.putString(KEY_NAME, resource.getFilename());
            context.putString("opFileName", "output" + k++ + ".xml");
            map.put(PARTITION_KEY + i, context);
            i++;
        }
        return map;
    }
}
