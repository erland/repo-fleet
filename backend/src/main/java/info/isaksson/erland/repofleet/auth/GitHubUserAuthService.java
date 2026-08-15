package info.isaksson.erland.repofleet.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class GitHubUserAuthService {
    private static final Duration ATTEMPT_TTL = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private final AuthConfig config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ConcurrentHashMap<String, OAuthAttempt> attempts = new ConcurrentHashMap<>();

    @Inject
    public GitHubUserAuthService(AuthConfig config, ObjectMapper objectMapper) {
        this(config, objectMapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    GitHubUserAuthService(AuthConfig config, ObjectMapper objectMapper, HttpClient httpClient) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public String beginLogin() {
        requireConfigured();
        cleanupExpiredAttempts();
        String state = randomValue(32);
        String verifier = randomValue(48);
        attempts.put(state, new OAuthAttempt(verifier, Instant.now().plus(ATTEMPT_TTL)));

        String challenge;
        try {
            challenge = BASE64_URL.encodeToString(MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception ex) {
            throw new AuthException("Could not initialize GitHub PKCE authentication.", ex);
        }

        return config.githubWebBaseUrl() + "/login/oauth/authorize"
            + "?client_id=" + enc(clientId())
            + "&redirect_uri=" + enc(callbackUrl())
            + "&state=" + enc(state)
            + "&code_challenge=" + enc(challenge)
            + "&code_challenge_method=S256"
            + "&allow_signup=false";
    }

    public AuthenticatedUser completeLogin(String code, String state) {
        requireConfigured();
        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            throw new AuthException("GitHub authentication callback is missing code or state.");
        }
        OAuthAttempt attempt = attempts.remove(state);
        if (attempt == null || attempt.expiresAt().isBefore(Instant.now())) {
            throw new AuthException("GitHub authentication state is invalid or expired. Start login again.");
        }

        String accessToken = exchangeCode(code, attempt.codeVerifier());
        AuthenticatedUser user = loadUser(accessToken);
        if (!allowedUsers().contains(user.login().toLowerCase(Locale.ROOT))) {
            throw new AuthException("This GitHub user is not allowed to access RepoFleet.");
        }
        return user;
    }

    private String exchangeCode(String code, String verifier) {
        String body = "client_id=" + enc(clientId())
            + "&client_secret=" + enc(clientSecret())
            + "&code=" + enc(code)
            + "&redirect_uri=" + enc(callbackUrl())
            + "&code_verifier=" + enc(verifier);
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.githubWebBaseUrl() + "/login/oauth/access_token"))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/json")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AuthException("GitHub rejected the authentication code exchange.");
            }
            JsonNode json = objectMapper.readTree(response.body());
            String token = json.path("access_token").asText("");
            if (token.isBlank()) {
                String error = json.path("error").asText("unknown_error");
                throw new AuthException("GitHub authentication failed: " + error);
            }
            return token;
        } catch (AuthException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AuthException("Could not exchange GitHub authentication code.", ex);
        }
    }

    private AuthenticatedUser loadUser(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.githubApiBaseUrl() + "/user"))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer " + accessToken)
            .header("X-GitHub-Api-Version", "2022-11-28")
            .GET()
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AuthException("GitHub user lookup failed after authentication.");
            }
            JsonNode json = objectMapper.readTree(response.body());
            String login = json.path("login").asText("");
            if (login.isBlank()) throw new AuthException("GitHub user response did not contain a login.");
            String name = nullableText(json, "name");
            String avatar = nullableText(json, "avatar_url");
            return new AuthenticatedUser(login, name, avatar);
        } catch (AuthException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AuthException("Could not load the authenticated GitHub user.", ex);
        }
    }

    private Set<String> allowedUsers() {
        Set<String> users = Arrays.stream(config.allowedUsers().orElse("").orElse("").split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(value -> value.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
        if (users.isEmpty()) {
            throw new AuthException("REPOFLEET_AUTH_ALLOWED_USERS must contain at least one GitHub login.");
        }
        return users;
    }

    private void requireConfigured() {
        if (!config.enabled()) throw new AuthException("GitHub user authentication is disabled.");
        clientId();
        clientSecret();
        callbackUrl();
        allowedUsers();
    }

    private String clientId() {
        return config.clientId().filter(value -> !value.isBlank())
            .orElseThrow(() -> new AuthException("REPOFLEET_AUTH_CLIENT_ID is required."));
    }

    private String clientSecret() {
        return config.clientSecret().filter(value -> !value.isBlank())
            .orElseThrow(() -> new AuthException("REPOFLEET_AUTH_CLIENT_SECRET is required."));
    }

    private String callbackUrl() {
        return config.callbackUrl().filter(value -> !value.isBlank())
            .orElseThrow(() -> new AuthException("REPOFLEET_AUTH_CALLBACK_URL is required."));
    }

    private void cleanupExpiredAttempts() {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private static String randomValue(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return BASE64_URL.encodeToString(value);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String nullableText(JsonNode json, String field) {
        JsonNode value = json.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
