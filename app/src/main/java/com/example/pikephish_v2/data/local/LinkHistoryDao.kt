package com.example.pikephish_v2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkHistoryDao {

    /**
     * Получить последние N ссылок
     */
    @Query("SELECT * FROM link_history ORDER BY checkedAt DESC LIMIT :limit")
    fun getRecentLinks(limit: Int = 15): Flow<List<LinkHistoryEntity>>

    /**
     * Получить все ссылки
     */
    @Query("SELECT * FROM link_history ORDER BY checkedAt DESC")
    fun getAllLinks(): Flow<List<LinkHistoryEntity>>

    /**
     * Добавить ссылку
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: LinkHistoryEntity): Long

    /**
     * Удалить ссылку по ID
     */
    @Query("DELETE FROM link_history WHERE id = :id")
    suspend fun deleteLink(id: Long)

    /**
     * Очистить всю историю
     */
    @Query("DELETE FROM link_history")
    suspend fun clearAll()

    /**
     * Оставить только последние N записей (автоматическая очистка)
     */
    @Query("""
        DELETE FROM link_history 
        WHERE id NOT IN (
            SELECT id FROM link_history 
            ORDER BY checkedAt DESC 
            LIMIT :limit
        )
    """)
    suspend fun keepOnlyRecent(limit: Int = 15)

    /**
     * Проверить, есть ли ссылка в истории
     */
    @Query("SELECT * FROM link_history WHERE url = :url LIMIT 1")
    suspend fun getLinkByUrl(url: String): LinkHistoryEntity?

    /**
     * Получить количество записей
     */
    @Query("SELECT COUNT(*) FROM link_history")
    suspend fun getCount(): Int
}
