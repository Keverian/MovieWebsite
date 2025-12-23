import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Declaring a WebServlet called SingleMovieServlet, which maps to url "/api/single-movie"
@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
    private static final long serialVersionUID = 4L;

    // Create a dataSource which registered in web.xml
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     * response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json"); // Response mime type

        // Retrieve parameter id from url request.
        String id = request.getParameter("id");

        // The log message can be found in localhost log
        request.getServletContext().log("getting id: " + id);

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            // Get a connection from dataSource

            // Construct a query with parameter represented by "?"
            String query = "SELECT movies.id " +
                    ", movies.title " +
                    ", movies.year " +
                    ", movies.director " +
                    ", ratings.rating " +
                    "FROM movies " +
                    "LEFT JOIN ratings ON movies.id = ratings.movieId " +
                    "WHERE movies.id = ?";

            // Declare our statement
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, id);
            ResultSet rs = statement.executeQuery();
            JsonArray rv = new JsonArray();
            if(!rs.isBeforeFirst()){
                request.getServletContext().log("ResultSet is empty!");
            }
            rv.add(getMovieInfo(rs, conn));

            rs.close();
            statement.close();

            // Write JSON string to output
            out.write(rv.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);

        } catch (Exception e) {
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error:", e);
            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }

        // Always remember to close db connection after usage. Here it's done by try-with-resources

    }

    protected JsonObject getMovieInfo(ResultSet rs, Connection conn) throws SQLException {
        JsonObject curObj = new JsonObject();
        while (rs.next()) {


            String movieId = rs.getString("movies.id");
            String title = rs.getString("movies.title");
            String director = rs.getString("movies.director");
            float rating = rs.getFloat("ratings.rating");
            int year = rs.getInt("movies.year");


            JsonArray stars = getAllMovieStars(movieId, conn);
            JsonArray genres = getAllGenres(movieId, conn);

            // Create a JsonObject based on the data we retrieve from rs

            curObj.addProperty("movieId", movieId);
            curObj.addProperty("title", title);
            curObj.addProperty("rating", rating);
            curObj.addProperty("year", year);
            curObj.addProperty("director", director);
            curObj.add("stars", stars);
            curObj.add("genres", genres);
        }
        return curObj;
    }
    protected  JsonArray getAllMovieStars(String movieId, Connection conn) throws SQLException {
        JsonArray rv = new JsonArray();

        String query = "SELECT stars.id, stars.name " +
                "FROM stars JOIN stars_in_movies ON stars.id = stars_in_movies.starId " +
                "WHERE stars_in_movies.movieId = ? " +
                "ORDER BY " +
                "(SELECT COUNT(*) " +
                "FROM stars_in_movies AS sim2 " +
                "WHERE sim2.starId = stars.id) DESC";





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

    protected  JsonArray getAllGenres(String movieId, Connection conn) throws SQLException {
        JsonArray rv = new JsonArray();

        String query = "SELECT genres.id AS id, genres.name AS genreName " +
                "FROM genres_in_movies AS gim " +
                "INNER JOIN genres ON genres.id = gim.genreId " +
                "WHERE gim.movieId = ? "
                + "ORDER BY genres.name";
        PreparedStatement statement = conn.prepareStatement(query);
        statement.setString(1, movieId);
        ResultSet rs = statement.executeQuery();

        while(rs.next()) {
            JsonObject jObj = new JsonObject();
            jObj.addProperty("id", rs.getString("id"));
            jObj.addProperty("name", rs.getString("genreName"));
            rv.add(jObj);
        }

        rs.close();
        statement.close();
        return rv;
    }

}
