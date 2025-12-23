

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * This IndexServlet is declared in the web annotation below,
 * which is mapped to the URL pattern /api/index.
 */
@WebServlet(name = "ShoppingCartServlet", urlPatterns = "/api/shopping-cart")
public class ShoppingCartServlet extends HttpServlet {
    private static final SessionAttribute<HashMap<String, Item>> CART_ITEM_MAP =
            new SessionAttribute<>(
                    (Class<HashMap<String, Item>>)(Class<?>) HashMap.class,
                    "cartItemMap"
            );

    /**
     * handles GET requests to store session information
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        HttpSession session = request.getSession();
        //String sessionId = session.getId();
        //long lastAccessTime = session.getLastAccessedTime();
        
        JsonArray rv = new JsonArray();
        //responseJsonObject.addProperty("sessionID", sessionId);
        //responseJsonObject.addProperty("lastAccessTime", new Date(lastAccessTime).toString());

        HashMap<String, Item> cartItemMap = CART_ITEM_MAP.get(session);
        if (cartItemMap == null) {
            cartItemMap = new HashMap<>();
            CART_ITEM_MAP.set(session, cartItemMap);
        }
        // write all the data into the jsonObject;
        synchronized (cartItemMap){
            ArrayList<Item> list = new ArrayList<>(cartItemMap.values());
            rv = getJsonArrayFromItemList(list);
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

    /**
     * handles POST requests to quantity change in shopping-cart
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String itemId = request.getParameter("id");
        String action = request.getParameter("action");
        HttpSession session = request.getSession();
        JsonObject responseJsonObject = new JsonObject();
        HashMap<String, Item> cartItemMap = CART_ITEM_MAP.get(session);
        PrintWriter out = response.getWriter();

        if (cartItemMap == null) {
            cartItemMap = new HashMap<>();
            CART_ITEM_MAP.set(session, cartItemMap);
        }
        synchronized (cartItemMap) {
            Item item = cartItemMap.get(itemId);
            if(item == null){
                responseJsonObject.addProperty("success", "false");
                responseJsonObject.addProperty("message", "Item does not exist in cartItemMap");
                out.write(responseJsonObject.toString());
                return;
            }
            if(action.equals("increase")){
                item.incrementQuantity(1);
            } else if(action.equals("decrease")){
                item.decrementQuantity(1);
            }
            int itemQuantity = item.getQuantity();
            double totalPrice = item.getTotalPrice();
            if (action.equals("delete") || item.getQuantity() <= 0) {
                cartItemMap.remove(itemId);
                itemQuantity = 0;
                totalPrice = 0;
            }
            responseJsonObject.addProperty("success", true);
            responseJsonObject.addProperty("message", "Item quantity updated");
            responseJsonObject.addProperty("id", itemId);
            responseJsonObject.addProperty("quantity", itemQuantity);
            responseJsonObject.addProperty("totalPrice", totalPrice);
        }

        out.write(responseJsonObject.toString());
        out.close();
    }

    protected JsonArray getJsonArrayFromItemList(ArrayList<Item> itemList){
        JsonArray rv = new JsonArray();
        for(Item cur : itemList){
            rv.add(cur.getJsonObj());
        }
        return rv;
    }
}
