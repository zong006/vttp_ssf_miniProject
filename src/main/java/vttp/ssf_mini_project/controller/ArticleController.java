package vttp.ssf_mini_project.controller;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vttp.ssf_mini_project.model.Article;
import vttp.ssf_mini_project.model.User;
import vttp.ssf_mini_project.service.ArticleService;
import vttp.ssf_mini_project.service.UserService;
import vttp.ssf_mini_project.util.Util;

@Controller
public class ArticleController {
    
    @Value("${api_key}") 
    private String api_key;

    @Autowired
    ArticleService articleService;

    @Autowired
    UserService userService;

    @GetMapping("/latest")
    public String displayLatestArticles(@RequestParam(defaultValue = "1") int page, Model model, HttpSession httpSession) throws ParseException, IOException{
        
        String latestNewsUrl = Util.newsUrl + Util.newsSearchQuery + Util.newsApiEntry + api_key + Util.newsPageSizeEntry + Util.newsPageSize + Util.newsPageEntry;
        
        List<Article> latestArticles = articleService.getArticleList(latestNewsUrl + Integer.toString(page));
        Map<String, String> sectionMap = articleService.getSections();

        httpSession.setAttribute("url", latestNewsUrl);
        httpSession.setAttribute("headerTitle", "Latest News");
        httpSession.setAttribute("latestPage", page);
        httpSession.setAttribute("searchIsNull", false);
        httpSession.setAttribute("atLatest", true);

        model.addAttribute("articles", latestArticles);
        model.addAttribute("sectionMap", sectionMap);

        System.out.println(httpSession.getAttribute("latestPage"));
        System.out.println(page);

        return "latestNews";
    }

    @GetMapping("/feed") // need to do some math to limit how much to search for each topic
    public String getNewsFeed(HttpSession httpSession, @RequestParam(defaultValue = "1") int page, Model model) throws ParseException, IOException{

        User user = (User) httpSession.getAttribute("user");
        
        List<Article> recArticleList = new ArrayList<>();
        Map<String, Integer> topicNumbers = articleService.getTopicNumbers(user.getUsername()); // math is here

        boolean atLatest = (boolean) httpSession.getAttribute("atLatest");
        if (atLatest){
            httpSession.setAttribute("atLatest", false);
            httpSession.setAttribute("latestPage", 1);
            
        }
        page = (int) httpSession.getAttribute("latestPage");
        System.out.println(page);
        System.out.println(httpSession.getAttribute("latestPage"));

        // gets articles by section according to articleService.getTopicNumbers, and add them to list of articles to display
        for(String topic : topicNumbers.keySet()){
            String pageSize = String.valueOf(topicNumbers.get(topic));
            String reccNewsUrl = Util.newsUrl + topic + "?" + Util.newsApiEntry + api_key + Util.newsPageSizeEntry + pageSize + Util.newsPageEntry + Integer.toString(page);
            List<Article> articles = articleService.getArticleList(reccNewsUrl); 
            recArticleList.addAll(articles);
        }

        // get articles according to search query, and add to list of articles to display
        String queryString = articleService.getQueryString(user.getUsername());
        String queryUrl = Util.newsUrl + Util.newsSearchQuery + Util.newsSearchQueryEntry + queryString 
                            + Util.newsApiEntry + api_key 
                            + Util.newsPageSizeEntry + Util.queryResultSize 
                            + Util.newsPageEntry + Integer.toString(page);
        List<Article> queryArticles = articleService.getArticleList(queryUrl); 
        // System.out.println(queryUrl);
        recArticleList.addAll(queryArticles);

        recArticleList.sort(Comparator.comparingLong(Article::getDate).reversed());

        model.addAttribute("articles", recArticleList);
        model.addAttribute("currentPage", page);

        // show reccomended topics to user based on other similar users
        Map<String, String> topicsToRec = articleService.getRecTopic(user.getUsername());

        if (topicsToRec.isEmpty()){
            System.out.println("emty rec topic map");

        } 
        model.addAttribute("topicsToRec", topicsToRec);
        httpSession.setAttribute("headerTitle", "News Feed");
        
        return "newsFeed";
    }

    @PostMapping("/track") // to track user clicks when user chooses an article to read
    public void trackClicks(@RequestParam(value = "sectionId") String sectionId, 
                        @RequestParam(value = "url") String url,
                        HttpSession httpSession,
                        HttpServletResponse response) throws IOException{

        User user = (User) httpSession.getAttribute("user");
        userService.updateUserPref(user.getUsername(), Collections.singletonList(sectionId));
        
        response.sendRedirect(url); // want to open article in a new tab, keep current latest news tab
    }

    @GetMapping("/toSection")
    public String goToSection(@RequestParam(value = "sectionId") String sectionId, 
                                @RequestParam(value = "sectionKey") String sectionKey,
                                @RequestParam(defaultValue = "1") int page, 
                                Model model, 
                                HttpSession httpSession) throws ParseException, IOException{
        
        User user = (User) httpSession.getAttribute("user");
        userService.updateUserPref(user.getUsername(), Collections.singletonList(sectionId));
        
        String sectionUrl = Util.newsUrl + Util.newsSearchQuery + Util.newsSectionEntry +  sectionId + Util.newsApiEntry + api_key + Util.newsPageSizeEntry + Util.newsPageSize + Util.newsPageEntry + Integer.toString(page);
        
        List<Article> articles = articleService.getArticleList(sectionUrl); 
        Map<String, String> sectionMap = articleService.getSections();
        model.addAttribute("articles", articles);
        model.addAttribute("sectionMap", sectionMap);

        httpSession.setAttribute("headerTitle", "Showing articles about: " + sectionKey);
        httpSession.setAttribute("url", sectionUrl);
        httpSession.setAttribute("latestPage", page);
        httpSession.setAttribute("atLatest", false);
        return "latestNews";
    }

    @GetMapping("/search")
    public String showSearchResults(@RequestParam(value = "query", required = false) String query, @RequestParam(defaultValue = "1", value = "page") int page, Model model, HttpSession httpSession) throws ParseException, IOException{
        
        int searchPage = page;
        // pass query and return search results
        boolean searchIsNull =  (boolean) httpSession.getAttribute("searchIsNull");
        if (!searchIsNull){
            searchPage = 1;
        }
        
        String queryUrl = Util.newsUrl + Util.newsSearchQuery + 
                                Util.newsSearchQueryEntry + query + 
                                Util.newsApiEntry + api_key + 
                                Util.newsPageSizeEntry + Util.newsPageSize + 
                                Util.newsPageEntry;

        List<Article> queryArticles = articleService.getArticleList(queryUrl + Integer.toString(searchPage));
        
        queryArticles.sort(Comparator.comparingLong(Article::getDate).reversed());

        httpSession.setAttribute("url", queryUrl);
        httpSession.setAttribute("headerTitle", "Showing articles about: " + query);
        httpSession.setAttribute("latestPage", searchPage);

        Map<String, String> sectionMap = articleService.getSections();
        model.addAttribute("articles", queryArticles);
        model.addAttribute("sectionMap", sectionMap);

        
        // save query to track user interests. want to create a deque of fixed length to only track the latest X number of queries
        if (queryArticles.size()!=0){
            User user = (User) httpSession.getAttribute("user");
            user.addQuery(query);
            // System.out.println(user.getQueryHist());
            userService.updateUserQueries(user, query);
            httpSession.setAttribute("searchIsNull", false);
        }
        else{
            httpSession.setAttribute("searchIsNull", true);
            model.addAttribute("noArticleMsg", "No search results found for: " + query);   
        }
        

        return "latestNews";
    }

    @GetMapping("/nextPage") // shared by both latest news and search page
    public String getNextPage(@RequestParam(defaultValue = "1") int page, Model model, HttpSession httpSession) throws ParseException, IOException{

        
        String url = (String) httpSession.getAttribute("url") + Integer.toString(page);
        List<Article> articles = articleService.getArticleList(url + Integer.toString(page));
        Map<String, String> sectionMap = articleService.getSections();
        model.addAttribute("sectionMap", sectionMap);

        model.addAttribute("articles", articles);
        httpSession.setAttribute("latestPage", page);

        System.out.println(httpSession.getAttribute("latestPage"));
        System.out.println(page);
        System.out.println(httpSession.getAttribute("atLatest"));

        boolean atLatest = (boolean) httpSession.getAttribute("atLatest");
        return atLatest? "latestNews" : "redirect:/feed";
    }
}
