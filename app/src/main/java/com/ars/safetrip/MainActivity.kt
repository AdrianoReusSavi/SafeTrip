package com.ars.safetrip

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.bt_login).setOnClickListener {
            val intent = Intent(this, VideoActivity::class.java)
            this.startActivityForResult(intent, 1)
        }
    }
}