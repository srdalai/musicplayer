package in.sdtechnocrat.musicplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    ArrayList<SongData> songList;
    private MediaPlayerService player;
    boolean serviceBound = false;
    public static final String Broadcast_PLAY_NEW_AUDIO = "in.sdtechnocrat.musicplayer.PlayNewAudio";
    public static final String Broadcast_CHANGE = "in.sdtechnocrat.musicplayer.CHNAGE";
    ProgressBar progressBar;
    ImageView albumArt, btnPrev, btnPlay, btnPause, btnNext;
    TextView textViewTitle, textViewSub;
    SongData currentSongData = null;
    int currentIndex = 0;

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

        textViewSub.setSelected(true);
        textViewTitle.setSelected(true);
        progressBar.setIndeterminate(false);
        progressBar.setProgress(50);

        tabs.setupWithViewPager(viewPager);

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);

        songList = new ArrayList<>();


        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //btnPlay.setVisibility(View.GONE);
                //btnPause.setVisibility(View.VISIBLE);
                modifyPlayer("continue");
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //btnPlay.setVisibility(View.VISIBLE);
                //btnPause.setVisibility(View.GONE);
                modifyPlayer("pause");
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                modifyPlayer("play_next");
                /*if (!(currentIndex == songList.size() - 1)) {
                    modifyPlayer("play_next");
                    currentIndex++;
                    changeWidget();
                }*/

            }
        });

        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                modifyPlayer("play_prev");
               /*if (!(currentIndex == 0)) {
                   modifyPlayer("play_prev");
                   currentIndex--;
                   changeWidget();
               }*/
            }
        });
        //loadAudio();
        btnPlay.setVisibility(View.GONE);
        btnPause.setVisibility(View.GONE);

        IntentFilter filter = new IntentFilter(MediaPlayerService.Broadcast_REBUILD_WIDGET);
        registerReceiver(rebuildWidget, filter);

    }

    private BroadcastReceiver rebuildWidget = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getStringExtra("action")) {
                case "pause":
                    btnPlay.setVisibility(View.VISIBLE);
                    btnPause.setVisibility(View.GONE);
                    break;
                case "continue":
                    btnPlay.setVisibility(View.GONE);
                    btnPause.setVisibility(View.VISIBLE);
                    break;
                case "play_next":
                    currentIndex++;
                    changeWidget();
                    break;
                case "play_prev":
                    currentIndex--;
                    changeWidget();
                    break;
            }
        }
    };

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
        }
        unregisterReceiver(rebuildWidget);
    }

    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) iBinder;
            player = binder.getService();
            serviceBound = true;
            Toast.makeText(getApplicationContext(), "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceBound = false;
        }
    };

    public void changeSongList(ArrayList<SongData> songData, int currentPosition) {
        songList = new ArrayList<>();
        songList = songData;
        currentIndex = currentPosition;
        playAudio(currentPosition);
        changeWidget();
    }

    //new
    private void playAudio(int audioIndex) {
        //Check is service is active
        if (!serviceBound) {
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(songList);
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
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

    private void modifyPlayer(String action) {
        Log.d("Action", action);
        Intent broadcastIntent = new Intent(Broadcast_CHANGE);
        broadcastIntent.putExtra("action", action);
        sendBroadcast(broadcastIntent);
    }

    public void changeWidget() {
        currentSongData = songList.get(currentIndex);
        textViewTitle.setText(currentSongData.getTitle());
        textViewSub.setText(currentSongData.getArtist() + " - " + currentSongData.getAlbum());
        Glide.with(this)
                .load(currentSongData.getAlbumArt())
                .apply(new RequestOptions()
                        .error(R.drawable.ic_album_acent_24dp)
                        .placeholder(R.drawable.ic_album_acent_24dp))
                .into(albumArt);
        btnPlay.setVisibility(View.GONE);
        btnPause.setVisibility(View.VISIBLE);
    }
}