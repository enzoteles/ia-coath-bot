package br.com.coin_project_ia_bot.presentation.fragments.signal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object TelegramNotifier {

    private const val TELEGRAM_TOKEN = "7625003010:AAHc89Nr-b1ApqLKk-6tyv4OGWwABuTgmmY"
    private const val CHAT_ID = "6780103156"

    suspend fun sendMessage(message: String) {
        withContext(Dispatchers.IO) {
            try {
                val encodedMessage = URLEncoder.encode(message, "UTF-8")
                val url = URL("https://api.telegram.org/bot$TELEGRAM_TOKEN/sendMessage?chat_id=$CHAT_ID&text=$encodedMessage&parse_mode=HTML")
                val conn = url.openConnection() as HttpsURLConnection
                conn.requestMethod = "GET"
                conn.inputStream.bufferedReader().readText()
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
