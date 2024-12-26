package vttp.ssf_mini_project.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import vttp.ssf_mini_project.model.Article;
import vttp.ssf_mini_project.repo.UserPrefRepo;
import vttp.ssf_mini_project.util.Util;

@Service
public class ArticleService {
    
    @Value("${api_key}") 
    private String api_key;

    @Autowired
    UserPrefRepo userPrefRepo;

    @Autowired
    UserService userService;

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
            article.setSectionId(x.getString("sectionId"));
            article.setUrl(x.getString("webUrl"));
            article.setImageUrl(getImageUrl(article.getUrl()+"#img-1"));

            String dateString = x.getString("webPublicationDate");
            Long dateTime = convertDateToLong(dateString);
            article.setDate(dateTime);

            articles.add(article);
        }
        return articles;
    }

    public Map<String, String> getSections(){

        String url = Util.newsUrl + Util.newsSectionQuery + Util.newsApiEntry + api_key;
        JsonReader jsonReader = generateJson(url);
        JsonObject jsonData = jsonReader.readObject();
        JsonObject response = jsonData.getJsonObject("response");
        JsonArray results = response.getJsonArray("results");

        Map<String, String> sections = new LinkedHashMap<>();
        for (int i=1 ; i<results.size() ; i++){ // first entry is always "about" page of guardian
            JsonObject entry = results.getJsonObject(i);
            sections.put(entry.getString("webTitle") ,entry.getString("id")); // value(id) is the one being saved to redis
        }
        return sections;
    }

    public Map<String, Integer> getTopicNumbers (String username) throws JsonMappingException, JsonProcessingException{
        Map<String, Integer> topicCount =  userService.getTopicCount(username); // this is a linked hashmap, so keys are in order
        
        int articleCount = Util.recSectionResultSize;
        int sumCount = topicCount.values().stream().mapToInt(Integer::intValue).sum();
        List<Double> probabilities = topicCount.values().stream().map(x -> x/(double)sumCount).toList();
        
        double[] cumulativeProbabilities = new double[probabilities.size()];
        cumulativeProbabilities[0] = probabilities.get(0);
        for (int i = 1; i < probabilities.size(); i++) {
            cumulativeProbabilities[i] = cumulativeProbabilities[i - 1] + probabilities.get(i);
        }

        int[] itemCount = new int[cumulativeProbabilities.length];
        Random r = new Random();
        for (int i=0 ; i<articleCount ; i++){
            double randD = r.nextDouble();
            for (int j=0 ; j<cumulativeProbabilities.length ; j++){
                if (randD <= cumulativeProbabilities[j]){
                    itemCount[j] += 1;
                    break;
                }
            }
        }
        Map<String, Integer> topicNumbers = new LinkedHashMap<>();
        int i=0;
        for (String topic : topicCount.keySet()){
            topicNumbers.put(topic, itemCount[i]);
            i+=1;
        }
        return topicNumbers;
    }

    public String getQueryString(String username){
        Deque<String> queryDeque = userService.getQueryEntries(username);
        StringBuilder stringBuilder = new StringBuilder();

        try {
            for (String query : queryDeque){
                stringBuilder.append(query.trim());
                stringBuilder.append("|");
            }
            String queryString = stringBuilder.toString();
            return queryString.substring(0, queryString.length()-1);
        } catch (NullPointerException e) { 
            // e.printStackTrace();
            System.out.println(e.getMessage());
            return "";
        }
    }

    public Map<String, String> getRecTopic(String username) throws JsonMappingException, JsonProcessingException{
        List<String> topMatches = getTopMatchesForUser(username);
        System.out.println(topMatches); // remove this later
        Map<String, Integer> usernameTopicCount = userService.getTopicCount(username);

        List<String> topicsToRec = new ArrayList<>(); // this is a list of topics in topMatch that are not in username
        // entries correspond to values in sectionMap

        for (String topMatch : topMatches){
            Map<String, Integer> topMatchTopicCount = userService.getTopicCount(topMatch);

            for (String topic : topMatchTopicCount.keySet()){
                if (!usernameTopicCount.containsKey(topic)){
                    topicsToRec.add(topic);
                }
            }
        }

        Map<String, String> sectionsMap = getSections();

        Map<String, String> recSectionMap = sectionsMap.entrySet().stream()
            .filter(entry -> topicsToRec.contains(entry.getValue())) // Keep entries with values in the list
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        System.out.println("initial rec map: " + recSectionMap.toString()); // remove this later
        int recSize = 5;    
        if (recSectionMap.isEmpty() || recSectionMap.size() < recSize){
            Random r = new Random();
            
            for (String key : sectionsMap.keySet()){
                if (!recSectionMap.containsKey(key)){
                    if (r.nextDouble() < 0.2){ // if no users are similar, randomly reccomend 5 topics 
                        recSectionMap.put(key, sectionsMap.get(key));
                    }
                    if (recSectionMap.size()>= recSize){
                        break;
                    }
                }
            }
        }
        return recSectionMap;
    }

    private List<String> getTopMatchesForUser(String username) throws JsonMappingException, JsonProcessingException{

        Map<String, Map<String, Integer>> allUserPrefMap = userService.getAllUserPrefMap();
        Map<String, Double> userScores = new HashMap<>();

        for (String user : allUserPrefMap.keySet()){
            if (!username.equals(user)){
                Map<String, Integer> userA = allUserPrefMap.get(username);
                Map<String, Integer> userB = allUserPrefMap.get(user);
                double cosScore = calcSimScore(userA, userB);
                if (cosScore > 0){
                    userScores.put(user, cosScore);
                }
                
            }
        }
        System.out.println(userScores); // remove this later
        return userScores.entrySet().stream()
                .sorted((entry1, entry2) -> Double.compare(entry2.getValue(), entry1.getValue()))  
                .limit(2)  
                .map(Map.Entry::getKey) 
                .collect(Collectors.toList());
    }

    private double calcSimScore(Map<String, Integer> userA, Map<String, Integer> userB){
        // this calculates the cosine similarity as a score between 2 users
        double dotProduct = 0;
        double magA = 0;
        double magB = 0;
        int n = 0;  //  number of common topics

        for (String topic : userA.keySet()) {
            if (userB.containsKey(topic)) {
                double x = userA.get(topic);  
                double y = userB.get(topic);  

                dotProduct += x * y;
                magA += Math.pow(x, 2);
                magB += Math.pow(y, 2);
                n += 1;
            }
        }

        
        if (n == 0) { //  no common topics  -> no similarity -> zero score
            return 0;
        }
        double magA_sqrt = Math.sqrt(magA);
        double magB_sqrt = Math.sqrt(magB);

        return (magA_sqrt == 0 || magB_sqrt == 0) ? 0 : dotProduct / (magA_sqrt * magB_sqrt);
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
