package de.purefm

import android.app.*
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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

const val CAST_TAG =  "purefm_cast"
const val LOCAL_TAG = "purefm_local"
const val SERVICE_STATUS_ACTION = "de.purefm.SERVICE_STATUS"
const val CHANNEL_ID = "106.4"

class MediaService(private val castContext: CastContext = CastContext.getSharedInstance(MainApplication.instance)): Service() {
    enum class Status(val ordinalInt: Int) {
        STOPPED(0), STARTING(1), PLAYING(2);
    }

    enum class Source(val ordinalInt: Int) {
        LOCAL(0), CAST(1);
    }

    enum class Command(val ordinalInt: Int) {
        INIT(0), PLAY(1), STOP(2);

        companion object {
            fun fromOrdinalInt(ordinalInt: Int): Command = when (ordinalInt) {
                PLAY.ordinalInt -> PLAY
                STOP.ordinalInt -> STOP
                INIT.ordinalInt -> INIT
                else -> throw IllegalArgumentException()
            }
        }
    }

    private var castPlayer: CastPlayer? = null

    private var exoPlayer: ExoPlayer? = null

    private var lastCommand: MutableMap<Source, Command> = mutableMapOf(
        Pair(Source.LOCAL, Command.STOP),
        Pair(Source.CAST, Command.STOP))

    private var lastStatus: MutableMap<Source, Status> = mutableMapOf(
        Pair(Source.LOCAL, Status.STOPPED),
        Pair(Source.CAST, Status.STOPPED))

    private var lastSource: Source = Source.LOCAL

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
        val commandOrdinalInt = intent?.getIntExtra("command", Command.INIT.ordinalInt) ?: Command.INIT.ordinalInt
        val command = Command.fromOrdinalInt(commandOrdinalInt)

        if (lastCommand[lastSource] != command) {
            when (command) {
                Command.INIT -> { }

                Command.PLAY -> {
                    play(lastSource)
                    startForeground()
                }

                Command.STOP -> {
                    stop(lastSource)
                    stopForeground(true)
                }
            }
        }

        sendBroadcast()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "channel"
            val descriptionText = "description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { intent ->
                PendingIntent.getActivity(this, 0, intent, 0)
            }

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("foo")
            .setContentText("foo")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("foo")
            .build()

        startForeground(1, notification)
    }

    private fun play(source: Source) {
        when (source) {
            Source.CAST -> playCastally()
            Source.LOCAL -> playLocally()
        }
    }

    private fun stop(source: Source) {
        when (source) {
            Source.CAST -> stopCastally()
            Source.LOCAL -> stopLocally()
        }
    }

    private fun playCastally() {
        if (lastStatus[Source.CAST] != Status.STOPPED) {
            return
        }

        lastSource = Source.CAST
        lastStatus[Source.CAST] = Status.STARTING
        lastCommand[Source.CAST] = Command.PLAY

        castPlayer?.apply {
            if (isCastSessionAvailable) {
                loadItem()
            }

            setSessionAvailabilityListener(sessionAvailabilityListener)
        }
    }

    private fun stopCastally() {
        if (lastStatus[Source.CAST] == Status.STOPPED) {
            return
        }

        lastStatus[Source.CAST] = Status.STOPPED
        lastCommand[Source.CAST] = Command.STOP

        castPlayer?.apply {
            setSessionAvailabilityListener(null)
            stop(true)
        }
    }

    private fun playLocally() {
        if (lastStatus[Source.LOCAL] != Status.STOPPED) {
            return
        }

        lastSource = Source.LOCAL
        lastStatus[Source.LOCAL] = Status.STARTING
        lastCommand[Source.LOCAL] = Command.PLAY

        exoPlayer?.apply {
            playWhenReady = true
            prepare(progressiveMediaSource())
        }
    }

    private fun stopLocally() {
        if (lastStatus[Source.LOCAL] == Status.STOPPED) {
            return
        }

        lastStatus[Source.LOCAL] = Status.STOPPED
        lastCommand[Source.LOCAL] = Command.STOP

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
                Log.d(CAST_TAG, "onCastSessionAvailable")
                loadItem()
            }

            override fun onCastSessionUnavailable() {
                Log.d(CAST_TAG, "onCastSessionUnavailable")
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
                Log.w(LOCAL_TAG, "onSeekProcessed")
            }

            override fun onPlaybackParametersChanged(
                eventTime: AnalyticsListener.EventTime?,
                playbackParameters: PlaybackParameters?
            ) {
                Log.w(LOCAL_TAG, "onPlaybackParametersChanged $playbackParameters")
            }

            override fun onPlayerError(
                eventTime: AnalyticsListener.EventTime?,
                error: ExoPlaybackException?
            ) {
                Log.w(LOCAL_TAG, "onPlayerError $error")
            }

            override fun onSeekStarted(eventTime: AnalyticsListener.EventTime?) {
                Log.w(LOCAL_TAG, "onSeekStarted")
            }

            override fun onLoadingChanged(
                eventTime: AnalyticsListener.EventTime?,
                isLoading: Boolean
            ) {
                Log.w(LOCAL_TAG, "onLoadingChanged: isLoading=$isLoading")
            }

            override fun onDownstreamFormatChanged(
                eventTime: AnalyticsListener.EventTime?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?
            ) {
                Log.d(LOCAL_TAG, "onDownstreamFormatChanged $mediaLoadData")
            }

            override fun onMediaPeriodCreated(eventTime: AnalyticsListener.EventTime?) {
                Log.d(LOCAL_TAG, "onMediaPeriodCreated")
            }

            override fun onReadingStarted(eventTime: AnalyticsListener.EventTime?) {
                Log.d(LOCAL_TAG, "onReadingStarted")
            }

            override fun onBandwidthEstimate(
                eventTime: AnalyticsListener.EventTime?,
                totalLoadTimeMs: Int,
                totalBytesLoaded: Long,
                bitrateEstimate: Long
            ) {
                Log.d(LOCAL_TAG, "onBandwidthEstimate $bitrateEstimate")
            }

            override fun onPlayerStateChanged(
                eventTime: AnalyticsListener.EventTime?,
                playWhenReady: Boolean,
                playbackState: Int
            ) {
                handleStateChange(Source.LOCAL, playbackState)
            }

            override fun onAudioAttributesChanged(
                eventTime: AnalyticsListener.EventTime?,
                audioAttributes: AudioAttributes?
            ) {
                Log.d(LOCAL_TAG, "onAudioAttributesChanged $audioAttributes")
            }

            override fun onVolumeChanged(
                eventTime: AnalyticsListener.EventTime?,
                volume: Float
            ) {
                Log.d(LOCAL_TAG, "onVolumeChanged $volume")
            }

            override fun onDecoderInputFormatChanged(
                eventTime: AnalyticsListener.EventTime?,
                trackType: Int,
                format: Format?
            ) {
                Log.d(LOCAL_TAG, "onDecoderInputFormatChanged $format")
            }

            override fun onAudioSessionId(
                eventTime: AnalyticsListener.EventTime?,
                audioSessionId: Int
            ) {
                Log.d(LOCAL_TAG, "onAudioSessionId $audioSessionId")
            }

            override fun onLoadStarted(
                eventTime: AnalyticsListener.EventTime?,
                loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?
            ) {
                Log.d(LOCAL_TAG, "onLoadStarted $loadEventInfo")
            }

            override fun onTracksChanged(
                eventTime: AnalyticsListener.EventTime?,
                trackGroups: TrackGroupArray?,
                trackSelections: TrackSelectionArray?
            ) {
                Log.d(LOCAL_TAG, "onTracksChanged $trackSelections")
            }

            override fun onPositionDiscontinuity(
                eventTime: AnalyticsListener.EventTime?,
                reason: Int
            ) {
                Log.d(LOCAL_TAG, "onPositionDiscontinuity $reason")
            }

            override fun onUpstreamDiscarded(
                eventTime: AnalyticsListener.EventTime?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?
            ) {
                Log.d(LOCAL_TAG, "onUpstreamDiscarded $mediaLoadData")
            }

            override fun onLoadCanceled(
                eventTime: AnalyticsListener.EventTime?,
                loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?
            ) {
                Log.d(LOCAL_TAG, "onLoadCanceled $mediaLoadData")
            }

            override fun onMediaPeriodReleased(eventTime: AnalyticsListener.EventTime?) {
                Log.d(LOCAL_TAG, "onMediaPeriodReleased")
            }

            override fun onTimelineChanged(
                eventTime: AnalyticsListener.EventTime?,
                reason: Int
            ) {
                when (reason) {
                    Player.TIMELINE_CHANGE_REASON_PREPARED -> Log.d(
                        LOCAL_TAG,
                        "onTimelineChanged TIMELINE_CHANGE_REASON_PREPARED"
                    )

                    Player.TIMELINE_CHANGE_REASON_RESET -> Log.d(
                        LOCAL_TAG,
                        "onTimelineChanged TIMELINE_CHANGE_REASON_RESET"
                    )

                    Player.TIMELINE_CHANGE_REASON_DYNAMIC -> Log.d(
                        LOCAL_TAG,
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
                Log.d(LOCAL_TAG, "onDecoderInitialized $decoderName")
            }

            override fun onAudioUnderrun(
                eventTime: AnalyticsListener.EventTime?,
                bufferSize: Int,
                bufferSizeMs: Long,
                elapsedSinceLastFeedMs: Long
            ) {
                Log.w(LOCAL_TAG, "onAudioUnderrun $bufferSizeMs")
            }

            override fun onLoadCompleted(
                eventTime: AnalyticsListener.EventTime?,
                loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?
            ) {
                Log.d(LOCAL_TAG, "onLoadCompleted $mediaLoadData")
            }

            override fun onLoadError(
                eventTime: AnalyticsListener.EventTime?,
                loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?,
                error: IOException?,
                wasCanceled: Boolean
            ) {
                Log.e(LOCAL_TAG, "onLoadError $error")
            }

            override fun onMetadata(
                eventTime: AnalyticsListener.EventTime?,
                metadata: Metadata?
            ) {
                val entry: IcyInfo = metadata?.get(0) as IcyInfo
                Log.d(LOCAL_TAG, "onMetadata title=$entry.title")
            }
        }
    }

    private fun handleStateChange(source: Source,
                                  playbackState: Int) {
        when (playbackState) {


            Player.STATE_IDLE -> {
                Log.d(LOCAL_TAG, "onPlayerStateChanged STATE_IDLE")
            }

            Player.STATE_BUFFERING -> {
                Log.d(LOCAL_TAG, "onPlayerStateChanged STATE_BUFFERING")
            }

            Player.STATE_READY -> {
                Log.d(LOCAL_TAG, "onPlayerStateChanged STATE_READY")
                lastStatus[Source.LOCAL] = Status.PLAYING
                sendBroadcast()
            }

            Player.STATE_ENDED -> {
                Log.d(LOCAL_TAG, "onPlayerStateChanged STATE_ENDED")
            }

            else -> return
        }
    }

    private fun sendBroadcast() {
        val broadcast = Intent(SERVICE_STATUS_ACTION)
        broadcast.putExtra("status", lastStatus[lastSource]?.ordinalInt)
        broadcast.putExtra("source", lastSource.ordinalInt)
        broadcast.putExtra("command", lastCommand[lastSource]?.ordinalInt)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(broadcast)
    }

    private val eventListener: Player.EventListener by lazy {
        object : Player.EventListener {
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
                Log.d(CAST_TAG, "onPlaybackParametersChanged $playbackParameters")
            }

            override fun onSeekProcessed() {
                Log.d(CAST_TAG, "onSeekProcessed")
            }

            override fun onTracksChanged(
                trackGroups: TrackGroupArray?,
                trackSelections: TrackSelectionArray?
            ) {
                Log.d(CAST_TAG, "onTracksChanged ${trackSelections?.get(0)}")
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                Log.d(CAST_TAG, "onPlayerError $error")
            }

            override fun onLoadingChanged(isLoading: Boolean) {
                Log.d(CAST_TAG, "onLoadingChanged $isLoading")
            }

            override fun onPositionDiscontinuity(reason: Int) {
                Log.d(CAST_TAG, "onPositionDiscontinuity $reason")
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                Log.d(CAST_TAG, "onRepeatModeChanged $repeatMode")
            }

            override fun onTimelineChanged(
                timeline: Timeline?,
                manifest: Any?,
                reason: Int
            ) {
                when (reason) {
                    Player.TIMELINE_CHANGE_REASON_PREPARED -> Log.d(
                        CAST_TAG,
                        "onTimelineChanged TIMELINE_CHANGE_REASON_PREPARED"
                    )

                    Player.TIMELINE_CHANGE_REASON_RESET -> Log.d(
                        CAST_TAG,
                        "onTimelineChanged TIMELINE_CHANGE_REASON_RESET"
                    )

                    Player.TIMELINE_CHANGE_REASON_DYNAMIC -> Log.d(
                        CAST_TAG,
                        "onTimelineChanged TIMELINE_CHANGE_REASON_DYNAMIC"
                    )
                }
            }

            override fun onPlayerStateChanged(
                playWhenReady: Boolean,
                playbackState: Int
            ) {
                when (playbackState) {
                    Player.STATE_IDLE -> Log.d(CAST_TAG, "onPlayerStateChanged STATE_IDLE")

                    Player.STATE_BUFFERING -> {
                        Log.d(CAST_TAG, "onPlayerStateChanged STATE_BUFFERING")
                        sendBroadcast()
                    }

                    Player.STATE_READY -> {
                        Log.d(CAST_TAG, "onPlayerStateChanged STATE_READY")
                        lastStatus[Source.CAST] = Status.PLAYING
                        sendBroadcast()
                    }

                    Player.STATE_ENDED -> Log.d(CAST_TAG, "onPlayerStateChanged STATE_ENDED")
                }
            }
        }
    }

    private val casteStateListener: CastStateListener by lazy {
        CastStateListener { p0 ->
            when (p0) {
                CastState.NO_DEVICES_AVAILABLE -> Log.d(CAST_TAG, "onCastStateChanged: NO_DEVICES_AVAILABLE")
                CastState.NOT_CONNECTED -> Log.d(CAST_TAG, "onCastStateChanged: NOT_CONNECTED")
                CastState.CONNECTING -> Log.d(CAST_TAG, "onCastStateChanged: CONNECTING")
                CastState.CONNECTED -> Log.d(CAST_TAG, "onCastStateChanged: CONNECTED")
            }
        }
    }
}
