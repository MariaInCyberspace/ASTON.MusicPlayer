package maria.incyberspace.musicplayer

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.net.toUri

class MusicService : Service(), SongQueue {

    private val binder = MyBinder()
    var player: MediaPlayer? = null
    private var currentSong = 0
    private val songsSource = listOf(SongsSource.song1, SongsSource.song2, SongsSource.song3, SongsSource.song4)
    private val songsName = listOf(R.string.song_1, R.string.song_2, R.string.song_3, R.string.song_4)

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    inner class MyBinder : Binder() {
        fun getService() : MusicService = this@MusicService

    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = createNotification()
            startForeground(Constants.NOTIFICATION_ID, notification)
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.START.toString() -> start()
            Actions.PLAY.toString() -> play()
            Actions.PAUSE.toString() -> pause()
            Actions.STOP.toString() -> stopSelf()
            Actions.SKIP.toString() -> skip()
            Actions.SKIP_PREVIOUS.toString() -> skipPrevious()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        if (player?.isPlaying == true) {
            stop()
        }
        super.onDestroy()
    }

    fun getCurrentSong() : Int {
        return currentSong
    }

    private fun playSong(song: String) {
        stop()
        start(song)
        player?.start()
        updateNotification()
    }

    private fun play() {
        player?.start()
        updateNotification()
    }

    private fun skip() {
        currentSong = getSongPosition(++currentSong, songsSource)
        val song = songsSource[currentSong]
        if (player?.isPlaying == true) {
            playSong(song)
        } else {
            start(song)
        }
    }


    private fun skipPrevious() {
        currentSong = getSongPosition(--currentSong, songsSource)
        val song = songsSource[currentSong]
        if (player?.isPlaying == true) {
            playSong(song)
        } else {
            start(song)
        }
    }


    private fun pause() {
        player?.pause()
        updateNotification()
    }

    private fun stop() {
        player?.stop()
        player?.release()
        player = null
    }

    private fun start(song: String = songsSource[currentSong]) {
        player = MediaPlayer.create(this, song.toUri())
        player?.setOnCompletionListener { it ->
            it.reset()
            currentSong = getSongPosition(++currentSong, songsSource)
            it.setDataSource(this, songsSource[currentSong].toUri())
            it.prepareAsync()
            it.setOnPreparedListener { it.start() }
            val broadcastIntent = Intent().apply {
                this.action = MainActivity.BroadcastActions.NEXT.toString()
                this.putExtra(Constants.CURRENT_SONG, currentSong)
            }
            sendBroadcast(broadcastIntent)
            updateNotification()
        }
        updateNotification()
    }

    enum class Actions {
        START, STOP, PLAY, PAUSE, SKIP, SKIP_PREVIOUS
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification() : Notification {
        val mainPendingIntent = PendingIntent.getActivity(this,
            0,
            Intent(this, MainActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            ,
            PendingIntent.FLAG_IMMUTABLE)

        val pausePendingIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, MusicService::class.java).apply { action = Actions.PAUSE.toString() },
            PendingIntent.FLAG_IMMUTABLE
        )

        val playPendingIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, MusicService::class.java).apply { action = Actions.PLAY.toString() },
            PendingIntent.FLAG_IMMUTABLE
        )

        val skipPendingIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, MusicService::class.java).apply { action = Actions.SKIP.toString() },
            PendingIntent.FLAG_IMMUTABLE
        )

        val skipPreviousPendingIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, MusicService::class.java).apply { action = Actions.SKIP_PREVIOUS.toString() },
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder =
            Notification.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_music)
                .setStyle(
                    Notification.MediaStyle()
                        .setShowActionsInCompactView(1)
                )
                .setContentIntent(mainPendingIntent)
                .setContentTitle(getString(songsName[currentSong]))


        return if (player?.isPlaying == true) {
            notificationBuilder
                .addAction(
                    Notification.Action.Builder(
                        R.drawable.ic_skip_previous,
                        "Previous",
                        skipPreviousPendingIntent
                    ).build()
                )
                .addAction(
                    Notification.Action.Builder(
                        R.drawable.ic_pause,
                        "Pause",
                        pausePendingIntent
                    ).build()
                )
                .addAction(
                    Notification.Action.Builder(
                        R.drawable.ic_skip,
                        "Next",
                        skipPendingIntent
                    ).build()
                )
                .build()
        }
        else {
            notificationBuilder
                .addAction(
                    Notification.Action.Builder(
                        R.drawable.ic_skip_previous,
                        "Previous",
                        skipPreviousPendingIntent
                    ).build()
                )
                .addAction(
                    Notification.Action.Builder(
                        R.drawable.ic_play,
                        "Play",
                        playPendingIntent
                    ).build()
                )
                .addAction(
                    Notification.Action.Builder(
                        R.drawable.ic_skip,
                        "Next",
                        skipPendingIntent
                    ).build()
                )
                .build()
        }
    }

    private fun updateNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = createNotification()
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(Constants.NOTIFICATION_ID, notification)
        }
    }

}

