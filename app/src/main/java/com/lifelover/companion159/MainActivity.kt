package com.lifelover.companion159

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.lifelover.companion159.presentation.navigation.AppNavigation
import com.lifelover.companion159.presentation.theme.Companion159Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // Launcher для запиту дозволу на сповіщення
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Notification permission granted: $isGranted")
        if (!isGranted) {
            showPermissionRationale()
        }
    }

    private var showRationaleDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Запитуємо необхідні дозволи
        requestNecessaryPermissions()

        setContent {
            Companion159Theme {
                val darkTheme = isSystemInDarkTheme()

                SideEffect {
                    window.statusBarColor = Color.Transparent.toArgb()
                    window.navigationBarColor = Color.Transparent.toArgb()

                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(navController = navController)
                }

                // Діалог пояснення для дозволів
                if (showRationaleDialog) {
                    PermissionRationaleDialog(
                        onDismiss = { showRationaleDialog = false },
                        onOpenSettings = {
                            showRationaleDialog = false
                            openAppSettings()
                        }
                    )
                }
            }
        }
    }

    private fun requestNecessaryPermissions() {
        // Android 13+ (API 33+) - дозвіл на сповіщення
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "✅ Notification permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.d(TAG, "📋 Showing notification permission rationale")
                    showPermissionRationale()
                }
                else -> {
                    Log.d(TAG, "📤 Requesting notification permission")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        // Android 12+ (API 31+) - дозвіл на точні алярми
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkExactAlarmPermission()
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.d(TAG, "⚠️ Exact alarm permission not granted")
                // Можна показати діалог з поясненням
            } else {
                Log.d(TAG, "✅ Exact alarm permission granted")
            }
        }
    }

    private fun showPermissionRationale() {
        showRationaleDialog = true
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}

@Composable
fun PermissionRationaleDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Дозволи для фонової синхронізації") },
        text = {
            Text(
                "Для автоматичної синхронізації даних необхідні наступні дозволи:\n\n" +
                        "• Сповіщення - для інформування про статус синхронізації\n" +
                        "• Фонова робота - для синхронізації коли додаток закритий\n\n" +
                        "Без цих дозволів синхронізація працюватиме тільки коли додаток відкритий."
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Відкрити налаштування")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Пізніше")
            }
        }
    )
}