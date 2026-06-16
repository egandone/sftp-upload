package com.egandone.sftpupload.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface StorageService {

    void store(String filename, InputStream inputStream) throws IOException;

    List<StoredFile> listFiles();

    byte[] read(String filename) throws IOException;
}
