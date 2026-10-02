package com.mathhelper.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mathhelper.app.data.local.entity.ReferenceMaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReferenceMaterialDao {

    @Query("SELECT * FROM reference_materials")
    fun observeAll(): Flow<List<ReferenceMaterialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ReferenceMaterialEntity>)

    @Query("SELECT COUNT(*) FROM reference_materials")
    suspend fun count(): Int
}
