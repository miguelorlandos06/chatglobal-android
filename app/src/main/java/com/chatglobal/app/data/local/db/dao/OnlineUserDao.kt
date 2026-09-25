package com.chatglobal.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.chatglobal.app.data.local.db.entity.OnlineUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OnlineUserDao {

    @Query("SELECT * FROM online_users ORDER BY name ASC")
    fun observeAll(): Flow<List<OnlineUserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<OnlineUserEntity>)

    @Query("DELETE FROM online_users")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(users: List<OnlineUserEntity>) {
        clear()
        insertAll(users)
    }
}
