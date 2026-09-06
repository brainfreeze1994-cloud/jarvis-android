package com.jarvis.ai;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a persistent chat message (user, AI, image, or document).
 */
@Entity(tableName = "chat_messages")
public class ChatMessageEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public int type;              // Message.TYPE_USER, TYPE_JARVIS, TYPE_IMAGE, TYPE_URL_IMAGE, TYPE_FILE_CARD
    public String role;          // "user" or "model" / "henry"
    public String text;          // Message content text
    public long timestamp;       // Epoch time in millis
    public String imageUri;      // Local uri for TYPE_IMAGE
    public String imageUrl;      // Remote url for TYPE_URL_IMAGE
    public String filePath;      // Path for TYPE_FILE_CARD
    public String fileMimeType;  // Mime type
    public String fileTitle;     // Title of file
    public String fileDetails;   // Details/summary
    public String fileBadge;     // Metadata badge
    public String fileIcon;      // Emoji or icon
    public boolean isOffline;    // Whether answered or saved in offline mode

    public ChatMessageEntity() {
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessageEntity(int type, String role, String text, long timestamp) {
        this.type = type;
        this.role = role;
        this.text = text;
        this.timestamp = timestamp;
        this.isOffline = false;
    }

    public static ChatMessageEntity fromMessage(Message m, String role, boolean isOffline) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.type = m.type;
        entity.role = role != null ? role : (m.type == Message.TYPE_USER ? "user" : "model");
        entity.text = m.text;
        entity.imageUri = m.imageUri;
        entity.imageUrl = m.imageUrl;
        entity.filePath = m.filePath;
        entity.fileMimeType = m.fileMimeType;
        entity.fileTitle = m.fileTitle;
        entity.fileDetails = m.fileDetails;
        entity.fileBadge = m.fileBadge;
        entity.fileIcon = m.fileIcon;
        entity.timestamp = System.currentTimeMillis();
        entity.isOffline = isOffline;
        return entity;
    }

    public Message toMessage() {
        switch (type) {
            case Message.TYPE_IMAGE:
                return new Message(type, text, imageUri);
            case Message.TYPE_URL_IMAGE:
                return new Message(type, text, null, imageUrl);
            case Message.TYPE_FILE_CARD:
                return new Message(type, text, filePath, fileMimeType, fileTitle, fileDetails, fileBadge, fileIcon);
            default:
                return new Message(type, text);
        }
    }
}
