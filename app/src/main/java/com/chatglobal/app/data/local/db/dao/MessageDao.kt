package com.chatglobal.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chatglobal.app.data.local.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isPending = 1 ORDER BY createdAt ASC")
    fun observePending(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM messages")
    suspend fun clear()

    /**
     * Elimina mensajes antiguos dejando solo los N más recientes.
     */
    @Query("""
        DELETE FROM messages
        WHERE id NOT IN (
            SELECT id FROM messages ORDER BY createdAt DESC LIMIT :keep
        )
        AND isPending = 0
    """)
    suspend fun trimTo(keep: Int = 200)
}
