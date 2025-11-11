package com.xmobile.project1groupstudyappnew.di

import android.content.Context
import androidx.room.Room
import com.cloudinary.android.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.xmobile.project1groupstudyappnew.api_service.CloudinaryApi
import com.xmobile.project1groupstudyappnew.repository.AuthRepository
import com.xmobile.project1groupstudyappnew.repository.AuthRepositoryImpl
import com.xmobile.project1groupstudyappnew.repository.ChatRepository
import com.xmobile.project1groupstudyappnew.repository.ChatRepositoryImpl
import com.xmobile.project1groupstudyappnew.repository.FileRepository
import com.xmobile.project1groupstudyappnew.repository.FileRepositoryImpl
import com.xmobile.project1groupstudyappnew.repository.GroupRepository
import com.xmobile.project1groupstudyappnew.repository.GroupRepositoryImpl
import com.xmobile.project1groupstudyappnew.repository.NotificationRepository
import com.xmobile.project1groupstudyappnew.repository.NotificationRepositoryImpl
import com.xmobile.project1groupstudyappnew.repository.PasswordRepository
import com.xmobile.project1groupstudyappnew.repository.PasswordRepositoryImpl
import com.xmobile.project1groupstudyappnew.repository.SearchRepository
import com.xmobile.project1groupstudyappnew.repository.SearchRepositoryImpl
import com.xmobile.project1groupstudyappnew.repository.TaskRepository
import com.xmobile.project1groupstudyappnew.repository.TaskRepositoryImpl
import com.xmobile.project1groupstudyappnew.repository.UserRepository
import com.xmobile.project1groupstudyappnew.repository.UserRepositoryImpl
import com.xmobile.project1groupstudyappnew.room.AppDatabase
import com.xmobile.project1groupstudyappnew.room.dao.FileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val CLOUD_NAME = "du9rpxawb"
    private const val CLOUD_API_KEY = ""
    private const val CLOUD_API_SECRET = ""

    @Provides
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    fun provideFirebaseDB(): FirebaseDatabase = FirebaseDatabase.getInstance()

    @Provides
    fun provideRoomDB(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context = context,
            klass = AppDatabase::class.java,
            name = "room_database"
        ).fallbackToDestructiveMigration(false).build()
    }

    @Provides
    fun provideFileDao(database: AppDatabase): FileDao = database.fileDao()

    @Provides
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firebaseDatabase: FirebaseDatabase
    ): AuthRepository = AuthRepositoryImpl(firebaseAuth, firebaseDatabase)

    @Provides
    fun providePasswordRepository(
        firebaseAuth: FirebaseAuth
    ): PasswordRepository = PasswordRepositoryImpl(firebaseAuth)

    @Provides
    fun provideGroupRepository(
        firebaseDatabase: FirebaseDatabase,
        mediaManager: MediaManager
    ): GroupRepository = GroupRepositoryImpl(firebaseDatabase, mediaManager)

    @Provides
    fun provideTaskRepository(
        firebaseDatabase: FirebaseDatabase
    ): TaskRepository = TaskRepositoryImpl(firebaseDatabase)

    @Provides
    fun provideUserRepository(
        firebaseDatabase: FirebaseDatabase,
        firebaseAuth: FirebaseAuth
    ): UserRepository = UserRepositoryImpl(firebaseDatabase, firebaseAuth)

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setLenient().create()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header(
                        "Authorization",
                        okhttp3.Credentials.basic(CLOUD_API_KEY, CLOUD_API_SECRET)
                    )
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.cloudinary.com/v1_1/$CLOUD_NAME/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideCloudinaryApi(retrofit: Retrofit): CloudinaryApi =
        retrofit.create(CloudinaryApi::class.java)

    @Provides
    @Singleton
    fun provideMediaManager(@ApplicationContext context: Context): MediaManager {
        return try {
            MediaManager.get()
        } catch (e: IllegalStateException) {
            MediaManager.init(context)
            MediaManager.get()
        }
    }

    @Provides
    fun provideFileRepository(
        firebaseDatabase: FirebaseDatabase,
        mediaManager: MediaManager,
        cloudinaryApi: CloudinaryApi,
        fileDao: FileDao
    ): FileRepository = FileRepositoryImpl(firebaseDatabase, cloudinaryApi, fileDao)

    @Provides
    fun provideChatRepository(
        firebaseDatabase: FirebaseDatabase
    ): ChatRepository = ChatRepositoryImpl(firebaseDatabase)

    @Provides
    fun provideNotiRepository(
        firebaseDatabase: FirebaseDatabase
    ): NotificationRepository = NotificationRepositoryImpl(firebaseDatabase)

    @Provides
    fun provideSearchRepository(
        firebaseDatabase: FirebaseDatabase
    ): SearchRepository = SearchRepositoryImpl(firebaseDatabase)
}
