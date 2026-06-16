package com.egandone.sftpupload.config;

import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.util.StringUtils;

@Configuration
public class SftpConfig {

    @Bean
    SessionFactory<DirEntry> sftpSessionFactory(SftpProperties properties) {
        DefaultSftpSessionFactory sessionFactory = new DefaultSftpSessionFactory(true);
        sessionFactory.setHost(properties.getHost());
        sessionFactory.setPort(properties.getPort());
        sessionFactory.setUser(properties.getUsername());
        sessionFactory.setAllowUnknownKeys(properties.isAllowUnknownKeys());

        if (StringUtils.hasText(properties.getPassword())) {
            sessionFactory.setPassword(properties.getPassword());
        }

        if (StringUtils.hasText(properties.getPrivateKeyLocation())) {
            ResourceLoader resourceLoader = new DefaultResourceLoader();
            sessionFactory.setPrivateKey(resourceLoader.getResource(properties.getPrivateKeyLocation()));
            if (StringUtils.hasText(properties.getPrivateKeyPassphrase())) {
                sessionFactory.setPrivateKeyPassphrase(properties.getPrivateKeyPassphrase());
            }
        }

        return new CachingSessionFactory<>(sessionFactory);
    }

    @Bean
    SftpRemoteFileTemplate sftpRemoteFileTemplate(SessionFactory<DirEntry> sftpSessionFactory) {
        return new SftpRemoteFileTemplate(sftpSessionFactory);
    }
}
