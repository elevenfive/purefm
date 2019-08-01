package de.purefm

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Surface
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.decoder.DecoderCounters
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.metadata.icy.IcyInfo
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.util.Util.getUserAgent
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        val dataSourceFactory = DefaultHttpDataSourceFactory(getUserAgent(this, "purefm"))

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse("http://radionetz.de:8000/purefm-bln.mp3"))

        val simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(this)
        simpleExoPlayer.prepare(mediaSource)
        simpleExoPlayer.playWhenReady = false

        simpleExoPlayer.addAnalyticsListener(object: AnalyticsListener {
            override fun onSeekProcessed(eventTime: AnalyticsListener.EventTime?) {

            }

            override fun onPlaybackParametersChanged(
                eventTime: AnalyticsListener.EventTime?,
                playbackParameters: PlaybackParameters?
            ) {

            }

            override fun onPlayerError(eventTime: AnalyticsListener.EventTime?, error: ExoPlaybackException?) {

            }

            override fun onSeekStarted(eventTime: AnalyticsListener.EventTime?) {

            }

            override fun onLoadingChanged(eventTime: AnalyticsListener.EventTime?, isLoading: Boolean) {

            }

            override fun onDownstreamFormatChanged(
                eventTime: AnalyticsListener.EventTime?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?
            ) {

            }

            override fun onMediaPeriodCreated(eventTime: AnalyticsListener.EventTime?) {

            }

            override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime?, surface: Surface?) {

            }

            override fun onReadingStarted(eventTime: AnalyticsListener.EventTime?) {

            }

            override fun onBandwidthEstimate(
                eventTime: AnalyticsListener.EventTime?,
                totalLoadTimeMs: Int,
                totalBytesLoaded: Long,
                bitrateEstimate: Long
            ) {

            }

            override fun onPlayerStateChanged(
                eventTime: AnalyticsListener.EventTime?,
                playWhenReady: Boolean,
                playbackState: Int
            ) {

            }

            override fun onAudioAttributesChanged(
                eventTime: AnalyticsListener.EventTime?,
                audioAttributes: AudioAttributes?
            ) {
                Log.d("foo", "onAudioAttributesChanged $audioAttributes")
            }

            override fun onVolumeChanged(eventTime: AnalyticsListener.EventTime?,
                                         volume: Float) {
                Log.d("foo", "onVolumeChanged $volume")
            }


            override fun onDecoderInputFormatChanged(
                eventTime: AnalyticsListener.EventTime?,
                trackType: Int,
                format: Format?
            ) {
                Log.d("foo", "onDecoderInputFormatChanged $format")
            }

            override fun onAudioSessionId(eventTime: AnalyticsListener.EventTime?,
                                          audioSessionId: Int) {
                Log.d("foo", "onAudioSessionId $audioSessionId")
            }

            override fun onLoadStarted(
                eventTime: AnalyticsListener.EventTime?,
                loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?
            ) {
                Log.d("foo", "onLoadStarted $mediaLoadData")
            }

            override fun onTracksChanged(
                eventTime: AnalyticsListener.EventTime?,
                trackGroups: TrackGroupArray?,
                trackSelections: TrackSelectionArray?
            ) {
                Log.d("foo", "onTracksChanged $trackSelections")
            }

            override fun onPositionDiscontinuity(eventTime: AnalyticsListener.EventTime?,
                                                 reason: Int) {
                Log.d("foo", "onPositionDiscontinuity $reason")
            }

            override fun onUpstreamDiscarded(
                eventTime: AnalyticsListener.EventTime?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?
            ) {
                Log.d("foo", "onUpstreamDiscarded $mediaLoadData")
            }

            override fun onLoadCanceled(
                eventTime: AnalyticsListener.EventTime?,
                loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?
            ) {
                Log.d("foo", "onLoadCanceled $mediaLoadData")
            }

            override fun onMediaPeriodReleased(eventTime: AnalyticsListener.EventTime?) {
                Log.d("foo", "onMediaPeriodReleased")
            }

            override fun onTimelineChanged(eventTime: AnalyticsListener.EventTime?,
                                           reason: Int) {
                Log.d("foo", "onTimelineChanged $reason")
            }

            override fun onDecoderInitialized(
                eventTime: AnalyticsListener.EventTime?,
                trackType: Int,
                decoderName: String?,
                initializationDurationMs: Long
            ) {
                Log.d("foo", "onDecoderInitialized $decoderName")
            }

            override fun onDecoderEnabled(
                eventTime: AnalyticsListener.EventTime?,
                trackType: Int,
                decoderCounters: DecoderCounters?
            ) {

            }

            override fun onVideoSizeChanged(
                eventTime: AnalyticsListener.EventTime?,
                width: Int,
                height: Int,
                unappliedRotationDegrees: Int,
                pixelWidthHeightRatio: Float
            ) {

            }

            override fun onAudioUnderrun(
                eventTime: AnalyticsListener.EventTime?,
                bufferSize: Int,
                bufferSizeMs: Long,
                elapsedSinceLastFeedMs: Long
            ) {

            }

            override fun onLoadCompleted(
                eventTime: AnalyticsListener.EventTime?,
                loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?
            ) {

            }

            override fun onDrmKeysRemoved(eventTime: AnalyticsListener.EventTime?) {

            }

            override fun onLoadError(
                eventTime: AnalyticsListener.EventTime?,
                loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?,
                error: IOException?,
                wasCanceled: Boolean
            ) {

            }

            override fun onMetadata(eventTime: AnalyticsListener.EventTime?,
                                    metadata: Metadata?) {
                val entry: IcyInfo = metadata?.get(0) as IcyInfo
                track_title_text_view.text = getString(R.string.now_on_air, entry.title)
            }
        })

        player_view.player = simpleExoPlayer
        exoPlayer = simpleExoPlayer
        player_view.showController()
        player_view.useArtwork = true
    }

    override fun onStop() {
        player_view.player = null
        exoPlayer?.stop(true)
        exoPlayer?.release()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // <img alt="play" src="play1.png" onclick="soundManager.play('purestream')" align="middle" height="20" width="20">
    //
    // http://radionetz.de:8000/purefm-bln.mp3
    // stream contains mp3 metadata.  Title Genre (comma separated), Now Playing
    //
    // Stereo / 44.1 / 32 bits per sample, bitrate 128 kb/s

    // <body style="margin: 0; background-color: #2B2728; background-image: url('http://www.pure-fm.de/player/images/hg-berlin-1.jpg');">
    //
    //<table align="center" style="width: 750px; height: 250px;" cellpadding="0" cellspacing="0">
    //	<tbody><tr>
    //		<td class="auto-style1" style="width: 750px; height: 250px">
    //		<table align="center" cellspacing="0" cellpadding="0">
    //			<tbody><tr>
    //				<td style="width: 495px; height: 200px">&nbsp;</td>
    //				<td rowspan="3" style="height: 250px; width: 250px;" class="auto-style4" onclick="load('')">
    //	<img src="http://www.pure-fm.de/wp-content/uploads/0034377.jpg" border="0" title="" height="250" width="250"></td>
    //			</tr>
    //			<tr>
    //				<td class="auto-style2" style="height: 25px; width: 500px;" onclick="load('')">
    //				<span class="auto-style5"><strong><font style="vertical-align: inherit;"><font style="vertical-align: inherit;">&nbsp;&nbsp;NOW ON AIR</font></font></strong></span><strong><font style="vertical-align: inherit;"><font style="vertical-align: inherit;"> &nbsp; &nbsp;Sarard</font></font></strong></td>
    //			</tr>
    //			<tr>
    //				<td class="auto-style3" style="height: 25px; width: 500px;" onclick="load('')">
    //				<strong><em><font style="vertical-align: inherit;"><font style="vertical-align: inherit;">Kummerbube&nbsp;</font></font></em>&nbsp;&nbsp; </strong></td>
    //			</tr>
    //		</tbody></table>
    //		</td>
    //	</tr>
    //</tbody></table><div id="goog-gt-tt" class="skiptranslate" dir="ltr"><div style="padding: 8px;"><div><div class="logo"><img src="https://www.gstatic.com/images/branding/product/1x/translate_24dp.png" width="20" height="20" alt="Google Translate"></div></div></div><div class="top" style="padding: 8px; float: left; width: 100%;"><h1 class="title gray">Original text</h1></div><div class="middle" style="padding: 8px;"><div class="original-text"></div></div><div class="bottom" style="padding: 8px;"><div class="activity-links"><span class="activity-link">Contribute a better translation</span><span class="activity-link"></span></div><div class="started-activity-container"><hr style="color: #CCC; background-color: #CCC; height: 1px; border: none;"><div class="activity-root"></div></div></div><div class="status-message" style="display: none;"></div></div>
    //
    //
    //
    //
    //<div class="goog-te-spinner-pos"><div class="goog-te-spinner-animation"><svg xmlns="http://www.w3.org/2000/svg" class="goog-te-spinner" width="96px" height="96px" viewBox="0 0 66 66"><circle class="goog-te-spinner-path" fill="none" stroke-width="6" stroke-linecap="round" cx="33" cy="33" r="30"></circle></svg></div></div></body>
}
