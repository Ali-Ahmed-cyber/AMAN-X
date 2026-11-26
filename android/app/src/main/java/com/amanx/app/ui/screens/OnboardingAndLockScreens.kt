package com.amanx.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.amanx.app.domain.model.AppLanguage
import com.amanx.app.ui.theme.AmanXCard
import com.amanx.app.ui.theme.AmanXColors
import com.amanx.app.ui.theme.findActivity

@Composable
fun WelcomeOnboardingScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onNext: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                OutlinedButton(
                    onClick = { onLanguageChange(if (isAr) AppLanguage.EN else AppLanguage.AR) }
                ) {
                    Text(if (isAr) "English" else "العربية")
                }
            }

            Text(
                text = if (isAr) "مرحباً بك في AMAN X" else "Welcome to AMAN X",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = if (isAr) {
                    "منظومتك الشخصية للحماية والتوعية الأمنية الرقمية، مصممة لتعمل محلياً على جهازك دون اتصال بالإنترنت وباحترام تام لخصوصيتك."
                } else {
                    "Your personal digital safety and fraud-awareness companion, built Local-First to operate offline on your device with strict privacy."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AmanXCard {
                Text(
                    text = if (isAr) "ماذا يقدم لك AMAN X؟" else "What AMAN X Provides",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                val bullets = if (isAr) listOf(
                    "• تحليل الإشعارات المصرح بها والنصوص الملصقة/المشاركة محلياً.",
                    "• استخراج المبالغ المالية (SDG / USD) وتلخيصها كإشعارات لا كفواتير.",
                    "• مركز طوارئ واستجابة يدوية موجّهة وخزنة أدلة مشفرة.",
                    "• أكاديمية أمنية ومحاكاة تفاعلية لأساليب الاحتيال."
                ) else listOf(
                    "• Local analysis of authorized notifications and pasted/shared texts.",
                    "• SDG / USD amount extraction & summary (notifications only, never invoices).",
                    "• Guided Emergency Response Center and encrypted Evidence Vault.",
                    "• Security Academy & isolated interactive fraud simulations."
                )
                bullets.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }

            AmanXCard(borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)) {
                Text(
                    text = if (isAr) "التزام الخصوصية والأمان" else "Privacy & Security Commitment",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = if (isAr) {
                        "لا يطلب AMAN X كلمات مرور البنوك أو رموز OTP أو صلاحية قراءة رسائل SMS، ولا يرفع بياناتك لأي خادم خارجي."
                    } else {
                        "AMAN X never asks for bank passwords, OTP codes, or READ_SMS permission, and never sends your data to external servers."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (isAr) "المتابعة لإعداد الحماية" else "Continue to Security Setup")
            }
        }
    }
}

@Composable
fun AppLockSetupScreen(
    language: AppLanguage,
    allowDefer: Boolean,
    onConfigurePin: (pin: String, biometric: Boolean) -> Unit,
    onDefer: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var enableBiometric by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val biometricAvailable = remember(context) {
        val mgr = BiometricManager.from(context)
        mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = if (isAr) "إعداد قفل AMAN X" else "Set Up AMAN X App Lock",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = if (isAr) {
                    "أنشئ رمز PIN خاصاً بتطبيق AMAN X فقط (مستقل تماماً عن رمز البنك). يمكنك تأجيله الآن، لكن الوظائف الحساسة كالأدلة والتصدير والخصوصية تتطلب تفعيله."
                } else {
                    "Create a dedicated AMAN X PIN (completely independent from your bank PIN). You can defer now, though Evidence Vault, Exports, and Privacy Controls require a PIN."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                label = { Text(if (isAr) "رمز PIN (4 إلى 8 أرقام)" else "PIN (4 to 8 digits)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) confirmPin = it },
                label = { Text(if (isAr) "تأكيد رمز PIN" else "Confirm PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (biometricAvailable) {
                AmanXCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAr) "تفعيل البصمة / التعرف البيومتري" else "Enable Biometric Unlock",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (isAr) "لفتح أسرع بجانب رمز PIN" else "Faster unlock alongside your PIN",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = enableBiometric, onCheckedChange = { enableBiometric = it })
                    }
                }
            }

            if (errorMsg != null) {
                Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {
                    when {
                        pin.length < 4 -> errorMsg = if (isAr) "الرمز يجب أن يكون 4 أرقام على الأقل" else "PIN must be at least 4 digits"
                        pin != confirmPin -> errorMsg = if (isAr) "الرمزان غير متطابقين" else "PINs do not match"
                        else -> onConfigurePin(pin, enableBiometric && biometricAvailable)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (isAr) "حفظ وتفعيل القفل" else "Save & Enable Lock")
            }

            if (allowDefer) {
                TextButton(
                    onClick = onDefer,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(if (isAr) "تأجيل إعداد القفل مؤقتاً" else "Defer Lock Setup for Now")
                }
            }
        }
    }
}

@Composable
fun PermissionsSetupScreen(
    language: AppLanguage,
    isListenerEnabled: Boolean,
    isPostNotificationsGranted: Boolean,
    onFinish: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    val context = LocalContext.current
    var postGranted by remember(isPostNotificationsGranted) { mutableStateOf(isPostNotificationsGranted) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        postGranted = granted
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = if (isAr) "الأذونات المتدرجة والشفافة" else "Transparent Progressive Permissions",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = if (isAr) {
                    "نطلب فقط الصلاحيات اللازمة فعلياً. يمكنك تخطي أي صلاحية غير أساسية وسيواصل التطبيق العمل بتحليل النصوص الملصقة والمشاركة."
                } else {
                    "We request only what is genuinely needed. You can skip optional permissions and still analyze pasted or shared texts offline."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Step 1: Notification Listener (Manual System Screen)
            AmanXCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (isListenerEnabled) Icons.Outlined.CheckCircle else Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = if (isListenerEnabled) AmanXColors.SafeGreen else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isAr) "1. مراقبة الإشعارات المصرح بها" else "1. Authorized Notification Listener",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = if (isAr) {
                        "تُستخدم لقراءة إشعارات التطبيقات التي تحددها أنت محلياً. يتطلب نظام Android تفعيلها يدوياً من إعدادات النظام (وفي Android 13+ عند التثبيت المباشر لـ APK، فعّل أولاً «السماح بالإعدادات المقيدة» من معلومات التطبيق)."
                    } else {
                        "Used locally to parse notifications from apps you approve. Android requires manual activation in system settings (on Android 13+ sideloaded APKs, first tap 'Allow restricted settings' in App Info)."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }) {
                        Text(if (isAr) "فتح إعدادات الإشعارات" else "Open Listener Settings")
                    }
                    OutlinedButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                    }) {
                        Text(if (isAr) "معلومات التطبيق" else "App Info")
                    }
                }
            }

            // Step 2: Optional POST_NOTIFICATIONS on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                AmanXCard {
                    Text(
                        text = if (isAr) "2. إشعارات التنبيهات الحرجة (اختياري)" else "2. Critical Alert Notifications (Optional)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isAr) {
                            "يُستخدم فقط لتنبيهك فوراً عند اكتشاف مؤشرات خطر مرتفعة أو حرجة."
                        } else {
                            "Used solely to notify you promptly when high or critical risk indicators are detected."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        enabled = !postGranted,
                        onClick = {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    ) {
                        Text(
                            if (postGranted) {
                                if (isAr) "تم منح الإذن" else "Granted"
                            } else {
                                if (isAr) "طلب الإذن" else "Grant Permission"
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (isAr) "الدخول إلى لوحة AMAN X" else "Enter AMAN X Dashboard")
            }
        }
    }
}

@Composable
fun AppLockGateScreen(
    language: AppLanguage,
    biometricEnabled: Boolean,
    lockoutUntilEpochMs: Long,
    onVerifyPin: (String, (Boolean) -> Unit) -> Unit,
    onBiometricSuccess: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val now = System.currentTimeMillis()
    val isLockedOut = lockoutUntilEpochMs > now

    fun triggerBiometric() {
        val activity = context.findActivity() as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onBiometricSuccess()
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (isAr) "فتح AMAN X" else "Unlock AMAN X")
            .setSubtitle(if (isAr) "استخدم البصمة أو التعرف البيومتري" else "Authenticate with biometrics")
            .setNegativeButtonText(if (isAr) "استخدام PIN" else "Use PIN")
            .build()
        prompt.authenticate(promptInfo)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isAr) "AMAN X مقفل" else "AMAN X Locked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                label = { Text(if (isAr) "أدخل رمز PIN" else "Enter PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                enabled = !isLockedOut,
                modifier = Modifier.fillMaxWidth()
            )

            if (isLockedOut) {
                val secs = ((lockoutUntilEpochMs - now) / 1000L).coerceAtLeast(1L)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isAr) "تم تقييد المحاولات مؤقتاً. انتظر $secs ثانية." else "Temporarily locked. Wait ${secs}s.",
                    color = MaterialTheme.colorScheme.error
                )
            } else if (errorMsg != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                enabled = !isLockedOut && pin.length >= 4,
                onClick = {
                    onVerifyPin(pin) { ok ->
                        if (!ok) {
                            errorMsg = if (isAr) "رمز PIN غير صحيح" else "Incorrect PIN"
                            pin = ""
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (isAr) "فتح التطبيق" else "Unlock")
            }

            if (biometricEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { triggerBiometric() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(if (isAr) "الفتح بالبصمة" else "Unlock with Biometrics")
                }
            }
        }
    }
}
