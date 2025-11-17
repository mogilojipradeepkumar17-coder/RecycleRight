package uk.ac.tees.mad.recycleright.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import uk.ac.tees.mad.recycleright.data.model.RecyclableItem
import uk.ac.tees.mad.recycleright.data.model.RecycleCategory

@Database(
    entities = [RecyclableItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RecycleRightDatabase : RoomDatabase() {
    abstract fun recyclableItemDao(): RecyclableItemDao
}

class Converters {
    @TypeConverter
    fun fromRecycleCategory(category: RecycleCategory): String {
        return category.name
    }

    @TypeConverter
    fun toRecycleCategory(value: String): RecycleCategory {
        return try {
            RecycleCategory.valueOf(value)
        } catch (e: IllegalArgumentException) {
            RecycleCategory.RECYCLABLE
        }
    }
}