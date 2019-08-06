package de.purefm

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
import kotlinx.android.synthetic.main.activity_main.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private var castMenu: Menu? = null
    private var readyToSetup = false
    private var setup = false

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

        button.setOnClickListener {
            val intent = serviceIntent()

            if (button.isSelected) {
                intent.putExtra("command", MediaService.Command.STOP.ordinalInt)
            } else {
                intent.putExtra("command", MediaService.Command.PLAY.ordinalInt)
            }

            startService(intent)
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter)
        startService(serviceIntent())
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
        castMenu = menu

        if (readyToSetup) {
            setupMediaRouteButton(menu)
        }

        return true
    }

    // http://radionetz.de:8000/purefm-bln.mp3
    // stream contains mp3 metadata.  Title Genre (comma separated), Now Playing
    //
    // Stereo / 44.1 / 32 bits per sample, bitrate 128 kb/s

    private fun onReceive(p1: Intent) {
        Log.d(TAG, "onReceive $p1 ${p1.extras}")

        when (p1.getIntExtra("status", -1)) {
            MediaService.Status.PLAYING.ordinalInt -> {
                button.isSelected = true
                track_title_text_view.text = p1.getStringExtra("title")
            }

            MediaService.Status.STOPPED.ordinalInt -> {
                button.isSelected = false
                track_title_text_view.text = null
            }
        }

        if (!setup) {
            markReadyToSetupMediaRouteButton()
            castMenu?.let { setupMediaRouteButton(it) }
        }
    }

    private fun markReadyToSetupMediaRouteButton() {
        readyToSetup = true
    }

    private fun setupMediaRouteButton(menu: Menu) {
        CastButtonFactory.setUpMediaRouteButton(applicationContext, menu, R.id.media_route_menu_item)
        Log.d(CAST_TAG, "setUpMediaRouteButton")
        setup = true
    }

    private fun serviceIntent(): Intent {
        val intent = Intent()
        intent.setClass(this, MediaService::class.java)
        return intent
    }
}
