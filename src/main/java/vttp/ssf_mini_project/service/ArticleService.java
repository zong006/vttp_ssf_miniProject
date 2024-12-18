package vttp.ssf_mini_project.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import vttp.ssf_mini_project.model.Article;
import vttp.ssf_mini_project.repo.SavedArticleRepo;
import vttp.ssf_mini_project.repo.UserPrefRepo;
import vttp.ssf_mini_project.util.Util;

@Service
public class ArticleService {
    

    @Autowired
    SavedArticleRepo articleRepo;

    @Autowired
    UserPrefRepo userPrefRepo;

    public List<Article> getArticleList(String url) throws ParseException, IOException{
        
        List<Article> articles = new ArrayList<>();

        JsonReader jsonReader = generateJson(url);
        JsonObject jsonData = jsonReader.readObject();
        JsonObject response = jsonData.getJsonObject("response");
        JsonArray results = response.getJsonArray("results");

        for (int i=0 ; i<results.size() ; i++){
            Article article = new Article();

            JsonObject x = results.getJsonObject(i);
            article.setId(x.getString("id"));
            article.setTitle(x.getString("webTitle"));
            article.setSection(x.getString("sectionName"));
            article.setUrl(x.getString("webUrl"));
            article.setImageUrl(getImageUrl(article.getUrl()+"#img-1"));

            String dateString = x.getString("webPublicationDate");
            Long dateTime = convertDateToLong(dateString);
            article.setDate(dateTime);

            articles.add(article);
        }
        return articles;
    }

    public Map<String, String> getSections(String url){

        
        JsonReader jsonReader = generateJson(url);
        JsonObject jsonData = jsonReader.readObject();
        JsonObject response = jsonData.getJsonObject("response");
        JsonArray results = response.getJsonArray("results");

        Map<String, String> sections = new HashMap<>();
        for (int i=1 ; i<results.size() ; i++){ // first entry is always "about" page of guardian
            JsonObject entry = results.getJsonObject(i);
            sections.put(entry.getString("webTitle") ,entry.getString("id"));
        }
        return sections;
    }

    public void saveArticle(String username, String articleId){
        articleRepo.saveArticle(username+Util.delimiter+"savedArticles", articleId);
    }

    public String generateRelatedQuery(String username){

        List<String> topicsOfInterest = userPrefRepo.getUserPref(username + Util.delimiter + "userPref");
        StringBuilder stringBuilder = new StringBuilder();

        for (String topic : topicsOfInterest){ 
            //for each topic of interest, generate 2 similar words and use those as query for articles
            String url = Util.wordUrl + topic + Util.wordTopicQuery + topic + Util.wordMaxTwo; 
            
            stringBuilder.append(topic);
            stringBuilder.append("|");
            JsonReader jsonReader = generateJson(url);
            JsonArray array = jsonReader.readArray();

            for (int i=0 ; i<array.size() ; i++){
                JsonObject entry = array.getJsonObject(i);
                String word = entry.getString("word");
                stringBuilder.append(word);
                stringBuilder.append("|");
            }
        }
        stringBuilder.setLength(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    private JsonReader generateJson(String url){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
        
        String respBody = responseEntity.getBody();
        InputStream is = new ByteArrayInputStream(respBody.getBytes());
        JsonReader jsonReader = Json.createReader(is);
        return jsonReader;
    }

    private Long convertDateToLong(String dateString) throws ParseException{

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = sdf.parse(dateString);
        return date.getTime();
    }

    private String getImageUrl(String url) throws IOException{

        try {
            Document document = Jsoup.connect(url).get();
            Elements imgElements = document.select("img");
            Element img = imgElements.first();
            return img.attr("src");

        } catch (NullPointerException e) {
            // e.printStackTrace();
            System.out.println(e.getMessage());
            return null;
        }
    }
}
