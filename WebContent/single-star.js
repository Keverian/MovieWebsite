/**
 * This example is following frontend and backend separation.
 *
 * Before this .js is loaded, the html skeleton is created.
 *
 * This .js performs three steps:
 *      1. Get parameter from request URL so it know which id to look for
 *      2. Use jQuery to talk to backend API to get the json data.
 *      3. Populate the data to correct html elements.
 */


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
 * Retrieve parameter from request URL, matching by parameter name
 * @param target String
 * @returns {*}
 */
function getParameterByName(target) {
    // Get request URL
    let url = window.location.href;
    // Encode target parameter name to url encoding
    target = target.replace(/[\[\]]/g, "\\$&");

    // Ues regular expression to find matched parameter value
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';

    // Return the decoded parameter value
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */

function handleResult(resultData) {

    console.log("handleResult: populating star info from resultData");

    // populate the star info h3
    // find the empty h3 body by id "star_info"
    let starInfoElement = jQuery("#star_info");
    let starObj = resultData[0];
    // append two html <p> created to the h3 body, which will refresh the page
    starInfoElement.append("<p>Star Name: " + starObj["star_name"] + "</p>" +
        "<p>Date Of Birth: " + starObj["star_dob"] + "</p>");

    console.log("handleResult: populating movie table from resultData");

    // Populate the star table
    // Find the empty table body by id "movie_table_body"
    let movieTableBodyElement = jQuery("#movie_table_body");
    let movies = starObj['movies']
    // Concatenate the html tags with resultData jsonObject to create table rows
    for (let i = 0; i < movies.length; i++) {
        let curMovie = movies[i];
        let rowHTML = "";
        rowHTML += "<tr>";
        //curHTML += '<a href="single-star.html?id=' + curObj['id'] + '">' + curObj['name'];
        rowHTML += "<th>" + '<a href="single-movie.html?id=' + curMovie['id'] + '">' + curMovie['title'] + '</a>' + "</th>";
        //curHTML += '<a href="single-star.html?id=' + curObj['id'] + '">' + curObj['name'];
        rowHTML += "<th>" + curMovie["year"] + "</th>";
        rowHTML += "<th>" + curMovie["director"] + "</th>";
        rowHTML += `<td><button type="button" class="add-to-cart-btn" data-id="${curMovie['id']}">Add To Cart</button></td>`;
        rowHTML += "</tr>";

        // Append the row created to the table body, which will refresh the page
        movieTableBodyElement.append(rowHTML);
    }
}

/**
 * Once this .js is loaded, following scripts will be executed by the browser\
 */

// Get id from URL
let starId = getParameterByName('id');

// Makes the HTTP GET request and registers on success callback function handleResult
jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: "api/single-star?id=" + starId, // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => handleResult(resultData) // Setting callback function to handle data returned successfully by the SingleStarServlet
});