package in.sdtechnocrat.musicplayer.model;

public class CustomMetadata {

    public int queueNum;

    public String title;

    public String subTitle;

    public String thumbImagePath;

    public CustomMetadata(int queueNum, String title, String subTitle, String thumbImagePath) {
        this.queueNum = queueNum;
        this.title = title;
        this.subTitle = subTitle;
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

    public String getSubTitle() {
        return subTitle;
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
