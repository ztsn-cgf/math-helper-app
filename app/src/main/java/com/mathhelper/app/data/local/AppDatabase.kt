package com.mathhelper.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mathhelper.app.data.local.dao.AttemptDao
import com.mathhelper.app.data.local.dao.ExplanationDao
import com.mathhelper.app.data.local.dao.KnowledgePointDao
import com.mathhelper.app.data.local.dao.MasteryDao
import com.mathhelper.app.data.local.dao.MisconceptionDao
import com.mathhelper.app.data.local.dao.MistakeDao
import com.mathhelper.app.data.local.dao.PracticeQuestionDao
import com.mathhelper.app.data.local.dao.ReferenceMaterialDao
import com.mathhelper.app.data.local.entity.AttemptEntity
import com.mathhelper.app.data.local.entity.ExplanationEntity
import com.mathhelper.app.data.local.entity.KnowledgePointEntity
import com.mathhelper.app.data.local.entity.MasteryEntity
import com.mathhelper.app.data.local.entity.MisconceptionEntity
import com.mathhelper.app.data.local.entity.MistakeEntity
import com.mathhelper.app.data.local.entity.PracticeQuestionEntity
import com.mathhelper.app.data.local.entity.ReferenceMaterialEntity

@Database(
    entities = [
        KnowledgePointEntity::class,
        MisconceptionEntity::class,
        ReferenceMaterialEntity::class,
        MistakeEntity::class,
        ExplanationEntity::class,
        PracticeQuestionEntity::class,
        MasteryEntity::class,
        AttemptEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun knowledgePointDao(): KnowledgePointDao
    abstract fun misconceptionDao(): MisconceptionDao
    abstract fun referenceMaterialDao(): ReferenceMaterialDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun explanationDao(): ExplanationDao
    abstract fun practiceQuestionDao(): PracticeQuestionDao
    abstract fun masteryDao(): MasteryDao
    abstract fun attemptDao(): AttemptDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "math-helper.db"
                )
                    // 开发期：结构变更直接重建，避免写迁移。上线前需改成显式 Migration。
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
