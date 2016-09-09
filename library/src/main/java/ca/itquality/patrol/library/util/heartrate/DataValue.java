package ca.itquality.patrol.library.util.heartrate;

import com.google.gson.annotations.SerializedName;

public class DataValue {

    @SerializedName("time")
    private Long time;
    @SerializedName("value")
    private String value;

    public DataValue(Long time, String value) {
        this.time = time;
        this.value = value;
    }

    public Long getTime() {
        return time;
    }

    public String getValue() {
        return value;
    }
}
