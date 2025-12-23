const resultCache = new Map();

$('#main-search input[name="Title"]').autocomplete({
    // documentation of the lookup function can be found under the "Custom lookup function" section
    lookup: function (query, doneCallback) {
        //TODO: if query is not in cache, proceed with handlookup,
        handleLookup(query, doneCallback);
    },
    onSelect: function(suggestion) {
        handleSelectSuggestion(suggestion);
    },
    onFocus: function(suggestion){
        $('#main-search input[name="Title"]').val(suggestion.value)
    },
    deferRequestBy: 300,  // delay in ms before sending the request
    minChars: 3,          // minimum number of characters before triggering the lookup
    lookupLimit: 10       // optional: limit number of results shown (if supported by the plugin)
});

function handleLookup(query, doneCallback) {
    console.log("autocomplete initiated")
    console.log("sending AJAX request to backend Java Servlet")

    // TODO: if you want to check past query results first, you can do it here
    if(resultCache.has(query)){
        doneCallback({suggestions:resultCache.get(query)})
    }
    else{
        jQuery.ajax({
            dataType: "json",
            method: "GET",
            // generate the request url from the query.
            // escape the query string to avoid errors caused by special characters
            url:"api/search-autocomplete",
            data: {search_value: query},
            success: function(data) {
                // pass the data, query, and doneCallback function into the success handler
                handleLookupAjaxSuccess(data, query, doneCallback)
            },
            error: function(errorData) {
                console.log("lookup ajax error")
                console.log(errorData)
            }
        })
    }
    // sending the HTTP GET request to the Java Servlet endpoint hero-suggestion
    // with the query data
}

function handleLookupAjaxSuccess(data, query, doneCallback) {
    console.log("lookup ajax successful")


    console.log(data)
    if(!resultCache.has(query)){
        resultCache.set(query, data);
    }

    doneCallback( { suggestions: data } );
}

function handleSelectSuggestion(suggestion) {
    // TODO: jump to the specific result page based on the selected suggestion


    const movieID = suggestion.data;  // or suggestion["data"], depends on your JSON structure

    // Redirect browser to the URL
    window.location.href = "single-movie.html?id=" + movieID;
}
