package ca.itquality.patrol.messages.data;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class MessageThread {

    @SerializedName("thread_id")
    private String id;
    @Nullable
    @SerializedName("title")
    private String title;
    @Nullable
    @SerializedName("photo")
    private String photo;
    @SerializedName("last_message")
    private Message lastMessage;

    public MessageThread(String id, @Nullable String title, @Nullable String photo,
                         Message lastMessage) {
        this.id = id;
        this.title = title;
        this.photo = photo;
        this.lastMessage = lastMessage;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getPhoto() {
        return photo;
    }

    public Message getLastMessage() {
        return lastMessage;
    }
}
