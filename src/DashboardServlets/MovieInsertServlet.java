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
import java.util.ArrayList;

@WebServlet(name = "DashboardServlets.MovieInsertServlet", urlPatterns = "/_dashboard/api/addmovie")
public class MovieInsertServlet extends HttpServlet{
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
            //keeps track of the query values
            ArrayList<Object> movieInfo = new ArrayList<Object>();
            movieInfo.add(request.getParameter("MovieTitle"));
            movieInfo.add(Integer.parseInt(request.getParameter("MovieRelease")));
            movieInfo.add(request.getParameter("MovieDirector"));
            movieInfo.add(request.getParameter("MovieGenre"));

            String MovieStar = request.getParameter("MovieStar");
            String StarBirthYear = request.getParameter("StarBirth");
            int birthYear = -1;

            if (StarBirthYear != null && !StarBirthYear.isEmpty()) {
                birthYear = Integer.parseInt(StarBirthYear);
            }

            //creating call statement
            int i = 1;
            CallableStatement statement = conn.prepareCall("{call add_movie(?, ?, ?, ?, ?, ?, ?, ?, ?)}");

            for (Object movie : movieInfo) {
                if (movie == null && (!(movie instanceof String) || !((String) movie).isEmpty())) {
                    throw new IllegalArgumentException("Information directly regarding movies can not be empty!");
                }

                if (movie instanceof Integer && ((Integer) movie) < 1)  {
                    throw new IllegalArgumentException("Movie release value can not be less than 1!");
                }

                if (movie instanceof Integer) {
                    statement.setInt(i, (Integer) movie);
                } else {
                    statement.setString(i, (String) movie);
                }
                ++i;
            }

            statement.setString(i, MovieStar);
            ++i;

            if (StarBirthYear == null || StarBirthYear.isEmpty()) {
                statement.setNull(i, Types.NULL);
            } else if (birthYear < 1){
                throw new IllegalArgumentException("Star birth year value can not be less than 1!");
            } else {
                statement.setInt(i, birthYear);
            }
            ++i;

            //setting up output
            statement.registerOutParameter(7, Types.VARCHAR);
            statement.registerOutParameter(8, Types.INTEGER);
            statement.registerOutParameter(9, Types.VARCHAR);

            statement.execute();

            String newMovieId = statement.getString(7);
            int newGenreId = statement.getInt(8);
            String newStarId = statement.getString(9);

            //closing necessary statements
            conn.close();
            statement.close();

            //get outputs
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("NewMovieId", newMovieId);
            jsonObject.addProperty("NewGenreId", newGenreId);
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
