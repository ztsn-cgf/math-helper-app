package com.mathhelper.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mathhelper.app.data.local.entity.MasteryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MasteryDao {

    @Query("SELECT * FROM mastery")
    fun observeAll(): Flow<List<MasteryEntity>>

    @Query("SELECT * FROM mastery WHERE knowledgePointId = :kpId")
    fun observe(kpId: String): Flow<MasteryEntity?>

    @Query("SELECT * FROM mastery WHERE knowledgePointId = :kpId")
    suspend fun get(kpId: String): MasteryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mastery: MasteryEntity)
}
