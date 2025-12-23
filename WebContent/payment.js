
$(document).on("submit", "#payment-form", handlePaymentSubmit);
let payment_form = $("#payment-form");



function handlePaymentSubmit(event){
    console.log("submit payment form");
    event.preventDefault();
    $.ajax({
        //dataType: "json",  // Setting return data type
        method: 'POST',// Setting request method
        url: "api/cart-payment", // Setting request url, which is mapped by StarsServlet in Stars.java
        data: payment_form.serialize(),
        success: handlePostPaymentSubmission
        // Setting callback function to handle data returned successfully by the SingleStarServlet
    });

}
function handlePostPaymentSubmission(response){
    console.log(" handlePostPaymentSubmission running");
    if (!response["validCreditCardInfo"]) {
        payment_form[0].reset();
        alert('Error: Invalid Credit Card Info');
        return;
    }
    window.location.replace('confirmation-page.html');
}



function displayTotalPrice(resultData){
    let totalPrice = resultData['totalPrice'];
    let totalPriceDisplay = $('#total-price-display');
    totalPriceDisplay.text(totalPrice);
}


$.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    url: "api/cart-payment", // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => displayTotalPrice(resultData),
    error: (xhr, status, error) => {
        console.error("Error occurred: " + error);
        alert("An error occurred while processing your request. Please try again.");
    }
});