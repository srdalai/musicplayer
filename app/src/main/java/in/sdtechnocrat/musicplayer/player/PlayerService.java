package in.sdtechnocrat.musicplayer.player;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
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
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
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

import in.sdtechnocrat.musicplayer.R;
import in.sdtechnocrat.musicplayer.activity.HomeActivity;
import in.sdtechnocrat.musicplayer.model.CustomMetadata;
import in.sdtechnocrat.musicplayer.model.SongData;
import in.sdtechnocrat.musicplayer.model.VideoData;
import in.sdtechnocrat.musicplayer.utils.Util;

import static in.sdtechnocrat.musicplayer.utils.Util.ACTION_NEXT;
import static in.sdtechnocrat.musicplayer.utils.Util.ACTION_PAUSE;
import static in.sdtechnocrat.musicplayer.utils.Util.ACTION_PLAY;
import static in.sdtechnocrat.musicplayer.utils.Util.ACTION_PREVIOUS;
import static in.sdtechnocrat.musicplayer.utils.Util.ACTION_STOP;
import static in.sdtechnocrat.musicplayer.utils.Util.Broadcast_REBUILD_WIDGET;
import static in.sdtechnocrat.musicplayer.utils.Util.NOTIFICATION_ID;
import static in.sdtechnocrat.musicplayer.utils.Util.playListMetadata;
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

    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    ExtractorsFactory extractorsFactory;
    DataSource.Factory dateSourceFactory;
    ConcatenatingMediaSource concatenatedSource;
    int position = 0;


    public PlayerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        callStateListener();
        //ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver();
        extractorsFactory = new DefaultExtractorsFactory();
        dateSourceFactory = new DefaultDataSourceFactory(this, null, new DefaultHttpDataSourceFactory(getUserAgent(this), null));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

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
        //Request audio focus
        if (!requestAudioFocus()) {
            //Could not gain focus
            stopSelf();
        }
        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);
        return START_STICKY;
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

    public class LocalBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    private void prepareMedia() {

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, new DefaultTrackSelector(trackSelectionFactory));

        prepareMediaSource();

        exoPlayer.addListener(listener);
        exoPlayer.prepare(concatenatedSource);
        exoPlayer.setPlayWhenReady(true);
        Util.playbackState = Util.PLAYBACK_STATE.PLAYING;

        notifyPlayerChangeListeners();
        buildNotification();
    }

    public void prepareMediaSource() {
        MediaSource mediaSource;
        CustomMetadata customMetadata;
        if (Util.playbackType == Util.PLAYBACK_TYPE.VIDEO) {
            VideoData videoData = Util.currentVideo;
            mediaSource = new ProgressiveMediaSource.Factory(dateSourceFactory).setTag(position).createMediaSource(Uri.parse(videoData.getFileName()));
            customMetadata = new CustomMetadata(position, videoData.getContentTitle(), videoData.getContentTitle(), videoData.getContentTitle(), videoData.getFileName());
        } else {
            SongData songData = Util.currentSong;
            mediaSource = new ProgressiveMediaSource.Factory(dateSourceFactory).setTag(position).createMediaSource(Uri.parse(Util.currentSong.getData()));
            customMetadata = new CustomMetadata(position, songData.getTitle(), songData.getArtist(), songData.getAlbum(), songData.getAlbumArt());
        }
        Util.playListMetadata = new ArrayList<>();
        Util.playListMetadata.add(customMetadata);
        Util.currentCustomMetadata = customMetadata;

        concatenatedSource = new ConcatenatingMediaSource(mediaSource);
        //return mediaSource;
    }

    public void addItem(SongData songData) {
        Uri uri = Uri.parse(songData.getData());
        if (position == 0) {
            position = 1;
        } else {
            position++;
        }
        CustomMetadata customMetadata = new CustomMetadata(position, songData.getTitle(), songData.getArtist(), songData.getAlbum(), songData.getAlbumArt());
        playListMetadata.add(customMetadata);
        // Add mediaId (e.g. uri) as tag to the MediaSource.
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dateSourceFactory).setTag(position).createMediaSource(uri);
        concatenatedSource.addMediaSource(mediaSource);
    }

    public void addVideoToQueue(VideoData videoData) {
        Uri uri = Uri.parse(videoData.getFileName());
        if (position == 0) {
            position = 1;
        } else {
            position++;
        }
        CustomMetadata customMetadata = new CustomMetadata(position, videoData.getContentTitle(), videoData.getContentTitle(), videoData.getContentTitle(), videoData.getFileName());
        playListMetadata.add(customMetadata);
        // Add mediaId (e.g. uri) as tag to the MediaSource.
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dateSourceFactory).setTag(position).createMediaSource(uri);
        concatenatedSource.addMediaSource(mediaSource);
    }

    public void clearPlaylist() {
        position = 0;
        playbackState = Util.PLAYBACK_STATE.STOPPED;
        releasePlayer();
        prepareMedia();
    }

    public int getCurrentPlayingItemNum() {
        return (int) exoPlayer.getCurrentTag();
    }

    public void playPosition(int index) {
        exoPlayer.prepare(concatenatedSource.getMediaSource(index));
        exoPlayer.setPlayWhenReady(true);
        Util.playbackState = Util.PLAYBACK_STATE.PLAYING;
        Util.currentCustomMetadata = playListMetadata.get(index);

        notifyPlayerChangeListeners();
        buildNotification();
    }

    private int millisecondsToString(int milliseconds) {
        // int seconds = (int) (milliseconds / 1000) % 60 ;
        int seconds = (int) (milliseconds / 1000);
        return seconds;
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        removeAudioFocus();

        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        removeNotification();

        //unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver);
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
            Util.currentCustomMetadata = Util.playListMetadata.get((Integer) exoPlayer.getCurrentTag());
            notifyPlayerChangeListeners();
            buildNotification();
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

        }

        @Override
        public void onSeekProcessed() {
        }
    };

    public interface MusicPlayerListener {
        void onPlayerStatusChanged();
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
            case 4:
                // Stop playback, remove notification, stop service
                playbackAction.setAction(ACTION_STOP);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void notifyChangeToRebuildWidget(String action) {
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
            notifyChangeToRebuildWidget("play");
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
            notifyChangeToRebuildWidget("pause");
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
            notifyChangeToRebuildWidget("play_next");
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
            notifyChangeToRebuildWidget("play_prev");
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    //Handle incoming phone calls
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        pause();
                        ongoingCall = true;
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (ongoingCall) {
                            ongoingCall = false;
                            pause();
                        }
                        break;
                }
            }
        };

        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    //Becoming noisy
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pause();
            buildNotification();
        }
    };

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
        Log.e("stopservice","stopServices");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onTaskRemoved(rootIntent);
    }

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return; //mediaSessionManager exists
        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        transportControls = mediaSession.getController().getTransportControls();
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //updateMetaData();
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mediaSession.setPlaybackState(stateBuilder.build());

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
                exoPlayer.next();
                //updateMetaData();
                buildNotification();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                exoPlayer.previous();
                //updateMetaData();
                buildNotification();
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                exoPlayer.release();
                stopSelf();
                System.exit(0);
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
        if (Util.currentCustomMetadata == null || Util.currentCustomMetadata.getThumbImagePath().equals("") || Util.currentCustomMetadata.getThumbImagePath() == null) {
            largeIcon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.album_art); //TODO replace with medias album_art
        } else {
            File file = new File(Util.currentCustomMetadata.getThumbImagePath());
            Uri photoURI = Uri.fromFile(file);
            try {
                largeIcon = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (Util.currentCustomMetadata != null) {
            artist = Util.currentCustomMetadata.getArtist();
            album = Util.currentCustomMetadata.getAlbum();
            title = Util.currentCustomMetadata.getTitle();
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
                .setSubText(album)
                .addAction(R.drawable.ic_skip_previous_black_24dp, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(R.drawable.ic_skip_next_black_24dp, "next", playbackAction(2))
                .addAction(R.drawable.ic_close, "stop", playbackAction(4));

        notificationBuilder.setDefaults(0);
        Intent notifyIntent = new Intent(this, HomeActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(
                this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        notificationBuilder.setContentIntent(notifyPendingIntent);

        //((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notificationBuilder.build());

        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        //Invoked when the audio focus of the system is updated.
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                play();
                exoPlayer.setVolume(1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                //removeNotification();
                //stopSelf();
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                exoPlayer.setVolume(0.1f);
                break;
        }
    }
}
