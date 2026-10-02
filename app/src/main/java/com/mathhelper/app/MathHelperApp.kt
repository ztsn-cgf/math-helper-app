package com.mathhelper.app

import android.app.Application
import com.mathhelper.app.data.local.AppDatabase
import com.mathhelper.app.data.seed.SeedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用入口：持有数据库单例，启动时把 assets 里的种子数据灌进 Room。
 */
class MathHelperApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            SeedData.seedIfEmpty(this@MathHelperApp, database)
        }
    }
}
