# GuildOfPioneers

Spring Boot backend for Guild of Pioneers, using Keycloak OIDC authentication and site-owned membership and project permissions.

## Requirements

- JDK 17, or a newer installed JDK with `-PbuildJavaVersion=<major>` (output remains Java 17 compatible).
- PostgreSQL and S3-compatible object storage; Docker Compose can provide PostgreSQL and RustFS.
- An existing Keycloak realm and confidential OIDC client.

See [Keycloak configuration and account migration](docs/keycloak-setup.md) before rollout. Existing administrators and users must be explicitly bound to their Keycloak identities; there is no local password login.

## Run with Docker

Copy `.env.example` to `.env`, fill the DB/storage secrets, OIDC settings and public site URL, then run:

```bash
docker compose up -d --build
```

The backend binds `127.0.0.1:8080`; health is `/actuator/health`. The frontend proxies `/api/` to it. The RustFS console uses port 9001.

## Run with local tools

With PostgreSQL on localhost and configured storage/OIDC environment variables:

```bash
./gradlew bootRun
./gradlew test
```

For a machine with Java 25 instead of Java 17:

```bash
./gradlew -PbuildJavaVersion=25 bootRun
./gradlew -PbuildJavaVersion=25 test
```

The development public URL defaults to `http://localhost:5173`; supply the actual realm, client ID and secret. Production requires explicit DB, storage and OIDC values. Default avatar initialization can be disabled with `APP_DEFAULT_AVATAR_ENABLED=false`.

## Environment variables

| Variable | Default | Description |
| --- | --- | --- |
| `OIDC_ISSUER` | localhost realm in dev; required in prod | Exact Keycloak realm issuer |
| `OIDC_CLIENT_ID` | `guild-backend` in dev; required in prod | Confidential client ID |
| `OIDC_CLIENT_SECRET` | placeholder in dev; required in prod | Client secret |
| `APP_PUBLIC_URL` | `http://localhost:5173` in dev; required in prod | Public frontend origin |
| `OIDC_BOOTSTRAP_ADMIN_SUBJECT` | empty | Explicit first administrator identity for a new deployment |
| `APP_SESSION_TIMEOUT` | `30m` | Local session inactivity timeout |
| `APP_SECURE_COOKIE` | false in dev / true in prod | Use Secure session cookies |
| `POSTGRES_PASSWORD` | required by compose | PostgreSQL container password |
| `DB_URL` | `jdbc:postgresql://localhost:5432/guild` in dev | JDBC URL |
| `DB_USERNAME` | `guild` in dev | Database user |
| `DB_PASSWORD` | `guild` in dev | Database password |
| `APP_SEED_DATA` | `false` | Opt in to unbound development data |
| `APP_DEFAULT_AVATAR` | `./default_avatar.jpg` | Default avatar source image |
| `APP_TIMEZONE` | `UTC+8` | Application timezone |
| `STORAGE_TYPE` | `s3` | Storage backend: `s3` or `memory` |
| `S3_ENDPOINT` | `http://localhost:9000` in dev | S3-compatible endpoint |
| `S3_REGION` | `us-east-1` | Region used by the client |
| `S3_ACCESS_KEY`, `S3_SECRET_KEY` | required for S3 | Storage credentials |
| `S3_BUCKET` | `guild` | Object bucket |
| `S3_PATH_STYLE` | `true` | Path-style addressing for RustFS |
| `S3_CREATE_BUCKET` | `true` | Create bucket if missing |

[API contract](doc/API.md) · [Domain glossary](CONTEXT.md) · [OIDC design](docs/keycloak-oidc-design.md)
