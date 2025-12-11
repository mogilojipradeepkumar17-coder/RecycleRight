package uk.ac.tees.mad.recycleright.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
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
    private val auth: FirebaseAuth,
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

    // GET BY BARCODE - Three-tier approach: Room → OpenFoodFacts → User's Firestore
//    suspend fun getItemByBarcode(barcode: String): Result<RecyclableItem> {
//        // TIER 1: Check Room first (instant, offline-friendly)
//        dao.getItemByBarcode(barcode)?.let {
//            return Result.success(it)
//        }
//
//        // No internet? Return failure early
//        if (!isNetworkAvailable()) {
//            return Result.failure(
//                Exception("No internet connection and item not found in local cache")
//            )
//        }
//
//        // TIER 2: Try OpenFoodFacts API (best data source)
//        try {
//
//            Log.d("api", "getItemByBarcode: $barcode ")
//
//            val apiResponse = openFoodFactsApi.getProductByBarcode(barcode)
//
//            if (apiResponse.isSuccessful && apiResponse.body()?.status == 1) {
//                val item = RecyclabilityMapper.mapToRecyclableItem(
//                    apiResponse.body()!!,
//                    barcode
//                )
//
//                Log.d("api", "getItemByBarcode: $item ")
//                item?.let {
//                    // Save to Room (source of truth)
//                    dao.insertItem(it)
//
//                    // Background sync to user's Firestore collection
//                    syncItemToFirestore(it)
//
//                    return Result.success(it)
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            // Continue to Firestore fallback
//        }
//
//        // TIER 3: Fallback to user's Firestore collection
//        return fetchFromUserFirestore(barcode)
//    }

    // GET BY BARCODE - Three-tier approach: Room → OpenFoodFacts → User's Firestore
    suspend fun getItemByBarcode(barcode: String): Result<RecyclableItem> {
        Log.d("RecycleRight", "🔍 Searching for barcode: $barcode")

        // TIER 1: Check Room first (instant, offline-friendly)
        dao.getItemByBarcode(barcode)?.let {
            Log.d("RecycleRight", "✅ Found in Room: ${it.name}")
            return Result.success(it)
        }

        // No internet? Return failure early
        if (!isNetworkAvailable()) {
            Log.w("RecycleRight", "⚠️ No internet connection")
            return Result.failure(
                Exception("No internet connection and item not found in local cache")
            )
        }

        // TIER 2: Try OpenFoodFacts API (best data source)
        try {
            Log.d("RecycleRight", "🌐 Calling OpenFoodFacts API for barcode: $barcode")

            val apiResponse = openFoodFactsApi.getProductByBarcode(barcode)

            Log.d("RecycleRight", "📡 API Response Code: ${apiResponse.code()}")
            Log.d("RecycleRight", "📡 API Response Success: ${apiResponse.isSuccessful}")
            Log.d("RecycleRight", "📡 API Response Body: ${apiResponse.body()}")
            Log.d("RecycleRight", "📡 API Response Status: ${apiResponse.body()?.status}")

            if (apiResponse.isSuccessful && apiResponse.body()?.status == 1) {
                Log.d("RecycleRight", "✅ Product found in API")

                val item = RecyclabilityMapper.mapToRecyclableItem(
                    apiResponse.body()!!,
                    barcode
                )

                Log.d("RecycleRight", "📦 Mapped item: $item")

                item?.let {
                    // Save to Room (source of truth)
                    dao.insertItem(it)
                    Log.d("RecycleRight", "💾 Saved to Room: ${it.name}")

                    // Background sync to user's Firestore collection
                    syncItemToFirestore(it)

                    return Result.success(it)
                } ?: run {
                    Log.e("RecycleRight", "❌ Mapping returned null")
                }
            } else {
                Log.w("RecycleRight", "⚠️ API returned unsuccessful or status != 1")
                Log.w("RecycleRight", "⚠️ Status code: ${apiResponse.body()?.status}")
                Log.w("RecycleRight", "⚠️ Error body: ${apiResponse.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("RecycleRight", "❌ API Exception: ${e.message}", e)
            e.printStackTrace()
            // Continue to Firestore fallback
        }

        // TIER 3: Fallback to user's Firestore collection
        Log.d("RecycleRight", "☁️ Trying Firestore fallback")
        return fetchFromUserFirestore(barcode)
    }


    // FIRESTORE SYNC Save to user's collection
    private suspend fun syncItemToFirestore(item: RecyclableItem) {
        try {
            val userId = auth.currentUser?.uid ?: return

            firestore.collection("users")
                .document(userId)
                .collection("items")
                .document(item.id)
                .set(item)
                .await()
        } catch (e: Exception) {
            // Silent fail - Room is our source of truth
            e.printStackTrace()
        }
    }

    private suspend fun fetchFromUserFirestore(barcode: String): Result<RecyclableItem> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("items")
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

//    // SYNC FROM FIRESTORE - Pull user's items when online
//    suspend fun syncItemsFromFirestore() {
//        if (!isNetworkAvailable()) return
//
//        try {
//            val userId = auth.currentUser?.uid ?: return
//
//            val snapshot = firestore.collection("users")
//                .document(userId)
//                .collection("items")
//                .get()
//                .await()
//
//            val items = snapshot.documents.mapNotNull { doc ->
//                doc.toObject(RecyclableItem::class.java)?.copy(id = doc.id)
//            }
//
//            if (items.isNotEmpty()) {
//                // Replace all items in Room with Firestore data
//                // This ensures favorites are synced correctly
//                dao.clearAll()
//                dao.insertItems(items)
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }

    // TOGGLE FAVORITE - Update Room immediately, sync to user's Firestore
    suspend fun toggleFavorite(item: RecyclableItem) {
        val updated = item.copy(isFavorite = !item.isFavorite)

        // Update Room first (instant UI feedback)
        dao.updateItem(updated)

        // Sync to user's Firestore collection
        if (isNetworkAvailable()) {
            try {
                val userId = auth.currentUser?.uid ?: return

                firestore.collection("users")
                    .document(userId)
                    .collection("items")
                    .document(item.id)
                    .set(updated) // Save entire item with updated favorite status
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // DELETE ITEM
    suspend fun deleteItem(itemId: String) {
        // Delete from Room
        dao.deleteItem(itemId)

        // Delete from Firestore
        if (isNetworkAvailable()) {
            try {
                val userId = auth.currentUser?.uid ?: return

                firestore.collection("users")
                    .document(userId)
                    .collection("items")
                    .document(itemId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getItemById(itemId: String): RecyclableItem? {
        return dao.getItemById(itemId)
    }

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

//    // SEED DATABASE - Run once on first launch OR when user signs in
//    suspend fun seedDatabaseIfEmpty() {
//        if (dao.getItemCount() == 0) {
//            val sampleItems = getSampleItems()
//
//            // Save to Room
//            dao.insertItems(sampleItems)
//
//            // Save to user's Firestore collection
//            if (isNetworkAvailable()) {
//                try {
//                    val userId = auth.currentUser?.uid ?: return
//
//                    sampleItems.forEach { item ->
//                        firestore.collection("users")
//                            .document(userId)
//                            .collection("items")
//                            .document(item.id)
//                            .set(item)
//                            .await()
//                    }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }
//        }
//    }

    // IMPROVED SYNC - Merge instead of replace
    suspend fun syncItemsFromFirestore() {
        if (!isNetworkAvailable()) return

        try {
            val userId = auth.currentUser?.uid ?: return

            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("items")
                .get()
                .await()

            val firestoreItems = snapshot.documents.mapNotNull { doc ->
                doc.toObject(RecyclableItem::class.java)?.copy(id = doc.id)
            }

            if (firestoreItems.isNotEmpty()) {
                // MERGE: Insert or update each item (don't clear!)
                firestoreItems.forEach { firestoreItem ->
                    val localItem = dao.getItemById(firestoreItem.id)

                    if (localItem == null) {
                        dao.insertItem(firestoreItem)
                    } else {
                        // Update if Firestore version is newer
                        if (firestoreItem.lastUpdated > localItem.lastUpdated) {
                            dao.updateItem(firestoreItem)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    suspend fun seedDatabaseIfEmpty() {
        val count = dao.getItemCount()
        Log.d("RecycleRight", "🔍 Current item count: $count")

        if (count == 0) {

            // check the firestore first if there are no items

            val userId = auth.currentUser?.uid
            if (userId != null && isNetworkAvailable()) {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("items")
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    // User has items in Firestore, don't seed
                    Log.d("RecycleRight", "⏭️ Skipping seed - Firestore sync will handle")
                    return
                }
            }



            val sampleItems = getSampleItems()

            // Save to Room
            dao.insertItems(sampleItems)
            Log.d("RecycleRight", "✅ Inserted ${sampleItems.size} items to Room")

            // Save to Firestore (background, non-blocking)
            if (isNetworkAvailable()) {
                try {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        sampleItems.forEach { item ->
                            firestore.collection("users")
                                .document(userId)
                                .collection("items")
                                .document(item.id)
                                .set(item)
                            // Not using .await() - fire and forget
                        }
                        Log.d("RecycleRight", "☁️ Firestore upload initiated")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }


    suspend fun clearAll(){
        dao.clearAll()
    }


    private fun getSampleItems() = listOf(
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
}