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

// Declaring a WebServlet called SingleStarServlet, which maps to url "/api/single-star"
@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/single-star")
public class SingleStarServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;

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
            String query = "SELECT * from stars where stars.id = ?";

            // Declare our statement
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, id);
            ResultSet rs = statement.executeQuery();
            JsonArray rv = new JsonArray();
            rv.add(getStarInfo(rs, conn));

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

    protected JsonObject getStarInfo(ResultSet rs, Connection conn) throws SQLException {
        JsonObject curObj = new JsonObject();
        while (rs.next()) {
            String starId = rs.getString("id");
            String starName = rs.getString("name");
            String starDob = rs.getString("birthYear");

            if (rs.wasNull()) {
                starDob = "N/A";
            }

            JsonArray movies = getAllStarsMovie(starId, conn);

            // Create a JsonObject based on the data we retrieve from rs

            curObj.addProperty("star_id", starId);
            curObj.addProperty("star_name", starName);
            curObj.addProperty("star_dob", starDob);
            curObj.add("movies", movies);
        }
        return curObj;
    }

    protected JsonArray getAllStarsMovie(String starId, Connection conn) throws SQLException {
        JsonArray rv = new JsonArray();
        String query = "SELECT movies.id AS id, movies.title AS title, movies.director AS director, movies.year AS year " +
                "FROM stars_in_movies AS sim " +
                "INNER JOIN movies ON movies.id = sim.movieId " +
                "WHERE sim.starId = ? " +
                "ORDER BY movies.year DESC, movies.title ASC";
        PreparedStatement statement = conn.prepareStatement(query);
        statement.setString(1, starId);
        ResultSet rs = statement.executeQuery();

        while (rs.next()) {
            JsonObject curObj = new JsonObject();
            curObj.addProperty("id", rs.getString("id"));
            curObj.addProperty("title", rs.getString("title"));
            curObj.addProperty("director", rs.getString("director"));
            curObj.addProperty("year", rs.getInt("year"));
            rv.add(curObj);
        }
        rs.close();
        statement.close();
        return rv;
    }

}
