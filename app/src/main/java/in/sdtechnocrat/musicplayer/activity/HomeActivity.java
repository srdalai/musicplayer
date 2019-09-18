package in.sdtechnocrat.musicplayer.activity;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.material.tabs.TabLayout;
import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import in.sdtechnocrat.musicplayer.player.MediaPlayerService;
import in.sdtechnocrat.musicplayer.R;
import in.sdtechnocrat.musicplayer.model.SongData;
import in.sdtechnocrat.musicplayer.player.PlayerService;
import in.sdtechnocrat.musicplayer.utils.StorageUtil;
import in.sdtechnocrat.musicplayer.adapter.ViewPagerAdapter;
import in.sdtechnocrat.musicplayer.utils.Util;

import static in.sdtechnocrat.musicplayer.utils.Util.playbackState;

public class HomeActivity extends AppCompatActivity implements PlayerService.MusicPlayerListener {

    ArrayList<SongData> songList;
    public static final String Broadcast_PLAY_NEW_AUDIO = "in.sdtechnocrat.musicplayer.PlayNewAudio";
    public static final String Broadcast_CHANGE = "in.sdtechnocrat.musicplayer.CHNAGE";
    ProgressBar progressBar;
    ImageView albumArt, btnPrev, btnPlay, btnPause, btnNext;
    TextView textViewTitle, textViewSub;
    SongData currentSongData = null;
    int currentIndex = 0;
    ProgressTracker progressTracker;

    PlayerService musicPlayerService;
    boolean mBound = false;
    SimpleExoPlayer exoPlayer;
    FrameLayout nowPlayingFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ViewPager viewPager = findViewById(R.id.view_pager);
        TabLayout tabs = findViewById(R.id.tabs);
        progressBar = findViewById(R.id.progressBar);
        albumArt = findViewById(R.id.albumArt);
        btnPrev = findViewById(R.id.btnPrev);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnNext = findViewById(R.id.btnNext);
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewSub = findViewById(R.id.textViewSub);
        nowPlayingFrame = findViewById(R.id.nowPlayingFrame);

        textViewSub.setSelected(true);
        textViewTitle.setSelected(true);
        progressBar.setIndeterminate(false);
        progressBar.setProgress(50);

        tabs.setupWithViewPager(viewPager);

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);

        songList = new ArrayList<>();

        if (isMyServiceRunning(PlayerService.class)) {
            // TODO Bind existing player to view
        }


        btnPlay.setOnClickListener(view -> {
            //btnPlay.setVisibility(View.GONE);
            //btnPause.setVisibility(View.VISIBLE);
            musicPlayerService.play();
        });

        btnPause.setOnClickListener(view -> {
            //btnPlay.setVisibility(View.VISIBLE);
            //btnPause.setVisibility(View.GONE);
            musicPlayerService.pause();
        });

        btnNext.setOnClickListener(view -> {
            /*if (!(currentIndex == songList.size() - 1)) {
                modifyPlayer("play_next");
                currentIndex++;
                changeWidget();
            }*/

        });

        btnPrev.setOnClickListener(view -> {
           /*if (!(currentIndex == 0)) {
               modifyPlayer("play_prev");
               currentIndex--;
               changeWidget();
           }*/
        });

        nowPlayingFrame.setOnClickListener((view) -> {
            startActivity(new Intent(this, NowPlayingActivityOne.class));
        });
        //loadAudio();
        btnPlay.setVisibility(View.GONE);
        btnPause.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, PlayerService.class));
        } else {
            startService(new Intent(this, PlayerService.class));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", mBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mBound = savedInstanceState.getBoolean("ServiceState");
    }

    public void playSong() {
        if (playbackState == Util.PLAYBACK_STATE.NONE || playbackState == Util.PLAYBACK_STATE.STOPPED) {
            musicPlayerService.play();
        } else {
            musicPlayerService.changeSong();
        }
    }



    /*public void changeSongList(ArrayList<SongData> songData, int currentPosition) {
        songList = new ArrayList<>();
        songList = songData;
        currentIndex = currentPosition;
        playAudio(currentPosition);
        changeWidget();
    }*/

    private void playAudio(int audioIndex) {
        //Check is service is active
        if (!mBound) {
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(songList);
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, connection, Context.BIND_AUTO_CREATE);
        } else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(audioIndex);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }



    public void changeWidget() {
        if (mBound) {

            if (playbackState != Util.PLAYBACK_STATE.NONE) {
                currentSongData = Util.currentSong;
                textViewTitle.setText(currentSongData.getTitle());
                textViewSub.setText(currentSongData.getArtist() + " - " + currentSongData.getAlbum());

                long seconds = TimeUnit.MILLISECONDS.toSeconds(musicPlayerService.playerInstance().getDuration());
                progressBar.setMax((int) seconds);
                Log.d("this", seconds+"");


                File file = new File(currentSongData.getAlbumArt());
                Uri photoURI = Uri.fromFile(file);

                Glide.with(this)
                        .load(photoURI)
                        .error(R.drawable.ic_album_acent_24dp)
                        .placeholder(R.drawable.ic_album_acent_24dp)
                        .into(albumArt);
            }

            if (progressTracker != null) {
                progressTracker.purgeHandler();
            }


            if (playbackState == Util.PLAYBACK_STATE.PLAYING) {
                progressTracker = new ProgressTracker(musicPlayerService.playerInstance());
                btnPlay.setVisibility(View.GONE);
                btnPause.setVisibility(View.VISIBLE);
            } else if (playbackState == Util.PLAYBACK_STATE.PAUSED) {
                btnPlay.setVisibility(View.VISIBLE);
                btnPause.setVisibility(View.GONE);
            } else {
                btnPlay.setVisibility(View.GONE);
                btnPause.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, PlayerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, PlayerService.class));
        }
        unbindService(connection);
        mBound = false;
        if (progressTracker != null) {
            progressTracker.purgeHandler();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(connection);
            mBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    void doUnbindService() {
        if (mBound) {
            if (musicPlayerService != null) {
                musicPlayerService.unregisterListener(this);
                musicPlayerService.stopSelf();
            }
            unbindService(connection);
            mBound = false;
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PlayerService.LocalBinder binder = (PlayerService.LocalBinder) iBinder;
            musicPlayerService = binder.getService();
            mBound = true;
            musicPlayerService.registerListener(HomeActivity.this);
            changeWidget();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };

    @Override
    public void onPlayerStatusChanged() {
        changeWidget();
    }

    public class ProgressTracker implements Runnable {

        private final Player player;
        private final Handler handler;
        private final static int DELAY_MS = 2000;

        private ProgressTracker(Player player) {
            this.player = player;
            handler = new Handler();
            handler.post(this);
        }

        public void run() {
            String curPos = String.valueOf(player.getCurrentPosition());
            long currentPosition = TimeUnit.MILLISECONDS.toSeconds(player.getCurrentPosition());
            //Log.e("Position::", String.valueOf(currentPosition));
            progressBar.setProgress((int) currentPosition);
            //txtProgress.setText(Util.formatDurationForPlayer(currentPosition));
            handler.postDelayed(this, DELAY_MS);
        }

        private void purgeHandler() {
            handler.removeCallbacks(this);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}