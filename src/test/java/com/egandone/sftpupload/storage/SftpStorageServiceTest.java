package com.egandone.sftpupload.storage;

import com.egandone.sftpupload.config.SftpProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.Attributes;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.file.remote.ClientCallback;
import org.springframework.integration.file.remote.SessionCallback;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SftpStorageServiceTest {

    @Mock
    private SftpRemoteFileTemplate remoteFileTemplate;

    private SftpStorageService storageService;

    @Mock
    private Session<DirEntry> session;

    @Mock
    private SftpClient client;

    @BeforeEach
    void setUp() {
        SftpProperties properties = new SftpProperties();
        properties.setRemoteDirectory("/uploads");
        storageService = new SftpStorageService(remoteFileTemplate, properties);
    }

    @SuppressWarnings("unchecked")
    private static <T> SessionCallback<DirEntry, T> sessionCallback(Object callback) {
        return (SessionCallback<DirEntry, T>) callback;
    }

    @SuppressWarnings("unchecked")
    private static <T> ClientCallback<SftpClient, T> clientCallback(Object callback) {
        return (ClientCallback<SftpClient, T>) callback;
    }

    @Test
    void storeRejectsPathTraversalFilename() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> storageService.store("../secret.txt", new ByteArrayInputStream(new byte[]{1})));

        assertEquals("Invalid filename", exception.getMessage());
        verifyNoInteractions(remoteFileTemplate);
    }

    @Test
    void readRejectsAbsoluteFilename() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> storageService.read("/etc/passwd"));

        assertEquals("Invalid filename", exception.getMessage());
        verifyNoInteractions(remoteFileTemplate);
    }

    @Test
    void storeWrapsTemplateErrorAsIOException() {
        given(remoteFileTemplate.execute(any())).willThrow(new RuntimeException("sftp down"));

        IOException exception = assertThrows(IOException.class,
                () -> storageService.store("ok.txt", new ByteArrayInputStream("x".getBytes())));

        assertEquals("Failed to upload file to SFTP server", exception.getMessage());
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    @Test
    void storeCreatesRemoteDirectoryWhenMissing() throws Exception {
        given(session.exists("/uploads")).willReturn(false);
        given(remoteFileTemplate.execute(any())).willAnswer(invocation ->
                sessionCallback(invocation.getArgument(0)).doInSession(session));

        storageService.store("ok.txt", new ByteArrayInputStream("hello".getBytes()));

        verify(session).exists("/uploads");
        verify(session).mkdir("/uploads");
        verify(session).write(any(), eq("/uploads/ok.txt"));
    }

    @Test
    void storeSkipsMkdirWhenDirectoryExistsAndTrimsTrailingSlash() throws Exception {
        SftpProperties slashProperties = new SftpProperties();
        slashProperties.setRemoteDirectory("/uploads/");
        SftpStorageService slashStorageService = new SftpStorageService(remoteFileTemplate, slashProperties);

        given(session.exists("/uploads/")).willReturn(true);
        given(remoteFileTemplate.execute(any())).willAnswer(invocation ->
                sessionCallback(invocation.getArgument(0)).doInSession(session));

        slashStorageService.store("ok.txt", new ByteArrayInputStream("hello".getBytes()));

        verify(session).exists("/uploads/");
        verify(session, never()).mkdir("/uploads/");
        verify(session).write(any(), eq("/uploads/ok.txt"));
    }

    @Test
    void readWrapsTemplateErrorAsIOException() {
        given(remoteFileTemplate.executeWithClient(any())).willThrow(new RuntimeException("sftp down"));

        IOException exception = assertThrows(IOException.class, () -> storageService.read("ok.txt"));

        assertEquals("Failed to read file from SFTP server", exception.getMessage());
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    @Test
    void readReturnsContentFromClient() throws Exception {
        given(client.read("/uploads/demo.txt")).willReturn(new ByteArrayInputStream("payload".getBytes()));
        given(remoteFileTemplate.executeWithClient(any())).willAnswer(invocation ->
                clientCallback(invocation.getArgument(0)).doWithClient(client));

        byte[] bytes = storageService.read("demo.txt");

        assertEquals("payload", new String(bytes));
        verify(client).read("/uploads/demo.txt");
    }

    @Test
    void listFilesReturnsTemplateResult() {
        List<StoredFile> files = List.of(
                new StoredFile("a.txt", 1L, Instant.EPOCH),
                new StoredFile("b.txt", 2L, Instant.EPOCH));
        given(remoteFileTemplate.execute(any())).willReturn(files);

        List<StoredFile> actual = storageService.listFiles();

        assertEquals(files, actual);
    }

    @Test
    void listFilesFiltersDotsSortsByNameAndUsesEpochForMissingModifyTime() throws Exception {
        Attributes aAttributes = new Attributes().size(10).modifyTime(FileTime.from(Instant.parse("2026-01-01T00:00:00Z")));
        Attributes zAttributes = new Attributes().size(30);

        DirEntry current = new DirEntry(".", ".", new Attributes());
        DirEntry parent = new DirEntry("..", "..", new Attributes());
        DirEntry zEntry = new DirEntry("z.txt", "z.txt", zAttributes);
        DirEntry aEntry = new DirEntry("a.txt", "a.txt", aAttributes);

        given(session.list("/uploads")).willReturn(new DirEntry[]{current, parent, zEntry, aEntry});
        given(remoteFileTemplate.execute(any())).willAnswer(invocation ->
                sessionCallback(invocation.getArgument(0)).doInSession(session));

        List<StoredFile> files = storageService.listFiles();

        assertEquals(2, files.size());
        assertEquals("a.txt", files.get(0).name());
        assertEquals(10L, files.get(0).size());
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), files.get(0).modifiedAt());
        assertEquals("z.txt", files.get(1).name());
        assertEquals(30L, files.get(1).size());
        assertEquals(Instant.EPOCH, files.get(1).modifiedAt());
    }
}
