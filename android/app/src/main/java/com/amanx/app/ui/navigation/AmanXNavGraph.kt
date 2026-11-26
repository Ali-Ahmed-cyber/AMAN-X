package com.amanx.app.ui.navigation

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.amanx.app.domain.model.AlertStatus
import com.amanx.app.domain.model.AppLanguage
import com.amanx.app.ui.screens.AcademyHubScreen
import com.amanx.app.ui.screens.AccountRoadmapScreen
import com.amanx.app.ui.screens.ActivityHistoryScreen
import com.amanx.app.ui.screens.AlertDetailScreen
import com.amanx.app.ui.screens.AlertsCenterScreen
import com.amanx.app.ui.screens.AppLockGateScreen
import com.amanx.app.ui.screens.AppLockSetupScreen
import com.amanx.app.ui.screens.AppearanceLanguageScreen
import com.amanx.app.ui.screens.BackupRestoreScreen
import com.amanx.app.ui.screens.CinematicIntroScreen
import com.amanx.app.ui.screens.ControlCenterScreen
import com.amanx.app.ui.screens.DataIntegrityMonitorScreen
import com.amanx.app.ui.screens.EmergencyCenterScreen
import com.amanx.app.ui.screens.EvidenceVaultScreen
import com.amanx.app.ui.screens.FalsePositiveManagementScreen
import com.amanx.app.ui.screens.HomeDashboardScreen
import com.amanx.app.ui.screens.IncidentDetailScreen
import com.amanx.app.ui.screens.NotificationAmountsScreen
import com.amanx.app.ui.screens.NotificationSourceControlScreen
import com.amanx.app.ui.screens.OfflineStatusCenterScreen
import com.amanx.app.ui.screens.PermissionsSetupScreen
import com.amanx.app.ui.screens.PrivacyControlCenterScreen
import com.amanx.app.ui.screens.ReceiptAnalyzerScreen
import com.amanx.app.ui.screens.SecureExportCenterScreen
import com.amanx.app.ui.screens.SecurityHealthAndReadinessScreen
import com.amanx.app.ui.screens.SecurityLockSettingsScreen
import com.amanx.app.ui.screens.WelcomeOnboardingScreen
import com.amanx.app.ui.theme.AmanXTheme
import com.amanx.app.ui.theme.findActivity
import com.amanx.app.ui.viewmodel.AmanXViewModel

@Composable
fun AmanXRootApp(viewModel: AmanXViewModel) {
    val prefs by viewModel.preferences.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val amounts by viewModel.extractedAmounts.collectAsState()
    val sources by viewModel.notificationSources.collectAsState()
    val fpRules by viewModel.falsePositiveRules.collectAsState()
    val receipts by viewModel.receiptAnalyses.collectAsState()
    val incidents by viewModel.incidents.collectAsState()
    val evidence by viewModel.evidenceItems.collectAsState()
    val activities by viewModel.activityLogs.collectAsState()
    val academyProgress by viewModel.academyProgress.collectAsState()
    val retentionPolicies by viewModel.retentionPolicies.collectAsState()
    val integrityLogs by viewModel.integrityLogs.collectAsState()
    val exportLogs by viewModel.exportLogs.collectAsState()
    val processedCount by viewModel.processedCount.collectAsState()
    val healthReport by viewModel.securityHealth.collectAsState()
    val isUnlocked by viewModel.isSessionUnlocked.collectAsState()
    val isAlarmActive by viewModel.isAlarmActive.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    val pendingSharedText by viewModel.pendingSharedText.collectAsState()
    val pendingSharedImage by viewModel.pendingSharedImageUri.collectAsState()
    val pendingAlertId by viewModel.pendingAlertDeepLink.collectAsState()
    val recoveryKey by viewModel.lastGeneratedRecoveryKey.collectAsState()

    val isAr = prefs.language == AppLanguage.AR
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Apply FLAG_SECURE on sensitive screens (PIN, Vault, Backup, Privacy)
    val sensitiveRoutes = setOf(
        "app_lock_gate", "app_lock_setup", "setup_pin_required",
        "evidence_vault", "backup_restore", "privacy_control", "secure_export"
    )
    DisposableEffect(currentRoute) {
        val activity: Activity? = context.findActivity()
        if (currentRoute in sensitiveRoutes) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {}
    }

    LaunchedEffect(feedback) {
        val msg = feedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(if (isAr) msg.textAr else msg.textEn)
        viewModel.clearFeedback()
    }

    // Handle deep links once unlocked and onboarded
    LaunchedEffect(pendingSharedImage, isUnlocked, prefs.onboardingCompleted) {
        if (prefs.onboardingCompleted && (!prefs.pinConfigured || isUnlocked) && pendingSharedImage != null) {
            navController.navigate("receipt_analyzer") { launchSingleTop = true }
        }
    }
    LaunchedEffect(pendingAlertId, isUnlocked, prefs.onboardingCompleted) {
        val id = pendingAlertId
        if (prefs.onboardingCompleted && (!prefs.pinConfigured || isUnlocked) && id != null) {
            viewModel.setPendingAlertDeepLink(null)
            navController.navigate("alert_detail/$id") { launchSingleTop = true }
        }
    }

    val startDestination = when {
        !prefs.onboardingCompleted -> "cinematic_intro"
        prefs.pinConfigured && !isUnlocked -> "app_lock_gate"
        else -> "home"
    }

    val mainTabs = listOf("home", "protection", "emergency", "academy", "control_center")
    val showBottomBar = currentRoute in mainTabs

    AmanXTheme(
        themeMode = prefs.themeMode,
        language = prefs.language,
        highContrast = prefs.highContrast,
        fontScaleMultiplier = prefs.fontScaleMultiplier
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == "home",
                            onClick = { navController.navigate("home") { launchSingleTop = true } },
                            icon = { Icon(Icons.Outlined.Home, null) },
                            label = { Text(if (isAr) "الرئيسية" else "Home") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "protection",
                            onClick = { navController.navigate("protection") { launchSingleTop = true } },
                            icon = { Icon(Icons.Outlined.NotificationsActive, null) },
                            label = { Text(if (isAr) "التنبيهات" else "Alerts") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "emergency",
                            onClick = { navController.navigate("emergency") { launchSingleTop = true } },
                            icon = { Icon(Icons.Outlined.WarningAmber, null) },
                            label = { Text(if (isAr) "الطوارئ" else "Emergency") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "academy",
                            onClick = { navController.navigate("academy") { launchSingleTop = true } },
                            icon = { Icon(Icons.Outlined.School, null) },
                            label = { Text(if (isAr) "الأكاديمية" else "Academy") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "control_center",
                            onClick = { navController.navigate("control_center") { launchSingleTop = true } },
                            icon = { Icon(Icons.Outlined.Settings, null) },
                            label = { Text(if (isAr) "التحكم" else "Control") }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavHost(navController = navController, startDestination = startDestination) {
                    composable("cinematic_intro") {
                        CinematicIntroScreen(
                            language = prefs.language,
                            reduceMotion = viewModel.isSystemPowerSaveOrReduceMotion(),
                            onIntroFinished = {
                                navController.navigate("welcome") {
                                    popUpTo("cinematic_intro") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("welcome") {
                        WelcomeOnboardingScreen(
                            language = prefs.language,
                            onLanguageChange = viewModel::setLanguage,
                            onNext = { navController.navigate("app_lock_setup") }
                        )
                    }

                    composable("app_lock_setup") {
                        AppLockSetupScreen(
                            language = prefs.language,
                            allowDefer = true,
                            onConfigurePin = { pin, bio ->
                                viewModel.configurePin(pin, bio) {
                                    navController.navigate("permissions_setup")
                                }
                            },
                            onDefer = {
                                viewModel.deferPinSetup {
                                    navController.navigate("permissions_setup")
                                }
                            }
                        )
                    }

                    composable("setup_pin_required") {
                        AppLockSetupScreen(
                            language = prefs.language,
                            allowDefer = false,
                            onConfigurePin = { pin, bio ->
                                viewModel.configurePin(pin, bio) {
                                    navController.popBackStack()
                                }
                            },
                            onDefer = {}
                        )
                    }

                    composable("permissions_setup") {
                        PermissionsSetupScreen(
                            language = prefs.language,
                            isListenerEnabled = viewModel.isNotificationListenerEnabled(),
                            isPostNotificationsGranted = viewModel.isPostNotificationsGranted(),
                            onFinish = {
                                viewModel.completeOnboarding {
                                    navController.navigate("home") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable("app_lock_gate") {
                        AppLockGateScreen(
                            language = prefs.language,
                            biometricEnabled = prefs.biometricEnabled,
                            lockoutUntilEpochMs = prefs.lockoutUntilEpochMs,
                            onVerifyPin = { pin, callback ->
                                viewModel.verifyPinAndUnlock(pin) { ok ->
                                    callback(ok)
                                    if (ok) {
                                        navController.navigate("home") {
                                            popUpTo("app_lock_gate") { inclusive = true }
                                        }
                                    }
                                }
                            },
                            onBiometricSuccess = {
                                viewModel.unlockWithBiometric()
                                navController.navigate("home") {
                                    popUpTo("app_lock_gate") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("home") {
                        HomeDashboardScreen(
                            language = prefs.language,
                            healthReport = healthReport,
                            alerts = alerts,
                            amounts = amounts,
                            processedCount = processedCount,
                            recentActivities = activities,
                            isOnline = viewModel.isDeviceOnline(),
                            isListenerEnabled = viewModel.isNotificationListenerEnabled(),
                            pendingQuickText = pendingSharedText,
                            onClearPendingQuickText = { viewModel.setPendingSharedText(null) },
                            onAnalyzeText = { txt ->
                                viewModel.analyzeManualText(txt) { alertId ->
                                    if (alertId != null) navController.navigate("alert_detail/$alertId")
                                }
                            },
                            onNavigate = { route -> navController.navigate(route) },
                            onOpenAlertDetail = { id -> navController.navigate("alert_detail/$id") }
                        )
                    }

                    composable("security_health") {
                        SecurityHealthAndReadinessScreen(
                            language = prefs.language,
                            report = healthReport,
                            isReadinessOnly = false,
                            onNavigate = { route ->
                                val target = if (route == "setup_pin") "setup_pin_required" else route
                                navController.navigate(target)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("readiness_check") {
                        SecurityHealthAndReadinessScreen(
                            language = prefs.language,
                            report = healthReport,
                            isReadinessOnly = true,
                            onNavigate = { route -> navController.navigate(route) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("offline_status") {
                        OfflineStatusCenterScreen(
                            language = prefs.language,
                            isOnline = viewModel.isDeviceOnline(),
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("protection") {
                        AlertsCenterScreen(
                            language = prefs.language,
                            alerts = alerts,
                            onOpenDetail = { id -> navController.navigate("alert_detail/$id") },
                            onMarkRead = { id -> viewModel.updateAlertStatus(id, AlertStatus.REVIEWED) },
                            onArchive = { id -> viewModel.updateAlertStatus(id, AlertStatus.ARCHIVED) },
                            onDelete = viewModel::deleteAlert,
                            onNavigate = { route -> navController.navigate(route) }
                        )
                    }

                    composable(
                        route = "alert_detail/{alertId}",
                        arguments = listOf(navArgument("alertId") { type = NavType.LongType })
                    ) { backStack ->
                        val alertId = backStack.arguments?.getLong("alertId") ?: 0L
                        val alert by viewModel.observeAlert(alertId).collectAsState(initial = null)
                        AlertDetailScreen(
                            language = prefs.language,
                            alert = alert,
                            loadDecryptedText = viewModel::getDecryptedRecordText,
                            onReportFalsePositive = viewModel::reportFalsePositive,
                            onCreateIncident = { t, type, sev, notes ->
                                viewModel.createIncident(t, type, sev, notes) { incId ->
                                    navController.navigate("incident_detail/$incId")
                                }
                            },
                            onTriggerAlarm = {
                                viewModel.triggerSecurityAlarm()
                                navController.navigate("emergency")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("notification_amounts") {
                        NotificationAmountsScreen(
                            language = prefs.language,
                            amounts = amounts,
                            onCorrectAmount = viewModel::correctAmount,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("notification_sources") {
                        NotificationSourceControlScreen(
                            language = prefs.language,
                            sources = sources,
                            onToggleSource = viewModel::setSourceEnabled,
                            onChangeSensitivity = viewModel::setSourceSensitivity,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("false_positives") {
                        FalsePositiveManagementScreen(
                            language = prefs.language,
                            rules = fpRules,
                            onToggleRule = viewModel::toggleFalsePositiveRule,
                            onDeleteRule = viewModel::deleteFalsePositiveRule,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("receipt_analyzer") {
                        ReceiptAnalyzerScreen(
                            language = prefs.language,
                            receipts = receipts,
                            pendingSharedImageUri = pendingSharedImage,
                            onClearPendingImage = { viewModel.setPendingSharedImageUri(null) },
                            onAnalyzeImage = viewModel::analyzeReceiptImage,
                            onUpdateReview = viewModel::updateReceiptReview,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("emergency") {
                        EmergencyCenterScreen(
                            language = prefs.language,
                            incidents = incidents,
                            isAlarmActive = isAlarmActive,
                            alarmVibration = prefs.alarmVibrationEnabled,
                            alarmSound = prefs.alarmSoundEnabled,
                            pinConfigured = prefs.pinConfigured,
                            onSetAlarmOptions = viewModel::setAlarmOptions,
                            onTriggerAlarm = viewModel::triggerSecurityAlarm,
                            onStopAlarm = viewModel::stopSecurityAlarm,
                            onCreateIncident = { t, type, sev, notes ->
                                viewModel.createIncident(t, type, sev, notes) { id ->
                                    navController.navigate("incident_detail/$id")
                                }
                            },
                            onOpenIncident = { id -> navController.navigate("incident_detail/$id") },
                            onNavigate = { route -> navController.navigate(route) }
                        )
                    }

                    composable(
                        route = "incident_detail/{incidentId}",
                        arguments = listOf(navArgument("incidentId") { type = NavType.LongType })
                    ) { backStack ->
                        val incidentId = backStack.arguments?.getLong("incidentId") ?: 0L
                        IncidentDetailScreen(
                            language = prefs.language,
                            incidentFlow = viewModel.observeIncident(incidentId),
                            stepsFlow = viewModel.observeRecoverySteps(incidentId),
                            timelineFlow = viewModel.observeIncidentTimeline(incidentId),
                            onToggleStep = viewModel::toggleRecoveryStep,
                            onResolve = viewModel::resolveIncident,
                            onExportReport = { uri, id -> viewModel.exportJsonToUri(uri, id) },
                            onAddEvidence = viewModel::addEvidence,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("evidence_vault") {
                        EvidenceVaultScreen(
                            language = prefs.language,
                            pinConfigured = prefs.pinConfigured,
                            evidenceItems = evidence,
                            onAddEvidence = viewModel::addEvidence,
                            onDeleteEvidence = viewModel::deleteEvidence,
                            onRunIntegrityCheck = viewModel::runIntegrityCheck,
                            onSetupPinRequired = { navController.navigate("setup_pin_required") },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("data_integrity") {
                        DataIntegrityMonitorScreen(
                            language = prefs.language,
                            logs = integrityLogs,
                            onRunCheck = viewModel::runIntegrityCheck,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("secure_export") {
                        SecureExportCenterScreen(
                            language = prefs.language,
                            pinConfigured = prefs.pinConfigured,
                            exportLogs = exportLogs,
                            onExportPrivacyJson = { uri -> viewModel.exportJsonToUri(uri, null) },
                            onNavigateBackup = { navController.navigate("backup_restore") },
                            onSetupPinRequired = { navController.navigate("setup_pin_required") },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("academy") {
                        AcademyHubScreen(
                            language = prefs.language,
                            lessons = viewModel.lessons,
                            simulations = viewModel.simulations,
                            progress = academyProgress,
                            onRecordScore = viewModel::recordAcademyAttempt
                        )
                    }

                    composable("control_center") {
                        ControlCenterScreen(
                            language = prefs.language,
                            prefs = prefs,
                            onNavigate = { route -> navController.navigate(route) }
                        )
                    }

                    composable("security_lock_settings") {
                        SecurityLockSettingsScreen(
                            language = prefs.language,
                            prefs = prefs,
                            onSetupOrChangePin = { navController.navigate("setup_pin_required") },
                            onToggleBiometric = viewModel::setBiometricEnabled,
                            onToggleSmartDedup = viewModel::setSmartDedup,
                            onLockNow = {
                                viewModel.lockSession()
                                navController.navigate("app_lock_gate") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("privacy_control") {
                        PrivacyControlCenterScreen(
                            language = prefs.language,
                            pinConfigured = prefs.pinConfigured,
                            policies = retentionPolicies,
                            processedCount = processedCount,
                            alertsCount = alerts.size,
                            evidenceCount = evidence.size,
                            onSetRetentionDays = viewModel::setRetentionDays,
                            onRunCleanup = viewModel::runManualRetentionCleanup,
                            onOpenExportCenter = { navController.navigate("secure_export") },
                            onWipeAllData = {
                                viewModel.wipeAllData {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            },
                            onSetupPinRequired = { navController.navigate("setup_pin_required") },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("backup_restore") {
                        BackupRestoreScreen(
                            language = prefs.language,
                            pinConfigured = prefs.pinConfigured,
                            lastGeneratedRecoveryKey = recoveryKey,
                            onClearRecoveryKey = viewModel::clearRecoveryKeyNotice,
                            onCreateBackup = viewModel::writeEncryptedBackupToUri,
                            onRestoreBackup = viewModel::restoreEncryptedBackupFromUri,
                            onSetupPinRequired = { navController.navigate("setup_pin_required") },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("activity_history") {
                        ActivityHistoryScreen(
                            language = prefs.language,
                            logs = activities,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("appearance_language") {
                        AppearanceLanguageScreen(
                            language = prefs.language,
                            prefs = prefs,
                            onSetTheme = viewModel::setThemeMode,
                            onSetLanguage = viewModel::setLanguage,
                            onSetReduceMotion = viewModel::setReduceMotion,
                            onSetHighContrast = viewModel::setHighContrast,
                            onSetFontScale = viewModel::setFontScale,
                            onResetDefaults = viewModel::resetAppearanceDefaults,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("account_roadmap") {
                        AccountRoadmapScreen(
                            language = prefs.language,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
