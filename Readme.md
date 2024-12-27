#### Vttp SSF Mini-Project

##### Overview
To build a reccomender system that: 
- tracks the user's read and query history and to use this information to push articles to the user via a news feed.
- performs analysis across other users to suggest topics to the user based on the interests of other similar users.

##### 1. Tracking the user's read and query history
- The following data is stored for each user:
    - a hashmap of topics and a corresponding count.
    - a deque of most recent search queries.
- About the hashmap:
    - Every article contains metadata such as its title, topic, url etc. When a user clicks on an article to read, the topic name is updated into the hashmap, and its count is increased by 1. As the user clicks on more articles to read over time, the hashmap grows and there can be a distribution of topics of interest for the user.
- About the deque:
    - The site supports a search function to query for articles. The query goes into the deque such that only the latest few queries are stored.

##### 2. Pushing articles to the user in the news feed
- Given the distribution of topics according to the hashmap of topics and its counts, a simple sampling can be done to select the latest articles of a given topic according to its probability ie. if the user really likes reading articles belonging to the topic 'Food', counts of 'Food' will be relatively higher in the hashmap compared to other topics. The expected number of articles about food will then be way higher than articles of other topics in the news feed.
- A portion of the news feed is also reserved for pushing articles to the user according to the entries in the recent query history (the query deque).
- All these articles will then be collated, sorted by published datetime, and pushed to the news feed.

##### 3. Suggesting alternative topics to the user
- With the data of all users' hashmaps of topics and its corresponding counts, we can score other users against the curent user using a cosine similarity score.
- The scores are ranked, and topics from the top ranked users not present in the current user's topic count is suggested to the user. A minimum number of topics to reccomend is set such that any difference between this minimum number and the number of reccomended topics based on calculations is made up for by a preset list of "popular topics".

##### 4. View flow
- New users are to register using a username and to select at least one topic of interest. This helps circumvent the cold start problem.
- Upon login or creating a new account, users are directed to a page displaying the latest news regardless of topic. From here, they can navigate to either the news feed, read an article, select a topic, or query for articles.
- The logout button brings them back to the landing page.
