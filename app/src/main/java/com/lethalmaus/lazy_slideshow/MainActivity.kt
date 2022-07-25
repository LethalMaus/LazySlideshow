package com.lethalmaus.lazy_slideshow

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
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {

    private val onlyWorkDays = true
    private val startHour = 8
    private val startMin = 0
    private val endHour = 17
    private val endMin = 0
    private val slideTimeInMillis = 60000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        requestPermission()

        val today = Date()
        val calendar: Calendar = GregorianCalendar.getInstance()
        calendar.clear()
        calendar.timeZone = TimeZone.getDefault()
        calendar.time = today
        if (onlyWorkDays && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            calendar.add(Calendar.DATE, 3)
        } else {
            calendar.add(Calendar.DATE, 1)
        }
        calendar.set(Calendar.HOUR_OF_DAY, startHour)
        calendar.set(Calendar.MINUTE, startMin)
        calendar.time

        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val mIntent = Intent(applicationContext, Receiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                1,
                mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }

        GlobalScope.launch {
            var imageNumber = 1
            while (true) {
                var id = resources.getIdentifier("image$imageNumber", "drawable", packageName)
                if (id == 0) {
                    imageNumber = 1
                    id = resources.getIdentifier("image$imageNumber", "drawable", packageName)
                }
                imageNumber++
                runOnUiThread {
                    findViewById<ImageView>(R.id.slide).setImageResource(id)
                }
                delay(slideTimeInMillis)
                Calendar.getInstance().apply {
                    if (this.get(Calendar.HOUR_OF_DAY) >= endHour && this.get(Calendar.MINUTE) >= endMin) {
                        finish()
                    }
                }
            }
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + this.packageName)
                )
                startActivityForResult(intent, 5)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 5 && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Cannot do without permission", Toast.LENGTH_SHORT).show()
            finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}