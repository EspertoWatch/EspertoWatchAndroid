package espertolabs.esperto_ble_watch;

/**
 * Created by m2macmah on 1/22/2018.
 */

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */

//UPDATE FOR DEVICE
public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String GAP_SERVICE =
            "00001800-0000-1000-8000-00805f91b34fb";
    public static String GATT_SERVICE =
            "00001801-0000-1000-8000-00805f9b34fb";
    public static String DEVICE_NAME =
            "00002a00-0000-1000-8000-00805f9b34fb";
    public static String APPEARANCE =
            "00002a01-0000-1000-8000-00805f9b34fb";
    public static String PER_PREF_CONN_PARAMS =
            "00002a04-0000-1000-8000-00805f9b34fb";
    public static String BATTERY_SERVICE =
            "0000180f-0000-1000-8000-00805f9b34fb";
    public static String BATTERY_LEVEL =
            "00002a19-0000-1000-8000-00805f9b34fb";
    public static String HEALTH_THERMOMETER_SERVICE =
            "00001809-0000-1000-8000-00805f9b34fb";
    public static String TEMPERATURE_MEASUREMENT =
            "00002a1c-0000-1000-8000-00805f9b34fb";
    public static String HEART_RATE_MEASUREMENT =
            "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG =
            "00002902-0000-1000-8000-00805f9b34fb";
    public static String FORCE_SERVICE =
            "b6578a34-0a7c-11e7-93ae-92361f002671";
    public static String FORCE =
            "a3f9601f-f7df-4ada-86d9-8c468321f9db";
    public static String UPDATE_RATE =
            "f81620d6-7692-4ba5-867e-bb4d14d70023";
    public static String GATE_CONSTANTS =
            "2f05a252-0b76-4a48-9093-cc8407adbc37";
    public static String LED_SERVICE =
            "0000f010-0000-1000-8000-00805f9b34fb";
    public static String LED_STATUS =
            "0000f011-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put(GAP_SERVICE, "GAP Service");
        attributes.put(GATT_SERVICE, "GATT Service");
        attributes.put(BATTERY_SERVICE, "Battery Service");
        attributes.put(HEALTH_THERMOMETER_SERVICE, "Health Thermometer Service");
        attributes.put(FORCE_SERVICE, "Force Service");
        attributes.put(LED_SERVICE, "LED Service");
        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        attributes.put(DEVICE_NAME, "Device Name");
        attributes.put(APPEARANCE, "Appearance");
        attributes.put(PER_PREF_CONN_PARAMS, "Peripheral Preferred CXN Params");
        attributes.put(BATTERY_LEVEL, "Battery Level");
        attributes.put(TEMPERATURE_MEASUREMENT, "Temperature Measurement");
        attributes.put(FORCE, "Force Measurement");
        attributes.put(UPDATE_RATE, "Force Update Rate");
        attributes.put(GATE_CONSTANTS, "Gate Constants");
        attributes.put(LED_STATUS, "LED Status");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
