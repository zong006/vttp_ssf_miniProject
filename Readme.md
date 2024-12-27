#### landing page
- enter username : use as key to the hashmap. (check if username exists in the hashmap. choose another one if alr does)
- select at least 1 topic of interest (from list gotten from /sections? endpoint, and add in form to fill in). this submits parameters to be used for content based reccomendations
- link to continue browsing news

#### view 1 (default, latest news)
- have a search bar to enter query term (use this for /q=...) to query for relevant news articles. this also submits the query terms to be saved (potentially for collab filtering?)
- display latest news 
- button to go to reccomended news 
- button to save news url (this submits the tag or something of the news to be used for either content or collab filtering. or use as a like button?)
- button to go to list of saved articles

#### view 2 (reccomended news, news feed)
- have a search bar to enter query term (use this for /q=...) to query for relevant news articles
- display reccomended news 
- button to save news url (this submits the tag or something of the news to be used for either content or collab filtering)
- button to go to list of saved articles

#### view 3 (list of saved articles)
- filter based on year?
- to display saved articles, extract the id of the article. url is https://www.theguardian.com/{id}
- path variable to filter out according to section

#### view 3 error page 
- link to go back to saved articles
- link to go back to latest news
- link to go back to reccomended newsfeed




#### others
- can save articel id. the url is https://www.theguardian.com/{article id}
- features: 
    - type (article, liveblog etc)
    - sectionName (world, lifestyle etc)
    - pillarName (news, sport, art etc)

- to retrieve article according to id, use https://content.guardianapis.com/ + {id} + ?&api-key=d526b545-40c8-402c-ae7b-16691d574c61 
    - see https://open-platform.theguardian.com/documentation/item
- to get articles according to section, use https://content.guardianapis.com/ + {section} + ?&api-key=
- the OR operator in query is | 
- both search query and latest news share the same view and hence same nextPage endpoint (since both get articles from single url). newsfeed on the other hand, gets articles from multiple urls (diff sections etc) hence it will have its own endpoint


#### kpi
- validation: used in the login page (done)
- controller: display latest articles etc (done)
- rest controller: for displaying rec articles?
- path variable: for filtering saved articles?
- 3 views or more: already done
- support omre than 1 user: via httpsessions (in progress)
    - how to ensure that no duplicate usernames in database? to have a create new user and also login page seperately?

#### to do
- add explore other sections to news feed page. this should be topics not inside the entire list
- split the topics in create user page to popular and others (done)
- add a route from section search page to newsfeed as well
- centralize the next page / previous page button
- think of a landing page title (News Read. // You read the news. We read you.)
- maybe add a password