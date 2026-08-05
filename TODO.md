# TODO

## Migrate session storage to Redis (Spring Session)

Store server-side sessions in Redis so they survive application restarts and are shareable across instances.

- [ ] `build.gradle.kts`: add `implementation("org.springframework.session:spring-session-data-redis")`
- [ ] `build.gradle.kts`: add test deps `testcontainers` and `testcontainers:junit-jupiter` (versions managed by the Boot BOM)
- [ ] `docker-compose.yml`: add `redis:7-alpine` service on port 6379 with `--appendonly yes` and a `redis_data` volume
- [ ] `application.properties` (dev): `spring.data.redis.host=localhost`, `spring.data.redis.port=6379`, `spring.session.timeout=30m`
- [ ] `application-prod.properties`: `spring.data.redis.host=${REDIS_HOST}`, `spring.data.redis.port=${REDIS_PORT:6379}`, `spring.data.redis.password=${REDIS_PASSWORD:}`, `spring.session.timeout=${SESSION_TIMEOUT:30m}`
- [ ] `AuthController.java`: remove the manual `buildSessionCookie`/`Max-Age` cookie override (Spring Session writes the cookie automatically); keep `session.setMaxInactiveInterval(30 days)` for remember-me; update Javadoc
- [ ] `SecurityConfig.java`: add `.deleteCookies("SESSION")` to logout so it clears the persistent cookie
- [ ] Tests: add an `AbstractRedisIntegrationTest` base class (static `RedisContainer` + `@DynamicPropertySource`); have `GuildOfPioneersBackendApplicationTests` and `AuthControllerIntegrationTest` extend it
- [ ] `AuthControllerIntegrationTest`: update cookie assertions from `JSESSIONID=` to `SESSION=` (keep `Max-Age=2592000` check for remember-me)
- [ ] `doc/API.md`: note sessions are stored in Redis and the session cookie name is `SESSION`
- [ ] Verify: `./gradlew test` and `npm run build` (frontend unchanged)

Notes:
- Cookie name changes from `JSESSIONID` to `SESSION`; transparent to the frontend (`credentials: 'same-origin'`).
- Non-remember-me sessions also get a `Max-Age` cookie equal to the default TTL (~30 min) instead of a pure session cookie.
