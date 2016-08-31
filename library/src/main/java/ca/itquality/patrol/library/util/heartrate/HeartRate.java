package ca.itquality.patrol.library.util.heartrate;

import com.google.gson.annotations.SerializedName;

public class HeartRate {

    @SerializedName("time")
    private Long time;
    @SerializedName("value")
    private Integer value;

    public HeartRate(Long time, Integer value) {
        this.time = time;
        this.value = value;
    }

    public Long getTime() {
        return time;
    }

    public Integer getValue() {
        return value;
    }
}
