package de.purefm

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.C.TIME_UNSET
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.icy.IcyInfo
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.util.Util.getUserAgent
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private var castPlayer: CastPlayer? = null
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button.isSelected = true
        button.isEnabled = true
    }

    override fun onStart() {
        super.onStart()

        val dataSourceFactory = DefaultHttpDataSourceFactory(getUserAgent(this, "pure-fm.de (Android)"))

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse("http://radionetz.de:8000/purefm-bln.mp3"))

        val simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(this)
        simpleExoPlayer.prepare(mediaSource)
        simpleExoPlayer.playWhenReady = false

        simpleExoPlayer.addAnalyticsListener(object: AnalyticsListener {
            override fun onSeekProcessed(eventTime: AnalyticsListener.EventTime?) {
                Log.w("SimpleExoPlayer", "onSeekProcessed")
            }

            override fun onPlaybackParametersChanged(
                eventTime: AnalyticsListener.EventTime?,
                playbackParameters: PlaybackParameters?
            ) {
                Log.w("SimpleExoPlayer", "onPlaybackParametersChanged $playbackParameters")
            }

            override fun onPlayerError(eventTime: AnalyticsListener.EventTime?,
                                       error: ExoPlaybackException?) {
                Log.w("SimpleExoPlayer", "onPlayerError $error")
            }

            override fun onSeekStarted(eventTime: AnalyticsListener.EventTime?) {
                Log.w("SimpleExoPlayer", "onSeekStarted")
            }

            override fun onLoadingChanged(eventTime: AnalyticsListener.EventTime?,
                                          isLoading: Boolean) {
                Log.w("SimpleExoPlayer", "onLoadingChanged: isLoading=$isLoading")
            }

            override fun onDownstreamFormatChanged(
                eventTime: AnalyticsListener.EventTime?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?
            ) {
                Log.d("SimpleExoPlayer", "onDownstreamFormatChanged $mediaLoadData")
            }

            override fun onMediaPeriodCreated(eventTime: AnalyticsListener.EventTime?) {
                Log.d("SimpleExoPlayer", "onMediaPeriodCreated")
            }

            override fun onReadingStarted(eventTime: AnalyticsListener.EventTime?) {
                Log.d("SimpleExoPlayer", "onReadingStarted")
            }

            override fun onBandwidthEstimate(
                eventTime: AnalyticsListener.EventTime?,
                totalLoadTimeMs: Int,
                totalBytesLoaded: Long,
                bitrateEstimate: Long
            ) {
                Log.d("SimpleExoPlayer", "onBandwidthEstimate $bitrateEstimate")
            }

            override fun onPlayerStateChanged(
                eventTime: AnalyticsListener.EventTime?,
                playWhenReady: Boolean,
                playbackState: Int
            ) {
                when(playbackState) {
                    Player.STATE_IDLE -> Log.d("SimpleExoPlayer", "onPlayerStateChanged STATE_IDLE")
                    Player.STATE_BUFFERING -> Log.d("SimpleExoPlayer", "onPlayerStateChanged STATE_BUFFERING")
                    Player.STATE_READY -> Log.d("SimpleExoPlayer", "onPlayerStateChanged STATE_READY")
                    Player.STATE_ENDED -> Log.d("SimpleExoPlayer", "onPlayerStateChanged STATE_ENDED")
                }
            }

            override fun onAudioAttributesChanged(
                eventTime: AnalyticsListener.EventTime?,
                audioAttributes: AudioAttributes?
            ) {
                Log.d("SimpleExoPlayer", "onAudioAttributesChanged $audioAttributes")
            }

            override fun onVolumeChanged(eventTime: AnalyticsListener.EventTime?,
                                         volume: Float) {
                Log.d("SimpleExoPlayer", "onVolumeChanged $volume")
            }


            override fun onDecoderInputFormatChanged(
                eventTime: AnalyticsListener.EventTime?,
                trackType: Int,
                format: Format?
            ) {
                Log.d("SimpleExoPlayer", "onDecoderInputFormatChanged $format")
            }

            override fun onAudioSessionId(eventTime: AnalyticsListener.EventTime?,
                                          audioSessionId: Int) {
                Log.d("SimpleExoPlayer", "onAudioSessionId $audioSessionId")
            }

            override fun onLoadStarted(
                eventTime: AnalyticsListener.EventTime?,
                loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?
            ) {
                Log.d("SimpleExoPlayer", "onLoadStarted $loadEventInfo")
            }

            override fun onTracksChanged(
                eventTime: AnalyticsListener.EventTime?,
                trackGroups: TrackGroupArray?,
                trackSelections: TrackSelectionArray?
            ) {
                Log.d("SimpleExoPlayer", "onTracksChanged $trackSelections")
            }

            override fun onPositionDiscontinuity(eventTime: AnalyticsListener.EventTime?,
                                                 reason: Int) {
                Log.d("SimpleExoPlayer", "onPositionDiscontinuity $reason")
            }

            override fun onUpstreamDiscarded(
                eventTime: AnalyticsListener.EventTime?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?
            ) {
                Log.d("SimpleExoPlayer", "onUpstreamDiscarded $mediaLoadData")
            }

            override fun onLoadCanceled(
                eventTime: AnalyticsListener.EventTime?,
                loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?
            ) {
                Log.d("SimpleExoPlayer", "onLoadCanceled $mediaLoadData")
            }

            override fun onMediaPeriodReleased(eventTime: AnalyticsListener.EventTime?) {
                Log.d("SimpleExoPlayer", "onMediaPeriodReleased")
            }

            override fun onTimelineChanged(eventTime: AnalyticsListener.EventTime?,
                                           reason: Int) {
                Log.d("SimpleExoPlayer", "onTimelineChanged $reason")
            }

            override fun onDecoderInitialized(
                eventTime: AnalyticsListener.EventTime?,
                trackType: Int,
                decoderName: String?,
                initializationDurationMs: Long
            ) {
                Log.d("SimpleExoPlayer", "onDecoderInitialized $decoderName")
            }

            override fun onAudioUnderrun(
                eventTime: AnalyticsListener.EventTime?,
                bufferSize: Int,
                bufferSizeMs: Long,
                elapsedSinceLastFeedMs: Long
            ) {
                Log.w("SimpleExoPlayer", "onAudioUnderrun $bufferSizeMs")
            }

            override fun onLoadCompleted(
                eventTime: AnalyticsListener.EventTime?,
                loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?
            ) {
                Log.d("SimpleExoPlayer", "onLoadCompleted $mediaLoadData")
            }

            override fun onLoadError(
                eventTime: AnalyticsListener.EventTime?,
                loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?,
                error: IOException?,
                wasCanceled: Boolean
            ) {
                Log.e("SimpleExoPlayer", "onLoadError $error")
            }

            override fun onMetadata(eventTime: AnalyticsListener.EventTime?,
                                    metadata: Metadata?) {
                val entry: IcyInfo = metadata?.get(0) as IcyInfo
                val title = entry.title
                Log.d("SimpleExoPlayer", "onMetadata title=$title")
                track_title_text_view.text = getString(R.string.now_on_air, title)

            }
        })

        exoPlayer = simpleExoPlayer

        val url  = "http://radionetz.de:8000/purefm-bln.mp3"
        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
        movieMetadata.putString(MediaMetadata.KEY_TITLE, "pure-fm.de")

        val mediaInfo: MediaInfo  = MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(MimeTypes.AUDIO_MPEG)
            .setMetadata(movieMetadata).build()

        val mediaQueueItem = MediaQueueItem.Builder(mediaInfo).build()
        val castContext = CastContext.getSharedInstance(this)
        val player = CastPlayer(castContext)
        player.playWhenReady = false
        castPlayer = player

        player.addListener(object: Player.EventListener {
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
                Log.d("CastPlayer", "onPlaybackParametersChanged $playbackParameters")
            }

            override fun onSeekProcessed() {
                Log.d("CastPlayer", "onSeekProcessed")
            }

            override fun onTracksChanged(trackGroups: TrackGroupArray?,
                                         trackSelections: TrackSelectionArray?) {
                Log.d("CastPlayer", "onTracksChanged $trackSelections")
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                Log.d("CastPlayer", "onPlayerError $error")
            }

            override fun onLoadingChanged(isLoading: Boolean) {
                Log.d("CastPlayer", "onLoadingChanged $isLoading")
            }

            override fun onPositionDiscontinuity(reason: Int) {
                Log.d("CastPlayer", "onPositionDiscontinuity $reason")
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                Log.d("CastPlayer", "onRepeatModeChanged $repeatMode")
            }

            override fun onTimelineChanged(timeline: Timeline?,
                                           manifest: Any?,
                                           reason: Int) {
                when(reason) {
                    Player.TIMELINE_CHANGE_REASON_PREPARED -> Log.d("CastPlayer", "onTimelineChanged TIMELINE_CHANGE_REASON_PREPARED")
                    Player.TIMELINE_CHANGE_REASON_RESET -> Log.d("CastPlayer", "onTimelineChanged TIMELINE_CHANGE_REASON_RESET")
                    Player.TIMELINE_CHANGE_REASON_DYNAMIC -> Log.d("CastPlayer", "onTimelineChanged TIMELINE_CHANGE_REASON_DYNAMIC")
                }
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean,
                                              playbackState: Int) {
                when(playbackState) {
                    Player.STATE_IDLE -> Log.d("CastPlayer", "onPlayerStateChanged STATE_IDLE")
                    Player.STATE_BUFFERING -> Log.d("CastPlayer", "onPlayerStateChanged STATE_BUFFERING")
                    Player.STATE_READY -> Log.d("CastPlayer", "onPlayerStateChanged STATE_READY")
                    Player.STATE_ENDED -> Log.d("CastPlayer", "onPlayerStateChanged STATE_ENDED")
                }
            }
        })

        player.setSessionAvailabilityListener(object: SessionAvailabilityListener {
            override fun onCastSessionAvailable() {
                Log.d("CastPlayer", "onCastSessionAvailable")
                player.loadItem(mediaQueueItem, TIME_UNSET)
            }

            override fun onCastSessionUnavailable() {
                Log.d("CastPlayer", "onCastSessionUnavailable")
                castPlayer = null

            }
        })


    }

    override fun onStop() {
        exoPlayer?.stop(true)
        exoPlayer?.release()
        castPlayer?.stop(true)
        castPlayer?.release()
        super.onStop()
    }

    override fun onDestroy() {
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
