package com.jarvis.ai;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

/**
 * Data Access Object for Room Chat Messages.
 */
@Dao
public interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ChatMessageEntity message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ChatMessageEntity> messages);

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    List<ChatMessageEntity> getAllMessages();

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    List<ChatMessageEntity> getRecentMessages(int limit);

    @Query("SELECT * FROM chat_messages WHERE text LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    List<ChatMessageEntity> searchMessages(String query);

    @Query("DELETE FROM chat_messages WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM chat_messages")
    void clearAll();

    @Query("SELECT COUNT(*) FROM chat_messages")
    int getMessageCount();
}
