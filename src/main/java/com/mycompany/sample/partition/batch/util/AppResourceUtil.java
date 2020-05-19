package com.mycompany.sample.partition.batch.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.info.GitProperties;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.net.URISyntaxException;

public class AppResourceUtil {
    private static final Logger logger = LoggerFactory.getLogger(AppResourceUtil.class);

    public static Resource getResource(Environment environment, GitProperties gitProperties) {
        if (environment.containsProperty("VCAP_APPLICATION") || environment.containsProperty("VCAP_SERVICES")) {
            logger.info("Running in CloudFoundry");
            return new ClassPathResource("config/application.yml");
        } else if (environment.containsProperty("INFO_KUBE_ENV") || environment.containsProperty("KUBERNETES_SERVICE_HOST")) {
            logger.info("Running in Kubernetes");
            final String appName = environment.getProperty("spring.application.name");
            // assuming application-name == docker-image-name
            return new DockerResource(appName + ":latest");
        } else {
            String appVersionName = "0.0.0-SNAPSHOT";
            if (gitProperties != null) {
                appVersionName = gitProperties.get("build.version");
            } else {
                logger.warn("git.properties not found. Add 'git-commit-id-plugin' to pom.xml");
            }
            final String appName = environment.getProperty("spring.application.name");
            File localResourceName = getPathToJar(String.format("%s-%s.jar", appName, appVersionName));
            Resource resource = new FileSystemResource(localResourceName);
            if (resource.exists()) {
                logger.info("Running locally, Using FileSystemResource {}", localResourceName);
            } else {
                throw new RuntimeException("Run 'mvn package', application jar not found - " + localResourceName);
            }
            return resource;
        }
    }

    private static File getPathToJar(String jarFileName) {
        try {
            File file = getLocationOfMainClass();
            if (file.isFile()) {
                return file;
            } else {
                return new File(file.getAbsolutePath() + File.separator + ".." + File.separator + jarFileName);
            }
        } catch (URISyntaxException | ClassNotFoundException e) {
            throw new RuntimeException("Unable to locate JAR file of Main class' - " + jarFileName, e);
        }
    }

    private static Class getMainClass() throws ClassNotFoundException {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        return Class.forName(stackTrace[stackTrace.length - 1].getClassName());
    }

    private static File getLocationOfMainClass() throws URISyntaxException, ClassNotFoundException {
        return new File(getMainClass().getProtectionDomain().getCodeSource().getLocation().toURI());
    }
}
