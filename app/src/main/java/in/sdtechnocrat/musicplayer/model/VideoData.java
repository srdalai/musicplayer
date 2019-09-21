package in.sdtechnocrat.musicplayer.model;

public class VideoData {

    private String fileName;
    private String filePath;

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
}
