package in.sdtechnocrat.musicplayer.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import in.sdtechnocrat.musicplayer.activity.TracksFragment;
import in.sdtechnocrat.musicplayer.activity.AlbumsFragment;
import in.sdtechnocrat.musicplayer.activity.ArtistsFragment;
import in.sdtechnocrat.musicplayer.activity.PlaylistsFragment;
import in.sdtechnocrat.musicplayer.activity.VideoListFragment;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    public ViewPagerAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                return new TracksFragment();
            case 1:
                return new AlbumsFragment();
            case 2:
                return new ArtistsFragment();
            case 3:
                return new PlaylistsFragment();
            case 4:
                return new VideoListFragment();
        }
        return null;
    }

    @Override
    public int getCount() {
        return 5;
    }

    @Override public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Tracks";
            case 1:
                return "Albums";
            case 2:
                return "Artists";
            case 3:
                return "Playlist";
            case 4:
                return "Videos";
            default:
                return null;
        }
    }
}
