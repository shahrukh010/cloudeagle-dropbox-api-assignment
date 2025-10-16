package com.cloudeagle.dropboxapi;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * DropboxService
 * <p>
 * High-level service layer that wraps specific Dropbox Business API calls.
 * Implements the three APIs required by the CloudEagle assignment:
 * 1. Get Team / Organization Info
 * 2. Get All Users
 * 3. Get Team Events
 */
public class DropboxService {

    private final DropboxClient client;

    public DropboxService(DropboxClient client) {
        this.client = client;
    }

    /**
     * 1️⃣ Get Team / Organization Info
     * Endpoint: https://api.dropboxapi.com/2/team/get_info
     */
    public void fetchTeamInfo(String accessToken) {
        String url = "https://api.dropboxapi.com/2/team/get_info";
        try {
            JSONObject response = client.postJson(url,null,accessToken);

            System.out.println("\n===== TEAM / ORGANIZATION INFO =====");
            System.out.println("Team ID: " + response.optString("team_id", "N/A"));
            System.out.println("Team Name: " + response.optString("name", "N/A"));
            System.out.println("Sharing Policies: " + response.optJSONObject("sharing_policies"));
            System.out.println("====================================");

        } catch (Exception e) {
            System.err.println("Error fetching team info: " + e.getMessage());
        }
    }

    /**
     * 2️⃣ Get All Users (Team Members)
     * Endpoint: https://api.dropboxapi.com/2/team/members/list
     */
    public void fetchAllUsers(String accessToken) {
        String url = "https://api.dropboxapi.com/2/team/members/list";
        JSONObject body = new JSONObject();
        body.put("limit", 100);

        try {
            JSONObject response = client.postJson(url, body.toString(), accessToken);

            System.out.println("\n===== TEAM MEMBERS LIST =====");
            JSONArray members = response.optJSONArray("members");
            if (members != null) {
                for (int i = 0; i < members.length(); i++) {
                    JSONObject profile = members.getJSONObject(i).optJSONObject("profile");
                    if (profile != null) {
                        System.out.println((i + 1) + ". " + profile.optString("email") +
                                " (" + profile.optString("status") + ")");
                    }
                }
            }
            System.out.println("Total members returned: " + (members != null ? members.length() : 0));
            System.out.println("====================================");

        } catch (Exception e) {
            System.err.println("Error fetching team members: " + e.getMessage());
        }
    }

    /**
     * 3️⃣ Get Team Events (Sign-in Events / Audit Log)
     * Endpoint: https://api.dropboxapi.com/2/team_log/get_events
     */
    public void fetchTeamEvents(String accessToken) {
        String url = "https://api.dropboxapi.com/2/team_log/get_events";
        JSONObject body = new JSONObject();
        body.put("limit", 20); // small sample for display

        try {
            JSONObject response = client.postJson(url, body.toString(), accessToken);

            System.out.println("\n===== TEAM EVENTS (Recent 20) =====");
            JSONArray events = response.optJSONArray("events");
            if (events != null) {
                for (int i = 0; i < events.length(); i++) {
                    JSONObject ev = events.getJSONObject(i);
                    String time = ev.optString("timestamp");
                    String category = ev.optString("category");
                    String action = ev.optString("event_type");
                    System.out.println((i + 1) + ". [" + time + "] " + category + " - " + action);
                }
            } else {
                System.out.println("No events found.");
            }
            System.out.println("====================================");

        } catch (Exception e) {
            System.err.println("Error fetching team events: " + e.getMessage());
        }
    }
}
