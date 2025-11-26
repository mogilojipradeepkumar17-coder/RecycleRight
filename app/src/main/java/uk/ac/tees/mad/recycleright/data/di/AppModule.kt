package uk.ac.tees.mad.recycleright.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uk.ac.tees.mad.recycleright.data.local.RecyclableItemDao
import uk.ac.tees.mad.recycleright.data.local.RecycleRightDatabase
import uk.ac.tees.mad.recycleright.data.remote.OpenFoodFactsApiService
import uk.ac.tees.mad.recycleright.data.repository.RecyclableItemRepository
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideRecycleRightDatabase(
        @ApplicationContext context: Context
    ): RecycleRightDatabase {
        return Room.databaseBuilder(
            context,
            RecycleRightDatabase::class.java,
            "recycle_right_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideRecyclableItemDao(
        database: RecycleRightDatabase
    ): RecyclableItemDao {
        return database.recyclableItemDao()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OpenFoodFactsApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenFoodFactsApiService(
        retrofit: Retrofit
    ): OpenFoodFactsApiService {
        return retrofit.create(OpenFoodFactsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRecyclableItemRepository(
        dao: RecyclableItemDao,
        firestore: FirebaseFirestore,
        openFoodFactsApi: OpenFoodFactsApiService,
        @ApplicationContext context: Context
    ): RecyclableItemRepository {
        return RecyclableItemRepository(dao, firestore, openFoodFactsApi, context)
    }
}