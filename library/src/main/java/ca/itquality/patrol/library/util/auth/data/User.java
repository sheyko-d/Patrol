package ca.itquality.patrol.library.util.auth.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class User {

    @SerializedName("token")
    private String token;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("assigned_shifts")
    private ArrayList<AssignedShift> assignedShifts;
    @SerializedName("assigned_object")
    private AssignedObject assignedObject;
    @SerializedName("name")
    private String name;
    @SerializedName("email")
    private String email;
    @SerializedName("photo")
    private String photo;

    public User(String token, String userId, ArrayList<AssignedShift> assignedShifts,
                AssignedObject assignedObject, String name,
                String email, String photo) {
        this.token = token;
        this.assignedObject = assignedObject;
        this.userId = userId;
        this.assignedShifts = assignedShifts;
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

    public ArrayList<AssignedShift> getAssignedShifts() {
        return assignedShifts;
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

    public User setAssignedObject(AssignedObject assignedObject) {
        this.assignedObject = assignedObject;
        return this;
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
        @SerializedName("contacts")
        private String contacts;
        @SerializedName("safety")
        private String safety;
        @SerializedName("video")
        private String video;
        @SerializedName("sitting_duration")
        private int sitting_duration;

        public AssignedObject(String assignedObjectId, String title, Float latitude,
                              Float longitude, String contacts, String safety, String video,
                              int sitting_duration) {
            this.assignedObjectId = assignedObjectId;
            this.title = title;
            this.latitude = latitude;
            this.longitude = longitude;
            this.contacts = contacts;
            this.safety = safety;
            this.video = video;
            this.sitting_duration = sitting_duration;
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

        public String getContacts() {
            return contacts;
        }

        public String getSafety() {
            return safety;
        }

        public String getVideo() {
            return video;
        }

        public int getSittingDuration() {
            return sitting_duration;
        }
    }

    public class AssignedShift {

        @SerializedName("assigned_shift_id")
        private String assignedShiftId;
        @SerializedName("name")
        private String name;
        @SerializedName("start_time")
        private Long startTime;
        @SerializedName("end_time")
        private Long endTime;

        public AssignedShift(String assignedShiftId, String assignedObjectId, String name,
                             Long startTime, Long endTime) {
            this.assignedShiftId = assignedShiftId;
            this.name = name;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String getAssignedShiftId() {
            return assignedShiftId;
        }

        public String getName() {
            return name;
        }

        public Long getStartTime() {
            return startTime;
        }

        public Long getEndTime() {
            return endTime;
        }
    }
}
