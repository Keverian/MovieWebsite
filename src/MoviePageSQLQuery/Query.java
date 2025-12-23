package MoviePageSQLQuery;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Query {
    static public JsonObject executeSqlStatements(Map<String, String> attributeMap, Connection conn) throws SQLException {
        //browse parameter
        String genre = attributeMap.get("Genre");
        String browseTitle = attributeMap.get("BrowseTitle");
        String orderByRequest = createOrderBy(attributeMap.get("SortedBy"), attributeMap.get("ArrangeOrderTitle"), attributeMap.get("ArrangeOrderRating"));
        String pageSizeAttribute = attributeMap.get("PageSize");
        int pageSize;

        if (pageSizeAttribute == null || pageSizeAttribute.isEmpty()) {
            pageSize = 25;
        } else {
            pageSize = Integer.parseInt(pageSizeAttribute);
        }

        ArrayList<Object> whereClauseAndParams = null;
        //these variables are used to simplify inner join if not needed
        boolean isGenre = genre != null && !genre.isEmpty();
        boolean isStarName = false;
        if (isGenre) {
            whereClauseAndParams = createGenreWhere(genre);
        } else if (browseTitle != null && !browseTitle.isEmpty()) {
            whereClauseAndParams = createBrowseTitleWhereClause(browseTitle);
        } else {
            String title = attributeMap.get("Title");
            String year = attributeMap.get("Year");
            String director = attributeMap.get("Director");
            String starname = attributeMap.get("StarName");

            isStarName = (starname != null && !starname.isEmpty());
            whereClauseAndParams = createSqlMoviesWhereClause(title, year, director, starname);
        }

        //used for pagination
        String pageString = attributeMap.get("CurrPage");
        int page;
        if (pageString == null || pageString.isEmpty()) {
            page = 0;
        } else {
            int num = Integer.parseInt(pageString);
            page = Math.max(num, 0);
        }

        //create query body section
        String queryBody = "FROM movies " +
                "LEFT JOIN ratings ON movies.id = ratings.movieId" +
                ((isStarName) ? " INNER JOIN stars_in_movies ON movies.id = stars_in_movies.movieId" +
                        " INNER JOIN stars ON stars.id = stars_in_movies.starId": "") +
                ((isGenre) ? " INNER JOIN genres_in_movies ON movies.id = genres_in_movies.movieId": "") +
                ((isGenre) ? " INNER JOIN genres ON genres.id = genres_in_movies.genreId": "") +
                ((whereClauseAndParams != null) ? " WHERE " +  whereClauseAndParams.get(0): "");

        String getMovieQueryCount = "SELECT count(movies.id) AS movies_count " +
                queryBody;

        PreparedStatement ppst = createPreparedStatement(getMovieQueryCount, whereClauseAndParams, conn);
        ResultSet rs = ppst.executeQuery();

        rs.next();
        int size = rs.getInt("movies_count");
        rs.close();
        ppst.close();

        //actual searches
        //page and pageSize are integers so they don't need to be sanitizied
        String getTopMovieQuery = "SELECT movies.id, movies.title, movies.year, movies.director, ratings.rating " +
                queryBody +
                " ORDER BY " + orderByRequest + " LIMIT " + pageSize +
                " OFFSET " + (page * pageSize) + ";";
        ppst = createPreparedStatement(getTopMovieQuery, whereClauseAndParams, conn);

        rs = ppst.executeQuery();
        JsonArray jsonArray = getTopMovieInfo(rs, conn);
        ppst.close();
        rs.close();

        //add size of query and info
        JsonObject jobj = new JsonObject();
        jobj.addProperty("movies_count", size);
        jobj.addProperty("CurrPage", attributeMap.get("CurrPage"));
        jobj.add("movies", jsonArray);

        return jobj;
    }

    static private PreparedStatement createPreparedStatement(String query, ArrayList<Object> parameters, Connection conn) throws SQLException {
        PreparedStatement ppst = conn.prepareStatement(query);

        if (parameters == null) return ppst;

        for (int i = 1; i < parameters.size(); ++i) {
            if (parameters.get(i) instanceof Integer) {
                ppst.setInt(i, (int) parameters.get(i));
            } else {
                ppst.setString(i, parameters.get(i).toString());
            }
        }

        return ppst;
    }

    static private ArrayList<Object> createGenreWhere(String genre) {
        ArrayList<Object> clauseAndParams = new ArrayList<>();
        clauseAndParams.add("genres.name = ?");
        clauseAndParams.add(genre);

        return clauseAndParams;
    }

    static private ArrayList<Object> createBrowseTitleWhereClause(String browseTitle) {
        ArrayList<Object> clauseAndParams = new ArrayList<>();
        if (browseTitle.equals("*")) {
            clauseAndParams.add("movies.title NOT REGEXP '^[a-z0-9]'");
        } else {
            clauseAndParams.add("movies.title LIKE ?");
            clauseAndParams.add(browseTitle + "%");
        }
        return clauseAndParams;
    }

    static private String createOrderBy(String orderBy, String arrangeByTitle, String arrangeByRating) {
        if (orderBy == null || orderBy.isEmpty()) orderBy = "title,rating";

        if (arrangeByTitle == null || arrangeByTitle.isEmpty()) arrangeByTitle = "ascend";

        if (arrangeByRating == null || arrangeByRating.isEmpty()) arrangeByRating = "ascend";

        //doesn't need to be prepared, merely compares strings
        String[] orderByParts = orderBy.split(",");
        ArrayList<String> orderByList = new ArrayList();
        Map<String, String> orderBySql = new HashMap<>();
        orderBySql.put("rating", "ratings.rating");
        orderBySql.put("title", "movies.title");

        //arrange by sql section
        Map<String, String> arrangeBySql = new HashMap<>();
        arrangeBySql.put("ascend", "ASC");
        arrangeBySql.put("descend", "DESC");

        for (String orderByPart : orderByParts) {
            String columnGotten = orderBySql.get(orderByPart);
            //uh oh, improper column gotten
            if (columnGotten == null) return null;

            StringBuilder orderBySqlString = new StringBuilder(columnGotten + " ");
            if (orderByPart.equals("rating")) {
                orderBySqlString.append(arrangeBySql.get(arrangeByRating));
            } else if (orderByPart.equals("title")) {
                orderBySqlString.append(arrangeBySql.get(arrangeByTitle));
            } else {
                //that's not good, rating and title weren't inputted so this must be invalid
                return null;
            }

            orderByList.add(orderBySqlString.toString());
        }

        return String.join(",", orderByList);
    }

    static private ArrayList<Object> createSqlMoviesWhereClause(String title, String year, String director, String starname) {
        ArrayList<String> searchParams = new ArrayList<>();
        ArrayList<Object> searchParamAndValues = new ArrayList<>();

        //dummy string to add to the front
        searchParamAndValues.add("");

        if (title != null && !title.isEmpty() && year == null && director == null && starname == null) {
            //full text search
        }
        //"SELECT title, id FROM movies WHERE MATCH (title) AGAINST (? IN BOOLEAN MODE) LIMIT 10";
        if (title != null && !title.isEmpty()) {
            searchParams.add("MATCH (movies.title) AGAINST (? IN BOOLEAN MODE)");
            searchParamAndValues.add(tokenizeTitleReformatToFullTextSearch(title));
        }

        //as per instructions year does NOT use language parsing zero or more
        if (year != null && !year.isEmpty()) {
            searchParams.add("movies.year = ?");
            searchParamAndValues.add(year);
        }

        if (director != null && !director.isEmpty()) {
            searchParams.add("movies.director LIKE ?");
            searchParamAndValues.add("%" + director + "%");
        }

        if (starname != null && !starname.isEmpty()) {
            searchParams.add("stars.name LIKE ?");
            searchParamAndValues.add("%" + starname + "%");
        }

        //if there are no search params simply return 0, otherwise return the clause
        if (searchParams.isEmpty()) {
            return null;
        }

        //sets it so the very first variable is the where clause and all variables afterwards are the individual values
        searchParamAndValues.set(0, String.join(" AND ", searchParams));
        return searchParamAndValues;
    }
    static private String tokenizeTitleReformatToFullTextSearch(String title) {
        String[] tokens = title.trim().split("\\s+");
        return Arrays.stream(tokens)
                .map(token -> "+" + token + "*")
                .collect(Collectors.joining(" "));
    }
    static protected JsonArray getTopMovieInfo(ResultSet rs, Connection conn) throws SQLException {
        JsonArray rv = new JsonArray();
        while (rs.next()) {
            JsonObject jObj = new JsonObject();
            String movieId = rs.getString("movies.id");

            jObj.addProperty("movieId", rs.getString("movies.id"));
            jObj.addProperty("title", rs.getString("movies.title"));
            jObj.addProperty("rating", rs.getFloat("ratings.rating"));
            jObj.addProperty("year", rs.getString("movies.year"));
            jObj.addProperty("director", rs.getString("movies.director"));

            JsonArray stars = getFirstThreeStars(movieId, conn);
            JsonArray genres = getFirstGenres(movieId, conn);

            jObj.add("stars", stars);
            jObj.add("genres", genres);
            rv.add(jObj);
        }
        return rv;
    }

    static protected  JsonArray getFirstThreeStars(String movieId, Connection conn) throws SQLException {
        JsonArray rv = new JsonArray();

        String query = "SELECT stars.name as name, stars.id as id " +
                "FROM stars_in_movies AS sim " +
                "INNER JOIN stars ON stars.id = sim.starId " +
                "WHERE sim.movieId = ?" +
                "LIMIT 3";
        PreparedStatement statement = conn.prepareStatement(query);
        statement.setString(1, movieId);
        ResultSet rs = statement.executeQuery();

        while(rs.next()) {
            JsonObject jObj = new JsonObject();
            jObj.addProperty("id", rs.getString("id"));
            jObj.addProperty("name", rs.getString("name"));
            rv.add(jObj);
        }
        rs.close();
        statement.close();
        return rv;
    }
    static protected JsonArray getFirstGenres(String movieId, Connection conn) throws SQLException {
        JsonArray rv = new JsonArray();

        String query = "SELECT genres.id AS id, genres.name AS name " +
                "FROM genres_in_movies AS gim " +
                "INNER JOIN genres ON genres.id = gim.genreId " +
                "WHERE gim.movieId = ? " +
                "LIMIT 3";
        PreparedStatement statement = conn.prepareStatement(query);
        statement.setString(1, movieId);
        ResultSet rs = statement.executeQuery();

        while(rs.next()) {
            JsonObject jObj = new JsonObject();
            jObj.addProperty("id", rs.getString("id"));
            jObj.addProperty("name", rs.getString("name"));
            rv.add(jObj);
        }

        rs.close();
        statement.close();
        return rv;
    }
}
