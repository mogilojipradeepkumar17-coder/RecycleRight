package uk.ac.tees.mad.recycleright.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "reminders")
data class Reminder(

    @PrimaryKey
    val id:String= UUID.randomUUID().toString(),
    val title:String="",
    val description:String="",
    val dayOfWeek:Int,   // 1->Monday ,7->Sunday
    val hourOfDay:Int, //0-23
    val minute:Int,
    val isEnabled:Boolean=true,
    val createdAt: Long=System.currentTimeMillis()
){

    fun getTimeString(): String {
        val hour12 = if (hourOfDay == 0) 12 else if (hourOfDay > 12) hourOfDay - 12 else hourOfDay
        val amPm = if (hourOfDay < 12) "AM" else "PM"
        return String.format("%02d:%02d %s", hour12, minute, amPm)
    }

    fun getDayString(): String {
        return when (dayOfWeek) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Unknown"
        }
    }

}
