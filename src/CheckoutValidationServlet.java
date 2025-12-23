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
import java.time.LocalDate;

@WebServlet(name = "CheckoutValidationServlet", urlPatterns = "/api/checkout-validation")
public class CheckoutValidationServlet extends HttpServlet {
    private static final SessionAttribute<HashMap<String, Item>> CART_ITEM_MAP =
            new SessionAttribute<>(
                    (Class<HashMap<String, Item>>)(Class<?>) HashMap.class,
                    "cartItemMap"
            );
    private static final SessionAttribute<HashMap<String, SaleItem>> LAST_SALE_CART_ITEM_MAP =
            new SessionAttribute<>(
                    (Class<HashMap<String, SaleItem>>)(Class<?>) HashMap.class,
                    "lastSaleCartItemMap"
            );
    private static final SessionAttribute<Integer> LOG_IN_USER_ID =
            new SessionAttribute<>(
                    Integer.class,
                    "userId"
            );
//    private DataSource dataSource;
//    public void init(ServletConfig config) {
//        try {
//            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
//
//        } catch (NamingException e) {
//            e.printStackTrace();
//        }
//    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        HttpSession session = request.getSession();
        JsonObject rv = new JsonObject();
        rv.addProperty("validCheckout",false);
        HashMap<String, Item> cartItemMap = CART_ITEM_MAP.get(session);
        if (cartItemMap == null) {
            cartItemMap = new HashMap<>();
            CART_ITEM_MAP.set(session, cartItemMap);
        }
        // write all the data into the jsonObject;
        synchronized (cartItemMap){
            ArrayList<Item> list = new ArrayList<>(cartItemMap.values());
            if(!list.isEmpty()){
                rv.addProperty("validCheckout",true);
            }
        }
        PrintWriter out = response.getWriter();

        try{
            out.write(rv.toString());
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


}
