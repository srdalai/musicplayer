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
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.ArrayList;

import in.sdtechnocrat.musicplayer.R;
import in.sdtechnocrat.musicplayer.activity.HomeActivity;
import in.sdtechnocrat.musicplayer.activity.NowPlayingActivityOne;
import in.sdtechnocrat.musicplayer.model.CustomMetadata;
import in.sdtechnocrat.musicplayer.utils.Util;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private Context mContext;
    private ArrayList<CustomMetadata> dataList;

    public PlaylistAdapter(Context mContext, ArrayList<CustomMetadata> dataList) {
        this.mContext = mContext;
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_playlist_adapter, parent, false);
        return new PlaylistViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        CustomMetadata metadata = dataList.get(position);

        holder.textViewTitle.setText(metadata.getTitle());
        holder.textViewArtist.setText(metadata.getArtist());

        Uri fileUri = Uri.fromFile(new File(metadata.getThumbImagePath()));
        RequestOptions options = new RequestOptions();
        options.circleCrop();

        Glide.with(mContext)
                .load(fileUri)
                .apply(options)
                .into(holder.imageAlbumArt);

        if (mContext instanceof NowPlayingActivityOne) {
            int posn = ((NowPlayingActivityOne) mContext).getPlaylistPlayingPosition();

            if (posn == position) {
                holder.imageIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_pause_outlined));
            } else {
                holder.imageIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_play_outlined));
            }
        } else if (mContext instanceof HomeActivity) {
            int posn = ((HomeActivity) mContext).getPlaylistPlayingPosition();

            if (posn == position) {
                holder.imageIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_pause_outlined));
            } else {
                holder.imageIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_play_outlined));
            }
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class PlaylistViewHolder extends RecyclerView.ViewHolder {

        TextView textViewTitle, textViewArtist, textViewRuntime;
        ImageView imageAlbumArt, imageIcon;


        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewArtist = itemView.findViewById(R.id.textViewArtist);
            textViewRuntime = itemView.findViewById(R.id.textViewRuntime);
            imageAlbumArt = itemView.findViewById(R.id.imageAlbumArt);
            imageIcon = itemView.findViewById(R.id.imageIcon);
        }
    }
}
