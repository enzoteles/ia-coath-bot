package br.com.coin_project_ia_bot.presentation.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import br.com.coin_project_ia_bot.R
import br.com.coin_project_ia_bot.presentation.MainActivity

object PumpNotifier {

    private const val CHANNEL_ID = "pump_alert_channel"

    fun showPumpNotification(context: Context, symbol: String, variation: Float) {
        createNotificationChannel(context)

        // 🔁 Intent para abrir a MainActivity e o fragmento correto
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "pump") // Usaremos isso para navegar ao fragment
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val vibrationPattern = longArrayOf(0, 500, 200, 500) // espera, vibra, pausa, vibra

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 Pump Detectado!")
            .setContentText("Moeda: $symbol subiu ${"%.2f".format(variation)}% nas últimas 2h.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🚨 PUMP DETECTADO\n💰 Par: $symbol\n📈 Variação: ${"%.2f".format(variation)}%")
            )
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(vibrationPattern) // 👈 Vibração personalizada
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        NotificationManagerCompat.from(context).notify(symbol.hashCode(), notification)
    }


    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Pump Alerts"
            val descriptionText = "Notificações para moedas com pump"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
