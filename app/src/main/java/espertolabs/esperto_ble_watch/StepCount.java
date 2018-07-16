package espertolabs.esperto_ble_watch;

import java.util.HashMap;
import java.util.Observable;
import java.util.List;


public class StepCount extends Observable{

    private String username;
    private int currentSteps;
    private List<Integer> dailySteps;
    private HashMap<String, Integer> totalDailySteps;

    //totalDailySteps is what we will actually be using (since our dynamoDB tables use a hashmap)
    //just keeping dailySteps cause all of the graphs currently depend on it

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getCurrentSteps() {
        return currentSteps;
    }

    public void setCurrentSteps(int currentSteps) {
        this.currentSteps = currentSteps;
        setChanged();
        notifyObservers();
    }

    public List<Integer> getDailySteps() {
        return dailySteps;
    }

    public void setDailySteps(List<Integer> dailySteps) {
        this.dailySteps = dailySteps;
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

}

