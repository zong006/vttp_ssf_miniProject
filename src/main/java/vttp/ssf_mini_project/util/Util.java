package vttp.ssf_mini_project.util;

public interface Util {
    String savedArticles = "mini_proj_saved";
    String interests = "mini_proj_interests";

    String newsUrl = "https://content.guardianapis.com/"; 
    String newsSearchQuery = "search?"; //this gives latest
    String newsSectionQuery = "sections?";
    
    String newsPageSize = "&page-size=15";
    String newsPageEntry = "&page=";
    String newsApiEntry = "&api-key=";
    String newsSearchQueryEntry = "&q=";

    String wordUrl = "https://api.datamuse.com/words?rel_jjb=";
    String wordTopicQuery = "&topics=";
    String wordMaxTwo = "&max=2";

    String template = "stringTemplate";
    String delimiter = "THIS_IS_A_DELIMITER";
}