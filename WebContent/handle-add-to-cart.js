let addToCartBtn = $(".add-to-cart-btn");
addToCartBtn.on("click", handleAddToCart);
function handleAddToCart(event){
    console.log("handle Add To Cart");
    //const itemId = $(this).data("id");
    const button = $(event.currentTarget);
    const itemId = button.data('id');

    $.ajax({
        url: "api/AddToCart", // The servlet URL
        method: "POST",
        data: { id: itemId }, // Sending item ID to the servlet
        success: function(response){
            alert("Item added to cart");
        },
        error: function(){
            alert("Error adding item to cart");
        }
    });
}