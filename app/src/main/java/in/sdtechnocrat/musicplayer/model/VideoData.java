package in.sdtechnocrat.musicplayer.model;

public class VideoData {

    private String fileName;
    private String contentTitle;

    public VideoData(String fileName) {
        this.fileName = fileName;
    }

    public VideoData() {
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentTitle() {
        return contentTitle;
    }

    public void setContentTitle(String contentTitle) {
        this.contentTitle = contentTitle;
    }
}
