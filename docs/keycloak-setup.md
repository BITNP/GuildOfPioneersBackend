# Keycloak deployment and account migration

The backend completes OIDC Authorization Code login with PKCE and keeps credentials in the server session. The frontend and API use the same public origin through nginx or the Vite proxy. There is no password login or default password administrator.

## Configure the existing Keycloak client

Create a confidential OpenID Connect client for this application:

- Client authentication: enabled; Standard Flow: enabled.
- Client ID: the value of `OIDC_CLIENT_ID`.
- PKCE code challenge method: `S256`.
- Valid redirect URI: exactly `APP_PUBLIC_URL/api/auth/oidc/callback/keycloak`.
- Scopes: `openid profile email`; expose `preferred_username` and, if applicable, `email` and `email_verified`.
- Direct Access Grants and Implicit Flow are not needed.
- Roles in Keycloak do not grant site departments or privileges.

`OIDC_ISSUER` is the exact realm issuer, such as `https://sso.example.com/realms/bitnp`. Discovery metadata must report the same issuer and contain endpoints reachable by the backend; the authorization endpoint must also be reachable by the browser. Do not substitute an internal container URL for the issuer in signed tokens.

Copy `.env.example` and fill the client secret and deployment settings. Production requires `OIDC_ISSUER`, `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET` and `APP_PUBLIC_URL`. The public URL is an origin, without a path, query or fragment; for example `https://guild.example.com`.

All authentication endpoints are under `/api/auth/`, so the existing frontend proxy handles them. Use HTTPS in production with `APP_SECURE_COOKIE=true`; for local HTTP only, set it to `false`. Cookies are HttpOnly, SameSite=Lax and browser-session scoped. `APP_SESSION_TIMEOUT` controls inactivity, default `30m`.

The current sessions are held in JVM memory. Restarting the backend requires users to sign in again. An active session can remain valid while requests continue; Keycloak account disablement or external SSO logout does not immediately revoke it. Clicking “Log out of this site” invalidates only the local session. The next explicit login may reuse Keycloak SSO.

## Initialize a new deployment

1. In Keycloak, find the intended administrator's user ID (the OIDC `sub`).
2. Set `OIDC_BOOTSTRAP_ADMIN_SUBJECT` to that exact ID; the issuer is `OIDC_ISSUER`.
3. Start the app, sign in as that identity and complete the local username, mobile number and optional email.
4. When no site administrator exists, that identity can create the first ADMIN/LEADER account without a ticket. Other identities still need invitations.

There is no automatic “first login wins” grant, username-based admin grant, or default password. The configuration does not promote an already bound ordinary user or overwrite an existing administrator.

Demo initialization is disabled by default. `APP_SEED_DATA=true` creates unbound development users and sample business data, not usable login identities; it does not create a password administrator.

## Bind existing accounts before rollout

Keep the original site user IDs. Independently verify each Keycloak identity, then create a JSON file containing the explicit associations:

```json
[
  {
    "userId": 1,
    "issuer": "https://sso.example.com/realms/bitnp",
    "subject": "the-verified-keycloak-user-id"
  }
]
```

Start with existing site administrators so someone can create invitation tickets after rollout. A matching username, email or development `user_cloaks.cloak_id` is not evidence of ownership.

Build the executable jar and run its narrow maintenance entry point, using the existing DB environment:

```bash
./gradlew bootJar
java -jar build/libs/GuildOfPioneers-0.0.1-SNAPSHOT.jar bind-identities \
  --app.identity-link.file=/absolute/path/bindings.json
```

With an existing newer JDK, use `./gradlew -PbuildJavaVersion=25 bootJar` (replace 25 with its installed major version). The jar still targets Java 17.

The command needs the DB settings and `OIDC_ISSUER`; it does not contact Keycloak, start HTTP, seed data or initialize object storage. Flyway migrations run first. It validates every entry, refuses missing users, mismatched issuers or conflicting identities, and rolls the entire batch back on any error. Reimporting the identical association is a no-op. It never replaces a binding or changes department privileges.

For the compose deployment, bring up the existing DB and storage services, pause the old backend, then run the new image in maintenance mode:

```bash
docker compose up -d postgres rustfs
docker compose build backend
docker compose stop backend
docker compose run --rm --no-deps \
  -v "$PWD/bindings.json:/run/bindings.json:ro" \
  backend bind-identities --app.identity-link.file=/run/bindings.json
docker compose up -d backend
```

After binding, users sign in again to resolve their site membership. An identity that was already logged in as pending also needs to log out and sign in again.

V14 adds the identity table and allows null passwords. Existing password hashes and business data remain, but hashes no longer participate in authentication. Unbound legacy users cannot recover their account by registering with the same name or phone; they must contact the maintainer to be bound.

## First-time member flow

The member clicks “Sign in with BITNP”, completes Keycloak authentication, and reaches the invitation page. They provide a still-valid ticket and site profile. The ticket assigns the local department and role; tickets retain their existing reuse/expiry behavior. Only after successful admission can the member access projects, users or profile APIs.

Verified Keycloak email and display name can prefill the initial form. Subsequent login never overwrites local profile fields. A duplicate local username or mobile number is a conflict, not automatic account merging.

## Verification

Backend tests include a local OIDC provider with real signed ID tokens and JWKS, invalid issuer/audience/signature/expiry/nonce, PKCE, state browser binding and replay rejection, admission isolation, CSRF, registration races, and atomic maintenance imports. Frontend tests cover routing, admission, CSRF writes and local logout.

For real-service verification after configuring your deployment:

1. Visit a protected project URL while signed out, complete login, and confirm a bound member returns there.
2. Sign in as an unbound identity: confirm it sees invitation/profile setup and receives 403 for business APIs.
3. Complete a valid invitation; confirm department membership, local profile and avatar updates work.
4. Confirm old users retain their IDs, projects and permissions after explicit binding.
5. Log out locally and confirm the site session ends; other Keycloak applications remain signed in.
6. Confirm failed or cancelled login offers retry, and URLs/logs do not contain a site token.

Local protocol tests do not constitute a successful connection to your real Keycloak service.
