window.addEventListener("pageshow", function (event) {
    // Always fetch updated cart when the user views this page
    if (window.location.pathname === '/shopping-cart.html') {
        getInfo()
    }
});
$(document).on("click", ".quantity-btn", handleQuantityChange);


function handleShoppingCartResult(resultData) {
    console.log("handleShoppingCartResult: populating movie table from resultData");
    //insert title and year

    // Find the empty table body by id "movie_table_body"
    let movieTableBodyElement = jQuery("#cart_table_body");
    let returnHtml = "";
    for (let i = 0; i < resultData.length; i++) {
        console.log(resultData[i]);
        let item = resultData[i];
        returnHtml += jsonItemToRow(item);
    }
    movieTableBodyElement.append(returnHtml);

    console.log("finish populating table");
}

function jsonItemToRow(item){
    let rowHTML = "";
    rowHTML += `<tr id="${item['id']}" data-id="${item['id']}">`;
    rowHTML +=
        '<th class="title-cell">' +
        // Add a link to single-movie.html with id passed with GET url parameter
        '<a href="single-movie.html?id=' + item['id'] + '">'
        + item["title"] +     // display star_name for the link text
        '</a>' +
        "</th>";

    rowHTML +=
        '<td class="quantity-cell">'
        + `<button type="button" class="quantity-btn" data-action="decrease" data-id="${item['id']}">-</button>`
        +  `<span class="quantity-display">${item['quantity']}</span>`
        +  `<button type="button" class="quantity-btn" data-action="increase" data-id="${item['id']}">+</button>`
        + '</td>';

    rowHTML += '<td>'
        + `<button type="button" class="quantity-btn" data-action="delete" data-id="${item['id']}">DELETE</button>`
        + '</td>';

    rowHTML += '<td>'
        + `<span class="single-price-cell">${item['price']}</span>`
        + '</td>';

    rowHTML += '<td>'
        + `<span class="total-price-cell">${item['totalPrice']}</span>`
        + '</td>';

    rowHTML +='</tr>';

    return rowHTML;
}

function getInfo(){
    jQuery.ajax({
        dataType: "json", // Setting return data type
        method: "GET", // Setting request method
        url: "api/shopping-cart", // Setting request url, which is mapped by StarsServlet in Stars.java
        success: (resultData) => handleShoppingCartResult(resultData) // Setting callback function to handle data returned successfully by the StarsServlet
    });

}
$(document).ready(function() {
    // Your JavaScript code here
    console.log("HTML is fully loaded and parsed!");
    getInfo()
});


function handleQuantityChange(event){
    const button = $(event.target);
    const itemId = button.data('id');
    const action = button.data('action');

    jQuery.ajax({
        dataType: "json",  // Setting return data type
        method: 'POST',// Setting request method
        url: "api/shopping-cart", // Setting request url, which is mapped by StarsServlet in Stars.java
        data: {
            id: itemId,
            action: action
        },
        success: updateQuantity
        // Setting callback function to handle data returned successfully by the SingleStarServlet
    });
}

function updateQuantity(response) {
    console.log("updatequantity running");
    if(!response.success){
        alert('Error: ' + response.message);
        console.log(response);
        return;
    }
    if(response['quantity'] === 0){
        removeRow(response['id']);
        return;
    }

    updateRowQuantity(response);
}

function updateRowQuantity(response){
    let newQuantity = response['quantity'];
    let itemId = response['id'];
    let row = $(`#${itemId}`);
    let totalPrice = response['totalPrice'];
    let quantityDisplay = row.find('.quantity-display');
    let totalPriceDisplay = row.find('.total-price-cell');
    console.log("row id:", itemId);
    console.log("row found:", row.length);
    console.log("quantityDisplay found:", quantityDisplay.length);
    quantityDisplay.text(newQuantity);
    totalPriceDisplay.text(totalPrice);
}

function removeRow(rowId){
    let row = $(`#${rowId}`);
    row.remove();
}
$(document).on("click", "#checkout-btn", getCheckoutValidation);
function getCheckoutValidation(){
    jQuery.ajax({
        dataType: "json", // Setting return data type
        method: "GET", // Setting request method
        url: "api/checkout-validation", // Setting request url, which is mapped by StarsServlet in Stars.java
        success: (resultData) => ProcessCheckOutValidation(resultData) // Setting callback function to handle data returned successfully by the StarsServlet
    });
}
function ProcessCheckOutValidation(resultData){
    if(resultData["validCheckout"]){
        window.location.href = 'payment.html';
    }
    else{
        alert('Error: invalid checkout');
        window.location.reload(true);
    }

}
function checkPaymentApi(resultData){
    alert("payment url called");
}

//TODO: add place order button only when theres at least 1 item in shopping cart session attribute
