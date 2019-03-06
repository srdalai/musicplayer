package in.sdtechnocrat.musicplayer;

public class SongData {

    private String data;
    private String title;
    private String album;
    private String artist;
    private String albumArt;
    private String duration;


    public SongData(String data, String title, String album, String artist, String albumArt, String duration) {
        this.data = data;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.albumArt = albumArt;
        this.duration = duration;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbumArt() {
        return albumArt;
    }

    public void setAlbumArt(String albumArt) {
        this.albumArt = albumArt;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
