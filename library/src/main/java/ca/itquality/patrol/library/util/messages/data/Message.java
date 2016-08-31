package ca.itquality.patrol.library.util.messages.data;

import com.google.gson.annotations.SerializedName;

public class Message {

    @SerializedName("message_id")
    private String id;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("user_name")
    private String userName;
    @SerializedName("user_photo")
    private String userPhoto;
    @SerializedName("text")
    private String text;
    @SerializedName("time")
    private Long time;
    private boolean pending;

    public Message(String id, String userId, String userName, String userPhoto, String text,
                   Long time, boolean pending) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userPhoto = userPhoto;
        this.text = text;
        this.time = time;
        this.pending = pending;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserPhoto() {
        return userPhoto;
    }

    public String getText() {
        return text;
    }

    public Long getTime() {
        return time;
    }

    public Boolean isPending() {
        return pending;
    }
}
