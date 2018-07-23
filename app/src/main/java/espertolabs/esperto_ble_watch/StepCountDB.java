package espertolabs.esperto_ble_watch;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

@Database(entities = {StepCount.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class StepCountDB extends RoomDatabase {
    public abstract StepCountDAO StepCountDAO();
}