package uk.ac.tees.mad.recycleright.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uk.ac.tees.mad.recycleright.data.model.RecyclableItem
import uk.ac.tees.mad.recycleright.data.repository.RecyclableItemRepository
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val items: List<RecyclableItem>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: RecyclableItemRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _scanningState = MutableStateFlow<ScanningState>(ScanningState.Idle)
    val scanningState: StateFlow<ScanningState> = _scanningState.asStateFlow()

    init {
        // Seed database on first launch
        seedDatabaseIfNeeded()

        // Background sync from Firestore
        syncFromFirestore()

        // Start observing items from Room
        observeItems()
    }

    private fun observeItems() {
        viewModelScope.launch {
            repository.searchItems("")
                .catch { e ->
                    _uiState.value = HomeUiState.Error(
                        e.message ?: "Failed to load items"
                    )
                }
                .collect { items ->
                    _uiState.value = if (items.isEmpty()) {
                        HomeUiState.Error("No items available yet. Try scanning a barcode!")
                    } else {
                        HomeUiState.Success(items)
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        performSearch(query)
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            repository.searchItems(query)
                .catch { e ->
                    _uiState.value = HomeUiState.Error(
                        e.message ?: "Search failed"
                    )
                }
                .collect { items ->
                    _uiState.value = if (items.isEmpty()) {
                        if (query.isBlank()) {
                            HomeUiState.Error("No items in database")
                        } else {
                            HomeUiState.Error("No results for '$query'")
                        }
                    } else {
                        HomeUiState.Success(items)
                    }
                }
        }
    }

    fun searchByBarcode(barcode: String) {
        viewModelScope.launch {
            _scanningState.value = ScanningState.Scanning

            try {
                val result = repository.getItemByBarcode(barcode)

                result.onSuccess { item ->
                    _scanningState.value = ScanningState.Success(item)
                    // Clear search to show all items including the new one
                    _searchQuery.value = ""
                }.onFailure { error ->
                    _scanningState.value = ScanningState.Error(
                        error.message ?: "Failed to scan barcode"
                    )
                }
            } catch (e: Exception) {
                _scanningState.value = ScanningState.Error(
                    e.message ?: "Unexpected error"
                )
            }
        }
    }

    fun clearScanningState() {
        _scanningState.value = ScanningState.Idle
    }

    fun toggleFavorite(item: RecyclableItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
            // Room Flow will automatically update the UI
        }
    }

    private fun syncFromFirestore() {
        viewModelScope.launch {
            try {
                repository.syncItemsFromFirestore()
            } catch (e: Exception) {
                // Silent fail - local data is fine
                e.printStackTrace()
            }
        }
    }

    private fun seedDatabaseIfNeeded() {
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        performSearch("")
    }
}

// Separate state for barcode scanning
sealed class ScanningState {
    object Idle : ScanningState()
    object Scanning : ScanningState()
    data class Success(val item: RecyclableItem) : ScanningState()
    data class Error(val message: String) : ScanningState()
}