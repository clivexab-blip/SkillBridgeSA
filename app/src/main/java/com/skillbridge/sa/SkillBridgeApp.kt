package com.skillbridge.sa

import android.app.Application
import com.skillbridge.sa.data.repository.AuthRepository
import com.skillbridge.sa.data.repository.DataRepository

class SkillBridgeApp : Application() {

    lateinit var authRepository: AuthRepository
        private set

    lateinit var dataRepository: DataRepository
        private set

    override fun onCreate() {
        super.onCreate()
        authRepository = AuthRepository()
        dataRepository = DataRepository(applicationContext)
    }
}
