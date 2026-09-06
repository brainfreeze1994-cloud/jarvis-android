package com.jarvis.ai;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room Entity caching AI query responses for instant offline access and fast local recall.
 */
@Entity(tableName = "ai_response_cache", indices = {@Index(value = {"queryKey"}, unique = true)})
public class AiResponseCacheEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String queryKey;       // Normalized query key for matching (lowercase, alphanumeric)
    public String originalPrompt; // Full user prompt
    public String responseText;   // Cached response from AI
    public String emotion;        // Emotion tag (e.g. "calm", "proud", "focused")
    public String intentType;     // Intent classification (chat, vision, search, etc.)
    public String imageUrl;       // URL if image was generated
    public long cachedAt;         // Epoch millis when response was cached
    public int hitCount;          // Times this cached response was reused offline

    public AiResponseCacheEntity() {
        this.cachedAt = System.currentTimeMillis();
        this.hitCount = 0;
    }

    public AiResponseCacheEntity(String queryKey, String originalPrompt, String responseText,
                                 String emotion, String intentType, String imageUrl) {
        this.queryKey = queryKey;
        this.originalPrompt = originalPrompt;
        this.responseText = responseText;
        this.emotion = emotion;
        this.intentType = intentType;
        this.imageUrl = imageUrl;
        this.cachedAt = System.currentTimeMillis();
        this.hitCount = 0;
    }
}
