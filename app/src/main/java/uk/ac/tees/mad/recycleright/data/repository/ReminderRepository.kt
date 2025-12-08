package uk.ac.tees.mad.recycleright.data.repository

import kotlinx.coroutines.flow.Flow
import uk.ac.tees.mad.recycleright.data.local.ReminderDao
import uk.ac.tees.mad.recycleright.data.model.Reminder
import javax.inject.Inject

class ReminderRepository  @Inject constructor(
    private val dao: ReminderDao
){

    fun getAllReminders(): Flow<List<Reminder>>{
        return dao.getAllReminders()
    }

    suspend fun getReminderById(reminderId:String): Reminder?{
        return dao.getReminderById(reminderId)
    }

    suspend fun addReminder(reminder: Reminder){
        dao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: Reminder){
        dao.updateReminder(reminder)
    }

    suspend fun deleteReminder(reminder: Reminder){
        dao.deleteReminder(reminder)
    }


    suspend fun toggleReminderEnabled(reminder: Reminder){
        val updated=reminder.copy(isEnabled = !reminder.isEnabled)
        dao.updateReminder(updated)
    }


    suspend fun clearAllReminders(){
        dao.clearAllReminders()
    }

}