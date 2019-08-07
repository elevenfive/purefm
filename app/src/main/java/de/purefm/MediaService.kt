package de.purefm

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.*
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.session.MediaButtonReceiver
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
import java.lang.StringBuilder

const val CAST_TAG =  "cast"
const val LOCAL_TAG = "local"
const val MEDIA_BUTTON_TAG = "media_button"
private const val TAG = "MediaService"

const val SERVICE_STATUS_ACTION = "de.purefm.SERVICE_STATUS"
const val CHANNEL_ID = "106.4"

class MediaService: Service() {
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
                INIT.ordinalInt -> INIT
                PLAY.ordinalInt -> PLAY
                STOP.ordinalInt -> STOP
                else -> throw IllegalArgumentException()
            }
        }
    }

    private lateinit var castContext: CastContext

    private var castPlayer: CastPlayer? = null

    private var exoPlayer: ExoPlayer? = null

    private var lastCommand: MutableMap<Source, Command> = mutableMapOf(
        Pair(Source.LOCAL, Command.STOP),
        Pair(Source.CAST, Command.STOP))

    private var lastStatus: MutableMap<Source, Status> = mutableMapOf(
        Pair(Source.LOCAL, Status.STOPPED),
        Pair(Source.CAST, Status.STOPPED))

    private var lastSource: Source = Source.LOCAL

    private var lastTitle: String? = null

    private var lastCastState: Int? = null

    private var mediaSession: MediaSessionCompat? = null

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "onCreate")
        castContext = CastContext.getSharedInstance(applicationContext)
        castContext.addCastStateListener(casteStateListener)

        val simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(this)
        simpleExoPlayer.playWhenReady = false
        simpleExoPlayer.addAnalyticsListener(localAnalyticsListener)
        exoPlayer = simpleExoPlayer

        val player = CastPlayer(castContext)
        player.playWhenReady = false
        player.addListener(castEventListener)
        castPlayer = player

        val mediaSessionCompat = MediaSessionCompat(applicationContext, "MediaSessionCompat")
        mediaSessionCompat.setCallback(callback)
        mediaSession = mediaSessionCompat
    }

    override fun onStartCommand(intent: Intent?,
                                flags: Int,
                                startId: Int): Int {
        mediaSession?.let {
            val keyEvent = MediaButtonReceiver.handleIntent(it, intent)

            keyEvent?.let {
                val keyName = when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY -> "play"
                    KeyEvent.KEYCODE_MEDIA_STOP -> "stop"
                    else -> "unknown"
                }

                Log.d(TAG, "onStartCommand $keyName")
                return super.onStartCommand(intent, flags, startId)
            }
        }

        val commandOrdinalInt = intent?.getIntExtra("command", Command.INIT.ordinalInt) ?: Command.INIT.ordinalInt
        val command = Command.fromOrdinalInt(commandOrdinalInt)
        Log.d(TAG, "onStartCommand $command")

        if (lastCommand[lastSource] != command) {
            when (command) {
                Command.INIT -> { }

                Command.PLAY -> {
                    play(lastSource)
                    startForeground()
                    setMediaSessionActive()
                }

                Command.STOP -> {
                    stop(lastSource)
                    stopForeground(true)
                    setMediaSessionInactive()
                }
            }
        }

        sendBroadcast()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind $intent ${intent?.extras}")
        return null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged $newConfig")
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.d(TAG, "onRebind $intent ${intent?.extras}")
    }

    override fun dump(fd: FileDescriptor?,
                      writer: PrintWriter?,
                      args: Array<out String>?) {
        super.dump(fd, writer, args)

        Log.d(TAG, "dump $fd $writer $args")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved $rootIntent ${rootIntent?.extras}")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.d(TAG, "onLowMemory")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(TAG, "onTrimMemory $level")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind $intent ${intent?.extras}")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")

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

        mediaSession?.let {
            it.isActive = false
            it.release()
            mediaSession = null
        }

        super.onDestroy()
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val callback: MediaSessionCompat.Callback by lazy {
        object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                Log.d(MEDIA_BUTTON_TAG, "onMediaButtonEvent $mediaButtonEvent ${mediaButtonEvent?.extras}")

                val keyEvent = mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)

                if (keyEvent == null || keyEvent.action != KeyEvent.ACTION_DOWN) {
                    return false
                }

                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        return true
                    }

                    KeyEvent.KEYCODE_MEDIA_STOP -> {
                        onStop()
                    }
                }

                return false
            }

            override fun onRewind() {
                super.onRewind()
                Log.d(MEDIA_BUTTON_TAG, "onRewind")
            }

            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
                Log.d(MEDIA_BUTTON_TAG, "onSeekTo $pos")
            }

            override fun onAddQueueItem(description: MediaDescriptionCompat?) {
                super.onAddQueueItem(description)
                Log.d(MEDIA_BUTTON_TAG, "onAddQueueItem $description")
            }

            override fun onAddQueueItem(description: MediaDescriptionCompat?,
                                        index: Int) {
                super.onAddQueueItem(description, index)
                Log.d(MEDIA_BUTTON_TAG, "onAddQueueItem $description $index")
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                Log.d(MEDIA_BUTTON_TAG, "onSkipToPrevious")
            }

            override fun onCustomAction(action: String?,
                                        extras: Bundle?) {
                super.onCustomAction(action, extras)
                Log.d(MEDIA_BUTTON_TAG, "onCustomAction $action $extras")
            }

            override fun onPrepare() {
                super.onPrepare()
                Log.d(MEDIA_BUTTON_TAG, "onPrepare")
            }

            override fun onFastForward() {
                super.onFastForward()
                Log.d(MEDIA_BUTTON_TAG, "onFastForward")
            }

            override fun onPlay() {
                super.onPlay()
                Log.d(MEDIA_BUTTON_TAG, "onPlay")
            }

            override fun onStop() {
                super.onStop()
                Log.d(MEDIA_BUTTON_TAG, "onStop")
            }

            override fun onSkipToQueueItem(id: Long) {
                super.onSkipToQueueItem(id)
                Log.d(MEDIA_BUTTON_TAG, "onSkipToQueueItem $id")
            }

            override fun onRemoveQueueItem(description: MediaDescriptionCompat?) {
                super.onRemoveQueueItem(description)
                Log.d(MEDIA_BUTTON_TAG, "onRemoveQueueItem $description")
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                Log.d(MEDIA_BUTTON_TAG, "onSkipToNext")
            }

            override fun onPrepareFromMediaId(mediaId: String?,
                                              extras: Bundle?) {
                super.onPrepareFromMediaId(mediaId, extras)
                Log.d(MEDIA_BUTTON_TAG, "onPrepareFromMediaId $mediaId $extras")
            }

            override fun onSetRepeatMode(repeatMode: Int) {
                super.onSetRepeatMode(repeatMode)
                Log.d(MEDIA_BUTTON_TAG, "onSetRepeatMode $repeatMode")
            }

            override fun onCommand(command: String?,
                                   extras: Bundle?,
                                   cb: ResultReceiver?) {
                super.onCommand(command, extras, cb)
                Log.d(MEDIA_BUTTON_TAG, "onCommand $command $extras $cb")
            }

            override fun onPause() {
                Log.d(MEDIA_BUTTON_TAG, "onPause")
                super.onPause()
            }

            override fun onPrepareFromSearch(query: String?,
                                             extras: Bundle?) {
                super.onPrepareFromSearch(query, extras)
                Log.d(MEDIA_BUTTON_TAG, "onPrepareFromSearch $query $extras")
            }

            override fun onPlayFromMediaId(mediaId: String?,
                                           extras: Bundle?) {
                super.onPlayFromMediaId(mediaId, extras)
                Log.d(MEDIA_BUTTON_TAG, "onPlayFromMediaId $mediaId $extras")
            }

            override fun onSetShuffleMode(shuffleMode: Int) {
                super.onSetShuffleMode(shuffleMode)
                Log.d(MEDIA_BUTTON_TAG, "onSetShuffleMode $shuffleMode")
            }

            override fun onPrepareFromUri(uri: Uri?,
                                          extras: Bundle?) {
                super.onPrepareFromUri(uri, extras)
                Log.d(MEDIA_BUTTON_TAG, "onPrepareFromUri $uri $extras")
            }

            override fun onPlayFromSearch(query: String?,
                                          extras: Bundle?) {
                super.onPlayFromSearch(query, extras)
                Log.d(MEDIA_BUTTON_TAG, "onPlayFromSearch $query $extras")
            }

            override fun onPlayFromUri(uri: Uri?,
                                       extras: Bundle?) {
                super.onPlayFromUri(uri, extras)
                Log.d(MEDIA_BUTTON_TAG, "onPlayFromUri $uri $extras")
            }

            override fun onSetRating(rating: RatingCompat?) {
                super.onSetRating(rating)
                Log.d(MEDIA_BUTTON_TAG, "onSetRating $rating")
            }

            override fun onSetRating(rating: RatingCompat?,
                                     extras: Bundle?) {
                super.onSetRating(rating, extras)
                Log.d(MEDIA_BUTTON_TAG, "onSetRating $rating $extras")
            }

            override fun onSetCaptioningEnabled(enabled: Boolean) {
                super.onSetCaptioningEnabled(enabled)
                Log.d(MEDIA_BUTTON_TAG, "onSetCaptioningEnabled $enabled")
            }
        }
    }

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

    private fun setMediaSessionActive() {
        mediaSession?.isActive = true
    }

    private fun setMediaSessionInactive() {
        mediaSession?.isActive = false
    }

    private fun startForeground() {
        Log.d(TAG, "startForeground")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel = NotificationChannel(CHANNEL_ID, name, importance)
            notificationChannel.description = descriptionText
            notificationChannel.enableVibration(false)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { intent ->
                PendingIntent.getActivity(this, 0, intent, 0)
            }

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0)

        val notificationBuilder = androidx.core.app.NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("pure-fm.de")
            .setSmallIcon(R.drawable.cast_ic_notification_small_icon)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                NotificationCompat.Action(R.drawable.exo_icon_pause, "stop",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        applicationContext, PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )
            .addAction(
                NotificationCompat.Action(R.drawable.exo_icon_stop, "pause",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        applicationContext, PlaybackStateCompat.ACTION_STOP
                    )
                )
            )
            .setStyle(mediaStyle)


        lastTitle?.let { notificationBuilder.setContentText(it) }
        startForeground(1, notificationBuilder.build())
    }

    private fun play(source: Source) {
        when (source) {
            Source.CAST -> {
                stopLocally()
                playCastally()
            }

            Source.LOCAL -> {
                stopCastally()
                playLocally()
            }
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

        stopForeground(true)
        sendBroadcast()
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
        stopForeground(true)
        sendBroadcast()
    }

    private fun loadItem() {
        castPlayer?.loadItem(mediaQueueItem(), C.TIME_UNSET)
    }

    private fun progressiveMediaSource() =
        ProgressiveMediaSource.Factory(DefaultHttpDataSourceFactory(
            Util.getUserAgent(this, "pure-fm.de (Android)")))
            .createMediaSource(Uri.parse("http://radionetz.de:8000/purefm-bln.mp3"))

    private fun mediaQueueItem(): MediaQueueItem {
        val url  = "http://radionetz.de:8000/purefm-bln.mp3"
        val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, "pure-fm.de")

        val mediaInfo: MediaInfo = MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
            .setContentType(MimeTypes.AUDIO_MPEG)
            .setMetadata(mediaMetadata).build()

        return MediaQueueItem.Builder(mediaInfo).build()
    }

    private val localAnalyticsListener: AnalyticsListener by lazy {
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
                val sb = StringBuilder()

                trackSelections?.all?.forEach {
                    it?.let {
                        if (sb.isNotEmpty()) {
                            sb.append("; ")
                        }

                        sb.append(it.getFormat(it.selectedIndex))
                    }
                }

                Log.d(LOCAL_TAG, "onTracksChanged $sb")
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
                val title = entry.title
                Log.d(LOCAL_TAG, "onMetadata title=$title")
                lastTitle = title
                sendBroadcast()
            }
        }
    }

    private fun handleStateChange(source: Source,
                                  playbackState: Int) {
        when (playbackState) {
            Player.STATE_IDLE -> {
                Log.d(LOCAL_TAG, "onPlayerStateChanged STATE_IDLE")

                if (lastStatus[source] != Status.STOPPED) {
                    lastStatus[source] = Status.STOPPED
                    sendBroadcast()
                }
            }

            Player.STATE_BUFFERING -> {
                Log.d(LOCAL_TAG, "onPlayerStateChanged STATE_BUFFERING")

                if (lastStatus[source] != Status.STARTING) {
                    lastStatus[source] = Status.STARTING
                    sendBroadcast()
                }
            }

            Player.STATE_READY -> {
                Log.d(LOCAL_TAG, "onPlayerStateChanged STATE_READY")

                if (lastStatus[source] != Status.PLAYING) {
                    lastStatus[source] = Status.PLAYING
                    sendBroadcast()
                }
            }

            Player.STATE_ENDED -> {
                Log.d(LOCAL_TAG, "onPlayerStateChanged STATE_ENDED")

                if (lastStatus[source] != Status.STOPPED) {
                    lastStatus[source] = Status.STOPPED
                    sendBroadcast()
                }
            }

            else -> return
        }
    }

    private fun sendBroadcast() {
        val broadcast = Intent(SERVICE_STATUS_ACTION)
        broadcast.putExtra("status", lastStatus[lastSource]?.ordinalInt)
        broadcast.putExtra("source", lastSource.ordinalInt)
        broadcast.putExtra("command", lastCommand[lastSource]?.ordinalInt)
        broadcast.putExtra("title", lastTitle)
        broadcast.putExtra("castState", lastCastState)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(broadcast)
    }

    private val castEventListener: Player.EventListener by lazy {
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
                handleStateChange(Source.CAST, playbackState)
            }
        }
    }

    private val casteStateListener: CastStateListener by lazy {
        CastStateListener { p0 ->
            if (lastCastState == p0 ) {
                return@CastStateListener
            }

            when (p0) {
                CastState.NO_DEVICES_AVAILABLE -> { Log.d(CAST_TAG, "onCastStateChanged: NO_DEVICES_AVAILABLE") }

                CastState.NOT_CONNECTED -> {
                    Log.d(CAST_TAG, "onCastStateChanged: NOT_CONNECTED")

                    if (lastSource == Source.CAST) {
                        lastSource = Source.LOCAL

                        if (lastStatus[Source.CAST] == Status.PLAYING) {
                            stopCastally()
                            playLocally()
                        }
                    }
                }

                CastState.CONNECTING -> {
                    Log.d(CAST_TAG, "onCastStateChanged: CONNECTING")

                    if (lastSource == Source.LOCAL) {
                        lastSource = Source.CAST
                    }
                }

                CastState.CONNECTED -> {
                    Log.d(CAST_TAG, "onCastStateChanged: CONNECTED")

                    if (lastSource == Source.LOCAL) {
                        lastSource = Source.CAST
                    }

                    if (lastCommand[Source.LOCAL] == Command.PLAY) {
                        stopLocally()
                        playCastally()
                    }
                }
            }

            lastCastState = p0
            sendBroadcast()
        }
    }
}

fun mediaServiceIntent(context: Context,
                       command: MediaService.Command? = null): Intent {
    val intent = Intent()
    intent.setClass(context, MediaService::class.java)
    command?.let { intent.putExtra("command", it.ordinalInt) }
    return intent
}
