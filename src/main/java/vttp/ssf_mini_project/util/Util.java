package vttp.ssf_mini_project.util;

public interface Util {

    String credentials = "mini_proj_credentials";
    String interests = "mini_proj_interests";
    String queries = "mini_proj_queries";

    String newsUrl = "https://content.guardianapis.com/"; 
    String newsSearchQuery = "search?"; //this gives latest
    String newsSectionQuery = "sections?";
    
    String newsPageSizeEntry = "&page-size=";
    String newsPageSize = "15";
    String newsPageEntry = "&page=";
    String newsApiEntry = "&api-key=";
    String newsSearchQueryEntry = "&q=";
    String newsSectionEntry = "&section=";

    String newsFromDate = "&from-date=";
    String newsToDate = "&to-date=";

    int queryHistMaxSize = 5;

    int queryResultSize = 5;
    int recSectionResultSize = 10;

    String template = "stringTemplate";
}