package espertolabs.esperto_ble_watch;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.util.Log;

import java.util.HashMap;
import java.util.Observable;

@Entity
public class HeartRate extends Observable{

    @PrimaryKey(autoGenerate = true)
    // Key for SQlite DB, do not modify
    private int uId;
    private String userId;
    private int currentHR;
    // Key: "YYYY-MM-dd kk", where kk is the current hour of the day from 01 to 24
    // Value: The average heart rate for that hour
    private HashMap<String, Float> avgHourlyHR;

    // For internal app use only, used to calculate the avg, not uploaded to server
    private HashMap<String, Integer> avgHourlyHRCount;

    HeartRate() {
        this.currentHR = 0;
        this.avgHourlyHR = new HashMap<>();
        this.avgHourlyHRCount = new HashMap<>();
    }

    public int getUId() {
        return uId;
    }

    public void setUId(int uId) {
        this.uId = uId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getCurrentHR() {
        return currentHR;
    }

    public void setCurrentHR(int currentHR) {
        this.currentHR = currentHR;
        setChanged();
        notifyObservers();
    }

    public HashMap<String, Integer> getAvgHourlyHRCount() {
        return avgHourlyHRCount;
    }

    public void setAvgHourlyHRCount(HashMap<String, Integer> avgHourlyHRCount) {
        this.avgHourlyHRCount = avgHourlyHRCount;
        setChanged();
        notifyObservers();
    }

    private void incrAvgHourlyHRCount(String formattedDateAndTime) {
        if (this.avgHourlyHRCount != null) {
            Integer heartRateCount = this.avgHourlyHRCount.get(formattedDateAndTime);
            Integer newHeartRateCount;

            if (heartRateCount != null) {
                // Avg count has already been recorded for this hour, increment
                newHeartRateCount = heartRateCount + 1;
            } else {
                // Avg count has not been recorded yet for this hour, this will be the initial value
                newHeartRateCount = 1;
            }

            this.avgHourlyHRCount.put(formattedDateAndTime, newHeartRateCount);
            setChanged();
            notifyObservers();
        }
    }

    public HashMap<String, Float> getAvgHourlyHR() {
        return avgHourlyHR;
    }

    public void setAvgHourlyHR(HashMap<String, Float> avgHourlyHR) {
        this.avgHourlyHR = avgHourlyHR;
        setChanged();
        notifyObservers();
    }

    public void addAvgHourlyHR(String formattedDateAndTime, Integer heartRate) {
        if (this.avgHourlyHR != null) {
            incrAvgHourlyHRCount(formattedDateAndTime);
            Float heartRateAvg = this.avgHourlyHR.get(formattedDateAndTime);
            Integer heartRateCount = this.avgHourlyHRCount.get(formattedDateAndTime);
            Float newHeartRateAvg;

            if (heartRateAvg != null) {
                // Avg has already been recorded for this hour, append to this avg
                float avgWeight = 1/(float) heartRateCount;
                newHeartRateAvg = (heartRateAvg * (1-avgWeight)) + (heartRate * (avgWeight));
            } else {
                // Avg has not been recorded yet for this hour, this will be the initial value
                newHeartRateAvg = (float) heartRate;
            }

            this.avgHourlyHR.put(formattedDateAndTime, newHeartRateAvg);
            setChanged();
            notifyObservers();
        }
    }

}

