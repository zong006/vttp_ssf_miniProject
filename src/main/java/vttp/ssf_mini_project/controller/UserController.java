package vttp.ssf_mini_project.controller;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import vttp.ssf_mini_project.model.User;
import vttp.ssf_mini_project.service.ArticleService;
import vttp.ssf_mini_project.service.UserService;
import vttp.ssf_mini_project.util.Util;

@Controller
public class UserController {
    
    @Autowired
    UserService userService;

    @Autowired
    ArticleService articleService;

    @Value("${api_key}") 
    private String api_key;

    @GetMapping("/")
    public String landingPage(){
        return "landingPage";
    }

    @GetMapping("/login")
    public String loginPage(Model model){
        model.addAttribute("user", new User());
        return "loginPage";
    }

    @PostMapping("/login")
    public String verifyLogin(@Valid @ModelAttribute User user, BindingResult bindingResult, HttpSession httpSession, Model model){

        if (bindingResult.hasErrors()){
            return "loginPage";
        }
        if (userService.userExists(user.getUsername())){
            List<String> topicsOfInterest = userService.getUserPref(user.getUsername());
            user.setTopicsOfInterest(topicsOfInterest);
            httpSession.setAttribute("user", user);
            
            return "redirect:/latest";
        }
        model.addAttribute("errorMessageUser", "The username you entered does not exist. Please create a new account.");
        return "errorPage";
    }

    @GetMapping("/create")
    public String newUserPage(HttpSession httpSession, Model model){
        String url = Util.newsUrl + Util.newsSectionQuery + Util.newsApiEntry + api_key;
        model.addAttribute("user", new User());
        Map<String, String> sectionMap = articleService.getSections(url);

        httpSession.setAttribute("sectionMap", sectionMap);
        
        return "newUserPage";
    }

    @PostMapping("/create")
    public String postLoginDetails(@Valid @ModelAttribute User user, BindingResult bindingResult, Model model, HttpSession httpSession){
        
        
        if (bindingResult.hasErrors()){
            // model.addAttribute("sectionMap", articleService.getSections());
            return "newUserPage";
        }

        httpSession.setAttribute("user", user); 
        // System.out.println(httpSession.getAttribute(user.getUsername()));
        
        if (userService.userExists(user.getUsername())){
            System.out.println("user exsits"); // delete this later
            model.addAttribute("errorMessageUser", "Username already exists. Please choose another username.");
            return "redirect:/create";
        }
        // save the preferences into redis
        userService.saveUserPref(user.getUsername(), user.getTopicsOfInterest());

        return "redirect:/latest";
    }

   


}
