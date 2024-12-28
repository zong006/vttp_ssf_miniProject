package vttp.ssf_mini_project.model;

public class Article {
    private String id;
    private String title;
    private String url;
    private String section;
    private Long date;
    private String imageUrl;
    private String sectionId;
    private int pages;
    
    public int getPages() {
        return pages;
    }
    public void setPages(int pages) {
        this.pages = pages;
    }
    public String getSectionId() {
        return sectionId;
    }
    public void setSectionId(String sectionId) {
        this.sectionId = sectionId;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
   
    public String getSection() {
        return section;
    }
    public void setSection(String section) {
        this.section = section;
    }
    public Long getDate() {
        return date;
    }
    public void setDate(Long date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Article [id=" + id + ", title=" + title + ", url=" + url +  ", section=" + section
                + ", date=" + date + "]";
    }
    
}
