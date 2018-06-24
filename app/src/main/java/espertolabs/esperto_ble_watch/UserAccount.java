package espertolabs.esperto_ble_watch;

import java.io.Serializable;
import java.util.Set;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIgnore;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.collect.Table;

//@DynamoDBTable(tableName="espertowatch-mobilehub-1699109079-Accounts")
public class UserAccount implements Serializable {

    private String userId;
    private String password;
    private String name;
    private String deviceAddress;
    private String handedness;
    private String weightUnit;
    private String heightUnit;
    private int birthDate;
    private int height;
    private int weight;
    private String gender;

    public String getUsername() {return userId; }
    public void setUsername(String username) { this.userId = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDeviceAddress() { return deviceAddress; }
    public void setDeviceAddress(String deviceAddress) { this.deviceAddress = deviceAddress; }

    public String getHandedness() {return handedness; }
    public void setHandedness(String handedness) { this.handedness = handedness; }

    public String getWeightUnit() {return weightUnit; }
    public void setWeightUnit(String weightUnit) { this.weightUnit = weightUnit; }

    public String getHeightUnit() {return heightUnit; }
    public void setHeightUnit(String heightUnit) { this.heightUnit = heightUnit; }

    public String getGender() {return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getWeight() {return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public int getHeight() {return height; }
    public void setHeight(int height) { this.height = height; }

    public int getBirthDate() {return birthDate; }
    public void setBirthDate(int birthDate) { this.birthDate = birthDate; }

}