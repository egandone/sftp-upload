# sftp-upload

Spring Boot web app for uploading, listing, and downloading files stored in an SFTP backend.

## Features

- Upload files from a web UI
- View/list uploaded files
- Download files from the UI
- SFTP storage backend configured at deployment time

## Run locally

Prerequisites:

- Java 17+
- Maven 3.9+
- Access to an SFTP server

Start the app:

```bash
mvn spring-boot:run
```

Open http://localhost:8080

## SFTP configuration

All SFTP settings are externalized and can be set with environment variables.

| Property | Environment variable | Default |
| --- | --- | --- |
| `app.sftp.host` | `SFTP_HOST` | `localhost` |
| `app.sftp.port` | `SFTP_PORT` | `22` |
| `app.sftp.username` | `SFTP_USERNAME` | `devuser` |
| `app.sftp.password` | `SFTP_PASSWORD` | _(empty)_ |
| `app.sftp.private-key-location` | `SFTP_PRIVATE_KEY_LOCATION` | _(empty)_ |
| `app.sftp.private-key-passphrase` | `SFTP_PRIVATE_KEY_PASSPHRASE` | _(empty)_ |
| `app.sftp.remote-directory` | `SFTP_REMOTE_DIRECTORY` | `/uploads` |
| `app.sftp.allow-unknown-keys` | `SFTP_ALLOW_UNKNOWN_KEYS` | `true` |

Example:

```bash
export SFTP_HOST=example-sftp
export SFTP_PORT=22
export SFTP_USERNAME=my-user
export SFTP_PASSWORD=my-password
export SFTP_REMOTE_DIRECTORY=/incoming
mvn spring-boot:run
```

For production, inject environment variables from your platform's secret/config management and avoid committing credentials.
