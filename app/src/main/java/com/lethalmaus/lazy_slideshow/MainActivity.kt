package com.lethalmaus.lazy_slideshow

import android.app.Activity
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var alarmManager: AlarmManager? = null
    private val calendar: Calendar = GregorianCalendar.getInstance()
    private lateinit var apps: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        apps = resources.getStringArray(R.array.apps)

        requestPermission()

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val keyguardManager = applicationContext.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        //The above doesn't always work on Android 10, below can cause lifecycle problems though...
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        setContentView(R.layout.activity_main)

        viewModel.imageId.observe(this) {
            Calendar.getInstance().apply {
                val currentHour = this.get(Calendar.HOUR_OF_DAY)
                val currentMin = this.get(Calendar.MINUTE)
                if (currentHour > endHour || (currentHour == endHour && currentMin >= endMin)) {
                    setNextStart()
                    finish()
                }
            }
            findViewById<ImageView>(R.id.slide).setImageResource(it)
        }

        viewModel.cycleImages(resources.obtainTypedArray(R.array.images), this::cycleApps)
    }

    private fun setNextStart() {
        calendar.clear()
        calendar.timeZone = TimeZone.getDefault()
        calendar.time = Date()
        if (onlyWorkDays && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            calendar.add(Calendar.DATE, 3)
        } else {
            calendar.add(Calendar.DATE, 1)
        }
        calendar.set(Calendar.HOUR_OF_DAY, startHour)
        calendar.set(Calendar.MINUTE, startMin)
        calendar.time

        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            PendingIntent.getBroadcast(
                applicationContext,
                1,
                Intent(applicationContext, Receiver::class.java),
                PendingIntent.FLAG_CANCEL_CURRENT
            )
        )
    }

    private fun cycleApps() {
        calendar.clear()
        calendar.timeZone = TimeZone.getDefault()
        calendar.time = Date()
        if (apps.isEmpty()) {
            viewModel.cycleImages(resources.obtainTypedArray(R.array.images))
            return
        }
        apps.drop(1).forEach {
            calendar.set(Calendar.MILLISECOND, calendar.get(Calendar.MILLISECOND) + slideTimeInMillis.toInt())
            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    getAppIntent(it),
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
            )
        }
        apps.getOrNull(0)?.let {
            calendar.set(Calendar.MILLISECOND, calendar.get(Calendar.MILLISECOND) + slideTimeInMillis.toInt())
            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                PendingIntent.getBroadcast(
                    applicationContext,
                    1,
                    Intent(applicationContext, Receiver::class.java),
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
            )
            startActivity(getAppIntent(it))
        }
    }

    private fun getAppIntent(appName: String): Intent {
        intent = if (appName.startsWith("http")) {
            Intent(Intent.ACTION_VIEW, Uri.parse(appName))
        } else {
            packageManager.getLaunchIntentForPackage(appName)
        }
        if (intent == null) {
            intent = Intent(Intent.ACTION_VIEW)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.data = Uri.parse("market://details?id=$appName")
        }
        return intent
    }

    private fun requestPermission() {
        if (!Settings.canDrawOverlays(this)) {
            startForResult.launch(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${this.packageName}")
            ))
        }
    }

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Cannot do without required manage overlay permission", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}