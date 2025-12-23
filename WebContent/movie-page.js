
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
const urlOriginalParams = new URLSearchParams(window.location.search);
const stringParam = urlOriginalParams.toString();
console.log(stringParam);

function handleMovieResult(resultData) {
    //if this is a return to the back button we also want to keep the query url as is
    let currUrl = window.location.search;
    //console.log(resultData);

    if (resultData["last_search"]) {
        const url = new URL(window.location.href);
        const oldUrl = `${url.pathname}?${resultData["last_search"]}`;
        history.replaceState(null, '', oldUrl);

        currUrl = "?" + resultData["last_search"];
    }

    //test attributes and data
    console.log("handling search parameters");
    const urlParams = new URLSearchParams(currUrl);
    console.log(urlParams.toString());
    //end of test attributes and data
    //global variable for page size
    let pageSize = urlParams.get("PageSize");

    if (!pageSize) {
        pageSize = 25;
    } else {
        pageSize = parseInt(pageSize);
    }
    //end

    //nested functions that take advantage of the url params
    //main base function for parts that don't need a database
    function createSortOptions() {
        //sort by options
        let pageUrl = new URLSearchParams(urlParams);
        pageUrl.delete("CurrPage");

        let preSorted = urlParams.get("SortedBy");
        let preArrangeTitle = urlParams.get("ArrangeOrderTitle");
        let preArrangeRating = urlParams.get("ArrangeOrderRating");

        //base sorted
        if (!preSorted) {
            preSorted = "title,rating";
        }

        if (!preArrangeTitle) {
            preArrangeTitle = "ascend";
        }

        if (!preArrangeRating) {
            preArrangeRating = "ascend";
        }

        console.log(preSorted);

        //sorting selection
        let sortedOptions = ["title,rating", "rating,title"];
        let sortedList = jQuery("#sortingContainer");
        for (let i = 0; i < sortedOptions.length; ++i) {
            pageUrl.set("SortedBy", sortedOptions[i]);
            let link = "movie-page.html?" + pageUrl.toString();
            let optionElement = `<option value="${link}"`;

            if (preSorted === sortedOptions[i]) {
                optionElement += " disabled selected";
            }
            optionElement += `>${sortedOptions[i]}</option>`;
            sortedList.append(optionElement);
        }
        //reset pageUrl
        pageUrl.set("SortedBy", preSorted);
        pageUrl.set("ArrangeOrderTitle", preArrangeTitle);
        pageUrl.set("ArrangeOrderRating", preArrangeRating);

        //ascend or descend list
        let ascendList = ["ascend", "descend"];
        let ascendElemTitle = jQuery("#titleAscendOrDescend");
        for (let i = 0; i < ascendList.length; ++i) {
            pageUrl.set("ArrangeOrderTitle", ascendList[i]);
            let link = "movie-page.html?" + pageUrl.toString();
            let optionElement = `<option value="${link}"`;

            if (preArrangeTitle === ascendList[i]) {
                optionElement += " disabled selected";
            }
            optionElement += `>${ascendList[i]}</option>`;
            ascendElemTitle.append(optionElement);
        }

        //reset pageUrl
        pageUrl.set("SortedBy", preSorted);
        pageUrl.set("ArrangeOrderTitle", preArrangeTitle);
        pageUrl.set("ArrangeOrderRating", preArrangeRating);

        let ascendElemRating = jQuery("#ratingAscendOrDescend");
        for (let i = 0; i < ascendList.length; ++i) {
            pageUrl.set("ArrangeOrderRating", ascendList[i]);
            let link = "movie-page.html?" + pageUrl.toString();
            let optionElement = `<option value="${link}"`;

            if (preArrangeRating === ascendList[i]) {
                optionElement += " disabled selected";
            }
            optionElement += `>${ascendList[i]}</option>`;
            ascendElemRating.append(optionElement);
        }
    }

    function createPageSizeOptions() {
        let pageSizeElem = jQuery("#sizeContainer");

        let pageUrl = new URLSearchParams(urlParams);
        pageUrl.delete("CurrPage");

        let sizeOptions = [10, 25, 50, 100];

        for (let i = 0; i < sizeOptions.length; ++i) {
            pageUrl.set("PageSize", sizeOptions[i].toString());
            let link = "movie-page.html?" + pageUrl.toString();
            let optionElement = `<option value="${link}"`;

            if (parseInt(pageSize) === sizeOptions[i]) {
                optionElement += " disabled selected";
            }
            optionElement += `>${sizeOptions[i]}</option>`;
            pageSizeElem.append(optionElement);
        }
    }

    function createPageHref(pageNum) {
        let pageUrl = new URLSearchParams(urlParams);
        pageUrl.set("CurrPage", `${pageNum}`);

        return "movie-page.html?" + pageUrl.toString();
    }
    //end of nested functions

    //creates the different sorting options
    createSortOptions();
    createPageSizeOptions();
    console.log("handleMovieResult: populating movie table from resultData");

    //add the query hits
    let movieCountElement = jQuery("#resultCount");
    movieCountElement.append(resultData["movies_count"] + " results");

    // Populate the star table
    // Find the empty table body by id "movie_table_body"m
    let movieTableBodyElement = jQuery("#movie_table_body");

    // Iterate through resultData, no more than 10 entries
    for (let i = 0; i < Math.min(pageSize, resultData["movies"].length); i++) {
        let curObj = resultData["movies"][i];
        let rowHTML = "";

        rowHTML += "<tr>";

        rowHTML +=
            "<th>" +
            // Add a link to single-movie.html with id passed with GET url parameter
            '<a href="single-movie.html?id=' + curObj['movieId'] + '">'
            + curObj["title"] +     // display star_name for the link text
            '</a>' +
            "</th>";

        rowHTML +=
            '<td>' +
            '<a>' + curObj['year'] + '</a>'
            +'</td>';

        rowHTML +=
            '<td>' +
            '<a>' + curObj['director'] + '</a>'
            +'</td>';

        rowHTML += PrintGenresArrayThree(curObj['genres']);

        rowHTML += PrintStars(curObj['stars']);

        rowHTML +=
            '<td>' +
            '<a>' + curObj['rating'] + '</a>'
            +'</td>';
        rowHTML += `<td><button type="button" class="add-to-cart-btn" data-id="${curObj['movieId']}">Add To Cart</button></td>`;
        rowHTML += "</tr>";


        // Append the row created to tche table body, which will refresh the page
        movieTableBodyElement.append(rowHTML);
    }

    //pagination work
    let pages = Math.trunc((resultData["movies_count"] / pageSize) + 1);
    let currPage = resultData["CurrPage"];
    //turns currPage param to int if not empty
    if (currPage) {
        currPage = parseInt(currPage);
        currPage = Math.min(currPage, pages - 1);
    } else {
        currPage = 0;
    }

    console.log(pages + " pages");

    let pageBtns = jQuery("#pages");

    //add previous button
    if (currPage !== 0) pageBtns.append(`<a href=\"${createPageHref(currPage - 1)}\" id=\"prev\">Previous</a>`);

    let i = Math.max(0, currPage - 4);
    if (i !== 0) {
        pageBtns.append(`<a href="${createPageHref(0)}" data-page="${1}">${1}</a>`);

        //if the last shown page is more than 1 page apart from the first page add the dots
        if (i > 1) pageBtns.append(`<a>...</a>`);
    }

    for (;i < pages && i < currPage + 3; ++i) {
        if (i === currPage) {
            pageBtns.append(`<a data-page="${i + 1}">${i + 1}</a>`);
        } else {
            pageBtns.append(`<a href="${createPageHref(i)}">${i + 1}</a>`);
        }
    }
    if (i < pages) {
        if (i < pages - 1) pageBtns.append(`<a>...</a>`);
        pageBtns.append(`<a href="${createPageHref(pages - 1)}" data-page="${pages}">${pages}</a>`);
    }


    //add next button
    if (currPage !== pages - 1) pageBtns.append(`<a href="${createPageHref(currPage + 1)}" id="next">Next</a>`);
    pageBtns.append(`<p id="page-numbers">${currPage + 1} of ${pages} pages</p>`);
}

function PrintGenresArrayThree(jArr){

    let tdHTML = "<td>";
    for (let i = 0; i < Math.min(3, jArr.length); i++){
        let curObj = jArr[i];
        let curHTML = "";
        curHTML += `<a href="movie-page.html?Genre=${curObj['name']}">` + curObj['name'];
        if(i !== Math.min(3, jArr.length) - 1){
            curHTML +=",";
        }
        curHTML += "</a>";
        tdHTML += curHTML;
    }
    tdHTML += "</td>";
    return tdHTML;
}

function PrintStars(starsArr){
    let tdHTML = "<td>";
    for (let i = 0; i < Math.min(3, starsArr.length); i++){
        let curObj = starsArr[i];
        let curHTML = "";
        curHTML += '<a href="single-star.html?id=' + curObj['id'] + '">' + curObj['name'];
        if(i !== Math.min(3, starsArr.length) - 1){
            curHTML +=",";
        }
        curHTML += "</a>";
        tdHTML += curHTML;
    }
    tdHTML += "</td>";
    return tdHTML;
}


/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */

// Makes the HTTP GET request and registers on success callback function handleMovieResult
jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    data: stringParam, //send in search/browse data
    url: "api/movie-page", // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => handleMovieResult(resultData) // Setting callback function to handle data returned successfully by the StarsServlet
});