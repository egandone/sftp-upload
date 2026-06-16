package com.egandone.sftpupload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SftpUploadApplication {

    public static void main(String[] args) {
        SpringApplication.run(SftpUploadApplication.class, args);
    }
}
