package espertolabs.esperto_ble_watch;

import java.util.Observable;
import java.util.Set;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIgnore;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.collect.Table;

//@DynamoDBTable(tableName="espertowatch-mobilehub-1699109079-HeartRate")
public class HeartRate extends Observable{

    private String username;
    private int currentHR;
    private Set<Integer> dailyHR;

    //@DynamoDBHashKey(attributeName = "username")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    //@DynamoDBAttribute(attributeName = "currentHR")
    public int getCurrentHR() {
        return currentHR;
    }

    public void setCurrentHR(int currentHR) {
        this.currentHR = currentHR;
        setChanged();
        notifyObservers();
    }

    //@DynamoDBAttribute(attributeName = "dailyHR")
    public Set<Integer> getDailyHR() {
        return dailyHR;
    }

    public void setDailyHR(Set<Integer> dailyHR) {
        this.dailyHR = dailyHR;
        setChanged();
        notifyObservers();
    }

}

