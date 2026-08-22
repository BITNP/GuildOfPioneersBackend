# GuildOfPioneers

Spring Boot backend for Guild of Pioneers.

## Requirements

- JDK 17
- Docker
- `POSTGRES_PASSWORD` environment variable (used by the database container)

An example env file is provided in `.env.example`. Copy it to `.env` and adjust
the values:

```bash
cp .env.example .env
```

## Run with Docker (backend + Postgres)

```bash
docker compose up -d --build
```

The backend is served at `http://localhost:8080`. Health check:
`http://localhost:8080/actuator/health`.

## Run with Gradle (Postgres only via Docker)

Start just the database:

```bash
docker compose up -d postgres
```

Then run the backend with the default profile (expects `jdbc:postgresql://localhost:5432/guild`,
user `guild`, password `guild`):

```bash
./gradlew bootRun
```

Or run against the prod profile (requires the same env vars used in `docker-compose.yml`):

```bash
export POSTGRES_PASSWORD=guild
export ADMIN_PASSWORD=password123
docker compose up -d postgres
./gradlew bootRun --args='--spring.profiles.active=prod' \
  -Dspring-boot.run.jvmArguments="-DDB_URL=jdbc:postgresql://localhost:5432/guild -DDB_USERNAME=guild -DDB_PASSWORD=guild -DAPP_ADMIN_PASSWORD=$ADMIN_PASSWORD"
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
| `APP_UPLOAD_DIR` | `./uploads` | File upload directory |
| `APP_TIMEZONE` | `UTC+8` | Application timezone |
| `VEIL_DB_TYPE` | `POSTGRES` | Veil metadata database engine |
