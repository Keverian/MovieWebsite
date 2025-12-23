let addStarForm = $("#addStar");
let starInfo = $("#StarInfo");

function handleAddStarResult(resultDataString) {
    let resultDataJson = JSON.parse(resultDataString);

    console.log("handle add star response");
    console.log(resultDataJson);
    console.log(resultDataJson["status"]);

    //there was an error, print the error
    if ("message" in resultDataJson) {
        addStarResponse("Error! " + resultDataJson["message"]);
        return;
    }

    let sucText = "Success! ";
    sucText += "StarId: " + resultDataJson["NewStarId"];

    addStarResponse(sucText);
}

function addStarResponse(text) {
    starInfo.append(`<p>${text}</p><br/>`);
}

function submitAddStarForm(formSubmitEvent) {
    console.log("submitting add star form");
    /*
    * triggers when the user clicks the submit button
    * for adding movies
     */

    formSubmitEvent.preventDefault();

    $.ajax(
        "api/addstar", {
            method: "POST",
            //Serialize the add movie form to the data sent by POST request
            data: addStarForm.serialize(),
            success: handleAddStarResult
        }
    )
}

addStarForm.submit(submitAddStarForm);