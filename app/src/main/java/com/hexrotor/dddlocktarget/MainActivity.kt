package com.hexrotor.dddlocktarget

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast


class MainActivity : Activity() {
    @SuppressLint("WorldReadableFiles", "UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lateinit var log : String
        var showToast = false
        try {
            val prefs = getSharedPreferences("function_config", Context.MODE_WORLD_READABLE)


            val switches = listOf(
                Pair(R.id.striker_lock, "striker_lock"),
                Pair(R.id.scorpio_lock, "scorpio_lock"),
                Pair(R.id.striker_remember_target, "striker_remember_target"),
                Pair(R.id.scorpio_remember_target, "scorpio_remember_target")
            )

            switches.forEach { (id, key) ->
                val switch: Switch = findViewById(id)
                val editor = prefs.edit()
                switch.isChecked = prefs.getBoolean(key, false)
                switch.setOnCheckedChangeListener { _, isChecked ->
                    editor.putBoolean(key, isChecked)
                    editor.apply()
                    logUtil(prefs.all.toString(), false)
                }
            }
            log = prefs.all.toString()
            showToast = false
        } catch (e: Exception) {
            log = "You need to enable this module in Xposed Manager first.\n$e"
            showToast = true
        } finally {
            logUtil(log, showToast)
        }
    }
    private fun logUtil(log: String, showToast : Boolean) {
        val logView : TextView = findViewById(R.id.logView)
        logView.text = log
        Log.d("DDD", log)
        if (showToast) Toast.makeText(this, log, Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        getMenuInflater().inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Hexrotor/TOAndroidHack"))
        startActivity(browserIntent)
        return true
    }
}