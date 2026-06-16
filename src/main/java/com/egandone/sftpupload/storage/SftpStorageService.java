package com.egandone.sftpupload.storage;

import com.egandone.sftpupload.config.SftpProperties;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.Attributes;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SftpStorageService implements StorageService {

    private final SftpRemoteFileTemplate remoteFileTemplate;
    private final SftpProperties properties;

    public SftpStorageService(SftpRemoteFileTemplate remoteFileTemplate, SftpProperties properties) {
        this.remoteFileTemplate = remoteFileTemplate;
        this.properties = properties;
    }

    @Override
    public void store(String filename, InputStream inputStream) throws IOException {
        String cleaned = sanitizeFilename(filename);
        String remotePath = remotePath(cleaned);

        try {
            remoteFileTemplate.execute(session -> {
                if (!session.exists(properties.getRemoteDirectory())) {
                    session.mkdir(properties.getRemoteDirectory());
                }
                session.write(inputStream, remotePath);
                return null;
            });
        } catch (Exception exception) {
            throw new IOException("Failed to upload file to SFTP server", exception);
        }
    }

    @Override
    public List<StoredFile> listFiles() {
        return remoteFileTemplate.execute(session -> Arrays.stream(session.list(properties.getRemoteDirectory()))
                .filter(entry -> !".".equals(entry.getFilename()) && !"..".equals(entry.getFilename()))
                .map(entry -> {
                    Attributes attributes = entry.getAttributes();
                    Instant modifiedAt = attributes.getModifyTime() != null
                            ? attributes.getModifyTime().toInstant()
                            : Instant.EPOCH;
                    return new StoredFile(entry.getFilename(), attributes.getSize(), modifiedAt);
                })
                .sorted(Comparator.comparing(StoredFile::name))
                .toList());
    }

    @Override
    public byte[] read(String filename) throws IOException {
        String cleaned = sanitizeFilename(filename);

        try {
            return remoteFileTemplate.executeWithClient((SftpClient client) -> {
                try (InputStream inputStream = client.read(remotePath(cleaned))) {
                    return inputStream.readAllBytes();
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        } catch (Exception exception) {
            throw new IOException("Failed to read file from SFTP server", exception);
        }
    }

    private String remotePath(String filename) {
        String directory = properties.getRemoteDirectory().endsWith("/")
                ? properties.getRemoteDirectory().substring(0, properties.getRemoteDirectory().length() - 1)
                : properties.getRemoteDirectory();
        return directory + "/" + filename;
    }

    private String sanitizeFilename(String filename) {
        String cleaned = StringUtils.cleanPath(filename);
        if (!StringUtils.hasText(cleaned) || cleaned.contains("..") || cleaned.startsWith("/")) {
            throw new IllegalArgumentException("Invalid filename");
        }
        return cleaned;
    }
}
