package com.example.connections

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.telephony.SmsManager
import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject

class IncomingCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.PHONE_STATE") return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        if (state == TelephonyManager.EXTRA_STATE_RINGING && incomingNumber != null) {
            if (isSleepModeActive(context)) {
                if (!calledRecently(context, incomingNumber)) {
                    sendSleepModeSms(context, incomingNumber)
                    rememberCall(context, incomingNumber)
                    logCall(context, incomingNumber)
                }
            }
        }
    }

    private fun isSleepModeActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences("SleepModePrefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("sleep_mode_enabled", false)
        val startHour = prefs.getInt("sleep_start_hour", 23)
        val endHour = prefs.getInt("sleep_end_hour", 7)

        if (!isEnabled) return false

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return if (startHour <= endHour) {
            currentHour in startHour until endHour
        } else {
            currentHour >= startHour || currentHour < endHour
        }
    }

    private fun calledRecently(context: Context, phoneNumber: String): Boolean {
        val prefs = context.getSharedPreferences("RecentCalls", Context.MODE_PRIVATE)
        val lastCallTime = prefs.getLong(phoneNumber, 0L)
        val now = System.currentTimeMillis()
        val windowMillis = 30 * 60 * 1000

        return (now - lastCallTime) < windowMillis
    }

    private fun rememberCall(context: Context, phoneNumber: String) {
        val prefs = context.getSharedPreferences("RecentCalls", Context.MODE_PRIVATE)
        prefs.edit().putLong(phoneNumber, System.currentTimeMillis()).apply()
    }

    private fun logCall(context: Context, phoneNumber: String) {
        try {
            val prefs = context.getSharedPreferences("CallLog", Context.MODE_PRIVATE)
            val existingJson = prefs.getString("entries", "[]")
            val array = JSONArray(existingJson)

            val entry = JSONObject()
            entry.put("number", phoneNumber)
            entry.put("timestamp", System.currentTimeMillis())

            array.put(entry)

            prefs.edit().putString("entries", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendSleepModeSms(context: Context, phoneNumber: String) {
        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as android.telephony.SubscriptionManager

            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_PHONE_STATE
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val subscriptionList = subscriptionManager.activeSubscriptionInfoList

            val smsManager = if (!subscriptionList.isNullOrEmpty()) {
                val subscriptionId = subscriptionList[0].subscriptionId
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            } else {
                SmsManager.getDefault()
            }

            val message = "Hey, I'm asleep right now. Call again if it's urgent, otherwise I'll call you back."
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}