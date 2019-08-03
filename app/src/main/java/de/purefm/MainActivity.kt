package de.purefm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.cast.framework.CastButtonFactory
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val broadcastReceiver: BroadcastReceiver by lazy {
        object: BroadcastReceiver() {
            override fun onReceive(p0: Context?,
                                   p1: Intent?) {
                when (p1?.getIntExtra("status", -1)) {
                    MediaService.Status.PLAYING.ordinalInt -> button.isSelected = true
                    MediaService.Status.STOPPED.ordinalInt -> button.isSelected = true
                }
            }
        }
    }

    private val filter: IntentFilter by lazy {
        IntentFilter("status")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button.isSelected = savedInstanceState?.getBoolean("playing") ?: false

        button.setOnClickListener {
            val intent = Intent()
            intent.setClass(this, MediaService::class.java)

            if (button.isSelected) {
                intent.putExtra("command", MediaService.Command.STOP.ordinalInt)
            } else {
                intent.putExtra("command", MediaService.Command.PLAY_LOCAL.ordinalInt)
            }

            startService(intent)
        }

        registerReceiver(broadcastReceiver, filter)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu, menu)
        CastButtonFactory.setUpMediaRouteButton(applicationContext, menu, R.id.media_route_menu_item)
        return true
    }

    // <img alt="play" src="play1.png" onclick="soundManager.play('purestream')" align="middle" height="20" width="20">
    //
    // http://radionetz.de:8000/purefm-bln.mp3
    // stream contains mp3 metadata.  Title Genre (comma separated), Now Playing
    //
    // Stereo / 44.1 / 32 bits per sample, bitrate 128 kb/s


}
