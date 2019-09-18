package in.sdtechnocrat.musicplayer.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.PathParser;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.Player;

import java.io.File;
import java.util.concurrent.TimeUnit;

import in.sdtechnocrat.musicplayer.R;
import in.sdtechnocrat.musicplayer.model.SongData;
import in.sdtechnocrat.musicplayer.player.PlayerService;
import in.sdtechnocrat.musicplayer.utils.BitmapUtils;
import in.sdtechnocrat.musicplayer.utils.Util;
import me.tankery.lib.circularseekbar.CircularSeekBar;

import static in.sdtechnocrat.musicplayer.utils.Util.playbackState;

public class NowPlayingActivityOne extends AppCompatActivity implements PlayerService.MusicPlayerListener {

    PlayerService musicPlayerService;
    ProgressTracker progressTracker;


    boolean mBound = false;

    ImageView albumArt, btnPrev, btnPlay, btnPause, btnNext;
    Animation rotation;
    CircularSeekBar progressBar;
    TextView textDuration, textProgress;
    ImageView fullScreenLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing_one);

        albumArt = findViewById(R.id.albumArt);
        btnPrev = findViewById(R.id.btnPrev);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnNext = findViewById(R.id.btnNext);
        progressBar = findViewById(R.id.progressBar);
        textDuration = findViewById(R.id.textDuration);
        textProgress = findViewById(R.id.textProgress);
        fullScreenLayout = findViewById(R.id.fullScreenLayout);


        rotation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        rotation.setFillAfter(true);


        btnPlay.setOnClickListener(view -> {
            musicPlayerService.play();
        });

        btnPause.setOnClickListener(view -> {
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

            long seconds = TimeUnit.MILLISECONDS.toSeconds(musicPlayerService.playerInstance().getDuration());
            progressBar.setMax((int) seconds);
            String totalDur = Util.convertToTime(String.valueOf(TimeUnit.SECONDS.toMillis(seconds)));
            Log.d("this", seconds+"");
            textDuration.setText(totalDur);

            progressTracker.purgeHandler();

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

            SongData currentSongData = Util.currentSong;

            File file = new File(currentSongData.getAlbumArt());
            Uri photoURI = Uri.fromFile(file);

            Glide.with(this)
                    .asBitmap()
                    .load(photoURI)
                    .error(R.drawable.album_art)
                    .placeholder(R.drawable.album_art)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            albumArt.setImageBitmap(convertToHeart(resource));
                            fullScreenLayout.setImageBitmap(resource);

                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        }
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
