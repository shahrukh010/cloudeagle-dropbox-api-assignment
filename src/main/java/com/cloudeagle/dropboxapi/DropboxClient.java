package com.cloudeagle.dropboxapi;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * DropboxClient
 * <p>
 * Low-level HTTP helper for calling Dropbox APIs with an access token.
 * Uses OkHttp for HTTP requests.
 */
public class DropboxClient {

    private final OkHttpClient httpClient;

    public DropboxClient() {
        // Basic client with reasonable timeouts
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * POST JSON to a Dropbox endpoint and return the JSON result as JSONObject.
     *
     * @param url         full URL (e.g., https://api.dropboxapi.com/2/team/get_info)
     * @param jsonBody    JSON string body (or "{}" for empty)
     * @param accessToken OAuth2 bearer token
     * @return JSONObject parsed response
     * @throws IOException on network/HTTP error
     */
    public JSONObject postJson(String url, String jsonBody, String accessToken) throws IOException {
        RequestBody body = (jsonBody == null)
                ? RequestBody.create(null, new byte[0])  // send no body
                : RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody
        );


        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer " + accessToken)
//                .header("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + respBody);
            }
            return new JSONObject(respBody);
        }
    }

}
