package com.mathhelper.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mathhelper.app.data.local.entity.ExplanationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExplanationDao {

    @Query("SELECT * FROM explanations WHERE knowledgePointId = :kpId LIMIT 1")
    fun observeForKnowledgePoint(kpId: String): Flow<ExplanationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(explanation: ExplanationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(explanations: List<ExplanationEntity>)
}
