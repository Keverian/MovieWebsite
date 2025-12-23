let tables = $("#tableInfo")

function handleTablesRequest(resultDataJson) {
    console.log("table info endpoint has been called!")
    console.log(resultDataJson);

    if ("message" in resultDataJson) {
        console.log("Error in fetching table data: " + resultDataJson["message"]);
        return;
    }

    resultDataJson["tables"].forEach(addTableInfo);
}

function addTableInfo(singularTable, index) {
    let tableSection = "<div>";

    tableSection += "<h2>" + singularTable["TableName"] + "</h2><br/>";
    tableSection += "<h3>Attribute  ---->  Type</h3>";

    singularTable["Columns"].forEach((column) => {
       tableSection += "<h4>" + column["Field"] + "  ---->  " + column["Type"] + "</h4>";
    });

    tableSection += "</div>";

    tables.append(tableSection);
}

$.ajax("api/tables", {
    dataType: "json",
    method: "GET",
    success: (resultData) => handleTablesRequest(resultData)
});

