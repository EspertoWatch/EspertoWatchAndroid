package espertolabs.esperto_ble_watch;

import java.util.Set;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIgnore;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.collect.Table;

//@DynamoDBTable(tableName="espertowatch-mobilehub-1699109079-Accounts")
public class UserAccount {

    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String deviceAddress;
    private String goalPreference;

    //@DynamoDBHashKey(attributeName="username")
    public String getUsername() {return username; }
    public void setUsername(String username) { this.username = username; }

    //@DynamoDBAttribute(attributeName="password")
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    //@DynamoDBAttribute(attributeName="firstName")
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    //@DynamoDBAttribute(attributeName="lastName")
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    //@DynamoDBAttribute(attributeName="goalSetting")
    public String getGoalSetting() { return goalPreference; }
    public void setGoalSetting(String goalPreference) { this.goalPreference = goalPreference; }

    //@DynamoDBAttribute(attributeName="deviceAddress")
    public String getDeviceAddress() { return deviceAddress; }
    public void setDeviceAddress(String deviceAddress) { this.deviceAddress = deviceAddress; }

}