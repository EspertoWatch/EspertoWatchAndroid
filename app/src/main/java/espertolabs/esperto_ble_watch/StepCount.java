package espertolabs.esperto_ble_watch;

import java.util.Set;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIgnore;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.collect.Table;

@DynamoDBTable(tableName="espertowatch-mobilehub-1699109079-StepCount")
public class StepCount {

    private String username;
    private int currentSteps;
    private Set<Integer> dailySteps;

    @DynamoDBHashKey(attributeName = "username")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @DynamoDBAttribute(attributeName = "currentSteps")
    public int getCurrentSteps() {
        return currentSteps;
    }

    public void setCurrentSteps(int currentSteps) {
        this.currentSteps = currentSteps;
    }

    @DynamoDBAttribute(attributeName = "dailySteps")
    public Set<Integer> getDailySteps() {
        return dailySteps;
    }

    public void setDailySteps(Set<Integer> dailySteps) {
        this.dailySteps = dailySteps;
    }

}

