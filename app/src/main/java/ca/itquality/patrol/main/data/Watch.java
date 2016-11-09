package ca.itquality.patrol.main.data;

import com.google.gson.annotations.SerializedName;

public class Watch {

    @SerializedName("id")
    private String id;
    @SerializedName("label")
    private String label;

    public Watch(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
