package com.example.connections

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SleepModeScreen()
            }
        }
    }
}

@Composable
fun SleepModeScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("SleepModePrefs", android.content.Context.MODE_PRIVATE)

    var isSleepModeOn by remember {
        mutableStateOf(prefs.getBoolean("sleep_mode_enabled", false))
    }

    var startHour by remember { mutableStateOf(prefs.getInt("sleep_start_hour", 23)) }
    var endHour by remember { mutableStateOf(prefs.getInt("sleep_end_hour", 7)) }

    var statusMessage by remember { mutableStateOf("Not checked yet") }

    fun formatHour(hour: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$displayHour:00 $period"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sleep Mode",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isSleepModeOn) "ON (${formatHour(startHour)} - ${formatHour(endHour)})" else "OFF",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Switch(
            checked = isSleepModeOn,
            onCheckedChange = { checked ->
                isSleepModeOn = checked
                prefs.edit().putBoolean("sleep_mode_enabled", checked).apply()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                TimePickerDialog(
                    context,
                    { _, hourOfDay, _ ->
                        startHour = hourOfDay
                        prefs.edit().putInt("sleep_start_hour", hourOfDay).apply()
                    },
                    startHour,
                    0,
                    false
                ).show()
            }) {
                Text("Start: ${formatHour(startHour)}")
            }

            Button(onClick = {
                TimePickerDialog(
                    context,
                    { _, hourOfDay, _ ->
                        endHour = hourOfDay
                        prefs.edit().putInt("sleep_end_hour", hourOfDay).apply()
                    },
                    endHour,
                    0,
                    false
                ).show()
            }) {
                Text("End: ${formatHour(endHour)}")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            try {
                val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
                if (roleManager == null) {
                    statusMessage = "RoleManager is null"
                } else if (!roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_CALL_SCREENING)) {
                    statusMessage = "Role NOT available on this device"
                } else if (roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING)) {
                    statusMessage = "Already enabled!"
                } else {
                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_CALL_SCREENING)
                    if (intent.resolveActivity(context.packageManager) != null) {
                        statusMessage = "Opening role request screen..."
                        context.startActivity(intent)
                    } else {
                        statusMessage = "No screen available to handle this request"
                    }
                }
            } catch (e: Exception) {
                statusMessage = "ERROR: ${e.message}"
            }
        }) {
            Text("Enable Call Screening")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "When ON, calls during sleep hours will be silently declined and the caller gets an auto-SMS. If they call again, it rings through.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}