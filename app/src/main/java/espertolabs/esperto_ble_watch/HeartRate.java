package espertolabs.esperto_ble_watch;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.HashMap;
import java.util.Observable;
import java.util.List;

@Entity
public class HeartRate extends Observable{

    @PrimaryKey(autoGenerate = true)
    // Key for SQlite DB, do not modify
    private int uId;

    private String userId;
    private int currentHR;
    private List<Integer> dailyHR;
    private HashMap<String, Integer> avgDailyHR;

    //avgDailyHR is what we will actually be using (since our dynamoDB tables use a hashmap)
    //just keeping dailyHR cause all of the graphs currently depend on it

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

    public List<Integer> getDailyHR() {
        return dailyHR;
    }

    public void setDailyHR(List<Integer> dailyHR) {
        this.dailyHR = dailyHR;
        setChanged();
        notifyObservers();
    }

    public void appendDailyHR(int heartRate) {
        List<Integer> tempDailyHR = dailyHR;
        tempDailyHR.add(heartRate);
        this.dailyHR = tempDailyHR;
        setChanged();
        notifyObservers();
    }

    public HashMap<String, Integer> getAvgDailyHR() {
        return avgDailyHR;
    }

    public void setAvgDailyHR(HashMap<String, Integer> avgDailyHR) {
        this.avgDailyHR = avgDailyHR;
        setChanged();
        notifyObservers();
    }

}

