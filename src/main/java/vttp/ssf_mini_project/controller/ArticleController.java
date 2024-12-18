package vttp.ssf_mini_project.controller;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import vttp.ssf_mini_project.model.Article;
import vttp.ssf_mini_project.model.User;
import vttp.ssf_mini_project.service.ArticleService;
import vttp.ssf_mini_project.util.Util;

@Controller
public class ArticleController {
    
    @Value("") 
    private String api_key;

    @Autowired
    ArticleService articleService;

    
    
    @GetMapping("/latest")
    public String displayLatestArticles(@RequestParam(defaultValue = "1") int page, Model model, HttpSession httpSession) throws ParseException, IOException{
        
        String latestNewsUrl = Util.newsUrl + Util.newsSearchQuery + Util.newsApiEntry + api_key + Util.newsPageSize + Util.newsPageEntry + Integer.toString(page);
        
        List<Article> latestArticles = articleService.getArticleList(latestNewsUrl);

        model.addAttribute("articles", latestArticles);
        model.addAttribute("currentPage", page);
        
        return "latestNews";
    }

    @PostMapping("/latest")
    public String saveArticle(@RequestParam(value = "id") String id, 
                                @RequestParam(value = "type") String type, 
                                @RequestParam(value = "section") String section, 
                                @RequestParam(value = "pillar") String pillar,
                                @RequestParam(value = "page") int page, 
                                Model model) throws ParseException, IOException{
        System.out.println(id);
        System.out.println(type);
        System.out.println(section);
        System.out.println(pillar);
        System.out.println(page);
        
        String latestNewsUrl = Util.newsUrl + Util.newsSearchQuery + Util.newsApiEntry + api_key + Util.newsPageSize + Util.newsPageEntry + Integer.toString(page);
        List<Article> latestArticles = articleService.getArticleList(latestNewsUrl);

        model.addAttribute("articles", latestArticles);
        model.addAttribute("currentPage", page);
        return "latestNews";
    }

    @GetMapping("/feed") // need to do some math to limit how much to search for each topic
    public String getNewsFeed(HttpSession httpSession, @RequestParam(defaultValue = "1") int page, Model model) throws ParseException, IOException{

        User user = (User) httpSession.getAttribute("user");
        String username = user.getUsername();
        String queryString = articleService.generateRelatedQuery(username);
        // System.out.println(queryString);
        List<Article> recArticleList = new ArrayList<>();

        String relatedNewsUrl = Util.newsUrl + Util.newsSearchQuery + Util.newsSearchQueryEntry + queryString + Util.newsApiEntry + api_key + Util.newsPageEntry + Integer.toString(page);
        List<Article> relArticles = articleService.getArticleList(relatedNewsUrl);
        recArticleList.addAll(relArticles);
        // generate articles according to this url, and also according to sections
        List<String> topicsOfInterest = user.getTopicsOfInterest();
        for(String topic : topicsOfInterest){
            String reccNewsUrl = Util.newsUrl + topic + "?" + Util.newsApiEntry + api_key + Util.newsPageSize + Util.newsPageEntry + Integer.toString(page);
            List<Article> articles = articleService.getArticleList(reccNewsUrl); 
            recArticleList.addAll(articles);
        }
        recArticleList.sort(Comparator.comparingLong(Article::getDate).reversed());

        model.addAttribute("articles", recArticleList);
        model.addAttribute("currentPage", page);
        return "newsFeed";
    }

    @GetMapping("/test")
    @ResponseBody
    public String test() throws JsonProcessingException{
        Map<String, Integer> testMap = new HashMap<>();
        testMap.put("books", 1);
        testMap.put("technology", 3);
        testMap.put("world", 6);
        testMap.merge("books", 1, Integer::sum);

        
        

        
        String jString = new ObjectMapper().writeValueAsString(testMap);
        Map<String, Integer> readMap = new ObjectMapper().readValue(jString, new TypeReference<Map<String, Integer>>() {});
        System.out.println(readMap.toString());
        System.out.println(testMap.toString());
        // return jString;
        return testMap.toString();
    }
    
}
