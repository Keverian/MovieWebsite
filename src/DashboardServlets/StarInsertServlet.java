package DashboardServlets;

import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;

@WebServlet(name = "DashboardServlets.StarInsertServlet", urlPatterns = "/_dashboard/api/addstar")
public class StarInsertServlet extends HttpServlet {
    // Create a dataSource which registered in web.
    private DataSource dataSource;
    //setup datasource
    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb-read-write");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // for output
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()){
            String MovieStar = request.getParameter("StarName");
            String StarBirthYear = request.getParameter("StarBirthYear");
            int birthYear = -1;

            if (StarBirthYear != null && !StarBirthYear.isEmpty()) {
                birthYear = Integer.parseInt(StarBirthYear);
            }

            //creating call statement
            CallableStatement statement = conn.prepareCall("{call add_star(?, ?, ?)}");

            statement.setString(1, MovieStar);

            if (StarBirthYear == null || StarBirthYear.isEmpty()) {
                statement.setNull(2, Types.NULL);
            } else if (birthYear < 1){
                throw new IllegalArgumentException("Star birth year value can not be less than 1!");
            } else {
                statement.setInt(2, birthYear);
            }

            //setting up output
            statement.registerOutParameter(3, Types.VARCHAR);

            statement.execute();

            String newStarId = statement.getString(3);

            //closing necessary statements
            conn.close();
            statement.close();

            //get outputs
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("NewStarId", newStarId);

            out.write(jsonObject.toString());
            response.setStatus(200);

            //set error responses to 200, employees want to see the errors
        } catch (NumberFormatException e) {
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("message", "Error on input, a numeric input was not numeric!");
            out.write(jsonObject.toString());

            response.setStatus(200);
        } catch (Exception e) {

            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("message", e.getMessage());
            out.write(jsonObject.toString());

            response.setStatus(200);
        } finally {
            out.close();
        }
    }
}
