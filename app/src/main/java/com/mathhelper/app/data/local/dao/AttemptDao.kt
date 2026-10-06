package com.mathhelper.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mathhelper.app.data.local.entity.AttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttemptDao {

    @Insert
    suspend fun insert(attempt: AttemptEntity)

    @Query("SELECT * FROM attempts ORDER BY time ASC")
    fun observeAll(): Flow<List<AttemptEntity>>
}
