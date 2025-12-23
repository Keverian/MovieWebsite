import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.net.URLDecoder;
import MoviePageSQLQuery.Query;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

@WebServlet(name = "MoviePageServlet", urlPatterns = "/api/movie-page")
public class MoviePageServlet extends HttpServlet{
    private static final long serialVersionUID = 3L;

    // Create a dataSource which registered in web.
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> createAttributeMap(String attribute) {
        Map<String, String> attributeMap = new HashMap<>();

        if (attribute == null || attribute.isEmpty()) {
            return attributeMap;
        }

        String[] attributeList = attribute.split("&");

        for (String attr : attributeList) {
            String[] keyValue = attr.split("=");
            try {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);

                String value = "";
                if (keyValue.length > 1) {
                    value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                }
                attributeMap.put(key, value);
            } catch (Exception e) {
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put("error", e.getMessage());
                return errorMap;
            }
        }

        return attributeMap;
    }
    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //this creates the attribute map, if a query string doesn't exist the back button must have been used
        //thus it will get the last valid query
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isEmpty()) {
            queryString = (String) request.getSession().getAttribute("LastValidMoviePageSearch");
        }

        Map<String, String> attributeMap = createAttributeMap(queryString);

        response.setContentType("application/json"); // Response mime type

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        //checks if there was an error with fetching attributes
        if (attributeMap.get("error") != null) {
            out.write(attributeMap.get("error"));
        }

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()){
            //add size of query and info

            JsonObject jobj = Query.executeSqlStatements(attributeMap, conn);
            conn.close();

            // Log to localhost log
            request.getServletContext().log("getting " + ((JsonArray) jobj.get("movies")).size() + " results");

            //saves query string for later call
            if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
                request.getSession().setAttribute("LastValidMoviePageSearch", request.getQueryString());
            } else {
                //because this was a go back search we want to save what the last search was
                jobj.addProperty("last_search",
                        (String) request.getSession().getAttribute("LastValidMoviePageSearch"));
            }

            // Write JSON string to output
            out.write(jobj.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);

        } catch (Exception e) {

            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}