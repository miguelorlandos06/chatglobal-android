package com.chatglobal.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chatglobal.app.data.local.db.dao.MessageDao
import com.chatglobal.app.data.local.db.dao.OnlineUserDao
import com.chatglobal.app.data.local.db.entity.MessageEntity
import com.chatglobal.app.data.local.db.entity.OnlineUserEntity

@Database(
    entities = [
        MessageEntity::class,
        OnlineUserEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun onlineUserDao(): OnlineUserDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun get(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chatglobal.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
