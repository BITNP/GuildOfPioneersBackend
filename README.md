# GuildOfPioneers

Spring Boot backend for Guild of Pioneers.

## Requirements

- JDK 17
- Docker
- `POSTGRES_PASSWORD`, `RUSTFS_ACCESS_KEY`, and `RUSTFS_SECRET_KEY` environment
  variables (used by the database and object-storage containers)

An example env file is provided in `.env.example`. Copy it to `.env` and adjust
the values:

```bash
cp .env.example .env
```

## Run with Docker (backend + Postgres + RustFS)

```bash
docker compose up -d --build
```

The backend is served at `http://localhost:8080`. Health check:
`http://localhost:8080/actuator/health`. The RustFS console is served at
`http://localhost:9001`.

## Run with Gradle (Postgres + RustFS only via Docker)

Start just the infrastructure:

```bash
docker compose up -d postgres rustfs
```

Then run the backend with the default profile (expects `jdbc:postgresql://localhost:5432/guild`,
user `guild`, password `guild`, and RustFS on `http://localhost:9000`):

```bash
./gradlew bootRun
```

Or run against the prod profile (requires the same env vars used in `docker-compose.yml`):

```bash
export POSTGRES_PASSWORD=guild
export ADMIN_PASSWORD=password123
export RUSTFS_ACCESS_KEY=dev-access-key
export RUSTFS_SECRET_KEY=dev-secret-key-change-me
docker compose up -d postgres rustfs
./gradlew bootRun --args='--spring.profiles.active=prod' \
  -Dspring-boot.run.jvmArguments="-DDB_URL=jdbc:postgresql://localhost:5432/guild -DDB_USERNAME=guild -DDB_PASSWORD=guild -DAPP_ADMIN_PASSWORD=$ADMIN_PASSWORD -DS3_ENDPOINT=http://localhost:9000 -DS3_ACCESS_KEY=$RUSTFS_ACCESS_KEY -DS3_SECRET_KEY=$RUSTFS_SECRET_KEY"
```

## Key environment variables

| Variable | Default | Description |
| --- | --- | --- |
| `POSTGRES_PASSWORD` | (required) | Postgres password |
| `DB_URL` | `jdbc:postgresql://localhost:5432/guild` (dev profile) | JDBC URL |
| `DB_USERNAME` | `guild` | Database user |
| `DB_PASSWORD` | `guild` (dev profile) | Database password |
| `APP_ADMIN_PASSWORD` | `password123` (dev profile), required in prod | Initial admin password |
| `APP_SEED_DATA` | `true` (dev) / `false` (prod) | Seed demo data |
| `APP_DEFAULT_AVATAR` | `./default_avatar.jpg` | Source image imported as the default avatar |
| `APP_TIMEZONE` | `UTC+8` | Application timezone |
| `STORAGE_TYPE` | `s3` | Storage backend, `s3` or `memory` |
| `S3_ENDPOINT` | `http://localhost:9000` (dev profile), required in prod | S3-compatible endpoint URL |
| `S3_REGION` | `us-east-1` | S3 region (required by the client even for RustFS) |
| `S3_ACCESS_KEY` | (required with `S3_SECRET_KEY`) | S3 access key |
| `S3_SECRET_KEY` | (required with `S3_ACCESS_KEY`) | S3 secret key |
| `S3_BUCKET` | `guild` | Bucket that stores all objects |
| `S3_PATH_STYLE` | `true` | Use path-style bucket addressing (required by RustFS) |
| `S3_CREATE_BUCKET` | `true` | Create the bucket at startup when missing |
