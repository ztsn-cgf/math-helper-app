package com.mathhelper.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import com.mathhelper.app.data.local.entity.AttemptEntity

@Dao
interface AttemptDao {

    @Insert
    suspend fun insert(attempt: AttemptEntity)
}
