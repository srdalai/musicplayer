package in.sdtechnocrat.musicplayer.player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import in.sdtechnocrat.musicplayer.R;
import in.sdtechnocrat.musicplayer.model.SongData;
import in.sdtechnocrat.musicplayer.utils.Util;

import static in.sdtechnocrat.musicplayer.utils.Util.ACTION_NEXT;
import static in.sdtechnocrat.musicplayer.utils.Util.ACTION_PAUSE;
import static in.sdtechnocrat.musicplayer.utils.Util.ACTION_PLAY;
import static in.sdtechnocrat.musicplayer.utils.Util.ACTION_PREVIOUS;
import static in.sdtechnocrat.musicplayer.utils.Util.ACTION_STOP;
import static in.sdtechnocrat.musicplayer.utils.Util.Broadcast_REBUILD_WIDGET;
import static in.sdtechnocrat.musicplayer.utils.Util.NOTIFICATION_ID;
import static in.sdtechnocrat.musicplayer.utils.Util.fileUri;
import static in.sdtechnocrat.musicplayer.utils.Util.playbackState;

public class PlayerService extends Service implements AudioManager.OnAudioFocusChangeListener {

    Context mContext;
    private final IBinder iBinder = new LocalBinder();
    SimpleExoPlayer exoPlayer;
    private final ArrayList<MusicPlayerListener> mListeners = new ArrayList<>();
    private final Handler mHandler = new Handler();

    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    public enum PlaybackStatus {
        PLAYING,
        PAUSED
    }


    public PlayerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return iBinder;
    }

    /** method for clients */

    public void play() {
        if (playbackState == Util.PLAYBACK_STATE.NONE) {
            // TODO to change ui at the start of the player when nothing is added to the player
            prepareMedia();
        } else {
            exoPlayer.setPlayWhenReady(true);
            playbackState = Util.PLAYBACK_STATE.PLAYING;
            notifyPlayerChangeListeners();
            buildNotification();
        }

    }

    public void pause() {
        exoPlayer.setPlayWhenReady(false);
        playbackState = Util.PLAYBACK_STATE.PAUSED;
        notifyPlayerChangeListeners();
        buildNotification();
    }

    public void changeSong() {
        releasePlayer();
        prepareMedia();
    }

    private void notifyPlayerChangeListeners() {
        for (int i = mListeners.size()-1; i>=0; i--) {
            mListeners.get(i).onPlayerStatusChanged();
        }
    }

    public void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
        }
    }

    public SimpleExoPlayer playerInstance() {
        return exoPlayer;
    }

    public int playerCurrentPosition() {
        if(exoPlayer!=null)
            return millisecondsToString((int) exoPlayer.getCurrentPosition());
        else
            return 0;
    }

    public SongData getCurrentSongMetaData() {
        return Util.currentSong;
    }

    @Override
    public void onAudioFocusChange(int i) {

    }

    public class LocalBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //buildPersistNotification(PlaybackStatus.PAUSED);

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                //initMediaPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
            buildPersistNotification();
        }
        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);
        return START_STICKY;
    }

    private void prepareMedia() {

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, new DefaultTrackSelector(trackSelectionFactory));
        exoPlayer.addListener(listener);
        exoPlayer.prepare(prepareMediaSource());
        exoPlayer.setPlayWhenReady(true);
        Util.playbackState = Util.PLAYBACK_STATE.PLAYING;

        notifyPlayerChangeListeners();
        buildNotification();
    }

    public MediaSource prepareMediaSource() {
        final ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        DataSource.Factory dateSourceFactory = new DefaultDataSourceFactory(this, null, new DefaultHttpDataSourceFactory(getUserAgent(this), null));
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dateSourceFactory).createMediaSource(Uri.parse(Util.currentSong.getData()));
        return mediaSource;
    }

    private int millisecondsToString(int milliseconds) {
        // int seconds = (int) (milliseconds / 1000) % 60 ;
        int seconds = (int) (milliseconds / 1000);
        return seconds;
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        Log.d("SATYA", "Player Destroyed");
        super.onDestroy();
    }

    public static String getUserAgent(@NonNull Context context) {
        String versionName;
        String applicationName = "monitiser";
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "?";
        }
        return applicationName + "/" + versionName + " (Linux;Android " + Build.VERSION.RELEASE
                + ") " + ExoPlayerLibraryInfo.VERSION_SLASHY;
    }

    Player.EventListener listener = new Player.EventListener() {
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }

        @Override
        public void onLoadingChanged(boolean isLoading) {

        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playbackState == Player.STATE_BUFFERING) {
                Util.playbackState = Util.PLAYBACK_STATE.BUFFERING;
                notifyPlayerChangeListeners();
            } else if (playbackState == Player.STATE_READY) {
                Util.playbackState = Util.PLAYBACK_STATE.PLAYING;
                notifyPlayerChangeListeners();
            } else if (playbackState == Player.STATE_ENDED) {
                Util.playbackState = Util.PLAYBACK_STATE.STOPPED;
                notifyPlayerChangeListeners();
                //exoPlayer.release();
                //stopSelf();
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {

        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {

        }

        @Override
        public void onPositionDiscontinuity(int reason) {

        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

        }

        @Override
        public void onSeekProcessed() {
        }
    };

    public interface MusicPlayerListener {
        public void onPlayerStatusChanged();
    }

    public void registerListener(MusicPlayerListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(MusicPlayerListener listener) {
        mListeners.remove(listener);
    }

    private AudioManager audioManager;

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(PlayerService.this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, PlayerService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void notifyChnageToRebuildWidget(String action) {
        Log.d("Action", action);
        Intent broadcastIntent = new Intent(Broadcast_REBUILD_WIDGET);
        broadcastIntent.putExtra("action", action);
        sendBroadcast(broadcastIntent);
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
            notifyChnageToRebuildWidget("play");
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
            notifyChnageToRebuildWidget("pause");
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
            notifyChnageToRebuildWidget("play_next");
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
            notifyChnageToRebuildWidget("play_prev");
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return; //mediaSessionManager exists
        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        transportControls = mediaSession.getController().getTransportControls();
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //updateMetaData();

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                play();
            }

            @Override
            public void onPause() {
                super.onPause();
                pause();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                //skipToNext();
                //updateMetaData();
                //buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                //skipToPrevious();
                //updateMetaData();
                //buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
            }
        });
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void updateMetaData() {
        Bitmap albumArt = null;
        SongData activeAudio = Util.currentSong;
        if (activeAudio.getAlbumArt().equals("") || activeAudio.getAlbumArt() == null) {
            albumArt = BitmapFactory.decodeResource(getResources(),
                    R.drawable.album_art); //TODO replace with medias album_art
        }

        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeAudio.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeAudio.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeAudio.getTitle())
                .build());
    }

    private void buildPersistNotification() {
        // Create a new Notification

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "10")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Track title")
                .setContentText("Artist - Album")
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle());


        startForeground(NOTIFICATION_ID, notificationBuilder.build());

    }

    private void buildNotification() {
        int notificationAction = R.drawable.ic_pause_filled;//needs to be initialized
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackState == Util.PLAYBACK_STATE.PLAYING) {
            notificationAction = R.drawable.ic_pause_filled;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackState == Util.PLAYBACK_STATE.PAUSED) {
            notificationAction = R.drawable.ic_play_filled;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        String artist = "", album = "", title = "";

        Bitmap largeIcon = null;
        if (Util.currentSong == null || Util.currentSong.getAlbumArt().equals("") || Util.currentSong.getAlbumArt() == null) {
            largeIcon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.album_art); //TODO replace with medias album_art
        } else {
            Uri imageUri = Uri.parse(Util.currentSong.getAlbumArt());
            File file = new File(Util.currentSong.getAlbumArt());
            Uri photoURI = Uri.fromFile(file);
            Log.d("TAG", "buildNotification: => " + imageUri);
            Log.d("TAG", "buildNotification: => " + photoURI);
            try {
                largeIcon = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (Util.currentSong != null) {
            artist = Util.currentSong.getArtist();
            album = Util.currentSong.getAlbum();
            title = Util.currentSong.getTitle();
        }

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder =  new NotificationCompat.Builder(this, "10")
                .setShowWhen(false)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setColor(getResources().getColor(R.color.notificationShade))
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentText(artist)
                .setContentTitle(title)
                .setContentInfo(album)
                .addAction(R.drawable.ic_skip_previous_black_24dp, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(R.drawable.ic_skip_next_black_24dp, "next", playbackAction(2));

        notificationBuilder.setDefaults(0);

        //((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notificationBuilder.build());

        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }
}
