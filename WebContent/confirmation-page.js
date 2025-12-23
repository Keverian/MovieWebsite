function handleConfirmationResult(resultData) {
    console.log("handleShoppingCartResult: populating movie table from resultData");
    //insert title and year
    let movieTableBodyElement = jQuery("#sale_table_body");
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
    rowHTML += `<tr id="${item['saleId']}" data-id="${item['saleId']}">`;
    rowHTML +=
        '<th class="sale-id-cell">' +
        // Add a link to single-movie.html with id passed with GET url parameter
        '<a>'
        + item['saleId'] +     // display star_name for the link text
        '</a>' +
        "</th>";
    rowHTML +=
        '<th class="title-cell">' +
        // Add a link to single-movie.html with id passed with GET url parameter
        '<a href="single-movie.html?id=' + item['id'] + '">'
        + item["title"] +     // display star_name for the link text
        '</a>' +
        "</th>";

    rowHTML +=
        '<td class="quantity-cell">'
        +  `<span class="quantity-display">${item['quantity']}</span>`
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

jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    url: "api/confirmation", // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => handleConfirmationResult(resultData) // Setting callback function to handle data returned successfully by the StarsServlet
});