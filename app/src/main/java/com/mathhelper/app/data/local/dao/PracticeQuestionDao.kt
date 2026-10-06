package com.mathhelper.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mathhelper.app.data.local.entity.PracticeQuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeQuestionDao {

    @Query("SELECT * FROM practice_questions WHERE knowledgePointId = :kpId")
    fun observeForKnowledgePoint(kpId: String): Flow<List<PracticeQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PracticeQuestionEntity>)

    @Query("SELECT COUNT(*) FROM practice_questions WHERE knowledgePointId = :kpId")
    suspend fun countFor(kpId: String): Int

    @Query("DELETE FROM practice_questions WHERE knowledgePointId = :kpId")
    suspend fun deleteForKnowledgePoint(kpId: String)
}
