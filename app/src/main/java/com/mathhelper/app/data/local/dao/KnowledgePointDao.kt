package com.mathhelper.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mathhelper.app.data.local.entity.KnowledgePointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgePointDao {

    @Query("SELECT * FROM knowledge_points ORDER BY orderIndex")
    fun observeAll(): Flow<List<KnowledgePointEntity>>

    @Query("SELECT * FROM knowledge_points WHERE type = 'topic' ORDER BY orderIndex")
    fun observeTopics(): Flow<List<KnowledgePointEntity>>

    @Query("SELECT * FROM knowledge_points WHERE type = 'topic' ORDER BY orderIndex")
    suspend fun getTopics(): List<KnowledgePointEntity>

    @Query("SELECT * FROM knowledge_points WHERE id = :id")
    suspend fun getById(id: String): KnowledgePointEntity?

    @Query("SELECT * FROM knowledge_points WHERE id = :id")
    fun observeById(id: String): Flow<KnowledgePointEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<KnowledgePointEntity>)

    @Query("SELECT COUNT(*) FROM knowledge_points")
    suspend fun count(): Int
}
