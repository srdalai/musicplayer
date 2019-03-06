package in.sdtechnocrat.musicplayer;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class TracksAdapter extends RecyclerView.Adapter<TracksAdapter.TracksViewHolder> {

    private Context mContext;
    private ArrayList<SongData> songsList;

    public TracksAdapter(Context mContext, ArrayList<SongData> songsList) {
        this.mContext = mContext;
        this.songsList = songsList;
    }

    @NonNull
    @Override
    public TracksViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_tracks_adapter, parent, false);
        return new TracksViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TracksViewHolder holder, int position) {
        SongData song = songsList.get(holder.getAdapterPosition());
        holder.textViewTitle.setText(song.getTitle());
        holder.textViewSub.setText(song.getArtist());
        //Log.d("Path", song.getAlbumArt());
        //Log.d("Dur", song.getDuration());
        Glide.with(mContext).load(song.getAlbumArt())
                .apply(new RequestOptions()
                        .error(R.drawable.ic_album_acent_24dp)
                        .placeholder(R.drawable.ic_album_acent_24dp))
                .into(holder.albumArt);

        holder.textViewDur.setText(convertToTime(song.getDuration()));
    }

    @Override
    public int getItemCount() {
        return songsList.size();
    }

    public class TracksViewHolder extends RecyclerView.ViewHolder {

        TextView textViewTitle, textViewSub, textViewDur;
        ImageView albumArt, imageViewPlaying;

        public TracksViewHolder(View view) {
            super(view);
            textViewTitle = view.findViewById(R.id.textViewTitle);
            textViewSub = view.findViewById(R.id.textViewSub);
            albumArt = view.findViewById(R.id.albumArt);
            textViewDur = view.findViewById(R.id.textViewDur);
            imageViewPlaying = view.findViewById(R.id.imageViewPlaying);
        }
    }

    private String convertToTime(String miliSeconds) {
        String returnTime = "--:--";
        long minutes = TimeUnit.MILLISECONDS.toMinutes(Long.valueOf(miliSeconds));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(Long.valueOf(miliSeconds));
        String minStr = String.valueOf(minutes);
        String secStr = String.valueOf(seconds).length() >2 ? String.valueOf(seconds).substring(0, 2) : String.valueOf(seconds);
        returnTime = minStr + ":" + secStr;
        return returnTime;
    }
}
