package in.sdtechnocrat.musicplayer.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.PathParser;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.Player;
import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;
import com.google.android.play.core.tasks.OnFailureListener;
import com.google.android.play.core.tasks.OnSuccessListener;

import java.io.File;
import java.util.concurrent.TimeUnit;

import in.sdtechnocrat.musicplayer.R;
import in.sdtechnocrat.musicplayer.adapter.PlaylistAdapter;
import in.sdtechnocrat.musicplayer.model.SongData;
import in.sdtechnocrat.musicplayer.model.VideoData;
import in.sdtechnocrat.musicplayer.player.PlayerService;
import in.sdtechnocrat.musicplayer.utils.BitmapUtils;
import in.sdtechnocrat.musicplayer.utils.RecyclerTouchListener;
import in.sdtechnocrat.musicplayer.utils.Util;
import me.tankery.lib.circularseekbar.CircularSeekBar;

import static in.sdtechnocrat.musicplayer.utils.Util.SHARED_PREFS;
import static in.sdtechnocrat.musicplayer.utils.Util.playbackState;

public class NowPlayingActivityOne extends AppCompatActivity implements PlayerService.MusicPlayerListener {

    PlayerService musicPlayerService;
    ProgressTracker progressTracker;


    boolean mBound = false;

    ImageView albumArt, btnPrev, btnPlay, btnPause, btnNext, btnSwitchVideo, btnPlaylist;
    Animation rotation;
    CircularSeekBar progressBar;
    TextView textDuration, textProgress;
    ImageView fullScreenLayout;

    private int mySessionId;
    private boolean isVideoPlayerAvailable = false;
    SharedPreferences sharedPreferences;
    RecyclerView playListRecycler;

    PlaylistAdapter playlistAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing_one);

        sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        isVideoPlayerAvailable = sharedPreferences.getBoolean("isVideoPlayerAvailable", false);

        albumArt = findViewById(R.id.albumArt);
        btnPrev = findViewById(R.id.btnPrev);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnNext = findViewById(R.id.btnNext);
        progressBar = findViewById(R.id.progressBar);
        textDuration = findViewById(R.id.textDuration);
        textProgress = findViewById(R.id.textProgress);
        fullScreenLayout = findViewById(R.id.fullScreenLayout);
        btnSwitchVideo = findViewById(R.id.btnSwitchVideo);
        btnPlaylist = findViewById(R.id.btnPlaylist);
        playListRecycler = findViewById(R.id.playListRecycler);


        rotation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        rotation.setFillAfter(true);

        playlistAdapter = new PlaylistAdapter(this, Util.playListMetadata);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        playListRecycler.setLayoutManager(layoutManager);
        playListRecycler.setAdapter(playlistAdapter);
        playListRecycler.setNestedScrollingEnabled(false);


        btnPlay.setOnClickListener(view -> {
            musicPlayerService.play();
        });

        btnPause.setOnClickListener(view -> {
            musicPlayerService.pause();
        });

        btnNext.setOnClickListener(view -> {
            musicPlayerService.playerInstance().next();
        });

        btnPrev.setOnClickListener(view -> {
           musicPlayerService.playerInstance().previous();
        });

        btnSwitchVideo.setOnClickListener((view -> {
            if (isVideoPlayerAvailable) {
                Intent intent = new Intent();
                intent.setClassName("in.sdtechnocrat.musicplayer", "com.example.videoplayer.VideoPlayer");
                startActivity(intent);
            } else {
                downloadDynamicModule();
            }
        }));

        btnPlaylist.setOnClickListener((view) -> {
            if (playListRecycler.getVisibility() == View.VISIBLE) {
                playListRecycler.setVisibility(View.GONE);
            } else {
                playListRecycler.setVisibility(View.VISIBLE);
            }
        });

        playListRecycler.addOnItemTouchListener(new RecyclerTouchListener(this, playListRecycler, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                int playlistPosn = Util.playListMetadata.get(position).getQueueNum();
                musicPlayerService.playPosition(playlistPosn);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));


        progressBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
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




    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PlayerService.LocalBinder binder = (PlayerService.LocalBinder) iBinder;
            musicPlayerService = binder.getService();
            mBound = true;
            musicPlayerService.registerListener(NowPlayingActivityOne.this);
            progressTracker = new ProgressTracker(musicPlayerService.playerInstance());
            changeWidget();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };

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

    @Override
    public void onPlayerStatusChanged() {
        changeWidget();
    }


    public void changeWidget() {
        if (mBound) {

            if (playbackState != Util.PLAYBACK_STATE.NONE) {
                playlistAdapter.notifyDataSetChanged();

                Uri photoURI;
                String title, subTitle;

                if (Util.playbackType == Util.PLAYBACK_TYPE.VIDEO) {
                    VideoData videoData = Util.currentVideo;
                    photoURI = Uri.fromFile(new File(videoData.getFileName()));
                    title = videoData.getContentTitle();
                    subTitle = videoData.getContentTitle();
                } else {
                    SongData songData = Util.currentSong;
                    photoURI = Uri.fromFile(new File(songData.getAlbumArt()));
                    title = songData.getTitle();
                    subTitle = songData.getArtist() + " - " + songData.getAlbum();
                }

                if (Util.currentCustomMetadata != null) {
                    title = Util.currentCustomMetadata.getTitle();
                    subTitle = Util.currentCustomMetadata.getSubTitle();
                    photoURI = Uri.fromFile(new File(Util.currentCustomMetadata.getThumbImagePath()));
                }
                updateWidgetMetaData(title, subTitle, photoURI);

                long seconds = TimeUnit.MILLISECONDS.toSeconds(musicPlayerService.playerInstance().getDuration());
                progressBar.setMax((int) seconds);
                String totalDur = Util.convertToTime(String.valueOf(TimeUnit.SECONDS.toMillis(seconds)));
                Log.d("this", seconds+"");
                textDuration.setText(totalDur);
            }

            if (progressTracker != null) {
                progressTracker.purgeHandler();
            }

            if (playbackState == Util.PLAYBACK_STATE.PLAYING) {
                progressTracker = new ProgressTracker(musicPlayerService.playerInstance());
                albumArt.startAnimation(rotation);
                btnPlay.setVisibility(View.GONE);
                btnPause.setVisibility(View.VISIBLE);
            } else if (playbackState == Util.PLAYBACK_STATE.PAUSED) {
                progressTracker = new ProgressTracker(musicPlayerService.playerInstance());
                albumArt.clearAnimation();
                btnPlay.setVisibility(View.VISIBLE);
                btnPause.setVisibility(View.GONE);
            } else {
                albumArt.clearAnimation();
                btnPlay.setVisibility(View.GONE);
                btnPause.setVisibility(View.GONE);
            }

        }
    }

    public void updateWidgetMetaData(String title, String subTitle, Uri mediaUri) {
        //textViewTitle.setText(title);
        //textViewSub.setText(subTitle);
        //textTitle.setText(title);
        //textArtist.setText(subTitle);

        Bitmap errorBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.album_art);
        Drawable errorDrawable = new BitmapDrawable(getResources(), Util.convertToHeart(this, errorBitmap));

        Glide.with(this)
                .asBitmap()
                .load(mediaUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(errorDrawable)
                .placeholder(errorDrawable)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        albumArt.setImageBitmap(convertToHeart(resource));
                        //fullScreenLayout.setImageBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
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

    public int getPlaylistPlayingPosition() {
        if (mBound) {
            try {
                return (int) musicPlayerService.playerInstance().getCurrentTag();
            } catch (Exception e) {
                return 0;
            }
        } else {
            return 0;
        }
    }


    private void downloadDynamicModule() {
        SplitInstallManager splitInstallManager = SplitInstallManagerFactory.create(this);

        SplitInstallRequest request = SplitInstallRequest
                        .newBuilder()
                        .addModule("videoplayer")
                        .build();

        SplitInstallStateUpdatedListener listener = splitInstallSessionState -> {
            if(splitInstallSessionState.sessionId() == mySessionId) {
                switch (splitInstallSessionState.status()) {
                    case SplitInstallSessionStatus.INSTALLED:
                        Log.d("TAG", "Dynamic Module downloaded");
                        Toast.makeText(NowPlayingActivityOne.this, "Dynamic Module downloaded", Toast.LENGTH_SHORT).show();
                        isVideoPlayerAvailable = true;
                        sharedPreferences.edit().putBoolean("isVideoPlayerAvailable", true).apply();
                        break;
                }
            }
        };


        splitInstallManager.registerListener(listener);

        splitInstallManager.startInstall(request)
                .addOnFailureListener(e -> Log.d("TAG", "Exception: " + e))
                .addOnSuccessListener(sessionId -> mySessionId = sessionId);
    }























    private Bitmap convertToHeart(Bitmap src) {
        return BitmapUtils.getCroppedBitmap(src, getHeartPath(src));
    }

    private Path getHeartPath(Bitmap src) {
        return resizePath(PathParser.createPathFromPathData(getString(R.string.albumN)),
                src.getWidth(), src.getHeight());
    }
    public static Path resizePath(Path path, float width, float height) {
        RectF bounds = new RectF(0, 0, width, height);
        Path resizedPath = new Path(path);
        RectF src = new RectF();
        resizedPath.computeBounds(src, true);

        Matrix resizeMatrix = new Matrix();
        resizeMatrix.setRectToRect(src, bounds, Matrix.ScaleToFit.CENTER);
        resizedPath.transform(resizeMatrix);

        return resizedPath;
    }
}
