package ca.itquality.patrol.auth.data;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("token")
    private String token;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("assigned_object")
    private AssignedObject assignedObject;
    @SerializedName("name")
    private String name;
    @SerializedName("email")
    private String email;
    @SerializedName("photo")
    private String photo;

    public User(String token, String userId, AssignedObject assignedObject, String name,
                String email, String photo) {
        this.token = token;
        this.assignedObject = assignedObject;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.photo = photo;
    }

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public AssignedObject getAssignedObject() {
        return assignedObject;
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

    public class AssignedObject {
        @SerializedName("assigned_object_id")
        private String assignedObjectId;
        @SerializedName("title")
        private String title;
        @SerializedName("latitude")
        private Float latitude;
        @SerializedName("longitude")
        private Float longitude;

        public AssignedObject(String assignedObjectId, String title, Float latitude,
                              Float longitude) {
            this.assignedObjectId = assignedObjectId;
            this.title = title;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getAssignedObjectId() {
            return assignedObjectId;
        }

        public String getTitle() {
            return title;
        }

        public Float getLatitude() {
            return latitude;
        }

        public Float getLongitude() {
            return longitude;
        }
    }
}
