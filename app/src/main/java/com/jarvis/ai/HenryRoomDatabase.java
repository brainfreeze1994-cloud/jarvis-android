package com.jarvis.ai;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room Database for persistent local caching of chat history and AI responses.
 */
@Database(entities = {ChatMessageEntity.class, AiResponseCacheEntity.class}, version = 1, exportSchema = false)
public abstract class HenryRoomDatabase extends RoomDatabase {

    private static volatile HenryRoomDatabase INSTANCE;

    public abstract ChatMessageDao chatMessageDao();
    public abstract AiResponseCacheDao aiResponseCacheDao();

    public static HenryRoomDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (HenryRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        HenryRoomDatabase.class,
                        "henry_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
