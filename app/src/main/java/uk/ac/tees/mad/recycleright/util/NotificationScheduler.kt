package uk.ac.tees.mad.recycleright.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import uk.ac.tees.mad.recycleright.R
import uk.ac.tees.mad.recycleright.data.model.Reminder
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class NotificationScheduler @Inject constructor(
    private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "recycleright_reminders"
        const val CHANNEL_NAME = "RecycleRight Reminders"
        const val WORK_TAG_PREFIX = "reminder_"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for bin days and eco challenges"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(reminder: Reminder) {
        val delay = calculateDelayUntilNextReminder(reminder)

        val data = workDataOf(
            "reminderId" to reminder.id,
            "title" to reminder.title,
            "description" to reminder.description
        )

        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            7, TimeUnit.DAYS // Repeat weekly
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("$WORK_TAG_PREFIX${reminder.id}")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "$WORK_TAG_PREFIX${reminder.id}",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelReminder(reminderId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag("$WORK_TAG_PREFIX$reminderId")
    }

    private fun calculateDelayUntilNextReminder(reminder: Reminder): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, convertDayToCalendar(reminder.dayOfWeek))
            set(Calendar.HOUR_OF_DAY, reminder.hourOfDay)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If target time is in the past, add 7 days
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 7)
        }

        return target.timeInMillis - now.timeInMillis
    }

    private fun convertDayToCalendar(dayOfWeek: Int): Int {
        // Convert our 1-7 (Mon-Sun) to Calendar's 2-1 (Mon=2, Sun=1)
        return when (dayOfWeek) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            7 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }
    }
}

// Add @HiltWorker annotation
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val reminderId = inputData.getString("reminderId") ?: return Result.failure()
        val title = inputData.getString("title") ?: "RecycleRight Reminder"
        val description = inputData.getString("description") ?: ""

        showNotification(reminderId, title, description)

        return Result.success()
    }

    private fun showNotification(reminderId: String, title: String, description: String) {
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)

        val notification = NotificationCompat.Builder(applicationContext, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.recycle_icon)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(reminderId.hashCode(), notification)
    }
}