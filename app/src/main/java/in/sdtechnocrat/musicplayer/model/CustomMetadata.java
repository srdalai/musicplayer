package in.sdtechnocrat.musicplayer.model;

public class CustomMetadata {

    public int queueNum;
    public String title;
    private String artist;
    public String album;
    public String subTitle;
    public String thumbImagePath;

    public CustomMetadata(int queueNum, String title, String artist, String album, String thumbImagePath) {
        this.queueNum = queueNum;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.thumbImagePath = thumbImagePath;
    }

    public int getQueueNum() {
        return queueNum;
    }

    public void setQueueNum(int queueNum) {
        this.queueNum = queueNum;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getSubTitle() {
        return getArtist() + " - " + getAlbum();
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public String getThumbImagePath() {
        return thumbImagePath;
    }

    public void setThumbImagePath(String thumbImagePath) {
        this.thumbImagePath = thumbImagePath;
    }
}
