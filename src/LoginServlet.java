
import JwtInfo.JwtUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jasypt.util.password.StrongPasswordEncryptor;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.text.DateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
    // Create a dataSource which registered in web.
    private DataSource dataSource;
    private static final SessionAttribute<Integer> LOG_IN_USER_ID =
            new SessionAttribute<>(
                    Integer.class,
                    "userId"
            );
    //setup datasource
    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        /* This example only allows username/password to be test/test
        /  in the real project, you should talk to the database to verify username/password
        */
        JsonObject responseJsonObject = new JsonObject();

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();


        try (Connection conn = dataSource.getConnection()) {
            //all statements concerned with login info
            if (!userExists(username, conn)) {
                request.getServletContext().log("Login failed");
                responseJsonObject.addProperty("message", "user " + username + " doesn't exist");
            } else if (!passwordMatches(request, username, password, conn)) {
                request.getServletContext().log("Login failed");
                responseJsonObject.addProperty("message", "incorrect password");
            } else {
                //added for proj 5 task 4
                String subject = username;
                Map<String, Object> claims = new HashMap<>();

                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                claims.put("loginTime", dateFormat.format(new Date()));
                claims.put("user", username);

                // Generate new JWT and add it to Header
                String token = JwtUtil.generateToken(subject, claims);
                JwtUtil.updateJwtCookie(request, response, token);

                responseJsonObject.addProperty("status", "success");
                responseJsonObject.addProperty("message", "success");
            }

            conn.close();
            out.write(responseJsonObject.toString());
            response.setStatus(200);
        } catch (Exception e) {

            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("user", username);
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }
    }

    private boolean userExists(String username, Connection conn) throws SQLException {
        String query = "SELECT EXISTS(SELECT customers.id FROM customers WHERE customers.email = ? ) as customer_exists";

        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setString(1, username);

        ResultSet rs = pstmt.executeQuery();

        //go one more from start
        rs.next();
        boolean res = rs.getBoolean("customer_exists");

        pstmt.close();
        rs.close();

        return res;
    }

    private boolean passwordMatches(HttpServletRequest request, String username,
                                    String password, Connection conn) throws SQLException {
        PreparedStatement ppst = conn.prepareStatement("SELECT customers.password as password, customers.id as userId " +
                                                "FROM customers " +
                                                "WHERE customers.email = ? ;");
        ppst.setString(1, username);

        ResultSet rs = ppst.executeQuery();
        rs.next();
        String dbPassword = rs.getString("password");
        //Integer uid = rs.getInt("userId");

        ppst.close();
        rs.close();

        return verifyPassword(password, dbPassword);

        //if (!verifyPassword(password, dbPassword)) {
        //    return false;
        //}

        //write output and modify session
        //changed for project 5, change the jwt info

        //LOG_IN_USER_ID.set(request.getSession(), uid);
        //LOG_IN_USER_ID.set(request.getSession(), uid);
        //request.getSession().setAttribute("user", new User(username));
        //return true;
    }
    protected boolean verifyPassword(String inputPassword, String encryptedDatabasePassword){
        return new StrongPasswordEncryptor().checkPassword(inputPassword, encryptedDatabasePassword);
    }
}
