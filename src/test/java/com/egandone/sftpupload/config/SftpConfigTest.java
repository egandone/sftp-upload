package com.egandone.sftpupload.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SftpConfigTest {

    private final SftpConfig config = new SftpConfig();

    @TempDir
    Path tempDir;

    @Test
    void sessionFactoryIsCachingWhenUsingPassword() {
        SftpProperties properties = new SftpProperties();
        properties.setHost("localhost");
        properties.setPort(22);
        properties.setUsername("devuser");
        properties.setPassword("pw");

        SessionFactory<DirEntry> factory = config.sftpSessionFactory(properties);

        assertNotNull(factory);
        assertInstanceOf(CachingSessionFactory.class, factory);
    }

    @Test
    void sessionFactorySupportsPrivateKeyConfiguration() throws Exception {
        Path key = tempDir.resolve("id_rsa");
        Files.writeString(key, "dummy-key");

        SftpProperties properties = new SftpProperties();
        properties.setHost("localhost");
        properties.setPort(22);
        properties.setUsername("devuser");
        properties.setPrivateKeyLocation("file:" + key.toAbsolutePath());
        properties.setPrivateKeyPassphrase("topsecret");

        SessionFactory<DirEntry> factory = config.sftpSessionFactory(properties);

        assertNotNull(factory);
        assertInstanceOf(CachingSessionFactory.class, factory);
    }

    @Test
    void sftpRemoteFileTemplateIsCreated() {
        SftpProperties properties = new SftpProperties();
        SessionFactory<DirEntry> sessionFactory = config.sftpSessionFactory(properties);

        SftpRemoteFileTemplate template = config.sftpRemoteFileTemplate(sessionFactory);

        assertNotNull(template);
    }
}
