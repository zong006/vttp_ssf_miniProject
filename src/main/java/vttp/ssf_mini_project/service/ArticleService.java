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
        int pages = response.getInt("pages");

        for (int i=0 ; i<results.size() ; i++){
            Article article = new Article();
            article.setPages(pages);

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
        // System.out.println(topMatches); // remove this later
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
        int recSize = 8;  // if the reccomended topic list is empty or too short, pick the top topics across all users
        if (topicsToRec.isEmpty() || topicsToRec.size() < recSize){
            int numberOfTopics = recSize - topicsToRec.size();
            List<String> popTopics = getMostPopularTopics(numberOfTopics);
            
            List<String> popTopicsSubset = popTopics.stream().filter(topic -> !usernameTopicCount.keySet().contains(topic)).toList();
            topicsToRec.addAll(popTopicsSubset); 
        }

        Map<String, String> sectionsMap = getSections();

        Map<String, String> recSectionMap = sectionsMap.entrySet().stream()
            .filter(entry -> topicsToRec.contains(entry.getValue())) // Keep entries with values in the list
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (recSectionMap.size() < recSize){
            Map<String, Map<String, String>> topicSplit = splitTopics();
            Map<String, String> popTopics = topicSplit.get("popular");

            for (String topic : popTopics.keySet()){
                if (!usernameTopicCount.containsKey(popTopics.get(topic))){
                    recSectionMap.put(topic, popTopics.get(topic));
                }
            }
        }

        if (recSectionMap.size() < recSize){
            Random r = new Random();
            int numberOfTopics = recSize - recSectionMap.size();

            Map<String, Map<String, String>> topicSplit = splitTopics();
            Map<String, String> others = topicSplit.get("others");

            int counter = 0;
            for (Map.Entry<String, String> entry : others.entrySet()){
                if (r.nextDouble() < 0.07){
                    recSectionMap.put(entry.getKey(), entry.getValue());
                    counter += 1;
                }
                if (counter == numberOfTopics){
                    break;
                }
            }
        }
        // System.out.println("rec map: " + recSectionMap.toString()); // remove this later
        return recSectionMap;
    }

    private List<String> getTopMatchesForUser(String username) throws JsonMappingException, JsonProcessingException{

        Map<String, Map<String, Integer>> allUserPrefMap = userService.getAllUserPrefMap();
        Map<String, Double> userScores = new HashMap<>();

        for (String user : allUserPrefMap.keySet()){
            if (!username.equals(user)){
                Map<String, Integer> currentUser = allUserPrefMap.get(username);
                Map<String, Integer> otherUser = allUserPrefMap.get(user);
                double cosScore = calcSimScore(currentUser, otherUser);
                if (cosScore > 0){
                    userScores.put(user, cosScore);
                }
                
            }
        }
        // System.out.println(userScores); // remove this later
        return userScores.entrySet().stream()
                .sorted((entry1, entry2) -> Double.compare(entry2.getValue(), entry1.getValue()))  
                .limit(2)  
                .map(Map.Entry::getKey) 
                .collect(Collectors.toList());
    }

    private double calcSimScore(Map<String, Integer> userA, Map<String, Integer> userB){
        // this calculates the cosine similarity as a score between 2 users
        double numerator = 0;
        double magA = 0;
        double magB = 0;

        for (String topic : userA.keySet()) {
            if (userB.containsKey(topic)) {
                double x = userA.get(topic);  
                double y = userB.get(topic);  

                numerator += x * y;
                magA += Math.pow(x, 2);
                magB += Math.pow(y, 2);
            }
        } 
        if (numerator==0){
            return 0;
        }
        double magA_sqrt = Math.sqrt(magA);
        double magB_sqrt = Math.sqrt(magB);

        return numerator / (magA_sqrt * magB_sqrt);
    }

    private List<String> getMostPopularTopics(int numberOfTopics) throws JsonMappingException, JsonProcessingException{
        Map<String, Map<String, Integer>> allUserPrefMap = userService.getAllUserPrefMap();
        Map<String, Integer> popularTopics = new HashMap<>();

        for (Map<String, Integer> userTopicCount : allUserPrefMap.values()){
            for (Map.Entry<String, Integer> entry : userTopicCount.entrySet()){
                String topic = entry.getKey();
                Integer count = entry.getValue();
                popularTopics.merge(topic, count, Integer::sum);
            }
        }
        List<Map.Entry<String, Integer>> topPopularTopics = popularTopics.entrySet().stream()
                                                            .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                                                            .limit(numberOfTopics)
                                                            .toList();

        return topPopularTopics.stream().map(entry -> entry.getKey()).toList();
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

    public Map<String, Map<String, String>> splitTopics(){
        // to split all topics into 5 "popular topics" and others
        Map<String, String> allTopics = getSections();
        Map<String, String> popTopics = new HashMap<>();

        popTopics.put("World news", "world");
        popTopics.put("Technology", "technology");
        popTopics.put("Environment", "environment");
        popTopics.put("Life and style","lifeandstyle");
        popTopics.put("Film", "film");
        popTopics.put("Business", "business");
        popTopics.put("Food", "food");
        popTopics.put("Sport", "sport");

        for (Map.Entry<String, String> entry : popTopics.entrySet()){
            allTopics.remove(entry.getKey());
        }

        Map<String, Map<String, String>> topicSplit = new HashMap<>();
        topicSplit.put("popular", popTopics);
        topicSplit.put("others", allTopics);

        return topicSplit;
    }


}
