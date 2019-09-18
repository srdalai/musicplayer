package in.sdtechnocrat.musicplayer.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import in.sdtechnocrat.musicplayer.R;
import in.sdtechnocrat.musicplayer.model.SongData;
import in.sdtechnocrat.musicplayer.utils.Util;

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

        File file = new File(song.getAlbumArt());
        Uri photoURI = Uri.fromFile(file);

        Glide.with(mContext).load(photoURI)
                .error(R.drawable.ic_album_acent_24dp)
                .placeholder(R.drawable.ic_album_acent_24dp)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.albumArt);

        holder.textViewDur.setText(Util.convertToTime(song.getDuration()));
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

}
