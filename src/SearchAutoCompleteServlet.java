import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

@WebServlet(name = "SearchAutoCompleteServlet", urlPatterns = "/api/search-autocomplete")
public class SearchAutoCompleteServlet extends HttpServlet {
    final static String fullTextSearchQuery = "SELECT title, id FROM movies WHERE MATCH (title) AGAINST (? IN BOOLEAN MODE) LIMIT 10";
    private DataSource dataSource;
    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");

        } catch (NamingException e) {
            e.printStackTrace();
        }
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        try(Connection conn = dataSource.getConnection()){
            PreparedStatement statement = conn.prepareStatement(fullTextSearchQuery);
            String searchTerm = tokenizeTitleReformatToFullTextSearch(request.getParameter("search_value"));
            statement.setString(1, searchTerm);
            ResultSet rs = statement.executeQuery();
            JsonArray returnJsonArray = getResultArray(rs);
            out.write(returnJsonArray.toString());
            statement.close();
            rs.close();
        } catch (java.lang.Exception e) {
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error:", e);
            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        }
        finally {
            out.close();
        }

    }
    protected JsonArray getResultArray(ResultSet rs) throws SQLException {
        JsonArray returnJsonArray = new JsonArray();
        while(rs.next()){
            JsonObject curObject = new JsonObject();
            curObject.addProperty("value", rs.getString("title"));
            curObject.addProperty("data", rs.getString("id"));
            returnJsonArray.add(curObject);
        }
        return returnJsonArray;
    }
    private String tokenizeTitleReformatToFullTextSearch(String title) {
        String[] tokens = title.trim().split("\\s+");
        return Arrays.stream(tokens)
                .map(token -> "+" + token + "*")
                .collect(Collectors.joining(" "));
    }
}
