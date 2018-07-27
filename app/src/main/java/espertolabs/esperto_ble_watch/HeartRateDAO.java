package espertolabs.esperto_ble_watch;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface HeartRateDAO {
    @Insert(onConflict = REPLACE)
    void insertHeartRate(HeartRate HR);

    @Query("SELECT * FROM HeartRate")
    List<HeartRate> getAllHeartRates();

    @Query("SELECT * FROM HeartRate WHERE userId LIKE :username ")
    HeartRate getUserHeartRate(String username);
}
