package uk.ac.tees.mad.recycleright.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import uk.ac.tees.mad.recycleright.data.local.RecyclableItemDao
import uk.ac.tees.mad.recycleright.data.mapper.RecyclabilityMapper
import uk.ac.tees.mad.recycleright.data.model.RecyclableItem
import uk.ac.tees.mad.recycleright.data.model.RecycleCategory
import uk.ac.tees.mad.recycleright.data.remote.OpenFoodFactsApiService
import javax.inject.Inject

class RecyclableItemRepository @Inject constructor(
    private val dao: RecyclableItemDao,
    private val firestore: FirebaseFirestore,
    private val openFoodFactsApi: OpenFoodFactsApiService,
    private val context: Context
) {

    // SEARCH - Always returns Room Flow (offline-first)
    fun searchItems(query: String): Flow<List<RecyclableItem>> {
        return if (query.isBlank()) {
            dao.getAllItems()
        } else {
            dao.searchItems(query)
        }
    }

    // GET BY BARCODE - Three-tier approach: Room → OpenFoodFacts → Firestore
    suspend fun getItemByBarcode(barcode: String): Result<RecyclableItem> {
        // TIER 1: Check Room first (instant, offline-friendly)
        dao.getItemByBarcode(barcode)?.let {
            return Result.success(it)
        }

        // No internet? Return failure early
        if (!isNetworkAvailable()) {
            return Result.failure(
                Exception("No internet connection and item not found in local cache")
            )
        }

        // TIER 2: Try OpenFoodFacts API (best data source)
        try {
            val apiResponse = openFoodFactsApi.getProductByBarcode(barcode)

            if (apiResponse.isSuccessful && apiResponse.body()?.status == 1) {
                val item = RecyclabilityMapper.mapToRecyclableItem(
                    apiResponse.body()!!,
                    barcode
                )

                item?.let {
                    // Save to Room (source of truth)
                    dao.insertItem(it)

                    // Background sync to Firestore (fire and forget)
                    syncItemToFirestore(it)

                    return Result.success(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Continue to Firestore fallback
        }

        // TIER 3: Fallback to Firestore (community data)
        return fetchFromFirestore(barcode)
    }

    // FIRESTORE SYNC - Background operation, doesn't block UI
    private suspend fun syncItemToFirestore(item: RecyclableItem) {
        try {
            firestore.collection("recyclable_items")
                .document(item.id)
                .set(item)
                .await()
        } catch (e: Exception) {
            // Silent fail - Room is our source of truth
            e.printStackTrace()
        }
    }

    private suspend fun fetchFromFirestore(barcode: String): Result<RecyclableItem> {
        return try {
            val snapshot = firestore.collection("recyclable_items")
                .whereEqualTo("barcode", barcode)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val item = snapshot.documents[0].toObject(RecyclableItem::class.java)
                item?.let {
                    // Save to Room immediately
                    val roomItem = it.copy(id = snapshot.documents[0].id)
                    dao.insertItem(roomItem)
                    Result.success(roomItem)
                } ?: Result.failure(Exception("Invalid data from Firestore"))
            } else {
                Result.failure(Exception("Product not found: $barcode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // SYNC FROM FIRESTORE - Pull community data when online
    suspend fun syncItemsFromFirestore() {
        if (!isNetworkAvailable()) return

        try {
            val snapshot = firestore.collection("recyclable_items")
                .get()
                .await()

            val items = snapshot.documents.mapNotNull { doc ->
                doc.toObject(RecyclableItem::class.java)?.copy(id = doc.id)
            }

            if (items.isNotEmpty()) {
                // IMPORTANT: Only insert items that don't exist locally
                // This prevents overwriting local favorites
                items.forEach { firestoreItem ->
                    val localItem = dao.getItemById(firestoreItem.id)
                    if (localItem == null) {
                        // Item doesn't exist locally, insert it
                        dao.insertItem(firestoreItem)
                    } else {
                        // Item exists - preserve local favorite status
                        // Only update if data is newer or different (excluding favorite)
                        if (firestoreItem.barcode != null && localItem.barcode == null) {
                            dao.updateItem(localItem.copy(
                                name = firestoreItem.name,
                                description = firestoreItem.description,
                                tips = firestoreItem.tips,
                                barcode = firestoreItem.barcode,
                                imageUrl = firestoreItem.imageUrl
                                // Keep local isFavorite value!
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // TOGGLE FAVORITE - Update Room immediately, sync to Firestore in background
    suspend fun toggleFavorite(item: RecyclableItem) {
        val updated = item.copy(isFavorite = !item.isFavorite)

        // Update Room first (instant UI feedback)
        dao.updateItem(updated)

        // Sync to Firestore in background
        if (isNetworkAvailable()) {
            try {
                firestore.collection("recyclable_items")
                    .document(item.id)
                    .set(updated) // Use set() instead of update() to ensure full object is saved
                    .await()
            } catch (e: Exception) {
                // If Firestore fails, Room still has correct state
                e.printStackTrace()
            }
        }
    }

    suspend fun getItemById(itemId: String): RecyclableItem? {
        return dao.getItemById(itemId)
    }

    // NEW: Observe item changes from Room
    fun observeItemById(itemId: String): Flow<RecyclableItem?> {
        return dao.observeItemById(itemId)
    }

    fun getFavoriteItems(): Flow<List<RecyclableItem>> {
        return dao.getFavoriteItems()
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // SEED DATABASE - Run once on first launch
    suspend fun seedDatabaseIfEmpty() {
        if (dao.getItemCount() == 0) {
            val sampleItems = listOf(
                RecyclableItem(
                    id = "sample_1",
                    name = "Plastic Bottle (PET)",
                    category = RecycleCategory.RECYCLABLE,
                    description = "PET plastic bottles can be recycled",
                    tips = "Remove caps and labels. Rinse before recycling.",
                    barcode = "5000112576009",
                    imageUrl = null,
                    isFavorite = false
                ),
                RecyclableItem(
                    id = "sample_2",
                    name = "Glass Jar",
                    category = RecycleCategory.RECYCLABLE,
                    description = "Glass jars are fully recyclable",
                    tips = "Remove lids and rinse thoroughly.",
                    barcode = "5000112576016",
                    imageUrl = null,
                    isFavorite = false
                ),
                RecyclableItem(
                    id = "sample_3",
                    name = "Pizza Box (Greasy)",
                    category = RecycleCategory.GENERAL_WASTE,
                    description = "Greasy pizza boxes cannot be recycled",
                    tips = "Remove clean cardboard for recycling. Greasy parts go in general waste.",
                    barcode = null,
                    imageUrl = null,
                    isFavorite = false
                ),
                RecyclableItem(
                    id = "sample_4",
                    name = "Banana Peel",
                    category = RecycleCategory.COMPOSTABLE,
                    description = "Fruit peels break down naturally",
                    tips = "Add to compost bin. Rich in potassium.",
                    barcode = null,
                    imageUrl = null,
                    isFavorite = false
                ),
                RecyclableItem(
                    id = "sample_5",
                    name = "Aluminum Can",
                    category = RecycleCategory.RECYCLABLE,
                    description = "Aluminum cans are highly recyclable",
                    tips = "Rinse and crush to save space.",
                    barcode = "5000112576023",
                    imageUrl = null,
                    isFavorite = false
                )
            )

            dao.insertItems(sampleItems)
        }
    }
}