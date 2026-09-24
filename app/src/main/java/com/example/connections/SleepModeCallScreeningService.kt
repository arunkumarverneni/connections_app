package com.example.connections

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.SmsManager
import java.util.Calendar

class SleepModeCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart

        if (isSleepModeActive() && phoneNumber != null) {

            if (calledRecently(phoneNumber)) {
                val response = CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .build()
                respondToCall(callDetails, response)
            } else {
                val response = CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
                respondToCall(callDetails, response)

                sendSleepModeSms(phoneNumber)
                rememberCall(phoneNumber)
            }

        } else {
            val response = CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build()
            respondToCall(callDetails, response)
        }
    }

    private fun isSleepModeActive(): Boolean {
        val prefs = getSharedPreferences("SleepModePrefs", MODE_PRIVATE)
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

    private fun calledRecently(phoneNumber: String): Boolean {
        val prefs = getSharedPreferences("RecentCalls", MODE_PRIVATE)
        val lastCallTime = prefs.getLong(phoneNumber, 0L)
        val now = System.currentTimeMillis()
        val windowMillis = 30 * 60 * 1000

        return (now - lastCallTime) < windowMillis
    }

    private fun rememberCall(phoneNumber: String) {
        val prefs = getSharedPreferences("RecentCalls", MODE_PRIVATE)
        prefs.edit().putLong(phoneNumber, System.currentTimeMillis()).apply()
    }

    private fun sendSleepModeSms(phoneNumber: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val message = "Hey, I'm asleep right now. Call again if it's urgent, otherwise I'll call you back."
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}