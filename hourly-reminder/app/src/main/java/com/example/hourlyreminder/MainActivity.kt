package com.example.hourlyreminder

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ReminderScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderScreen() {
    val context = LocalContext.current
    val repository = remember { ReminderRepository(context) }
    var reminders by remember { mutableStateOf(repository.loadReminders()) }
    var newLabel by rememberSaveable { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Hourly reminders",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Create lightweight hourly notifications with a single tap.",
            style = MaterialTheme.typography.bodyMedium
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = newLabel,
                onValueChange = { newLabel = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Reminder note") }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    val label = newLabel.trim()
                    if (label.isNotEmpty()) {
                        val reminder = Reminder(UUID.randomUUID().toString(), label, true)
                        reminders = reminders + reminder
                        repository.saveReminders(reminders)
                        scheduleReminder(context, reminder)
                        newLabel = ""
                    }
                }
            ) {
                Text("Add")
            }
        }

        Divider()

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reminders, key = { it.id }) { reminder ->
                ReminderRow(
                    reminder = reminder,
                    onToggle = { enabled ->
                        val updated = reminder.copy(enabled = enabled)
                        reminders = reminders.map { if (it.id == reminder.id) updated else it }
                        repository.saveReminders(reminders)
                        if (enabled) {
                            scheduleReminder(context, updated)
                        } else {
                            cancelReminder(context, reminder)
                        }
                    },
                    onDelete = {
                        reminders = reminders.filterNot { it.id == reminder.id }
                        repository.saveReminders(reminders)
                        cancelReminder(context, reminder)
                    }
                )
            }
        }
    }
}

@Composable
private fun ReminderRow(
    reminder: Reminder,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = reminder.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (reminder.enabled) "Active every hour" else "Paused",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(checked = reminder.enabled, onCheckedChange = onToggle)
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onDelete) {
            Text("Remove")
        }
    }
}

private fun scheduleReminder(context: android.content.Context, reminder: Reminder) {
    val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS)
        .setInputData(workDataOf(ReminderWorker.KEY_REMINDER_LABEL to reminder.label))
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        reminderWorkName(reminder),
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

private fun cancelReminder(context: android.content.Context, reminder: Reminder) {
    WorkManager.getInstance(context).cancelUniqueWork(reminderWorkName(reminder))
}

private fun reminderWorkName(reminder: Reminder): String = "hourly_reminder_${reminder.id}"
