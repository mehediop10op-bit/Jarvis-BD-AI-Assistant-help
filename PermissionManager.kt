package com.example.manager

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

enum class PermissionType(
    val permissionString: String,
    val titleBengali: String,
    val titleEnglish: String,
    val descriptionBengali: String,
    val descriptionEnglish: String,
    val isEssential: Boolean
) {
    MICROPHONE(
        permissionString = Manifest.permission.RECORD_AUDIO,
        titleBengali = "মাইক্রোফোন (Microphone)",
        titleEnglish = "Microphone",
        descriptionBengali = "ভয়েস কমান্ড গ্রহণ ও 'Hi Jarvis' ওয়েক-ওয়ার্ড শোনার জন্য আবশ্যক।",
        descriptionEnglish = "Required for recognizing voice commands and 'Hi Jarvis' wake-word.",
        isEssential = true
    ),
    CONTACTS(
        permissionString = Manifest.permission.READ_CONTACTS,
        titleBengali = "কন্টাক্টস (Contacts)",
        titleEnglish = "Contacts",
        descriptionBengali = "নাম বলে কল বা এসএমএস প্রেরণের সময় সঠিক নম্বর অনুসন্ধান করতে ব্যবহৃত হয়।",
        descriptionEnglish = "Used to search phonebook contacts when calling or sending SMS.",
        isEssential = true
    ),
    SMS(
        permissionString = Manifest.permission.SEND_SMS,
        titleBengali = "এসএমএস (SMS)",
        titleEnglish = "SMS",
        descriptionBengali = "ব্যবহারকারীর নিশ্চিতকরণ নিয়ে সরাসরি বা কম্পোজারের মাধ্যমে বার্তা প্রেরণের জন্য।",
        descriptionEnglish = "Required to prepare and dispatch SMS messages upon user confirmation.",
        isEssential = true
    ),
    CALL_PHONE(
        permissionString = Manifest.permission.CALL_PHONE,
        titleBengali = "ফোন কল (Call Phone)",
        titleEnglish = "Call Phone",
        descriptionBengali = "ব্যবহারকারীর নিশ্চিতকরণ সাপেক্ষে সরাসরি কল সংযোগের জন্য।",
        descriptionEnglish = "Used to initiate calls upon user confirmation.",
        isEssential = false
    ),
    NOTIFICATIONS(
        permissionString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            ""
        },
        titleBengali = "নোটিফিকেশন (Notifications)",
        titleEnglish = "Notifications",
        descriptionBengali = "ওয়েক-ওয়ার্ডের মাইক্রোফোন স্ট্যাটাস এবং স্মার্ট রিসেপশনিস্ট বার্তা প্রদর্শনে আবশ্যক।",
        descriptionEnglish = "Shows ongoing wake-word microphone status and receptionist missed call alerts.",
        isEssential = false
    ),
    PHONE_STATE(
        permissionString = Manifest.permission.READ_PHONE_STATE,
        titleBengali = "ফোন স্টেট (Phone State)",
        titleEnglish = "Phone State",
        descriptionBengali = "ইনকামিং মিসড কল সনাক্ত করে AI স্মার্ট রিসেপশনিস্ট চালু করার জন্য।",
        descriptionEnglish = "Detects incoming and missed calls to activate AI Smart Receptionist.",
        isEssential = false
    ),
    CAMERA(
        permissionString = Manifest.permission.CAMERA,
        titleBengali = "ক্যামেরা ও ফ্ল্যাশলাইট (Camera)",
        titleEnglish = "Camera & Torch",
        descriptionBengali = "টর্চ লাইট অন/অফ এবং ক্যামেরা দ্রুত চালু করার জন্য।",
        descriptionEnglish = "Used to toggle device flashlight and launch camera quickly.",
        isEssential = false
    )
}

enum class PermissionState {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

data class PermissionDetail(
    val type: PermissionType,
    val state: PermissionState
)

/**
 * State holder that tracks permissions status and provides functions
 * to trigger runtime permission requests in Jetpack Compose.
 */
class PermissionRequesterState(
    val hasAudioPermission: Boolean,
    val hasContactsPermission: Boolean,
    val hasSmsPermission: Boolean,
    val allEssentialGranted: Boolean,
    private val launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    private val onOpenSettings: () -> Unit
) {
    /**
     * Request all essential permissions (RECORD_AUDIO, READ_CONTACTS, SEND_SMS).
     */
    fun requestEssentialPermissions() {
        launcher.launch(PermissionManager.ESSENTIAL_PERMISSIONS)
    }

    /**
     * Request only Microphone (RECORD_AUDIO) permission.
     */
    fun requestMicrophonePermission() {
        launcher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
    }

    /**
     * Request only Contacts (READ_CONTACTS) permission.
     */
    fun requestContactsPermission() {
        launcher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
    }

    /**
     * Request only SMS (SEND_SMS) permission.
     */
    fun requestSmsPermission() {
        launcher.launch(arrayOf(Manifest.permission.SEND_SMS))
    }

    /**
     * Open app details in system settings for manual permission granting.
     */
    fun openAppSettings() {
        onOpenSettings()
    }
}

/**
 * Jetpack Compose helper to check current status of essential permissions
 * (RECORD_AUDIO, READ_CONTACTS, SEND_SMS) and provide an actionable request flow.
 *
 * Automatically refreshes statuses when returning to the app from System Settings.
 */
@Composable
fun rememberPermissionRequester(
    onPermissionsResult: (allGranted: Boolean) -> Unit = {}
): PermissionRequesterState {
    val context = LocalContext.current

    var hasAudio by remember { mutableStateOf(PermissionManager.hasAudioPermission(context)) }
    var hasContacts by remember { mutableStateOf(PermissionManager.hasContactsPermission(context)) }
    var hasSms by remember { mutableStateOf(PermissionManager.hasSmsPermission(context)) }

    fun refresh() {
        hasAudio = PermissionManager.hasAudioPermission(context)
        hasContacts = PermissionManager.hasContactsPermission(context)
        hasSms = PermissionManager.hasSmsPermission(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        refresh()
        val allGranted = results.values.all { it }
        onPermissionsResult(allGranted)
    }

    return remember(hasAudio, hasContacts, hasSms, launcher) {
        PermissionRequesterState(
            hasAudioPermission = hasAudio,
            hasContactsPermission = hasContacts,
            hasSmsPermission = hasSms,
            allEssentialGranted = hasAudio && hasContacts && hasSms,
            launcher = launcher,
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        )
    }
}

class PermissionManager(private val context: Context) {

    fun isGranted(type: PermissionType): Boolean {
        if (type.permissionString.isBlank()) return true
        return isPermissionGranted(context, type.permissionString)
    }

    fun hasAudioPermission(): Boolean = hasAudioPermission(context)
    fun hasContactsPermission(): Boolean = hasContactsPermission(context)
    fun hasSmsPermission(): Boolean = hasSmsPermission(context)

    fun getPermissionState(activity: Activity?, type: PermissionType): PermissionState {
        if (type.permissionString.isBlank()) return PermissionState.GRANTED
        val isGranted = isGranted(type)

        if (isGranted) return PermissionState.GRANTED

        if (activity != null) {
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                type.permissionString
            )
            return if (!shouldShowRationale) PermissionState.DENIED else PermissionState.DENIED
        }

        return PermissionState.DENIED
    }

    fun getAllPermissionsDetails(activity: Activity? = null): List<PermissionDetail> {
        return PermissionType.values().map { type ->
            PermissionDetail(
                type = type,
                state = getPermissionState(activity, type)
            )
        }
    }

    fun getRequiredRuntimePermissions(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.toTypedArray()
    }

    fun areEssentialPermissionsGranted(): Boolean {
        return areEssentialPermissionsGranted(context)
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    companion object {
        /**
         * The essential permissions array: RECORD_AUDIO, READ_CONTACTS, SEND_SMS.
         */
        val ESSENTIAL_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS
        )

        /**
         * Checks if a specific permission string is granted.
         */
        fun isPermissionGranted(context: Context, permission: String): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Checks if RECORD_AUDIO is granted.
         */
        fun hasAudioPermission(context: Context): Boolean {
            return isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)
        }

        /**
         * Checks if READ_CONTACTS is granted.
         */
        fun hasContactsPermission(context: Context): Boolean {
            return isPermissionGranted(context, Manifest.permission.READ_CONTACTS)
        }

        /**
         * Checks if SEND_SMS is granted.
         */
        fun hasSmsPermission(context: Context): Boolean {
            return isPermissionGranted(context, Manifest.permission.SEND_SMS)
        }

        /**
         * Checks if all essential permissions (audio, contacts, and SMS) are granted.
         */
        fun areEssentialPermissionsGranted(context: Context): Boolean {
            return hasAudioPermission(context) &&
                    hasContactsPermission(context) &&
                    hasSmsPermission(context)
        }
    }
}
