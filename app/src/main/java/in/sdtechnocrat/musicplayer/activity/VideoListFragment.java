package in.sdtechnocrat.musicplayer.activity;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;

import in.sdtechnocrat.musicplayer.R;
import in.sdtechnocrat.musicplayer.adapter.SimpleVideoRecyclerViewAdapter;
import in.sdtechnocrat.musicplayer.model.VideoData;
import in.sdtechnocrat.musicplayer.utils.RecyclerTouchListener;
import in.sdtechnocrat.musicplayer.utils.Util;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class VideoListFragment extends Fragment {

    // TODO: Customize parameters
    private int mColumnCount = 1;
    ArrayList<String> videoList;
    ArrayList<VideoData> videoDataList = new ArrayList<>();

    private OnListFragmentInteractionListener mListener;
    SimpleVideoRecyclerViewAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            adapter = new SimpleVideoRecyclerViewAdapter(requireContext(), videoDataList);
            recyclerView.setAdapter(adapter);

            recyclerView.addOnItemTouchListener(new RecyclerTouchListener(requireContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
                @Override
                public void onClick(View view, int position) {
                    VideoData videoData = videoDataList.get(position);
                    Toast.makeText(requireContext(), videoData.getFileName(), Toast.LENGTH_SHORT).show();
                    Util.currentVideo = videoDataList.get(position);

                    Util.playbackType = Util.PLAYBACK_TYPE.VIDEO;
                    if (requireContext() instanceof HomeActivity) {
                        ((HomeActivity) requireContext()).playSong();
                    }
                }

                @Override
                public void onLongClick(View view, int position) {

                }
            }));


            videoList = getAllMedia();
            Log.d("Video List", videoList.size()+"");
            for (int i = 0; i < videoList.size(); i++) {
                //videoDataList.add(new VideoData(videoList.get(i)));
            }
            parseAllVideo();
            adapter.notifyDataSetChanged();
        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /*if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        //void onListFragmentInteraction(DummyItem item);
    }


    public ArrayList<String> getAllMedia() {
        HashSet<String> videoItemHashSet = new HashSet<>();
        String[] projection = { MediaStore.Video.VideoColumns.DATA ,MediaStore.Video.Media.DISPLAY_NAME};
        Cursor cursor = getContext().getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, null);
        try {
            cursor.moveToFirst();
            do{
                videoItemHashSet.add((cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))));
            }while(cursor.moveToNext());

            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ArrayList<String> downloadedList = new ArrayList<>(videoItemHashSet);
        return downloadedList;
    }

    public void prepareMediaList() {
        String[] thumbColumns = {MediaStore.Video.Thumbnails.DATA, MediaStore.Video.Thumbnails.VIDEO_ID};

        String[] mediaColumns = {MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA, MediaStore.Video.Media.TITLE, MediaStore.Video.Media.MIME_TYPE};



    }

    private void parseAllVideo() {
        try {
            String name = null;
            String[] thumbColumns = {MediaStore.Video.Thumbnails.DATA,
                    MediaStore.Video.Thumbnails.VIDEO_ID};

            int video_column_index;
            String[] proj = {MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.SIZE};
            Cursor videocursor = requireContext().getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    proj, null, null, null);
            int count = videocursor.getCount();
            Log.d("No of video", "" + count);
            for (int i = 0; i < count; i++) {
                VideoData mediaFileInfo = new VideoData();
                video_column_index = videocursor
                        .getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                videocursor.moveToPosition(i);
                name = videocursor.getString(video_column_index);

                mediaFileInfo.setFileName(name);

                int column_index = videocursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                videocursor.moveToPosition(i);
                String filepath = videocursor.getString(column_index);

                mediaFileInfo.setFileName(filepath);
                //mediaFileInfo.setFileType(type);
                videoDataList.add(mediaFileInfo);
                // id += " Size(KB):" +
                // videocursor.getString(video_column_index);


            }
            videocursor.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
