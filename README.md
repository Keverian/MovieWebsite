# Project 4
## Video URL
[https://youtu.be/BZfR089JRn0] (Link to video)

## Contributions
### Kevin Hu
Completed task 1

### Karson Tran
Completed tasks 2, 3, and 4 deployed the code via aws and recorded the video

### Task 2 questions
- We used JBDC connection for all of our servlets, some notable places would be our MoviePageServlet, SingleMovieServlet, etc. Anywhere that interacts with the database we utilized the pooling that's described in context.xml, we also used two different pools depending on only reads or read-write requests to the database.
- Our prepared statements were not changed in project 3, we used prepared statements where ever user input is sent into the database.

### Task 3 questions, connection pooling with two backend servers
- in order to utilize connection pooling with two backend servers I expanded the context.xml in order to encompass 2 seperate database file paths. One is meant for only read and is identified by a simple jbdc/moviedb path. While the read-write path is named jbdc/moviedb-read-write this is all defined within web.xml
- Both connection pools respectively connect to two seperate ip addresses depending on if theyre read or read-write, read connects to the slave private ip address and read-write connects to the master private ip address
- Further more to make sure the paths are used properly anywhere where a read-write is called in the code the connection path is changed to reflect that, this happens in only 3 places, CartPaymentServlet which writes purchases, and the Employee DashBoardServlets MovieInsertServlet and StarInsertServlet. These were the main places where read-writes were used, all other instances were reads.

# Project 3
## Video URL
[https://www.youtube.com/watch?v=7z5BEedA4_Y] (Link to video)

## Contributions
### Kevin Hu
Completed task 4 and task 6.

### Karson Tran
Completed task 1, 2, 3, 4, 5 Deploy the code on AWS and record the video.

## Parser Related Info
### Optimizations
- Used multi-threading and threadpools to execute insert queries in parallel/close to parallel
- Used prepare statements batch update instead of inserting queries one by one, reducing the number of communication needed with mysql server.

## Parser Behavior Summary

The parser enforces **strict parsing** with minimal data correction. Any parse that violates consistency or database constraints is rejected and logged in `rejected-parse.txt` along with the reason.

---

### Movies

- **ID**: All spaces are removed.
- **Title / Director**: Accepted as-is.
- **Year**: Must contain only numeric characters.
- **Rejection Conditions**: A movie is rejected if any of the following fields are null or empty:
    - `id`
    - `title`
    - `director`
    - `genre`

---

### Genres / Categories

- **Normalization**:
    - Removes special characters and spaces from parsed category strings.
- **Validation**:
    - Case-insensitive comparison to known category codes from the Stanford Movie site.
- **Rejection Condition**:
    - Rejected if no match is found with existing codes.

---

### Actors

- **Name**: Must be non-empty.
- **ID**:
    - If provided, it is used.
    - Otherwise, a unique ID is generated across both the XML file and database.
- **Birth Year**: May be null.
- **Rejection Condition**:
    - Only rejected if the actor's name is empty.

---

### Cast

- **Actor Handling**:
    - If the actor is not found in the database or XML but the stage name is non-empty, a new actor is created with a generated unique ID.
    - If the stage name is empty, the parse is rejected.
- **Movie Linking**:
    - If the referenced `movieID` does not match an existing movie, the cast parse is rejected (since the movie record cannot be created without complete information like title, director, and genre).

## Prepared Statements

We have been using prepared statements as part of our project since project 1 since it is way elegant than string concatenation, so it is difficult to tell you guys exactly where we used them.

But in general, we used prepared statements for every query that takes user input, contains a variable (example: SELECT * FROM stars WHERE ?), or batch inserts.


# Project 2
## Video URL
[https://www.youtube.com/watch?v=gUVrzDZXf9w] (Link to video)

## Contributions
### Kevin Hu
Completed task 4 and part of task 3.

### Karson Tran
Completed task 1, 2 and part task 3. Deploy the code on AWS and record the video.

## Substring Matching Design

For each of the three search queries, Title, Director, and Stars, they each used the same query into sql, that being the column they are associated with LIKE %searched term%.

Title browsing followed a similiar pattern, however because the letters and numbers had to be the first, the liked matching was left to just r% if we were searching for movies beginning with r.

Title browsing for * was by far the trickiest if * was detected as an input rather than use LIKE, regex is actually used instead, opting to use "movies.title NOT REGEXP '^[a-z0-9]'" as this avoids all movies beginning with any alphanumeric values.

# Project 1
## Video Url
[https://www.youtube.com/watch?v=VaBhkLYqm1w] (Link to video)

## Contributions
## Kevin Hu
Wrote a very good chunk and much of the code, was responsible in ensuring fast queries for the first stage of the project.

### Karson Tran
Was responsible for understanding and getting AWS deployed for the website, also tweaked some of the sql queries to ensure faster execution.
