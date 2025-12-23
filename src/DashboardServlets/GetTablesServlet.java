package DashboardServlets;

import com.google.gson.JsonArray;
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
import java.sql.*;
import java.util.ArrayList;

@WebServlet(name = "DashboardServlets.GetTableServlet", urlPatterns = "/_dashboard/api/tables")
public class GetTablesServlet extends HttpServlet {
    // Create a dataSource which registered in web.
    private DataSource dataSource;
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // for output
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()){
            //keeps track of the query values
            JsonObject jobj = new JsonObject();

            JsonArray jarr = getTables(conn);
            jobj.add("tables", jarr);

            conn.close();
            out.write(jobj.toString());
            response.setStatus(200);

            //set error responses to 200, employees want to see the errors
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

    private JsonArray getTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SHOW TABLES;");
        JsonArray jsonArray = new JsonArray();

        while (rs.next()) {
            JsonObject jobj = new JsonObject();
            String tableName = rs.getString(1);
            jobj.addProperty("TableName", tableName);
            JsonArray cols = getColumns(conn, tableName);
            jobj.add("Columns", cols);

            jsonArray.add(jobj);
        }
        stmt.close();

        return jsonArray;
    }

    private JsonArray getColumns(Connection conn, String tableName) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM " + tableName);

        JsonArray res = new JsonArray();

        while (rs.next()) {
            JsonObject jobj = new JsonObject();
            jobj.addProperty("Field", rs.getString(1));
            jobj.addProperty("Type", rs.getString(2));
            res.add(jobj);
        }
        stmt.close();

        return res;
    }
}
