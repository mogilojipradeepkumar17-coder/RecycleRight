package uk.ac.tees.mad.recycleright.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName


// to store in the room
@Entity(tableName = "recyclable_items")
data class RecyclableItem(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val category: RecycleCategory = RecycleCategory.RECYCLABLE,
    val description: String = "",
    val tips: String = "",
    val barcode: String? = null,
    val imageUrl: String? = null,
    @PropertyName("favorite")  // ← Add this
    val isFavorite: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

enum class RecycleCategory(val displayName: String, val color: Long) {
    RECYCLABLE("Recyclable", 0xFF4CAF50),
    GENERAL_WASTE("General Waste", 0xFFF44336),
    COMPOSTABLE("Compostable", 0xFFFF9800)
}