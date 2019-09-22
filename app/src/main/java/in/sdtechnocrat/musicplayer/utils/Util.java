package in.sdtechnocrat.musicplayer.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;

import androidx.core.graphics.PathParser;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import in.sdtechnocrat.musicplayer.R;
import in.sdtechnocrat.musicplayer.model.CustomMetadata;
import in.sdtechnocrat.musicplayer.model.SongData;
import in.sdtechnocrat.musicplayer.model.VideoData;

public class Util {

    public static String fileUri = "";
    public static SongData currentSong;
    public static VideoData currentVideo;
    public static ArrayList<CustomMetadata> playListMetadata;
    public static CustomMetadata currentCustomMetadata;

    public static PLAYBACK_STATE playbackState = PLAYBACK_STATE.NONE;
    public enum PLAYBACK_STATE {NONE, PLAYING, PAUSED, STOPPED, BUFFERING}
    public enum PLAYBACK_TYPE {NONE, AUDIO, VIDEO}
    public static PLAYBACK_TYPE playbackType = PLAYBACK_TYPE.NONE;

    public static final String ACTION_PLAY = "in.sdtechnocrat.musicplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "in.sdtechnocrat.musicplayer.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "in.sdtechnocrat.musicplayer.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "in.sdtechnocrat.musicplayer.ACTION_NEXT";
    public static final String ACTION_STOP = "in.sdtechnocrat.musicplayer.ACTION_STOP";

    public static final String Broadcast_REBUILD_WIDGET = "in.sdtechnocrat.musicplayer.REBUILD_WIDGET";

    //AudioPlayer notification ID
    public static final int NOTIFICATION_ID = 101;
    public static final String SHARED_PREFS = "in.sdtechnocrat.musicplayer.userprefs";


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

    public static Bitmap convertToHeart(Context ctx, Bitmap src) {
        return BitmapUtils.getCroppedBitmap(src, getHeartPath(ctx, src));
    }

    private static Path getHeartPath(Context ctx, Bitmap src) {
        return resizePath(PathParser.createPathFromPathData(ctx.getResources().getString(R.string.albumN)),
                src.getWidth(), src.getHeight());
    }
    private static Path resizePath(Path path, float width, float height) {
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
