package de.purefm

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.IBinder
import android.util.Log
import com.google.android.exoplayer2.*
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
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import java.io.FileDescriptor
import java.io.IOException
import java.io.PrintWriter

class MediaService(private val castContext: CastContext = CastContext.getSharedInstance(MainApplication.instance)): Service() {
    enum class Command(val ordinalInt: Int) {
        PLAY_LOCAL(0), PLAY_CAST(1), STOP(2), SEND_STATUS(3)
    }

    enum class Status(val ordinalInt: Int) {
        STOPPED(0), STARTING(1), PLAYING(2)
    }

    private var castPlayer: CastPlayer? = null

    private var exoPlayer: ExoPlayer? = null

    private var lastCommand: Command = Command.STOP

    private var lastStatus: Status = Status.STOPPED

    override fun onCreate() {
        super.onCreate()
        castContext.addCastStateListener(casteStateListener)

        val simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(this)
        simpleExoPlayer.playWhenReady = false
        simpleExoPlayer.addAnalyticsListener(analyticsListener)
        exoPlayer = simpleExoPlayer

        val player = CastPlayer(castContext)
        player.playWhenReady = false
        player.addListener(eventListener)
        castPlayer = player
    }

    override fun onStartCommand(intent: Intent?,
                                flags: Int,
                                startId: Int): Int {
        when (intent?.getIntExtra("command", -1)) {
            Command.SEND_STATUS.ordinalInt -> {
                sendBroadcast(lastStatus)
                return super.onStartCommand(intent, flags, startId)
            }

            lastCommand.ordinalInt -> {
                return super.onStartCommand(intent, flags, startId)
            }

            Command.PLAY_LOCAL.ordinalInt -> {
                when (lastCommand) {
                    Command.STOP -> {
                        playLocally()
                    }

                    Command.PLAY_CAST -> {
                        stopCastally()
                        playLocally()
                    }

                    else -> { }
                }

                lastCommand = Command.PLAY_LOCAL
            }

            Command.PLAY_CAST.ordinalInt -> {
                when (lastCommand) {
                    Command.STOP -> {
                        playCastally()
                    }

                    Command.PLAY_LOCAL -> {
                        stopLocally()
                        playCastally()
                    }

                    else -> { }
                }
            }

            Command.STOP.ordinalInt -> {
                when (lastCommand) {
                    Command.PLAY_LOCAL -> {
                        stopLocally()
                    }

                    Command.PLAY_CAST -> {
                        stopCastally()
                    }

                    else -> { }
                }
            }

            null -> {
                return super.onStartCommand(intent, flags, startId)
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun playCastally() {
        castPlayer?.apply {
            if (isCastSessionAvailable) {
                loadItem()
            }

            setSessionAvailabilityListener(sessionAvailabilityListener)
        }
    }

    private fun stopCastally() {
        castPlayer?.apply {
            setSessionAvailabilityListener(null)
            stop(true)
            lastStatus = Status.STOPPED
        }
    }

    private fun playLocally() {
        exoPlayer?.apply {
            playWhenReady = true
            prepare(progressiveMediaSource())
        }
    }

    private fun stopLocally() {
        exoPlayer?.stop(true)
    }

    override fun onDestroy() {
        castPlayer?.let {
            it.stop()
            it.release()
            castPlayer = null
        }

        exoPlayer?.let {
            it.stop()
            it.release()
            exoPlayer = null
        }

        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    override fun dump(fd: FileDescriptor?,
                      writer: PrintWriter?,
                      args: Array<out String>?) {
        super.dump(fd, writer, args)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val sessionAvailabilityListener: SessionAvailabilityListener by lazy {
        object : SessionAvailabilityListener {
            override fun onCastSessionAvailable() {
                Log.d("CastPlayer", "onCastSessionAvailable")
                loadItem()
            }

            override fun onCastSessionUnavailable() {
                Log.d("CastPlayer", "onCastSessionUnavailable")
                castPlayer = null
            }
        }
    }

    private fun loadItem() {
        castPlayer?.loadItem(mediaQueueItem(), C.TIME_UNSET)
    }

    private fun progressiveMediaSource() =
        ProgressiveMediaSource.Factory(DefaultHttpDataSourceFactory(Util.getUserAgent(this, "pure-fm.de (Android)")))
            .createMediaSource(Uri.parse("http://radionetz.de:8000/purefm-bln.mp3"))

    private fun mediaQueueItem(): MediaQueueItem {
        val url  = "http://radionetz.de:8000/purefm-bln.mp3"
        val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, "pure-fm.de")

        val mediaInfo: MediaInfo = MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(MimeTypes.AUDIO_MPEG)
            .setMetadata(mediaMetadata).build()

        return MediaQueueItem.Builder(mediaInfo).build()
    }

    private val analyticsListener: AnalyticsListener by lazy {
        object : AnalyticsListener {
            override fun onSeekProcessed(eventTime: AnalyticsListener.EventTime?) {
                Log.w("SimpleExoPlayer", "onSeekProcessed")
            }

            override fun onPlaybackParametersChanged(
                eventTime: AnalyticsListener.EventTime?,
                playbackParameters: PlaybackParameters?
            ) {
                Log.w("SimpleExoPlayer", "onPlaybackParametersChanged $playbackParameters")
            }

            override fun onPlayerError(
                eventTime: AnalyticsListener.EventTime?,
                error: ExoPlaybackException?
            ) {
                Log.w("SimpleExoPlayer", "onPlayerError $error")
            }

            override fun onSeekStarted(eventTime: AnalyticsListener.EventTime?) {
                Log.w("SimpleExoPlayer", "onSeekStarted")
            }

            override fun onLoadingChanged(
                eventTime: AnalyticsListener.EventTime?,
                isLoading: Boolean
            ) {
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
                when (playbackState) {
                    Player.STATE_IDLE -> {
                        lastStatus = Status.STOPPED
                        sendBroadcast(Status.STOPPED)
                        Log.d("SimpleExoPlayer", "onPlayerStateChanged STATE_IDLE")
                    }

                    Player.STATE_BUFFERING -> {
                        Log.d("SimpleExoPlayer", "onPlayerStateChanged STATE_BUFFERING")
                        lastStatus = Status.STARTING
                        sendBroadcast(Status.STARTING)
                    }

                    Player.STATE_READY -> {
                        Log.d("SimpleExoPlayer", "onPlayerStateChanged STATE_READY")
                        lastStatus = Status.PLAYING
                        sendBroadcast(Status.PLAYING)
                    }

                    Player.STATE_ENDED -> {
                        lastStatus = Status.STOPPED
                        sendBroadcast(Status.STOPPED)
                        Log.d("SimpleExoPlayer", "onPlayerStateChanged STATE_ENDED")
                    }
                }
            }

            override fun onAudioAttributesChanged(
                eventTime: AnalyticsListener.EventTime?,
                audioAttributes: AudioAttributes?
            ) {
                Log.d("SimpleExoPlayer", "onAudioAttributesChanged $audioAttributes")
            }

            override fun onVolumeChanged(
                eventTime: AnalyticsListener.EventTime?,
                volume: Float
            ) {
                Log.d("SimpleExoPlayer", "onVolumeChanged $volume")
            }

            override fun onDecoderInputFormatChanged(
                eventTime: AnalyticsListener.EventTime?,
                trackType: Int,
                format: Format?
            ) {
                Log.d("SimpleExoPlayer", "onDecoderInputFormatChanged $format")
            }

            override fun onAudioSessionId(
                eventTime: AnalyticsListener.EventTime?,
                audioSessionId: Int
            ) {
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

            override fun onPositionDiscontinuity(
                eventTime: AnalyticsListener.EventTime?,
                reason: Int
            ) {
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

            override fun onTimelineChanged(
                eventTime: AnalyticsListener.EventTime?,
                reason: Int
            ) {
                when (reason) {
                    Player.TIMELINE_CHANGE_REASON_PREPARED -> Log.d(
                        "SimpleExoPlayer",
                        "onTimelineChanged TIMELINE_CHANGE_REASON_PREPARED"
                    )

                    Player.TIMELINE_CHANGE_REASON_RESET -> Log.d(
                        "SimpleExoPlayer",
                        "onTimelineChanged TIMELINE_CHANGE_REASON_RESET"
                    )

                    Player.TIMELINE_CHANGE_REASON_DYNAMIC -> Log.d(
                        "SimpleExoPlayer",
                        "onTimelineChanged TIMELINE_CHANGE_REASON_DYNAMIC"
                    )
                }
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

            override fun onMetadata(
                eventTime: AnalyticsListener.EventTime?,
                metadata: Metadata?
            ) {
                val entry: IcyInfo = metadata?.get(0) as IcyInfo
                Log.d("SimpleExoPlayer", "onMetadata title=$entry.title")
            }
        }
    }

    private fun sendBroadcast(status: Status) {
        val broadcast = Intent()
        broadcast.putExtra("status", status)
        sendBroadcast(broadcast)
    }

    private val eventListener: Player.EventListener by lazy {
        object : Player.EventListener {
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
                Log.d("CastPlayer", "onPlaybackParametersChanged $playbackParameters")
            }

            override fun onSeekProcessed() {
                Log.d("CastPlayer", "onSeekProcessed")
            }

            override fun onTracksChanged(
                trackGroups: TrackGroupArray?,
                trackSelections: TrackSelectionArray?
            ) {
                Log.d("CastPlayer", "onTracksChanged ${trackSelections?.get(0)}")
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

            override fun onTimelineChanged(
                timeline: Timeline?,
                manifest: Any?,
                reason: Int
            ) {
                when (reason) {
                    Player.TIMELINE_CHANGE_REASON_PREPARED -> Log.d(
                        "CastPlayer",
                        "onTimelineChanged TIMELINE_CHANGE_REASON_PREPARED"
                    )

                    Player.TIMELINE_CHANGE_REASON_RESET -> Log.d(
                        "CastPlayer",
                        "onTimelineChanged TIMELINE_CHANGE_REASON_RESET"
                    )

                    Player.TIMELINE_CHANGE_REASON_DYNAMIC -> Log.d(
                        "CastPlayer",
                        "onTimelineChanged TIMELINE_CHANGE_REASON_DYNAMIC"
                    )
                }
            }

            override fun onPlayerStateChanged(
                playWhenReady: Boolean,
                playbackState: Int
            ) {
                when (playbackState) {
                    Player.STATE_IDLE -> Log.d("CastPlayer", "onPlayerStateChanged STATE_IDLE")

                    Player.STATE_BUFFERING -> {
                        Log.d("CastPlayer", "onPlayerStateChanged STATE_BUFFERING")
                        sendBroadcast(STARTING)
                    }

                    Player.STATE_READY -> {
                        Log.d("CastPlayer", "onPlayerStateChanged STATE_READY")
                        sendBroadcast(PLAYING)
                    }

                    Player.STATE_ENDED -> Log.d("CastPlayer", "onPlayerStateChanged STATE_ENDED")
                }
            }
        }
    }

    private val casteStateListener: CastStateListener by lazy {
        CastStateListener { p0 ->
            when (p0) {
                CastState.NO_DEVICES_AVAILABLE -> Log.d("CastPlayer", "onCastStateChanged: NO_DEVICES_AVAILABLE")
                CastState.NOT_CONNECTED -> Log.d("CastPlayer", "onCastStateChanged: NOT_CONNECTED")
                CastState.CONNECTING -> Log.d("CastPlayer", "onCastStateChanged: CONNECTING")
                CastState.CONNECTED -> Log.d("CastPlayer", "onCastStateChanged: CONNECTED")
            }
        }
    }
}
