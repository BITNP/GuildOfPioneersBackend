package net.bitnp.guildofpioneers.auth;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import tools.jackson.databind.ObjectMapper;

/** Local protocol fixture providing real discovery, token exchange, JWKS and signed ID tokens. */
final class MockOidcProvider implements AutoCloseable {
    private final HttpServer server;
    private final RSAKey key;
    private final RSAKey attacker;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Grant> grants = new ConcurrentHashMap<>();
    final AtomicInteger exchanges = new AtomicInteger();
    volatile boolean metadataUnavailable;
    volatile boolean pkceVerified;
    record Grant(String nonce, String challenge, String mode) { }

    MockOidcProvider() {
        try {
            key = new RSAKeyGenerator(2048).keyID("test-key").generate();
            attacker = new RSAKeyGenerator(2048).keyID("test-key").generate();
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
            server.start();
        } catch (Exception ex) { throw new IllegalStateException("Cannot start local OIDC fixture", ex); }
    }

    String issuer() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/realms/guild"; }
    void grant(String code, String nonce, String challenge, String mode) {
        grants.put(code, new Grant(nonce, challenge, mode));
        pkceVerified = false;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.contains("openid-configuration")) {
                if (metadataUnavailable) { respond(exchange, 503, Map.of("error", "unavailable")); return; }
                respond(exchange, 200, Map.of("issuer", issuer(), "authorization_endpoint", issuer() + "/authorize",
                        "token_endpoint", issuer() + "/token", "jwks_uri", issuer() + "/jwks",
                        "response_types_supported", List.of("code"), "subject_types_supported", List.of("public"),
                        "id_token_signing_alg_values_supported", List.of("RS256"),
                        "token_endpoint_auth_methods_supported", List.of("client_secret_basic")));
            } else if (path.endsWith("/jwks")) {
                respond(exchange, 200, new JWKSet(key.toPublicJWK()).toJSONObject());
            } else if (path.endsWith("/token")) {
                exchanges.incrementAndGet();
                Map<String, String> form = parse(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                Grant grant = grants.remove(form.get("code"));
                String basic = "Basic " + Base64.getEncoder().encodeToString("guild-test:test-secret".getBytes(StandardCharsets.UTF_8));
                if (grant == null || !basic.equals(exchange.getRequestHeaders().getFirst("Authorization"))
                        || !"http://localhost:5173/api/auth/oidc/callback/keycloak".equals(form.get("redirect_uri"))) {
                    respond(exchange, 400, Map.of("error", "invalid_grant")); return;
                }
                String verifier = form.getOrDefault("code_verifier", "");
                String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                        MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
                pkceVerified = !verifier.isBlank() && challenge.equals(grant.challenge());
                if (!pkceVerified) { respond(exchange, 400, Map.of("error", "invalid_grant")); return; }
                respond(exchange, 200, Map.of("access_token", "fixture-access-token", "token_type", "Bearer",
                        "expires_in", 300, "scope", "openid profile email", "id_token", token(grant)));
            } else { respond(exchange, 404, Map.of("error", "not_found")); }
        } catch (Exception ex) {
            respond(exchange, 500, Map.of("error", "fixture_failure"));
        } finally { exchange.close(); }
    }

    private String token(Grant grant) throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(grant.mode().equals("issuer") ? issuer() + "/wrong" : issuer())
                .subject("protocol-subject")
                .audience(grant.mode().equals("audience") ? "other-client" : "guild-test")
                .issueTime(Date.from(now.minusSeconds(10)))
                .expirationTime(Date.from(grant.mode().equals("expired") ? now.minusSeconds(300) : now.plusSeconds(300)))
                .claim("nonce", grant.mode().equals("nonce") ? "wrong-nonce" : grant.nonce())
                .claim("preferred_username", "Alice")
                .claim("email", "alice@example.com").claim("email_verified", true)
                .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(grant.mode().equals("signature") ? attacker : key));
        return jwt.serialize();
    }

    private void respond(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] data = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, data.length);
        exchange.getResponseBody().write(data);
    }

    static Map<String, String> parse(String raw) {
        Map<String, String> result = new HashMap<>();
        for (String part : raw.split("&")) {
            String[] pair = part.split("=", 2);
            result.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                    pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
        }
        return result;
    }
    @Override public void close() { server.stop(0); }
}
