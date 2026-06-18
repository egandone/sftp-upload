package com.egandone.sftpupload.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SftpPropertiesTest {

    @Test
    void defaultsAreInitialized() {
        SftpProperties properties = new SftpProperties();

        assertEquals("localhost", properties.getHost());
        assertEquals(22, properties.getPort());
        assertEquals("devuser", properties.getUsername());
        assertEquals("", properties.getPassword());
        assertEquals("", properties.getPrivateKeyLocation());
        assertEquals("", properties.getPrivateKeyPassphrase());
        assertEquals("/uploads", properties.getRemoteDirectory());
        assertTrue(properties.isAllowUnknownKeys());
    }

    @Test
    void settersUpdateAllFields() {
        SftpProperties properties = new SftpProperties();

        properties.setHost("sftp.example.com");
        properties.setPort(2022);
        properties.setUsername("alice");
        properties.setPassword("secret");
        properties.setPrivateKeyLocation("classpath:key.pem");
        properties.setPrivateKeyPassphrase("passphrase");
        properties.setRemoteDirectory("/incoming");
        properties.setAllowUnknownKeys(false);

        assertEquals("sftp.example.com", properties.getHost());
        assertEquals(2022, properties.getPort());
        assertEquals("alice", properties.getUsername());
        assertEquals("secret", properties.getPassword());
        assertEquals("classpath:key.pem", properties.getPrivateKeyLocation());
        assertEquals("passphrase", properties.getPrivateKeyPassphrase());
        assertEquals("/incoming", properties.getRemoteDirectory());
        assertFalse(properties.isAllowUnknownKeys());
    }
}
