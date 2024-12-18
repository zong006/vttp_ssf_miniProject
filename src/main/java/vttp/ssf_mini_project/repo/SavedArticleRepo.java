package vttp.ssf_mini_project.repo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import vttp.ssf_mini_project.util.Util;


@Repository
public class SavedArticleRepo{

    @Autowired
    @Qualifier(Util.template)
    RedisTemplate<String, String> stringTemplate;

    public boolean saveArticle(String id, String entry){
        stringTemplate.opsForList().leftPush(id, entry);
        return true;
    }

    public List<String> getAllArticles(String id){
        Long lengthOfList = stringTemplate.opsForList().size(id);
        return stringTemplate.opsForList().range(id, 0, lengthOfList);
    }

    public boolean deleteById(String id, String entry){
        long number = stringTemplate.opsForList().remove(id, 1, entry);
        return (number>0)? true : false;
    }

    
    // public Set<String> getKeys(){
    //     Set<Object> keys = stringTemplate.opsForHash().keys(Util.redisKey);
    //     return keys.stream().map(k -> (String) k).collect(Collectors.toSet());
    // }
}