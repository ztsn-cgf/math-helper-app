package com.mathhelper.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mathhelper.app.data.local.entity.MisconceptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MisconceptionDao {

    @Query("SELECT * FROM misconceptions")
    fun observeAll(): Flow<List<MisconceptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MisconceptionEntity>)

    @Query("SELECT COUNT(*) FROM misconceptions")
    suspend fun count(): Int
}
