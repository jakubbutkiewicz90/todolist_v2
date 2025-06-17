package com.example.todolist_v2.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.todolist_v2.database.AppDatabase
import com.example.todolist_v2.database.ToDoDao
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE shops ADD COLUMN owner TEXT NOT NULL DEFAULT 'default_shopping_user'")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE todo_lists ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tasks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE shops ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "todo_database"
        )
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
            .build()
    }

    @Provides
    @Singleton
    fun provideToDoDao(appDatabase: AppDatabase): ToDoDao {
        return appDatabase.toDoDao()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}