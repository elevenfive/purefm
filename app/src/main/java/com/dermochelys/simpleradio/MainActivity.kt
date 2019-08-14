package com.dermochelys.simpleradio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastState
import com.dermochelys.simpleradio.MediaService.Command.INIT
import kotlinx.android.synthetic.main.activity_main.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private val filter: IntentFilter by lazy { IntentFilter(SERVICE_STATUS_ACTION) }

    private val broadcastReceiver: BroadcastReceiver by lazy {
        object: BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) { p1?.let { onReceive(it) } }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate $savedInstanceState")

        setContentView(R.layout.activity_main)

        button.isSelected = savedInstanceState?.getSerializable("command") == MediaService.Command.PLAY
        button.setOnClickListener { onButtonClick() }

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter)
        startService(mediaServiceIntent(applicationContext, INIT))
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart")
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        Log.d(TAG, "onCreateOptionsMenu $menu")
        menuInflater.inflate(R.menu.menu, menu)

        CastButtonFactory.setUpMediaRouteButton(applicationContext, menu, R.id.media_route_menu_item)
        Log.d(CAST_TAG, "setUpMediaRouteButton")

        return true
    }

    // http://radionetz.de:8000/purefm-bln.mp3
    // stream contains mp3 metadata.  Title Genre (comma separated), Now Playing
    //
    // Stereo / 44.1 / 32 bits per sample, bitrate 128 kb/s

    private fun onButtonClick() {
        val command = if (button.isSelected) {
            MediaService.Command.STOP
        } else {
            MediaService.Command.PLAY
        }

        startService(mediaServiceIntent(applicationContext, command))
    }

    private fun onReceive(p1: Intent) {
        Log.d(TAG, "onReceive $p1 ${p1.extras}")

        val status = p1.getIntExtra("status", -1)
        val castState = p1.getIntExtra("castState", -1)
        button.isEnabled = status != MediaService.Status.STARTING.ordinalInt && castState != CastState.CONNECTING

        when (status) {
            MediaService.Status.PLAYING.ordinalInt -> {
                button.isSelected = true
                track_title_text_view.text = p1.getStringExtra("title")
            }

            MediaService.Status.STOPPED.ordinalInt -> {
                button.isSelected = false
                track_title_text_view.text = null
            }
        }
    }
}
