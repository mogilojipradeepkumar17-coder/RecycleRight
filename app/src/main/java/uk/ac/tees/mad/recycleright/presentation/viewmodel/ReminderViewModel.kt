package uk.ac.tees.mad.recycleright.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import uk.ac.tees.mad.recycleright.data.model.Reminder
import uk.ac.tees.mad.recycleright.data.repository.ReminderRepository
import uk.ac.tees.mad.recycleright.util.NotificationScheduler
import javax.inject.Inject

sealed class ReminderUiState {
    object Loading : ReminderUiState()
    data class Success(val reminders: List<Reminder>) : ReminderUiState()
    data class Error(val message: String) : ReminderUiState()
}

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: ReminderRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {
    private val _uiState = MutableStateFlow<ReminderUiState>(ReminderUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadReminders()
    }

    private fun loadReminders() {
        viewModelScope.launch {
            repository.getAllReminders()
                .catch { e ->
                    _uiState.value = ReminderUiState.Error(
                        e.message ?: "Failed to load"
                    )
                }
                .collect { reminders ->
                    _uiState.value = if (reminders.isEmpty()) {
                        ReminderUiState.Success(emptyList())
                    } else {
                        ReminderUiState.Success(reminders)
                    }
                }
        }
    }

    fun addReminder(reminder: Reminder){
        viewModelScope.launch {
            try{
                repository.addReminder(reminder)
                if(reminder.isEnabled){
                    notificationScheduler.scheduleReminder(reminder)
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }
    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                repository.updateReminder(reminder)
                notificationScheduler.cancelReminder(reminder.id)
                if (reminder.isEnabled) {
                    notificationScheduler.scheduleReminder(reminder)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                repository.deleteReminder(reminder)
                notificationScheduler.cancelReminder(reminder.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                repository.toggleReminderEnabled(reminder)
                if (!reminder.isEnabled) {
                    // Was enabled, now disabled - schedule notification
                    notificationScheduler.scheduleReminder(reminder.copy(isEnabled = true))
                } else {
                    // Was disabled, now enabled - cancel notification
                    notificationScheduler.cancelReminder(reminder.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}


