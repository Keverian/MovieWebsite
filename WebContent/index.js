
//section for browsing title names
let browseTitles = jQuery("#title_list");

for (let charCode = 'a'.charCodeAt(0); charCode <= 'z'.charCodeAt(0); ++charCode) {
    let letter = String.fromCharCode(charCode);
    browseTitles.append(`<a href="${createHrefRequest("BrowseTitle", letter)}">${letter}</a>`);
}
browseTitles.append("<br>");

for (let number = '0'; number <= '9'; ++ number) {
    browseTitles.append(`<a href="${createHrefRequest("BrowseTitle", number)}">${number}</a>`);
}
browseTitles.append("<br>");

browseTitles.append(`<a href="${createHrefRequest("BrowseTitle", "*")}">*</a>`)
//end of browsing title names

/**
 * Handle the data returned by IndexServlet
 * @param resultDataString jsonObject, consists of session info
 */
function handleSessionData(resultDataString) {
    let resultDataJson = JSON.parse(resultDataString);

    console.log("handle session response");
    console.log(resultDataJson);
    console.log(resultDataJson["sessionID"]);

    // show the session information 
    $("#sessionID").text("Session ID: " + resultDataJson["sessionID"]);
    $("#lastAccessTime").text("Last access time: " + resultDataJson["lastAccessTime"]);

    // // show cart information
    // handleCartArray(resultDataJson["previousItems"]);
}



function handleGenreList(genreData) {
    let genreDisplay = jQuery("#genre_list");
    for (let i = 0; i < genreData.length; ++i) {
        if (i % 5 === 0) genreDisplay.append("<br>");
        genreDisplay.append(`<a href=\"${createHrefRequest("Genre", genreData[i])}\">${genreData[i]}</a>`);
    }
}

function createHrefRequest(attributeName, attribute) {
    let urlParams = new URLSearchParams(window.location.search);
    urlParams.set(attributeName, attribute);

    return "movie-page.html?" + urlParams.toString();
}

$.ajax("api/index", {
    method: "GET",
    success: handleSessionData
});

$.ajax("api/genre-list", {
    dataType: "json",
    method: "GET",
    success: (resultData) => handleGenreList(resultData)
});
