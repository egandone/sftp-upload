package com.egandone.sftpupload.storage;

import java.time.Instant;

public record StoredFile(String name, long size, Instant modifiedAt) {
}
