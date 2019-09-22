package in.sdtechnocrat.musicplayer.activity;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import in.sdtechnocrat.musicplayer.model.VideoData;
import in.sdtechnocrat.musicplayer.player.MediaPlayerService;
import in.sdtechnocrat.musicplayer.R;
import in.sdtechnocrat.musicplayer.model.SongData;
import in.sdtechnocrat.musicplayer.player.PlayerService;
import in.sdtechnocrat.musicplayer.utils.StorageUtil;
import in.sdtechnocrat.musicplayer.adapter.ViewPagerAdapter;
import in.sdtechnocrat.musicplayer.utils.Util;
import me.tankery.lib.circularseekbar.CircularSeekBar;

import static in.sdtechnocrat.musicplayer.utils.Util.playbackState;

public class HomeActivity extends AppCompatActivity implements PlayerService.MusicPlayerListener {

    ArrayList<SongData> songList;
    public static final String Broadcast_PLAY_NEW_AUDIO = "in.sdtechnocrat.musicplayer.PlayNewAudio";
    public static final String Broadcast_CHANGE = "in.sdtechnocrat.musicplayer.CHNAGE";
    ProgressBar progressBar;
    CircularSeekBar progressBarRound;
    ImageView albumArt, btnPrev, btnPlay, btnPause, btnNext;
    ImageView albumArtRound, btnPrevFull, btnPlayFull, btnPauseFull, btnNextFull;
    TextView textViewTitle, textViewSub;
    SongData currentSongData = null;
    int currentIndex = 0;
    ProgressTracker progressTracker;
    BottomSheetBehavior sheetBehavior;
    LinearLayout bottom_sheet;

    PlayerService musicPlayerService;
    boolean mBound = false;
    SimpleExoPlayer exoPlayer;
    FrameLayout nowPlayingFrame;
    LinearLayout playerControlLinear;
    ConstraintLayout fullScreenLayout;
    RecyclerView playListRecycler;

    TextView textDuration, textProgress;

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
        playerControlLinear = findViewById(R.id.playerControlLinear);


        albumArtRound = findViewById(R.id.albumArtRound);
        btnPrevFull = findViewById(R.id.btnPrevFull);
        btnPlayFull = findViewById(R.id.btnPlayFull);
        btnPauseFull = findViewById(R.id.btnPauseFull);
        btnNextFull = findViewById(R.id.btnNextFull);
        progressBarRound = findViewById(R.id.progressBarRound);
        fullScreenLayout = findViewById(R.id.fullScreenLayout);
        playListRecycler = findViewById(R.id.playListRecycler);

        textDuration = findViewById(R.id.textDuration);
        textProgress = findViewById(R.id.textProgress);

        textViewSub.setSelected(true);
        textViewTitle.setSelected(true);
        progressBar.setIndeterminate(false);
        progressBar.setProgress(50);
        progressBarRound.setScaleX(-1f);

        bottom_sheet = findViewById(R.id.bottom_sheet);
        sheetBehavior = BottomSheetBehavior.from(bottom_sheet);

        tabs.setupWithViewPager(viewPager);

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);

        songList = new ArrayList<>();

        if (isMyServiceRunning(PlayerService.class)) {
            // TODO Bind existing player to view
        }


        attachClickListeners();
        initCircularProgressTracker();
        initBottomSheetBehaviour();

        btnPlay.setVisibility(View.GONE);
        btnPause.setVisibility(View.GONE);
        btnPlayFull.setVisibility(View.GONE);
        btnPauseFull.setVisibility(View.GONE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, PlayerService.class));
        } else {
            startService(new Intent(this, PlayerService.class));
        }
    }

    private void attachClickListeners() {
        btnPlay.setOnClickListener(view -> musicPlayerService.play());

        btnPause.setOnClickListener(view -> musicPlayerService.pause());

        btnNext.setOnClickListener(view -> musicPlayerService.playerInstance().next());

        btnPrev.setOnClickListener(view -> musicPlayerService.playerInstance().previous());

        btnPlayFull.setOnClickListener(view -> musicPlayerService.play());

        btnPauseFull.setOnClickListener(view -> musicPlayerService.pause());

        btnNextFull.setOnClickListener(view -> musicPlayerService.playerInstance().next());

        btnPrevFull.setOnClickListener(view -> musicPlayerService.playerInstance().previous());

        nowPlayingFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                    sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                //startActivity(new Intent(this, NowPlayingActivityOne.class));

            }
        });
    }

    private void initBottomSheetBehaviour() {

        sheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    nowPlayingFrame.setVisibility(View.GONE);
                    fullScreenLayout.setVisibility(View.VISIBLE);
                } else {
                    if (newState != BottomSheetBehavior.STATE_DRAGGING) {
                        nowPlayingFrame.setVisibility(View.VISIBLE);
                        fullScreenLayout.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                animateView(slideOffset);
            }
        });
    }

    public void initCircularProgressTracker() {
        progressBarRound.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {

            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {
                float progress = seekBar.getProgress();
                musicPlayerService.playerInstance().seekTo(TimeUnit.SECONDS.toMillis((long) progress));
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {

            }
        });
    }

    private void animateView(float slideOffset) {
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

    @Override
    public void onBackPressed() {
        if (sheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    public void playSong() {
        if (playbackState == Util.PLAYBACK_STATE.NONE || playbackState == Util.PLAYBACK_STATE.STOPPED) {
            musicPlayerService.play();
        } else {
            //musicPlayerService.changeSong();
            musicPlayerService.addItem(Util.currentSong);
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

                Uri photoURI;
                String title, subTitle;

                if (Util.playbackType == Util.PLAYBACK_TYPE.VIDEO) {
                    VideoData videoData = Util.currentVideo;
                    photoURI = Uri.fromFile(new File(videoData.getFileName()));
                    title = videoData.getFileName();
                    subTitle = videoData.getFileName();
                } else {
                    photoURI = Uri.fromFile(new File(currentSongData.getAlbumArt()));
                    title = currentSongData.getTitle();
                    subTitle = currentSongData.getArtist() + " - " + currentSongData.getAlbum();
                }

                if (Util.currentCustomMetadata != null) {
                    title = Util.currentCustomMetadata.getTitle();
                    subTitle = Util.currentCustomMetadata.getSubTitle();
                    photoURI = Uri.fromFile(new File(Util.currentCustomMetadata.getThumbImagePath()));
                }

                updateWidgetMetaData(title, subTitle, photoURI);

                long seconds = TimeUnit.MILLISECONDS.toSeconds(musicPlayerService.playerInstance().getDuration());
                String totalDur = Util.convertToTime(String.valueOf(TimeUnit.SECONDS.toMillis(seconds)));
                progressBar.setMax((int) seconds);
                progressBarRound.setMax((int) seconds);
                textDuration.setText(totalDur);
                //Log.d("this", seconds+"");
            }

            if (progressTracker != null) {
                progressTracker.purgeHandler();
            }


            if (playbackState == Util.PLAYBACK_STATE.PLAYING) {
                progressTracker = new ProgressTracker(musicPlayerService.playerInstance());
                btnPlay.setVisibility(View.GONE);
                btnPause.setVisibility(View.VISIBLE);
                btnPlayFull.setVisibility(View.GONE);
                btnPauseFull.setVisibility(View.VISIBLE);
            } else if (playbackState == Util.PLAYBACK_STATE.PAUSED) {
                btnPlay.setVisibility(View.VISIBLE);
                btnPause.setVisibility(View.GONE);
                btnPlayFull.setVisibility(View.VISIBLE);
                btnPauseFull.setVisibility(View.GONE);
            } else {
                btnPlay.setVisibility(View.GONE);
                btnPause.setVisibility(View.GONE);
                btnPlayFull.setVisibility(View.GONE);
                btnPauseFull.setVisibility(View.GONE);
            }
        }
    }

    public void updateWidgetMetaData(String title, String subTitle, Uri mediaUri) {
        textViewTitle.setText(title);
        textViewSub.setText(subTitle);

        Glide.with(this)
                .asBitmap()
                .load(mediaUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.ic_album_acent_24dp)
                .placeholder(R.drawable.ic_album_acent_24dp)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        albumArt.setImageBitmap(resource);
                        albumArtRound.setImageBitmap(Util.convertToHeart(HomeActivity.this, resource));
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

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
            if (playbackState != Util.PLAYBACK_STATE.NONE) {
                changeWidget();
            }
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
            progressBarRound.setProgress((int) currentPosition);
            String curProgress = Util.convertToTime(String.valueOf(TimeUnit.SECONDS.toMillis(currentPosition)));
            textProgress.setText(curProgress);
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