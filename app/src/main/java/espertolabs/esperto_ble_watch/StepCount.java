package espertolabs.esperto_ble_watch;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Observable;
import java.util.List;

@Entity
public class StepCount extends Observable{

    @PrimaryKey(autoGenerate = true)
    // Key for SQlite DB, do not modify
    private int uId;

    private String userId;
    private int currentSteps;
    private int lastSetUnixTimeSeconds;
    // Key: The Unix time in seconds as a string (stored as a string on DynamoDB)
    // Value: The cumulative steps for that day, resets at midnight
    private HashMap<String, Integer> stepsMap;
    // Buffer to keep step values before sending and clearing
    private HashMap<String, Integer> stepsMapBuffer;

    StepCount() {
        this.userId = "";
        this.currentSteps = 0;
        this.lastSetUnixTimeSeconds = 0;
        this.stepsMap = new HashMap<>();
        this.stepsMapBuffer = new HashMap<>();
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

    public int getCurrentSteps() {
        return currentSteps;
    }

    public void setCurrentSteps(int currentSteps) {
        this.currentSteps = currentSteps;
        setChanged();
        notifyObservers();
    }

    public int getLastSetUnixTimeSeconds() {
        return lastSetUnixTimeSeconds;
    }

    public void setLastSetUnixTimeSeconds(int lastSetUnixTimeSeconds) {
        this.lastSetUnixTimeSeconds = lastSetUnixTimeSeconds;
        setChanged();
        notifyObservers();
    }

    public HashMap<String, Integer> getStepsMap() {
        return stepsMap;
    }

    public void setStepsMap(HashMap<String, Integer> stepsMap) {
        if (stepsMap != null) {
            this.stepsMap = stepsMap;
            setChanged();
            notifyObservers();
        }
    }

    public void addStepsMap(String unixTimeSeconds, Integer steps) {
        Calendar midnight = Calendar.getInstance();

        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);

        int midnightUnixTimeSeconds = (int) (midnight.getTimeInMillis() / 1000L);
        Integer newStepCount;

        // Check if watch has been restarted as the watch's internal step count for the day will
        // also have been reset
        // Also account for the expected step count reset at midnight of everyday
        if (steps < this.currentSteps && lastSetUnixTimeSeconds > midnightUnixTimeSeconds) {
            newStepCount = this.currentSteps + steps;
        } else {
            newStepCount = steps;
        }

        this.stepsMap.put(unixTimeSeconds, newStepCount);
        this.stepsMapBuffer.put(unixTimeSeconds, newStepCount);
        this.currentSteps = newStepCount;
        this.lastSetUnixTimeSeconds = Integer.parseInt(unixTimeSeconds);
        setChanged();
        notifyObservers();
    }

    public HashMap<String, Integer> getStepsMapBuffer() {
        return stepsMapBuffer;
    }

    public void setStepsMapBuffer(HashMap<String, Integer> stepsMapBuffer) {
        if (stepsMapBuffer != null) {
            this.stepsMapBuffer = stepsMapBuffer;
            setChanged();
            notifyObservers();
        }
    }

}

