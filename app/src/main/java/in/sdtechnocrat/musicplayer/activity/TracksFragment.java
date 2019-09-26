package in.sdtechnocrat.musicplayer.activity;


import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import in.sdtechnocrat.musicplayer.R;
import in.sdtechnocrat.musicplayer.player.PlayerService;
import in.sdtechnocrat.musicplayer.utils.RecyclerTouchListener;
import in.sdtechnocrat.musicplayer.adapter.TracksAdapter;
import in.sdtechnocrat.musicplayer.model.SongData;
import in.sdtechnocrat.musicplayer.utils.Util;


/**
 * A simple {@link Fragment} subclass.
 */
public class TracksFragment extends Fragment {

    ArrayList<SongData> songList;
    RecyclerView recyclerTracks;
    TracksAdapter tracksAdapter;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tracks, container, false);


        recyclerTracks = rootView.findViewById(R.id.recyclerTracks);

        songList = new ArrayList<>();

        tracksAdapter = new TracksAdapter(requireContext(), songList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerTracks.setLayoutManager(layoutManager);
        recyclerTracks.setAdapter(tracksAdapter);
        recyclerTracks.addOnItemTouchListener(new RecyclerTouchListener(requireContext(), recyclerTracks, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                SongData songData = songList.get(position);
                Toast.makeText(requireContext(), songData.getTitle(), Toast.LENGTH_SHORT).show();
                Util.currentSong = songList.get(position);

                if (requireContext() instanceof HomeActivity) {
                    ((HomeActivity) requireContext()).playOrQueueAudio(songData);
                }
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        new Thread(new Runnable() {
            @Override
            public void run() {
                loadAudio();
            }
        }).start();

        return rootView;
    }


    private void loadAudio() {
        ContentResolver contentResolver = requireContext().getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

        if (cursor != null && cursor.getCount() > 0) {
           // songList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String albumID = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                String albumArt = getAlbumArt(albumID);

                // Save to audioList
                songList.add(new SongData(data, title, album, artist, albumArt, duration));
            }
        }
        cursor.close();
        Log.d("List Size", songList.size()+"");
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tracksAdapter.notifyDataSetChanged();
            }
        });
    }

    private String getAlbumArt(String albumID) {
        String albumArt = "";
        Cursor cursor = requireContext().getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID+ "=?",
                new String[] {String.valueOf(albumID)},
                null);

        if (cursor.moveToFirst()) {
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            // do whatever you need to do
            albumArt = path;
        }
        cursor.close();
        if (albumArt == null) {
            return "";
        }
        return albumArt;
    }
}
