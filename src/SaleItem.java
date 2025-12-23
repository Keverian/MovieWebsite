import com.google.gson.JsonObject;

public class SaleItem extends Item {
    private final Integer saleId;
    public SaleItem(String id, String title, double price, Integer saleId) {
        super(id, title, price);
        this.saleId = saleId;
    }
    public SaleItem(Item item, Integer saleId) {
        super(item);
        this.saleId = saleId;
    }
    public Integer getSaleId() {
        return saleId;
    }
    public JsonObject getJsonObj(){
        JsonObject rv = new JsonObject();
        rv.addProperty("id", super.getId());
        rv.addProperty("title", super.getTitle());
        rv.addProperty("price", super.getPrice());
        rv.addProperty("quantity", super.getQuantity());
        rv.addProperty("totalPrice", super.getTotalPrice());
        rv.addProperty("saleId", saleId);
        return rv;
    }
}
