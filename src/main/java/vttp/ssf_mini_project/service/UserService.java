package vttp.ssf_mini_project.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import vttp.ssf_mini_project.model.User;
import vttp.ssf_mini_project.repo.UserPrefRepo;
import vttp.ssf_mini_project.util.Util;

@Service
public class UserService {

    @Autowired
    UserPrefRepo userPrefRepo;

    public boolean userExists(String username){
        return userPrefRepo.userExists(username);
    }

    // for section counts
    public List<String> getUserPref(String username) throws JsonMappingException, JsonProcessingException{
        Map<String, Integer> topicCount = getTopicCount(username);
        List<String> topics = new ArrayList<>(topicCount.keySet());
        return topics;
    }

    // for section counts
    public void updateUserPref(String username, List<String> topicsOfInterest) throws JsonMappingException, JsonProcessingException{
        
        Map<String, Integer> topicCount = userExists(username) ? getTopicCount(username) : new HashMap<>();

        for(String topic : topicsOfInterest){
            topicCount.merge(topic, 1, Integer::sum);
        }
        String topicCountString = new ObjectMapper().writeValueAsString(topicCount);
        userPrefRepo.saveUserInfo(Util.interests, username, topicCountString);
    }

    // for section counts
    public Map<String, Integer> getTopicCount(String username) throws JsonMappingException, JsonProcessingException{
        String userPrefString = userPrefRepo.getUserInfo(Util.interests, username);
        Map<String, Integer> topicCount = new ObjectMapper().readValue(userPrefString, new TypeReference<LinkedHashMap<String, Integer>>() {});
        return topicCount;
    }

    // for section counts
    public Map<Object, Object> getPrefEntries(){
        return userPrefRepo.getPrefEntries();
    }

    // for queries
    public void updateUserQueries(User user, String query){
        userPrefRepo.saveUserInfo(Util.queries, user.getUsername(), user.getQueryHist().toString()); 
    }

    public Deque<String> getQueryEntries(String username){
        String entry = userPrefRepo.getUserInfo(Util.queries, username);
        if (entry==null){
            return null;
        }
        String userEntry = entry.substring(1, entry.length()-1);
        
        String[] queries = userEntry.split(",");
        Deque<String> queryDeque = new ArrayDeque<>();
        for (String query : queries) {
            queryDeque.add(query.trim()); 
        }
        
        return queryDeque;
    }

    public Map<String, Map<String, Integer>> getAllUserPrefMap() throws JsonMappingException, JsonProcessingException{

        Map<Object, Object> entries = getPrefEntries();

        ObjectMapper objectMapper = new ObjectMapper();

        String entriesString = objectMapper.writeValueAsString(entries);

        Map<String, String> outerMap = objectMapper.readValue(entriesString, new TypeReference<Map<String, String>>() {});

        Map<String, Map<String, Integer>> allUserPrefMap = new HashMap<>();
        for (Map.Entry<String, String> entry : outerMap.entrySet()) {
            String key = entry.getKey();
            String nestedJsonString = entry.getValue();
            Map<String, Integer> innerMap = objectMapper.readValue(nestedJsonString, new TypeReference<Map<String, Integer>>() {});
            allUserPrefMap.put(key, innerMap);
        }

        return allUserPrefMap;
    }

}



