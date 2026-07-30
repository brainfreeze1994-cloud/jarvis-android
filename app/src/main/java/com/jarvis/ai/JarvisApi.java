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
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    public interface Callback {
        void onSuccess(String reply, String imageUrl);
        void onError(String error);
    }

    public static void ask(List<HistoryItem> history, Callback cb) {
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
                body.put("messages", messages);

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
