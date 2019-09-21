package com.example.videoplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;

import java.io.File;
import java.util.concurrent.TimeUnit;

import in.sdtechnocrat.musicplayer.activity.NowPlayingActivityOne;
import in.sdtechnocrat.musicplayer.model.SongData;
import in.sdtechnocrat.musicplayer.player.PlayerService;
import in.sdtechnocrat.musicplayer.utils.Util;

import static in.sdtechnocrat.musicplayer.utils.Util.playbackState;

public class VideoPlayer extends AppCompatActivity implements PlayerService.MusicPlayerListener {

    PlayerService musicPlayerService;
    ProgressTracker progressTracker;
    boolean mBound = false;

    PlayerView playerView;
    ImageView btnPlay, btnPause;
    SeekBar progressBar;
    TextView textProgress, textDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        playerView = findViewById(R.id.player_view);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        progressBar = findViewById(R.id.progressBar);
        textProgress = findViewById(R.id.textProgress);
        textDuration = findViewById(R.id.textDuration);

        progressBar.setIndeterminate(false);
        //progressBar.setProgress(50);

        playerView.setDefaultArtwork(getResources().getDrawable(R.drawable.album_art));

        btnPlay.setOnClickListener((view) -> {
            musicPlayerService.play();
        });

        btnPause.setOnClickListener((view) -> {
            musicPlayerService.pause();
        });

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float progress = seekBar.getProgress();
                musicPlayerService.playerInstance().seekTo(TimeUnit.SECONDS.toMillis((long) progress));
            }
        });
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

    public void changeWidget() {
        if (mBound) {

            long seconds = TimeUnit.MILLISECONDS.toSeconds(musicPlayerService.playerInstance().getDuration());
            progressBar.setMax((int) seconds);
            String totalDur = Util.convertToTime(String.valueOf(TimeUnit.SECONDS.toMillis(seconds)));
            textDuration.setText(totalDur);

            progressTracker.purgeHandler();

            if (playbackState == Util.PLAYBACK_STATE.PLAYING) {
                progressTracker = new ProgressTracker(musicPlayerService.playerInstance());
                btnPlay.setVisibility(View.GONE);
                btnPause.setVisibility(View.VISIBLE);
            } else if (playbackState == Util.PLAYBACK_STATE.PAUSED) {
                progressTracker = new ProgressTracker(musicPlayerService.playerInstance());
                btnPlay.setVisibility(View.VISIBLE);
                btnPause.setVisibility(View.GONE);
            } else {
                btnPlay.setVisibility(View.GONE);
                btnPause.setVisibility(View.GONE);
            }

            SongData currentSongData = Util.currentSong;


            String fileName = "";
            if (Util.playbackType == Util.PLAYBACK_TYPE.VIDEO) {
                fileName = Util.currentVideo.getFileName();
            } else {
                fileName = currentSongData.getAlbumArt();
            }
            File file = new File(fileName);
            Uri photoURI = Uri.fromFile(file);
            //updateWidgetMetaData(photoURI);

        }
    }



    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PlayerService.LocalBinder binder = (PlayerService.LocalBinder) iBinder;
            musicPlayerService = binder.getService();
            mBound = true;
            musicPlayerService.registerListener(VideoPlayer.this);
            progressTracker = new ProgressTracker(musicPlayerService.playerInstance());
            playerView.setPlayer(musicPlayerService.playerInstance());
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
        private final static int DELAY_MS = 1000;

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
            String curProgress = Util.convertToTime(String.valueOf(TimeUnit.SECONDS.toMillis(currentPosition)));
            textProgress.setText(curProgress);
            handler.postDelayed(this, DELAY_MS);
        }

        private void purgeHandler() {
            handler.removeCallbacks(this);
        }
    }
}
