
import com.google.gson.JsonObject;


import io.jsonwebtoken.Claims;
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
import java.util.HashMap;
import java.sql.Date;


@WebServlet(name = "CartPaymentServlet", urlPatterns = "/api/cart-payment")
public class CartPaymentServlet extends HttpServlet {
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
    private DataSource dataSource;
    public void init(ServletConfig config) {
        try {

            System.out.println("PaymentServlet initialized!");
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb-read-write");

        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        HttpSession session = request.getSession();
        //LOG_IN_USER_ID.set(session, 490007);
        double totalPrice = 0;
        JsonObject rv = new JsonObject();
        HashMap<String, Item> cartItemMap = CART_ITEM_MAP.get(session);
        if (cartItemMap == null) {
            cartItemMap = new HashMap<>();
            CART_ITEM_MAP.set(session, cartItemMap);
        }
        // write all the data into the jsonObject;
        synchronized (cartItemMap){
            ArrayList<Item> list = new ArrayList<>(cartItemMap.values());
            totalPrice = getTotalPrice(list);
        }
        rv.addProperty("totalPrice", totalPrice);
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

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String cardNumber = request.getParameter("cardNumber");
        String expirationDate = request.getParameter("expiryDate");

        //project 5 task 4
        // accessCount: This state only exists in star service
        // We use sticky session to ensure star service always use the same pod for a client.
        HttpSession session = request.getSession(true);
        Integer accessCount = (Integer) session.getAttribute("accessCount");

        if (accessCount == null) {
            // Which means the user is never seen before
            accessCount = 0;
        } else {
            accessCount++;
        }

        session.setAttribute("accessCount", accessCount);

        // loginCount: This state is shared between login and star services
        // Login service stores loginCount in JWT, and star service retrieves the state from JWT.
        // We can get the claims from request (attached in loginFilter)
        Claims claims = (Claims) request.getAttribute("claims");
        String loginTime = claims.get("loginTime", String.class); // Login time is set in login servlet
        System.out.println("SingleStarServlet: user " + claims.getSubject() + ", login time: " + loginTime + ", accessCount: " + accessCount);
        //end

        response.setContentType("application/json");
        JsonObject responseJsonObject = new JsonObject();
        PrintWriter out = response.getWriter();

        try(Connection conn = dataSource.getConnection()){
            boolean validCreditCardInfo = isCardValid(firstName, lastName, cardNumber, expirationDate, conn);
            responseJsonObject.addProperty("validCreditCardInfo", validCreditCardInfo);
            if(validCreditCardInfo){
                String email = claims.get("user", String.class);

                PreparedStatement ppst = conn.prepareStatement("SELECT customers.id as userId " +
                        "FROM customers " +
                        "WHERE customers.email = ? ;");
                ppst.setString(1, email);
                ResultSet rs = ppst.executeQuery();
                rs.next();

                Integer uid = rs.getInt("userId");

                rs.close();
                ppst.close();

                makeSaleRecord(conn, session, uid);
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

    protected void makeSaleRecord(Connection conn, HttpSession session, Integer userId) throws SQLException {
        HashMap<String, Item> cartItemMap = CART_ITEM_MAP.get(session);

        if (cartItemMap == null) {
            throw new IllegalStateException("cartItemMap session attribute is null during checkout/payment");
        }
        synchronized (cartItemMap) {
            ArrayList<Item> cartItemList = new ArrayList<>(CART_ITEM_MAP.get(session).values());
            if(cartItemList.isEmpty()){
                throw new IllegalStateException("No items in cart during checkout/payment");
            }
            HashMap<String, SaleItem> lastSaleItemMap = new HashMap<>();
            for (Item cur : cartItemList) {
                insertItemsSaleRecord(cur, userId,lastSaleItemMap, conn);
            }

            LAST_SALE_CART_ITEM_MAP.set(session,lastSaleItemMap);
            CART_ITEM_MAP.set(session, new HashMap<>());
        }
    }
    protected void insertItemsSaleRecord(Item item, Integer customerId,HashMap<String, SaleItem> saleCartItemMap, Connection conn) {
        try{
            String query = "INSERT INTO sales (customerId, movieId, saleDate) VALUES (?, ?,NOW())";
            String saleQuantityQuery = "INSERT INTO salesQuantity (saleId,quantity) VALUES (?, ?)";
            PreparedStatement statement = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
            PreparedStatement saleQuantityStatement = conn.prepareStatement(saleQuantityQuery);
            System.out.println("prepared statement created");
            if(item.getQuantity() <= 0){
                throw new IllegalArgumentException("Shop cart item quantity must be greater than 0");
            }
//            HashMap<String, Item> lastSaleItemMap = new HashMap<>();
            System.out.println("setting prepared statement");
            statement.setInt(1, customerId);
            statement.setString(2, item.getId());
            System.out.println("prepared statement set");
            statement.executeUpdate();
            System.out.println("prepared statement executed");
            ResultSet rs = statement.getGeneratedKeys();



            if(rs.next()){
                saleCartItemMap.put(item.getId(), new SaleItem(item, rs.getInt(1)));
            }
            saleQuantityStatement.setInt(1, rs.getInt(1));
            saleQuantityStatement.setInt(2, item.getQuantity());
            System.out.println("prepared saleQuantityStatement statement");
            saleQuantityStatement.executeUpdate();
            System.out.println("saleQuantityStatement executed");


            rs.close();
            statement.close();
        }
        catch(Exception e){
            String msg = "Error while inserting sales record" + e.getMessage();
            throw new RuntimeException(msg, e);
        }

    }
    protected double getTotalPrice(ArrayList<Item> itemList){
        double totalPrice = 0;
        for(Item cur : itemList){
            totalPrice += cur.getTotalPrice();
        }
        return totalPrice;
    }

    protected boolean isCardValid(String firstName, String lastName, String cardNumber, String expirationDate, Connection conn) throws SQLException {
        String query = "SELECT id AS cardNum, firstName, lastName, expiration " +
                "FROM creditcards " +
                "WHERE id = ?";
        PreparedStatement statement = conn.prepareStatement(query);
        statement.setString(1, cardNumber);
        ResultSet rs = statement.executeQuery();
        if (!rs.next()) {
            rs.close();
            statement.close();
            return false;
        }
        String firstNameFromDB = rs.getString("firstName");
        String lastNameFromDB = rs.getString("lastName");
        Date expirationFromDB = rs.getDate("expiration");
        rs.close();
        statement.close();
        return firstName.equals(firstNameFromDB) && lastName.equals(lastNameFromDB) && expirationDate.equals(expirationFromDB.toString());
    }


}
