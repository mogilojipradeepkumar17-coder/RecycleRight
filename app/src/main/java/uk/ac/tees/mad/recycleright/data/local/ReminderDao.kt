package uk.ac.tees.mad.recycleright.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import uk.ac.tees.mad.recycleright.data.model.Reminder

@Dao
interface ReminderDao{

    @Query("select * from reminders order by dayOfWeek ASC, hourOfDay ASC, minute ASC")
    fun getAllReminders(): Flow<List<Reminder>>


    // can be null
    @Query("select * from reminders where id=:reminderId")
    suspend fun getReminderById(reminderId:String): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(remainder: Reminder)

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: String)

    @Query("SELECT COUNT(*) FROM reminders")
    suspend fun getReminderCount(): Int
}