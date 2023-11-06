package maria.incyberspace.musicplayer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), SongQueue {

    private val intentFilter: IntentFilter = IntentFilter()

    private lateinit var mService: MusicService
    private var mBound: Boolean = false

    private lateinit var playPauseButton: ImageView
    private lateinit var skipButton: ImageView
    private lateinit var skipPreviousButton: ImageView
    private lateinit var songAlbumArt: ImageView
    private lateinit var songName: TextView
    private var currentSong: Int = 0
    private val songsAlbumArt = listOf(R.drawable.song_1, R.drawable.song_2, R.drawable.song_3, R.drawable.song_4)
    private val songsName = listOf(R.string.song_1, R.string.song_2, R.string.song_3, R.string.song_4)
    private var isPlaying = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MyBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            mBound = false
        }

    }

    private val br: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val curSong = intent?.getIntExtra(Constants.CURRENT_SONG, 0)
            currentSong = curSong!!
            val songText = songsName[currentSong]
            val songArt = songsAlbumArt[currentSong]
            songName.setText(songText)
            songAlbumArt.setImageResource(songArt)
        }

    }

    override fun onStart() {
        super.onStart()
        Intent(this, MusicService::class.java).also {intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

    }

    override fun onResume() {
        super.onResume()
        registerReceiver(br, intentFilter)
    }

    override fun onRestart() {
        super.onRestart()
        if (mService.player?.isPlaying == true) {
            playPauseButton.setImageResource(R.drawable.ic_pause)
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play)
        }
        songAlbumArt.setImageResource(songsAlbumArt[mService.getCurrentSong()])
        songName.setText(songsName[mService.getCurrentSong()])
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        intentFilter.addAction(BroadcastActions.NEXT.toString())


        val startIntent = Intent(this, MusicService::class.java)
        startIntent.action = MusicService.Actions.START.toString()

        val playIntent = Intent(this, MusicService::class.java)
        playIntent.action = MusicService.Actions.PLAY.toString()

        val pauseIntent = Intent(this, MusicService::class.java)
        pauseIntent.action = MusicService.Actions.PAUSE.toString()

        val skipIntent = Intent(this, MusicService::class.java)
        skipIntent.action = MusicService.Actions.SKIP.toString()

        val skipPreviousIntent = Intent(this, MusicService::class.java)
        skipPreviousIntent.action = MusicService.Actions.SKIP_PREVIOUS.toString()

        val pendingPlayIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_IMMUTABLE)
        val pendingPauseIntent = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
        val pendingSkipIntent = PendingIntent.getService(this, 0, skipIntent, PendingIntent.FLAG_IMMUTABLE)
        val pendingSkipPreviousIntent = PendingIntent.getService(this, 0, skipPreviousIntent, PendingIntent.FLAG_IMMUTABLE)

        thread {
            Log.d("main", "thread ${Thread.currentThread().id}")
            startService(startIntent)
        }

        songAlbumArt = findViewById(R.id.ivAlbumArt)
        songName = findViewById(R.id.tvSongName)

        songAlbumArt.setImageResource(R.drawable.song_1)
        songName.setText(R.string.song_1)

        playPauseButton = findViewById(R.id.ivPlayPauseDetail)
        skipButton = findViewById(R.id.ivSkip)
        skipPreviousButton = findViewById(R.id.ivSkipPrevious)

        playPauseButton.setOnClickListener {
            if (isPlaying) {
                pendingPauseIntent.send()
                isPlaying = false
                playPauseButton.setImageResource(R.drawable.ic_play)
            } else {
                pendingPlayIntent.send()
                isPlaying = true
                playPauseButton.setImageResource(R.drawable.ic_pause)
            }
        }

        skipButton.setOnClickListener {
            pendingSkipIntent.send()
            currentSong = mService.getCurrentSong()
            currentSong = getSongPosition(++currentSong, songsName)
            val songText = songsName[currentSong]
            val songImage = songsAlbumArt[currentSong]
            songAlbumArt.setImageResource(songImage)
            songName.setText(songText)
        }

        skipPreviousButton.setOnClickListener {
            pendingSkipPreviousIntent.send()
            currentSong = mService.getCurrentSong()
            currentSong = getSongPosition(--currentSong, songsName)
            val songText = songsName[currentSong]
            val songImage = songsAlbumArt[currentSong]
            songAlbumArt.setImageResource(songImage)
            songName.setText(songText)
        }

    }

    enum class BroadcastActions {
        NEXT
    }

}