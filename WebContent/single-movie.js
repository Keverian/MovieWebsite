
function handleSingleMovieResult(resultData) {
    let curObj = resultData[0];
    console.log("handleStarResult: populating movie table from resultData");
    //insert title and year

    let titleHeader = jQuery("#movie_title");
    titleHeader.append(curObj['title']);
    //insert year
    let yearHeader = jQuery("#year");
    yearHeader.append(curObj['year']);

    // Find the empty table body by id "movie_table_body"
    let movieTableBodyElement = jQuery("#movie_table_body");




    movieTableBodyElement.append(getRowHtml(curObj));
}
function getRowHtml(curObj){
    let rowHTML = "";

    rowHTML += "<tr>";

    rowHTML +=
        '<td>' +
        '<a>' + curObj['director'] + '</a>'
        +'</td>';

    rowHTML += PrintGenresArray(curObj['genres']);

    rowHTML += PrintStars(curObj['stars']);

    rowHTML +=
        '<td>' +
        '<a>' + curObj['rating'] + '</a>'
        +'</td>';

    rowHTML += `<td><button class="add-to-cart-btn" data-id="${curObj['movieId']}">Add To Cart</button></td>`;
    rowHTML += "</tr>";
    return rowHTML;

}
function PrintGenresArray(jArr){

    let tdHTML = "<td>";
    for (let i = 0; i < jArr.length; i++){
        let curObj = jArr[i];
        let curHTML = "";
        curHTML += '<a>' + curObj['name'];
        curHTML = `<a href="movie-page.html?Genre=${curObj['name']}">${curObj['name']}</a>`
        if(i !== jArr.length - 1){
            curHTML +="<a>, </a>";
        }
        tdHTML += curHTML;
    }
    tdHTML += "</td>";
    return tdHTML;
}

function PrintStars(starsArr){
    let tdHTML = "<td>";
    for (let i = 0; i < starsArr.length; i++){
        let curObj = starsArr[i];
        let curHTML = "";
        curHTML += '<a href="single-star.html?id=' + curObj['id'] + '">' + curObj['name'];
        if(i !== starsArr.length - 1){
            curHTML +=", ";
        }
        curHTML += "</a>";
        tdHTML += curHTML;
    }
    tdHTML += "</td>";
    return tdHTML;
}
// let addToCartBtn = $(".add-to-cart-btn");
// addToCartBtn.on("click", handleAddToCart);
$(document).on("click", ".add-to-cart-btn", handleAddToCart);
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
/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */

//test attribute
console.log("handling search parameters");
const urlParams = new URLSearchParams(window.location.search);
const stringParam = urlParams.toString();

console.log(stringParam);

// Makes the HTTP GET request and registers on success callback function handleStarResult
jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    data: stringParam, //data param
    url: "api/single-movie", // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => handleSingleMovieResult(resultData) // Setting callback function to handle data returned successfully by the StarsServlet
});