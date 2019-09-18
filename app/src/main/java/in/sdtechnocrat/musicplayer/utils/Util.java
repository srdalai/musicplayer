package in.sdtechnocrat.musicplayer.utils;

import java.util.concurrent.TimeUnit;

import in.sdtechnocrat.musicplayer.model.SongData;

public class Util {

    public static String fileUri = "";
    public static SongData currentSong;

    public static PLAYBACK_STATE playbackState = PLAYBACK_STATE.NONE;
    public enum PLAYBACK_STATE {NONE, PLAYING, PAUSED, STOPPED, BUFFERING}

    public static final String ACTION_PLAY = "in.sdtechnocrat.musicplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "in.sdtechnocrat.musicplayer.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "in.sdtechnocrat.musicplayer.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "in.sdtechnocrat.musicplayer.ACTION_NEXT";
    public static final String ACTION_STOP = "in.sdtechnocrat.musicplayer.ACTION_STOP";

    public static final String Broadcast_REBUILD_WIDGET = "in.sdtechnocrat.musicplayer.REBUILD_WIDGET";

    //AudioPlayer notification ID
    public static final int NOTIFICATION_ID = 101;


    public static String convertToTime(String milliSeconds) {
        String returnTime = "--:--";
        long minutes = TimeUnit.MILLISECONDS.toMinutes(Long.parseLong(milliSeconds));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(Long.parseLong(milliSeconds));
        String minStr = String.valueOf(minutes);
        if (minutes == 0) {
            minStr = "00";
        }
        String secStr = String.valueOf(seconds).length() >2 ? String.valueOf(seconds).substring(0, 2) : String.valueOf(seconds);
        if (secStr.length() == 1) {
            secStr = "0"+secStr;
        }
        returnTime = minStr + ":" + secStr;
        return returnTime;
    }
}
