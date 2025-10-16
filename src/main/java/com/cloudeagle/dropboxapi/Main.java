package com.cloudeagle.dropboxapi;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Main
 * Entry point for the CloudEagle Dropbox Business API demo.
 * Supports automatic local callback capture (recommended) and falls back to manual code paste.
 */
public class Main {


    public static void main(String[] args) throws UnsupportedEncodingException {
        Logger logger = LoggerFactory.getLogger(Main.class.getName());
        logger.info("CloudEagle — Dropbox Business API Demo (Java 8, Maven)");
        Properties cfg = new Properties();

        // Load config.properties from project root
        try (InputStream in = new FileInputStream("config.properties")) {
            cfg.load(in);
        } catch (Exception e) {
            logger.error("Failed to load config.properties. Please create it in project root with dropbox.client_id, dropbox.client_secret, dropbox.redirect_uri, and dropbox.scopes");


            e.printStackTrace();
            return;
        }

        String clientId = cfg.getProperty("dropbox.client_id");
        String clientSecret = cfg.getProperty("dropbox.client_secret");
        String redirectUri = cfg.getProperty("dropbox.redirect_uri", "https://oauth.pstmn.io/v1/callback");
        String scopes = cfg.getProperty("dropbox.scopes", "team_info.read members.read events.read");

        if (clientId == null || clientSecret == null) {
            logger.error("Please set dropbox.client_id and dropbox.client_secret in config.properties");
            return;
        }

        // If redirectUri is localhost, we will attempt automatic capture
        boolean useLocalCallback = redirectUri != null && redirectUri.startsWith("http://localhost");

        AuthService authService = new AuthService(clientId.trim(), clientSecret.trim(), redirectUri.trim(), scopes.trim());

        DropboxClient client = new DropboxClient();
        DropboxService service = new DropboxService(client);

        String authUrl = authService.buildAuthorizationUrl("cloudeagle_state");
        String code = null;

        AuthHttpServer authServer = null;
        Scanner scanner = null;

        try {
            if (useLocalCallback) {
                // parse port and path from redirectUri (basic)
                URI ruri = new URI(redirectUri);
                int port = ruri.getPort() == -1 ? 80 : ruri.getPort();
                String path = ruri.getPath();

                // start local server to capture the code
                logger.info("Starting local HTTP server to capture OAuth callback at " + redirectUri);
                authServer = new AuthHttpServer(port, path);
                authServer.start();

                logger.info("\nOpening browser for authorization. If browser does not open automatically, copy-paste the URL below:");
                logger.info(authUrl);

                // try to open browser
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(new URI(authUrl));
                    } else {
                        logger.info("Desktop API not supported; please open the URL above manually.");
                    }
                } catch (Exception ex) {
                    logger.info("Unable to open browser automatically. Please open the URL above manually.");
                }

                // wait up to 120 seconds for the code
                try {
                    code = authServer.waitForCode(120, TimeUnit.SECONDS);
                    logger.info("Authorization code received automatically from the browser redirect.");
                } catch (TimeoutException te) {
                    logger.error("Timed out waiting for authorization code via local callback (120s).");
                    // will fall back to manual paste below
                } catch (ExecutionException | InterruptedException ee) {
                    logger.error("Error while waiting for authorization code: " + ee.getMessage());
                    // fall back to manual paste
                }
            }

            // fallback: manual paste if code not captured automatically
            if (code == null) {
                logger.info("\nIf the browser flow didn't complete, you can manually obtain the code.");
                logger.info("Open this URL in your browser:");
                logger.info(authUrl);
                logger.info("\nAfter allowing the app, you will be redirected to the redirect URI with ?code=<AUTH_CODE>");
                System.out.print("\nPaste the authorization code here: ");

                scanner = new Scanner(System.in);
                code = scanner.nextLine().trim();
                if (code.isEmpty()) {
                    logger.error("No code provided. Exiting.");
                    return;
                }
            }

            // Exchange code for tokens
            JSONObject tokenResponse = authService.exchangeCodeForToken(code);
            String accessToken = tokenResponse.optString("access_token", null);
            String refreshToken = tokenResponse.optString("refresh_token", null);
            String scopeReturned = tokenResponse.optString("scope", null);

            logger.info("\nToken exchange successful.");
            logger.info("Scopes returned: " + scopeReturned);
            if (accessToken == null || accessToken.isEmpty()) {
                logger.info("No access token received. Exiting.");
                return;
            }

            // Call  APIs
            service.fetchTeamInfo(accessToken);
            service.fetchAllUsers(accessToken);
            service.fetchTeamEvents(accessToken);

        } catch (Exception ex) {
            logger.error("Error during OAuth or API calls: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            // cleanup
            if (authServer != null) {
                authServer.stop();
            }
            if (scanner != null) {
                scanner.close();
            }
        }
    }
}
