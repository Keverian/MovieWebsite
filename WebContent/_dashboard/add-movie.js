let addMovieForm = $("#addMovie");
let movieInfo = $("#MovieInfo");

function handleAddMovieResult(resultDataString) {
    let resultDataJson = JSON.parse(resultDataString);

    console.log("handle add movie response");
    console.log(resultDataJson);
    console.log(resultDataJson["status"]);

    //there was an error, print the error
    if ("message" in resultDataJson) {
        addMovieResponse("Error! " + resultDataJson["message"]);
        return;
    }

    let sucText = "Success! ";
    sucText += "MovieId: " + resultDataJson["NewMovieId"] + ",";
    sucText += "GenreId: " + resultDataJson["NewGenreId"] + ",";
    sucText += "StarId: " + resultDataJson["NewStarId"];

    addMovieResponse(sucText);
}

function addMovieResponse(text) {
    movieInfo.append(`<p>${text}</p><br/>`);
}

function submitAddMovieForm(formSubmitEvent) {
    console.log("submitting add movie form");
    /*
    * triggers when the user clicks the submit button
    * for adding movies
    * */

    formSubmitEvent.preventDefault();

    $.ajax(
        "api/addmovie", {
            method: "POST",
            //Serialize the add movie form to the data sent by POST request
            data: addMovieForm.serialize(),
            success: handleAddMovieResult
        }
    );
}

addMovieForm.submit(submitAddMovieForm);