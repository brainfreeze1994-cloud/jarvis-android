package com.jarvis.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JarvisApi {

    private static final String API_URL = "https://jarvis-ai-seven-dun.vercel.app/api/jarvis";
    private static final MediaType JSON  = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    public interface Callback {
        void onSuccess(String reply, String imageUrl);
        void onError(String error);
    }

    /** Backward-compatible. */
    public static void ask(List<HistoryItem> history, String imageBase64, Callback cb) {
        ask(history, imageBase64, "balanced", null, cb);
    }

    /** With response mode only. */
    public static void ask(List<HistoryItem> history, String imageBase64,
                           String responseMode, Callback cb) {
        ask(history, imageBase64, responseMode, null, cb);
    }

    /**
     * Full call with user profile for personalisation.
     */
    public static void ask(List<HistoryItem> history, String imageBase64,
                           String responseMode, UserProfile profile, Callback cb) {
        new Thread(() -> {
            try {
                JSONArray messages = new JSONArray();
                for (HistoryItem item : history) {
                    JSONObject msg = new JSONObject();
                    msg.put("role", item.role);
                    msg.put("text", item.text);
                    messages.put(msg);
                }

                JSONObject body = new JSONObject();
                body.put("messages",     messages);
                body.put("responseMode", responseMode != null ? responseMode : "balanced");

                if (imageBase64 != null && !imageBase64.isEmpty())
                    body.put("imageBase64", imageBase64);

                // Send user profile if available
                if (profile != null && !profile.isEmpty())
                    body.put("userProfile", profile.toJson());

                RequestBody rb = RequestBody.create(body.toString(), JSON);
                Request req = new Request.Builder()
                    .url(API_URL)
                    .post(rb)
                    .addHeader("Content-Type", "application/json")
                    .build();

                try (Response resp = client.newCall(req).execute()) {
                    String bodyStr = resp.body() != null ? resp.body().string() : "";
                    if (!resp.isSuccessful()) { cb.onError("Server error " + resp.code()); return; }
                    JSONObject data = new JSONObject(bodyStr);
                    String reply    = data.optString("reply", "I have no response.");
                    String imageUrl = data.optString("imageUrl", null);
                    cb.onSuccess(reply, imageUrl);
                }
            } catch (Exception e) {
                cb.onError(e.getMessage() != null ? e.getMessage() : "Network error");
            }
        }).start();
    }
}
