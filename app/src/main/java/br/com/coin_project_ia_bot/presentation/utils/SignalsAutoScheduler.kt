package br.com.coin_project_ia_bot.presentation.utils

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class SignalsAutoScheduler(private val context: Context) {

    fun start() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SignalWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "SignalCheckJob",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

