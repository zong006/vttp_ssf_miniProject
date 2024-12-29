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
        
        String latestNewsUrl = Util.newsUrl + Util.newsSearchQuery 
                                + Util.newsApiEntry + api_key 
                                + Util.newsPageSizeEntry + Util.newsPageSize 
                                + Util.newsPageEntry + Integer.toString(page);
        
        List<Article> latestArticles = articleService.getArticleList(latestNewsUrl);
        int totalPages = latestArticles.get(0).getPages();

        Map<String, String> sectionMap = articleService.getSections();

        httpSession.setAttribute("url", latestNewsUrl);
        httpSession.setAttribute("headerTitle", "Latest News");
        httpSession.setAttribute("latestPage", page);
        httpSession.setAttribute("searchIsNull", false);
        httpSession.setAttribute("atLatest", true);
        httpSession.setAttribute("atSection", false);
        httpSession.setAttribute("atQuery", false);
        httpSession.removeAttribute("filter");

        model.addAttribute("articles", latestArticles);
        model.addAttribute("sectionMap", sectionMap);
        model.addAttribute("totalPages", totalPages);

        // System.out.println(latestNewsUrl);

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
            httpSession.setAttribute("atSection", false);
            httpSession.setAttribute("latestPage", 1);
            
        }
        page = (int) httpSession.getAttribute("latestPage");

        // gets articles by section according to articleService.getTopicNumbers, and add them to list of articles to display
        for(String topic : topicNumbers.keySet()){
            String pageSize = String.valueOf(topicNumbers.get(topic));
            StringBuilder sb = new StringBuilder();
            String reccNewsUrl = sb.append(Util.newsUrl).append(topic).append("?")
                                    .append(Util.newsApiEntry).append(api_key)
                                    .append(Util.newsPageSizeEntry).append(pageSize)
                                    .append(Util.newsPageEntry).append(Integer.toString(page))
                                    .toString();
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
        recArticleList.addAll(queryArticles);

        recArticleList.sort(Comparator.comparingLong(Article::getDate).reversed());
        int totalPages = recArticleList.get(0).getPages();

        model.addAttribute("articles", recArticleList);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        // show reccomended topics to user based on other similar users
        Map<String, String> topicsToRec = articleService.getRecTopic(user.getUsername());

        // if (topicsToRec.isEmpty()){
        //     System.out.println("emty rec topic map");

        // } 
        model.addAttribute("topicsToRec", topicsToRec);
        httpSession.setAttribute("headerTitle", "News Feed");

        // System.out.println(queryUrl);
        
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
        // set atLatest = true even when browsing a particular section
        httpSession.setAttribute("atLatest", true);
        httpSession.setAttribute("atSection", true);
        httpSession.setAttribute("atQuery", false);

        String sectionUrl = Util.newsUrl + Util.newsSearchQuery + Util.newsSectionEntry +  sectionId + Util.newsApiEntry + api_key + Util.newsPageSizeEntry + Util.newsPageSize + Util.newsPageEntry + Integer.toString(page);
        
        List<Article> articles = articleService.getArticleList(sectionUrl); 
        if (articles.size()==0){
            model.addAttribute("errorMessageUser", "Sorry, no articles found for this topic.");
            return "errorPage";
        }
        int totalPages = articles.get(0).getPages();
        Map<String, String> sectionMap = articleService.getSections();
        model.addAttribute("articles", articles);
        model.addAttribute("sectionMap", sectionMap);
        model.addAttribute("totalPages", totalPages);
        
        httpSession.setAttribute("headerTitle", "Topic: ");
        httpSession.setAttribute("sectionKey", sectionKey);
        httpSession.setAttribute("url", sectionUrl);
        httpSession.setAttribute("latestPage", page);
        httpSession.removeAttribute("filter");
        
        // System.out.println(sectionUrl);
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
        if (queryArticles.size()!=0){  
            // System.out.println("not empty");
            int totalPages = queryArticles.get(0).getPages();
            queryArticles.sort(Comparator.comparingLong(Article::getDate).reversed());

            httpSession.setAttribute("url", queryUrl);
            httpSession.setAttribute("headerTitle", "Search: ");
            httpSession.setAttribute("latestPage", searchPage);
            httpSession.setAttribute("atLatest", true);
            httpSession.setAttribute("atSection", false);
            httpSession.setAttribute("atQuery", true);
            httpSession.setAttribute("query", query);
            httpSession.removeAttribute("filter");

            
            model.addAttribute("articles", queryArticles);
            model.addAttribute("totalPages", totalPages);
        
        
        // save     query to track user interests. want to create a deque of fixed length to only track the latest X number of queries
        
            User user = (User) httpSession.getAttribute("user");
            user.addQuery(query);
            // System.out.println(user.getQueryHist());
            userService.updateUserQueries(user, query);
            httpSession.setAttribute("searchIsNull", false);
        }
        else{
            // System.out.println("is empty");
            httpSession.setAttribute("searchIsNull", true);
            model.addAttribute("noArticleMsg", "No search results found for: " + query);   
        }
        Map<String, String> sectionMap = articleService.getSections();
        model.addAttribute("sectionMap", sectionMap);

        // System.out.println(queryUrl); 
        return "latestNews";
    }

    @GetMapping("/nextPage") // shared by both latest news and search page. as well as newsfeed and sections.
    public String getNextPage(@RequestParam(defaultValue = "1") int page, Model model, HttpSession httpSession) throws ParseException, IOException{

        boolean atLatest = (boolean) httpSession.getAttribute("atLatest");
        
        String sessionUrl = (String) httpSession.getAttribute("url");
        
        int pageIndex = sessionUrl.indexOf("&page=");
        httpSession.setAttribute("latestPage", page);

        if (atLatest){
            int filterIndex = sessionUrl.indexOf("&from-date=");
            String url;
            if (filterIndex==-1){
                url = sessionUrl.substring(0, pageIndex) 
                            + Util.newsPageEntry + Integer.toString(page);
            }
            else {
                url = sessionUrl.substring(0, pageIndex) 
                            + Util.newsPageEntry + Integer.toString(page)
                            + sessionUrl.substring(filterIndex, sessionUrl.length());
            }
        
            List<Article> articles = articleService.getArticleList(url);
            int totalPages = articles.get(0).getPages();
            Map<String, String> sectionMap = articleService.getSections();
            model.addAttribute("sectionMap", sectionMap);
            model.addAttribute("articles", articles);
            model.addAttribute("totalPages", totalPages);

            httpSession.setAttribute("url", url);

            // System.out.println(url); 
            return "latestNews";
        }
        return "redirect:/feed";
    }

    @GetMapping("/filterByDate")
    public String filterArticlesByDate(HttpSession httpSession, 
                                        Model model,
                                        @RequestParam(value = "fromDate") String fromDate,
                                        @RequestParam(value = "toDate") String toDate,
                                        @RequestParam("url") String url) throws ParseException, IOException{
                        
        String sessionUrl = (String) httpSession.getAttribute("url");
        
        int page = (int) httpSession.getAttribute("latestPage");
        if (page==1){
            sessionUrl += Integer.toString(page);
        }
        else{ // if filter articles by date, bring it back to page 1 instead of the filtered list
            int pageIndex = sessionUrl.indexOf("&page=");
            sessionUrl = sessionUrl.substring(0, pageIndex) + Util.newsPageEntry + "1";
            httpSession.setAttribute("latestPage", 1);
        }
        StringBuilder sb = new StringBuilder();
        String filteredUrl = sb.append(sessionUrl)
                    .append(Util.newsFromDate).append(fromDate).append("-01")
                    .append(Util.newsToDate).append(toDate).append("-01")
                    .toString();
        
        List<Article> filtredArticles = articleService.getArticleList(filteredUrl);
        if (filtredArticles.size()==0){
            model.addAttribute("errorMessageUser", "No articles found within that date range.");
            return "errorPage";
        }
        int totalPages = filtredArticles.get(0).getPages();
        Map<String, String> sectionMap = articleService.getSections();

        boolean atSection = (boolean) httpSession.getAttribute("atSection");
        boolean atQuery = (boolean) httpSession.getAttribute("atQuery");
        String headerTitle;
        if (atSection){
            headerTitle = "Topic: ";
        }
        else if(atQuery){
            headerTitle = "Search: ";
        }
        else {
            headerTitle = "Latest News";
        }
        httpSession.setAttribute("headerTitle", headerTitle);
        httpSession.setAttribute("filter", "From " + fromDate + " To " + toDate);
        
        model.addAttribute("articles", filtredArticles);
        model.addAttribute("sectionMap", sectionMap);
        model.addAttribute("totalPages", totalPages);
        httpSession.setAttribute("url", filteredUrl);


        // System.out.println(filteredUrl);
        

        return "latestNews";
    }

    @GetMapping("/back")
    public String removeDateFilter(HttpSession httpSession, Model model) throws ParseException, IOException{

        String sessionUrl = (String) httpSession.getAttribute("url");
        
        int pageIndex = sessionUrl.indexOf("&from-date=");
        if (pageIndex!=-1){
            sessionUrl = sessionUrl.substring(0, pageIndex);
        }
        
        
        List<Article> articles = articleService.getArticleList(sessionUrl);
        Map<String, String> sectionMap = articleService.getSections();
        int totalPages = articles.get(0).getPages();

        boolean atSection = (boolean) httpSession.getAttribute("atSection");
        boolean atQuery = (boolean) httpSession.getAttribute("atQuery");
        String headerTitle;
        if (atSection){
            headerTitle = "Topic: ";
        }
        else if(atQuery){
            headerTitle = "Search: ";
        }
        else {
            headerTitle = "Latest News";
        }
        httpSession.setAttribute("headerTitle", headerTitle);
        httpSession.removeAttribute("filter");

        model.addAttribute("articles", articles);
        model.addAttribute("sectionMap", sectionMap);
        model.addAttribute("totalPages", totalPages);
        httpSession.setAttribute("url", sessionUrl);


        return "latestNews";
    }
}
