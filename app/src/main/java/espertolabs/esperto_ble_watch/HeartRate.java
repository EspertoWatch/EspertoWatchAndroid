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
    // Key: The Unix time in seconds as a string (stored as a string on DynamoDB)
    // Value: The integer heart rate for that time
    private HashMap<String, Integer> HRMap;
    // Buffer to keep HR values before sending and clearing
    private HashMap<String, Integer> HRMapBuffer;

    HeartRate() {
        this.userId = "";
        this.currentHR = 0;
        this.HRMap = new HashMap<>();
        this.HRMapBuffer = new HashMap<>();
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

    public HashMap<String, Integer> getHRMap() {
        return HRMap;
    }

    public void setHRMap(HashMap<String, Integer> HRMap) {
        if (HRMap != null) {
            this.HRMap = HRMap;
            setChanged();
            notifyObservers();
        }
    }

    public void addHRMap(String unixTimeSeconds, Integer heartRate) {
        this.HRMap.put(unixTimeSeconds, heartRate);
        this.HRMapBuffer.put(unixTimeSeconds, heartRate);
        this.currentHR = heartRate;
        setChanged();
        notifyObservers();
    }

    public HashMap<String, Integer> getHRMapBuffer() {
        return HRMapBuffer;
    }

    public void setHRMapBuffer(HashMap<String, Integer> HRMapBuffer) {
        if (HRMapBuffer != null) {
            this.HRMapBuffer = HRMapBuffer;
            setChanged();
            notifyObservers();
        }
    }

}

