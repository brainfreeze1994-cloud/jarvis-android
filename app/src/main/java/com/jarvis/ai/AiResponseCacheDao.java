package com.jarvis.ai;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

/**
 * Data Access Object for Room AI response caching.
 */
@Dao
public interface AiResponseCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(AiResponseCacheEntity cache);

    @Query("SELECT * FROM ai_response_cache WHERE queryKey = :key LIMIT 1")
    AiResponseCacheEntity findByQueryKey(String key);

    @Query("SELECT * FROM ai_response_cache WHERE queryKey LIKE '%' || :keyword || '%' OR originalPrompt LIKE '%' || :keyword || '%' ORDER BY cachedAt DESC LIMIT 1")
    AiResponseCacheEntity findFuzzyMatch(String keyword);

    @Query("SELECT * FROM ai_response_cache ORDER BY cachedAt DESC LIMIT :limit")
    List<AiResponseCacheEntity> getRecentCaches(int limit);

    @Query("UPDATE ai_response_cache SET hitCount = hitCount + 1 WHERE queryKey = :key")
    void incrementHit(String key);

    @Query("SELECT COUNT(*) FROM ai_response_cache")
    int getCacheCount();

    @Query("DELETE FROM ai_response_cache")
    void clearAll();
}
