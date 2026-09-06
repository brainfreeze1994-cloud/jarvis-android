package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository pattern implementation for Room persistence.
 * Coordinates database queries, caching of AI responses,
 * offline fallback detection, and history synchronization.
 */
public class ChatPersistenceRepository {

    private static volatile ChatPersistenceRepository INSTANCE;

    private final HenryRoomDatabase database;
    private final ChatMessageDao chatMessageDao;
    private final AiResponseCacheDao aiResponseCacheDao;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Context appContext;

    public interface LoadCallback {
        void onLoaded(List<HistoryItem> historyItems, List<Message> messages);
    }

    public interface ResultCallback<T> {
        void onResult(T result);
    }

    private ChatPersistenceRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.database = HenryRoomDatabase.getInstance(appContext);
        this.chatMessageDao = database.chatMessageDao();
        this.aiResponseCacheDao = database.aiResponseCacheDao();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static ChatPersistenceRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ChatPersistenceRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ChatPersistenceRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    // ── Saving Messages ──────────────────────────────────────────────────────────

    public void saveMessage(Message message, String role, boolean isOffline) {
        if (message == null) return;
        executor.execute(() -> {
            try {
                ChatMessageEntity entity = ChatMessageEntity.fromMessage(message, role, isOffline);
                chatMessageDao.insert(entity);
            } catch (Exception ignored) {}
        });
    }

    public void saveUserMessage(String text) {
        if (text == null || text.trim().isEmpty()) return;
        saveMessage(new Message(Message.TYPE_USER, text), "user", false);
    }

    public void saveAiMessage(String text, boolean isOffline) {
        if (text == null || text.trim().isEmpty()) return;
        saveMessage(new Message(Message.TYPE_JARVIS, text), "model", isOffline);
    }

    // ── Caching AI Responses ───────────────────────────────────────────────────

    public void cacheAiResponse(String prompt, String responseText, String emotion,
                                String intentType, String imageUrl) {
        if (prompt == null || responseText == null || responseText.trim().isEmpty()) return;
        executor.execute(() -> {
            try {
                String key = normalizeKey(prompt);
                AiResponseCacheEntity cache = new AiResponseCacheEntity(
                    key, prompt.trim(), responseText.trim(), emotion, intentType, imageUrl
                );
                aiResponseCacheDao.insertOrUpdate(cache);
            } catch (Exception ignored) {}
        });
    }

    /**
     * Synchronous lookup for offline-first querying (run on background thread).
     */
    public AiResponseCacheEntity lookupCachedResponseSync(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) return null;
        try {
            String key = normalizeKey(prompt);
            // 1. Direct exact key match
            AiResponseCacheEntity exact = aiResponseCacheDao.findByQueryKey(key);
            if (exact != null) {
                aiResponseCacheDao.incrementHit(exact.queryKey);
                return exact;
            }

            // 2. Keyword fuzzy match if query is longer than 3 characters
            if (key.length() > 3) {
                AiResponseCacheEntity fuzzy = aiResponseCacheDao.findFuzzyMatch(key);
                if (fuzzy != null) {
                    aiResponseCacheDao.incrementHit(fuzzy.queryKey);
                    return fuzzy;
                }
            }

            // 3. Match against significant sub-terms (e.g. nouns or core topic)
            String[] tokens = key.split("\\s+");
            if (tokens.length >= 2) {
                for (String token : tokens) {
                    if (token.length() >= 4) {
                        AiResponseCacheEntity tokenMatch = aiResponseCacheDao.findFuzzyMatch(token);
                        if (tokenMatch != null) {
                            aiResponseCacheDao.incrementHit(tokenMatch.queryKey);
                            return tokenMatch;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public void findCachedResponse(String prompt, ResultCallback<AiResponseCacheEntity> callback) {
        executor.execute(() -> {
            AiResponseCacheEntity result = lookupCachedResponseSync(prompt);
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    // ── Loading and History Migration ──────────────────────────────────────────

    /**
     * Loads messages from Room. If Room is empty, migrates existing history from
     * SharedPreferences into Room database so past conversations are preserved.
     */
    public void loadHistory(int limit, LoadCallback callback) {
        executor.execute(() -> {
            List<HistoryItem> historyList = new ArrayList<>();
            List<Message> messageList = new ArrayList<>();

            try {
                int count = chatMessageDao.getMessageCount();
                if (count > 0) {
                    // Load from Room
                    List<ChatMessageEntity> entities = chatMessageDao.getRecentMessages(limit);
                    // Entities are ordered DESC by timestamp, reverse for chronological ASC
                    Collections.reverse(entities);
                    for (ChatMessageEntity entity : entities) {
                        historyList.add(new HistoryItem(entity.role, entity.text));
                        messageList.add(entity.toMessage());
                    }
                } else {
                    // Check SharedPreferences for legacy history to migrate into Room
                    SharedPreferences prefs = appContext.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE);
                    String legacyJson = prefs.getString("history_v2", null);
                    if (legacyJson != null && !legacyJson.isEmpty()) {
                        Gson gson = new Gson();
                        Type type = new TypeToken<List<HistoryItem>>(){}.getType();
                        List<HistoryItem> legacyItems = gson.fromJson(legacyJson, type);
                        if (legacyItems != null && !legacyItems.isEmpty()) {
                            List<ChatMessageEntity> entitiesToMigrate = new ArrayList<>();
                            long baseTime = System.currentTimeMillis() - (legacyItems.size() * 1000L);
                            for (int i = 0; i < legacyItems.size(); i++) {
                                HistoryItem item = legacyItems.get(i);
                                int msgType = "user".equals(item.role) ? Message.TYPE_USER : Message.TYPE_JARVIS;
                                ChatMessageEntity entity = new ChatMessageEntity(msgType, item.role, item.text, baseTime + (i * 1000L));
                                entitiesToMigrate.add(entity);
                                historyList.add(item);
                            }
                            chatMessageDao.insertAll(entitiesToMigrate);

                            // Load visible subset
                            int startIndex = Math.max(0, legacyItems.size() - 25);
                            for (int i = startIndex; i < legacyItems.size(); i++) {
                                HistoryItem it = legacyItems.get(i);
                                messageList.add(new Message("user".equals(it.role) ? Message.TYPE_USER : Message.TYPE_JARVIS, it.text));
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            mainHandler.post(() -> callback.onLoaded(historyList, messageList));
        });
    }

    /**
     * Batch syncs the current in-memory history items into Room if needed.
     */
    public void syncHistoryBatch(List<HistoryItem> items) {
        if (items == null || items.isEmpty()) return;
        executor.execute(() -> {
            try {
                // Ensure latest items are recorded
                long now = System.currentTimeMillis();
                List<ChatMessageEntity> batch = new ArrayList<>();
                int take = Math.min(items.size(), 20);
                int start = items.size() - take;
                for (int i = start; i < items.size(); i++) {
                    HistoryItem it = items.get(i);
                    int type = "user".equals(it.role) ? Message.TYPE_USER : Message.TYPE_JARVIS;
                    batch.add(new ChatMessageEntity(type, it.role, it.text, now - (take - i) * 500L));
                }
                // Save any unpersisted messages
                chatMessageDao.insertAll(batch);
            } catch (Exception ignored) {}
        });
    }

    // ── Search & Cache Management ──────────────────────────────────────────────

    public void searchChatMessages(String query, ResultCallback<List<ChatMessageEntity>> callback) {
        executor.execute(() -> {
            List<ChatMessageEntity> results = new ArrayList<>();
            try {
                results = chatMessageDao.searchMessages(query);
            } catch (Exception ignored) {}
            List<ChatMessageEntity> finalResults = results;
            mainHandler.post(() -> callback.onResult(finalResults));
        });
    }

    public void clearAll(Runnable onComplete) {
        executor.execute(() -> {
            try {
                chatMessageDao.clearAll();
                aiResponseCacheDao.clearAll();
            } catch (Exception ignored) {}
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
        });
    }

    // ── Network Utility ────────────────────────────────────────────────────────

    public boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork == null) return false;
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
                return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                );
            } else {
                android.net.NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        } catch (Exception e) {
            return false;
        }
    }

    // ── Key Normalization ──────────────────────────────────────────────────────

    public static String normalizeKey(String input) {
        if (input == null) return "";
        String s = input.trim().toLowerCase(Locale.US);

        // Remove punctuation
        s = s.replaceAll("[^a-z0-9\\s]", " ");

        // Remove conversational filler prefixes and keywords
        s = s.replaceAll("\\b(hey|hello|hi|ok|okay|henry|jarvis)\\b", " ");
        s = s.replaceAll("\\b(please|can you|could you|would you|tell me about|tell me|what is|what are|who is|who are|explain|how do I|how to)\\b", " ");

        // Collapse whitespace
        s = s.replaceAll("\\s+", " ").trim();
        return s.isEmpty() ? input.trim().toLowerCase(Locale.US) : s;
    }
}
