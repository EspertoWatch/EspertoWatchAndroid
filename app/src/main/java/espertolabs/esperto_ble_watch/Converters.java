package espertolabs.esperto_ble_watch;

import android.arch.persistence.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

public class Converters {
    @TypeConverter
    public static HashMap<String, Integer> intMapFromString(String value) {
        Type listType = new TypeToken<HashMap<String, Integer>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromIntMap(HashMap<String, Integer> list) {
        Gson gson = new Gson();
        String json = gson.toJson(list);
        return json;
    }

    @TypeConverter
    public static HashMap<String, Float> floatMapFromString(String value) {
        Type mapType = new TypeToken<HashMap<String, Float>>() {}.getType();
        return new Gson().fromJson(value, mapType);
    }

    @TypeConverter
    public static String fromFloatMap(HashMap<String, Float> map) {
        Gson gson = new Gson();
        String json = gson.toJson(map);
        return json;
    }
}