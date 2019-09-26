package in.sdtechnocrat.musicplayer.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import in.sdtechnocrat.musicplayer.R;
import in.sdtechnocrat.musicplayer.activity.VideoListFragment.OnListFragmentInteractionListener;
import in.sdtechnocrat.musicplayer.model.SongData;
import in.sdtechnocrat.musicplayer.model.VideoData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimpleVideoRecyclerViewAdapter extends RecyclerView.Adapter<SimpleVideoRecyclerViewAdapter.ViewHolder> {

    private Context mContext;
    private  ArrayList<VideoData> videoDataList;

    public SimpleVideoRecyclerViewAdapter(Context mContext, ArrayList<VideoData> videoDataList) {
        this.mContext = mContext;
        this.videoDataList = videoDataList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        VideoData data = videoDataList.get(position);
        holder.mContentView.setText(data.getContentTitle());

        Uri mediaUri = Uri.fromFile(new File(data.getFileName()));
        Glide.with(mContext)
                .asBitmap()
                .load(mediaUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imageThumb);
    }

    @Override
    public int getItemCount() {
        return videoDataList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mContentView;
        ImageView imageThumb;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mContentView = view.findViewById(R.id.content);
            imageThumb = view.findViewById(R.id.imageThumb);
        }

    }
}
