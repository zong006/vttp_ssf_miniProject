package vttp.ssf_mini_project.restController;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import vttp.ssf_mini_project.model.Article;
import vttp.ssf_mini_project.service.ArticleService;
import vttp.ssf_mini_project.util.Util;

@RestController
@RequestMapping(path = "/search", produces = "application/json")
public class ArticleRestController {
    
    @Autowired
    ArticleService articleService;

    @Value("${api_key}") 
    private String api_key;

    @GetMapping("/{query}")
    public ResponseEntity<Object> getEntry(@PathVariable String query) throws ParseException, IOException{
        String url = Util.newsUrl + Util.newsSearchQuery + Util.newsSearchQueryEntry + query + Util.newsApiEntry + api_key;

        try {
            List<Article> queryArticles = articleService.getArticleList(url);
            if (queryArticles.isEmpty()){
                JsonObject response = Json.createObjectBuilder()
                                            .add("status", "no content")
                                            .add("message", "No search results found. Try using different search terms.")
                                            .add("query", query)
                                            .build();

                JsonObject job = Json.createObjectBuilder().add("response", response).build();

                return ResponseEntity.ok().body(job.toString());
            }
            return ResponseEntity.ok().body(queryArticles);

        } catch (IOException e) {
            // System.out.println(e.getMessage());
            // might happen if there is an issue with getting data from guardian api before processing it
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                        .body("Error fetching data from the news API. Please try again later.");
        } catch (HttpClientErrorException e){
            // System.out.println(e.getResponseBodyAsString());
            // System.out.println(e.getMessage());
            
            return ResponseEntity.badRequest().body(e.getResponseBodyAsString());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> test(){
        Map<String, Map<String, String>>res = articleService.splitTopics();

        return ResponseEntity.ok().body(res.toString());
    }
}
