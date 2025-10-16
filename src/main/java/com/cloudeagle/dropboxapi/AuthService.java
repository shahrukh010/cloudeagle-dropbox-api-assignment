package com.cloudeagle.dropboxapi;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * AuthService
 *
 * Responsible for building the Dropbox authorization URL, exchanging an authorization code
 * for tokens, and refreshing access tokens.
 *
 * Uses OkHttp for HTTP requests and org.json for JSON parsing.
 */
public class AuthService {

    private static final String AUTH_URL = "https://www.dropbox.com/oauth2/authorize";
    private static final String TOKEN_URL = "https://api.dropboxapi.com/oauth2/token";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope; // space-separated scopes
    private final OkHttpClient httpClient;

    /**
     * Constructor.
     *
     * @param clientId     Dropbox App Key
     * @param clientSecret Dropbox App Secret
     * @param redirectUri  Redirect URI configured in the app
     * @param scope        space-separated scopes (e.g., "team_info.read members.read events.read")
     */
    public AuthService(String clientId, String clientSecret, String redirectUri, String scope) {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret");
        this.redirectUri = Objects.requireNonNull(redirectUri, "redirectUri");
        this.scope = scope == null ? "" : scope;
        this.httpClient = new OkHttpClient();
    }

    /**
     * Build the authorization URL you open in a browser to obtain the authorization code.
     *
     * @param state optional state value (useful for CSRF protection)
     * @return authorization URL
     */
    public String buildAuthorizationUrl(String state) throws UnsupportedEncodingException {
        StringJoiner sj = new StringJoiner("&", AUTH_URL + "?", "");
        sj.add("response_type=code");
        sj.add("client_id=" + urlEncode(clientId));
        sj.add("redirect_uri=" + urlEncode(redirectUri));
        sj.add("token_access_type=offline"); // request refresh token
        if (!scope.trim().isEmpty()) {
            sj.add("scope=" + urlEncode(scope));
        }
        if (state != null && !state.isEmpty()) {
            sj.add("state=" + urlEncode(state));
        }
        return sj.toString();
    }

    /**
     * Exchange authorization code for access + refresh tokens.
     *
     * @param code authorization code obtained after user consent
     * @return JSONObject containing token data (access_token, refresh_token, scope, expires_in, etc.)
     * @throws IOException on HTTP or network errors
     */
    public JSONObject exchangeCodeForToken(String code) throws IOException {
        RequestBody form = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .build();

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(form)
                .header("Authorization", Credentials.basic(clientId, clientSecret))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Token exchange failed: HTTP " + response.code() + " - " + body);
            }
            return new JSONObject(body);
        }
    }


    private static String urlEncode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, String.valueOf(StandardCharsets.UTF_8));
    }
}
