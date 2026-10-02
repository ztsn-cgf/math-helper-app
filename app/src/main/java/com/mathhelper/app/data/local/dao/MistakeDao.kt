package com.mathhelper.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mathhelper.app.data.local.entity.MistakeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MistakeDao {

    @Query("SELECT * FROM mistakes ORDER BY createTime DESC")
    fun observeAll(): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes WHERE id = :id")
    suspend fun getById(id: String): MistakeEntity?

    @Insert
    suspend fun insert(mistake: MistakeEntity)

    @Query("DELETE FROM mistakes WHERE id = :id")
    suspend fun deleteById(id: String)
}
