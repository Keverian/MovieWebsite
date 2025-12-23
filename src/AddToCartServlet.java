import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.google.protobuf.TextFormat;
import com.mysql.cj.x.protobuf.MysqlxPrepare;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.NoSuchElementException;
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
import java.util.HashMap;
@WebServlet(name = "AddToCartServlet", urlPatterns = "/api/AddToCart")
public class AddToCartServlet extends HttpServlet {
    private static final SessionAttribute<HashMap<String, Item>> CART_ITEM_MAP =
            new SessionAttribute<>(
                    (Class<HashMap<String, Item>>)(Class<?>) HashMap.class,
                    "cartItemMap"
            );
    private DataSource dataSource;
    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String itemId = request.getParameter("id");
        //String action = request.getParameter("action");
        request.getServletContext().log("getting item id: " + itemId);
        HttpSession session = request.getSession();
        response.setContentType("application/json");
        JsonObject responseJsonObject = new JsonObject();
        HashMap<String, Item> cartItemMap = CART_ITEM_MAP.get(session);
        PrintWriter out = response.getWriter();

        try(Connection conn = dataSource.getConnection()){
            if (cartItemMap == null) {
                cartItemMap = new HashMap<>();
                CART_ITEM_MAP.set(session, cartItemMap);
            }
            synchronized (cartItemMap) {
                if(cartItemMap.containsKey(itemId)){
                    cartItemMap.get(itemId).incrementQuantity(1);
                    responseJsonObject.addProperty("message", "Item quantity updated");
                }
                else{
                    double price = getItemPrice(itemId, conn);
                    String title = getItemTitle(itemId, conn);
                    cartItemMap.put(itemId, new Item(itemId, title, price));
                    responseJsonObject.addProperty("message", "New Item Added to Cart");
                }
                int itemQuantity = cartItemMap.get(itemId).getQuantity();
                double totalPrice = cartItemMap.get(itemId).getTotalPrice();
                responseJsonObject.addProperty("success", true);
                responseJsonObject.addProperty("id", itemId);
                responseJsonObject.addProperty("quantity", itemQuantity);
                responseJsonObject.addProperty("totalPrice", totalPrice);
            }

            out.write(responseJsonObject.toString());

        }catch(Exception e){
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            //jsonObject.addProperty("info query call", getTopMovieQuery);
            //jsonObject.addProperty("size query call", getMovieQueryCount);
            out.write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        }
        finally {
            out.close();
        }

    }
    protected double getItemPrice(String id, Connection conn) throws SQLException {
        String query = "SELECT price " +
                "FROM movies INNER JOIN movie_prices " +
                "ON movies.id = movie_prices.movieId " +
                "WHERE id = ?";
        PreparedStatement statement = conn.prepareStatement(query);
        statement.setString(1, id);
        ResultSet rs = statement.executeQuery();

        if (!rs.next()) {
            rs.close();
            statement.close();
            throw new NoSuchElementException("No price found for movie ID: " + id);
        }
        double price = rs.getFloat("price");
        rs.close();
        statement.close();
        return price;
    }
    protected String getItemTitle(String id, Connection conn) throws SQLException {
        String query = "SELECT title " +
                "FROM movies INNER JOIN movie_prices " +
                "ON movies.id = movie_prices.movieId " +
                "WHERE id = ?";
        PreparedStatement statement = conn.prepareStatement(query);
        statement.setString(1, id);
        ResultSet rs = statement.executeQuery();
        if (!rs.next()) {
            rs.close();
            statement.close();
            throw new NoSuchElementException("No title found for movie ID: " + id);
        }
        String title = rs.getString("title");
        rs.close();
        statement.close();
        return title;
    }



}
