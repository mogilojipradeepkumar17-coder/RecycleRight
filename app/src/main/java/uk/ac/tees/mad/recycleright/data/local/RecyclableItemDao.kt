package uk.ac.tees.mad.recycleright.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import uk.ac.tees.mad.recycleright.data.model.RecyclableItem

@Dao
interface RecyclableItemDao {

    @Query("SELECT * FROM recyclable_items WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchItems(query: String): Flow<List<RecyclableItem>>

    @Query("SELECT * FROM recyclable_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getItemByBarcode(barcode: String): RecyclableItem?

    @Query("SELECT * FROM recyclable_items WHERE id = :itemId")
    suspend fun getItemById(itemId: String): RecyclableItem?

    @Query("SELECT * FROM recyclable_items WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteItems(): Flow<List<RecyclableItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: RecyclableItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<RecyclableItem>)

    @Update
    suspend fun updateItem(item: RecyclableItem)

    @Query("DELETE FROM recyclable_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String)

    @Query("DELETE FROM recyclable_items")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM recyclable_items")
    suspend fun getItemCount(): Int

    @Query("SELECT * FROM recyclable_items ORDER BY name ASC")
    fun getAllItems(): Flow<List<RecyclableItem>>

}