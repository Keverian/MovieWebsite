import com.google.gson.JsonObject;



public class Item {
    private String title;
    private double price;
    private String id;
    private int quantity;
    private double totalPrice;

    public Item(String id, String title, double price) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.quantity = 1;
        this.totalPrice = 1 * price;
    }
    public Item(Item copyFrom){
        this.id = copyFrom.id;
        this.title = copyFrom.title;
        this.price = copyFrom.price;
        this.quantity = copyFrom.quantity;
        this.totalPrice = copyFrom.totalPrice;
    }

    public String getTitle() { return title; }
    public String getId() { return id; }
    public double getPrice(){ return price; }
    public JsonObject getJsonObj(){
        JsonObject rv = new JsonObject();
        rv.addProperty("id", id);
        rv.addProperty("title", title);
        rv.addProperty("price", price);
        rv.addProperty("quantity", quantity);
        rv.addProperty("totalPrice", totalPrice);
        return rv;
    }
    public void incrementQuantity(int changeQuantity){
        this.quantity+= changeQuantity;
        this.totalPrice = price * quantity;
    }
    public void decrementQuantity(int changeQuantity){
        this.quantity-= changeQuantity;
        if(quantity < 0) quantity = 0;
        this.totalPrice = price * quantity;
    }

    public double getTotalPrice(){ return totalPrice;}
    public int getQuantity(){ return quantity;}
    public void setQuantity(int quantity){ this.quantity = quantity;}
    public void setPrice(double price){
        this.price = price;
        this.totalPrice = price * quantity;
    }

}
