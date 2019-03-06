package in.sdtechnocrat.musicplayer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;

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
        }
        return null;
    }

    @Override
    public int getCount() {
        return 4;
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
            default:
                return null;
        }
    }
}
