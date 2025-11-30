package uk.ac.tees.mad.recycleright.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import uk.ac.tees.mad.recycleright.data.model.RecyclableItem
import uk.ac.tees.mad.recycleright.data.repository.RecyclableItemRepository
import javax.inject.Inject

sealed class ItemDetailsUiState {
    object Loading : ItemDetailsUiState()
    data class Success(val item: RecyclableItem) : ItemDetailsUiState()
    data class Error(val message: String) : ItemDetailsUiState()
}

@HiltViewModel
class ItemDetailsViewModel @Inject constructor(
    private val repository: RecyclableItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ItemDetailsUiState>(ItemDetailsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // NEW: Observe item changes from Room
    fun loadItem(itemId: String) {
        viewModelScope.launch {
            _uiState.value = ItemDetailsUiState.Loading

            try {
                // Start observing the item from Room
                repository.observeItemById(itemId)
                    .catch { e ->
                        _uiState.value = ItemDetailsUiState.Error(
                            e.message ?: "Failed to load item"
                        )
                    }
                    .collect { item ->
                        if (item != null) {
                            _uiState.value = ItemDetailsUiState.Success(item)
                        } else {
                            _uiState.value = ItemDetailsUiState.Error("Item not found")
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = ItemDetailsUiState.Error(
                    e.message ?: "Failed to load item details"
                )
            }
        }
    }

    fun toggleFavorite(item: RecyclableItem) {
        viewModelScope.launch {
            try {
                // Just update Room - the Flow will automatically update the UI
                repository.toggleFavorite(item)
                // No need to manually update UI state - Flow handles it!
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}