package espertolabs.esperto_ble_watch;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface StepCountDAO {
    @Insert(onConflict = REPLACE)
    void insertStepCount(StepCount SC);

    @Query("SELECT * FROM StepCount")
    List<StepCount> getAllStepCounts();

    @Query("SELECT * FROM StepCount WHERE userId LIKE :username ")
    StepCount getUserStepCount(String username);
}
