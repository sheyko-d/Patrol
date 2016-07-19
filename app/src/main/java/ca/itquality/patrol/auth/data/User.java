package ca.itquality.patrol.auth.data;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("user_id")
    private String userId;
    @SerializedName("name")
    private String name;
    @SerializedName("email")
    private String email;
    @SerializedName("photo")
    private String photo;

    public User(String userId, String name, String email, String photo) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.photo = photo;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoto() {
        return photo;
    }
}
