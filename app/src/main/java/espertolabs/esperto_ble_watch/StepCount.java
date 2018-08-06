package espertolabs.esperto_ble_watch;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

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
    // Key: "dd-MM-YYYY"
    // Value: The total steps for that day
    private HashMap<String, Integer> totalDailySteps;

    StepCount() {
        this.currentSteps = 0;
        this.totalDailySteps = new HashMap<>();
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

    public void addCurrentSteps(int currentSteps) {
        this.currentSteps += currentSteps;
        setChanged();
        notifyObservers();
    }

    public HashMap<String, Integer> getTotalDailySteps() {
        return totalDailySteps;
    }

    public void setTotalDailySteps(HashMap<String, Integer> totalDailySteps) {
        this.totalDailySteps = totalDailySteps;
        setChanged();
        notifyObservers();
    }

    public void addTotalDailySteps(String formattedDateAndTime, Integer steps) {
        if (this.totalDailySteps != null) {
//            Integer totalDailySteps = this.totalDailySteps.get(formattedDateAndTime);
            Integer newTotalDailySteps;

//            if (totalDailySteps != null) {
//                // Total has already been recorded for this day, append to this
//                newTotalDailySteps = totalDailySteps + steps;
//            } else {
//                // Total has not been recorded yet for this day, this will be the initial value
//                newTotalDailySteps = steps;
//            }

            newTotalDailySteps = steps;

            this.totalDailySteps.put(formattedDateAndTime, newTotalDailySteps);
            setChanged();
            notifyObservers();
        }
    }

}

