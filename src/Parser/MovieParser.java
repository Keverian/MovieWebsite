package Parser;//Use a stack to keep track of current tag for detecting whenther a element is missing START OR END ELEMENT TAG

//Create Parser.Movie and Genre Objects during parsing, connect them after insert those objects into database

//XML DATA CATEGORY CODE --MAP--> Genre Object --MAP--> genre ID

//MOVIE ID --MAP--> MOVIE Object


// tags to care about
//
//
//<film>
//        <fid>H1</fid>
//        <t>Always Tell Your Wife</t>
//        <year>1922</year>
//        <dirs>
//        <dir>
//        <dirk>R</dirk> DONT CARE
//        <dirn>Se.Hicks</dirn>
//        </dir>
//        <dir>
//        <dirk>R</dirk> DONT CARE
//        <dirn>Hitchcock</dirn>
//        </dir>
//        </dirs>
//<CATS><CAT>GENRE</CAT><CATS>




//
import java.io.IOException;

import javax.swing.text.Element;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.HashMap;


public class MovieParser extends DefaultHandler {



    private final Connection conn;
    private StringBuilder characterBuffer;
    private Movie currentMovie;
    protected static final int MOVIE_ELEMENT = ElementTags.MOVIE_ELEMENT;
    protected static final int TITLE_ELEMENT = ElementTags.TITLE_ELEMENT;
    protected static final int MOVIE_ID_ELEMENT = ElementTags.MOVIE_ID_ELEMENT;
    protected static final int YEAR_ELEMENT = ElementTags.YEAR_ELEMENT;
    protected static final int DIRECTOR_LIST_ELEMENT = ElementTags.DIRECTOR_LIST_ELEMENT;
    protected static final int DIRECTOR_NAME_ELEMENT = ElementTags.DIRECTOR_NAME_ELEMENT;
    protected static final int GENRE_LIST_ELEMENT = ElementTags.GENRE_LIST_ELEMENT;
    protected static final int GENRE_ELEMENT = ElementTags.GENRE_ELEMENT;
    protected static final int NA_IDENTIFIER = ElementTags.NA_IDENTIFIER;

    protected Deque<Integer> elementStack;
    private static final HashMap<String, Integer> ElementNameToTagMap = new HashMap<>();
    private static final HashMap<Integer, String> ElementTagToNameMap = new HashMap<>();
    protected int testCounter;
    protected String lastRecognizedTag;

    protected PrintWriter out;
    private static void initializeElementNameToTagMap(){
        ElementNameToTagMap.put(cleanKey("film"), MOVIE_ELEMENT);
        ElementNameToTagMap.put(cleanKey("fid"), MOVIE_ID_ELEMENT);
        ElementNameToTagMap.put(cleanKey("t"), TITLE_ELEMENT);
        ElementNameToTagMap.put(cleanKey("year"), YEAR_ELEMENT);
//        ElementNameToTagMap.put("dirs", DIRECTOR_LIST_ELEMENT);
        ElementNameToTagMap.put(cleanKey("dir"), DIRECTOR_NAME_ELEMENT);
//        ElementNameToTagMap.put("CATS", GENRE_LIST_ELEMENT);
        ElementNameToTagMap.put(cleanKey("cat"), GENRE_ELEMENT);

        ElementTagToNameMap.put(MOVIE_ELEMENT, "film");
        ElementTagToNameMap.put(MOVIE_ID_ELEMENT, "fid");
        ElementTagToNameMap.put(TITLE_ELEMENT, "t");
        ElementTagToNameMap.put(YEAR_ELEMENT, "year");
        ElementTagToNameMap.put(DIRECTOR_NAME_ELEMENT, "dir");
        ElementTagToNameMap.put(GENRE_ELEMENT, "cat");
    }

    static{
        initializeElementNameToTagMap();
    }

    public MovieParser(Connection conn, PrintWriter out){
        this.out = out;
        this.conn = conn;
        characterBuffer = new StringBuilder();
        elementStack = new ArrayDeque<>();
        testCounter = 0;
    }

    protected void parseDocument() {

        //get a factory
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(false); // Disable DTD/XSD validation

        try {

            //get a new instance of parser
            SAXParser sp = spf.newSAXParser();

            //parse the file and also register this class for call backs
//            sp.parse("../standford-movies/mains243.xml", this);
            sp.parse("stanford-movies/mains243.xml", this);

        } catch (SAXException se) {
            se.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }


//  Stack must start with a MOVIE_ELEMENT.
//
//  Child elements (e.g., title, year, etc.) can appear after MOVIE_ELEMENT.
//
//  No nested MOVIE_ELEMENTs are allowed — if one appears, the stack is reset.
//
//  Invalid children (i.e., those outside the movie context or unclosed) are cleaned up.

//    Then set correct element tag

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

//        System.out.println("Counter: " + Integer.toString(testCounter));

        String elementName = cleanKey(qName);
        lastRecognizedTag = elementName;
        if (!ElementNameToTagMap.containsKey(elementName)) {
            return; // Skip unknown element
        }

        int currentElementTag = ElementNameToTagMap.get(elementName);

        if (elementStack.isEmpty()) {
            if (currentElementTag == MOVIE_ELEMENT) {
                testCounter += 1;
                initializeNewMovieParse();
            }
            return;
        }

        // If the current context is not a valid movie root, reset
        if (elementStack.peekFirst() != MOVIE_ELEMENT) {
            elementStack.clear();
            return;
        }

        // If trying to start a new movie while already inside one, reset
        if (currentElementTag == MOVIE_ELEMENT) {
            elementStack.clear();
            initializeNewMovieParse();
            return;
        }

        // Cleanup previous sub-elements that were not closed
        while (elementStack.size() > 1) {
            setAttributeNA(elementStack.removeLast());
        }

        // Set the new attribute
        elementStack.addLast(currentElementTag);
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (ElementNameToTagMap.containsKey(lastRecognizedTag) && !elementStack.isEmpty() && elementStack.getFirst() == MOVIE_ELEMENT && elementStack.getLast() != MOVIE_ELEMENT && ElementTagToNameMap.containsKey(elementStack.getLast())) {
            characterBuffer.append(ch, start, length);
        }

    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        String elementName = cleanKey(qName);
        if (!ElementNameToTagMap.containsKey(elementName)) {
            return; // Skip unknown element
        }

        int currentElementTag = ElementNameToTagMap.get(elementName);

        // If stack is empty, nothing to do
        if (elementStack.isEmpty()) {
            return;
        }

        // If the top of the stack matches this element, pop it

        if (elementStack.peekLast() == currentElementTag && currentElementTag != MOVIE_ELEMENT) {
            setAttribute(currentElementTag);
            elementStack.removeLast();
        }
        else if (currentElementTag == MOVIE_ELEMENT && elementStack.size() == 1 && elementStack.peekLast() == MOVIE_ELEMENT) {
            elementStack.removeLast(); // Clean end of movie
            finalizeCurrentMovieParse();      // Optional finalization hook
        }



    }
    //TODO: when visit film tag start: create whole new instance, remove everything thats store by previous parse, self-handle inccorect film tag by forbid
    //TODO: when visit film tag end: run current movie object to isValidMovie



    private void finalizeCurrentMovieParse(){
        if(currentMovie.id.isEmpty() || currentMovie.title.isEmpty() || currentMovie.directors.isEmpty() || currentMovie.genres.isEmpty() || currentMovie.year == NA_IDENTIFIER || currentMovie.year == 0){
            Movie.REJECTED_MOVIE_LIST.add(currentMovie);
            currentMovie = null;
            return;
        }

        if(handleMovieExistInDatabase()){
            Movie.DATABASE_EXIST_MOVIE_LIST.put(currentMovie.id, currentMovie);
        }
        else{
            Movie.MOVIE_LIST.put(currentMovie.id, currentMovie);
        }
        currentMovie = null;
        return;
    }

    private void initializeNewMovieParse(){
        elementStack.clear();
        elementStack.push(MOVIE_ELEMENT);
        currentMovie = new Movie();
        characterBuffer.setLength(0);
    }


    private void setAttributeNA(int elementTag){
        String info = characterBuffer.toString();
        characterBuffer.setLength(0);
        switch (elementTag){
            case TITLE_ELEMENT:
                currentMovie.title = Integer.toString(NA_IDENTIFIER);
                break;
            case MOVIE_ID_ELEMENT:
                currentMovie.title = "";
                break;
            case YEAR_ELEMENT:
                currentMovie.year = NA_IDENTIFIER;
                break;
        }
    }
    private void setAttribute(int elementTag){
        String info = characterBuffer.toString();
        characterBuffer.setLength(0);

        switch (elementTag){
            case TITLE_ELEMENT:
                currentMovie.title = info;
                break;
            case MOVIE_ID_ELEMENT: //TODO remove all space
                currentMovie.id = cleanMovieID(info);
                break;
            case YEAR_ELEMENT:
                setYear(info);
                break;
            case DIRECTOR_NAME_ELEMENT:
                currentMovie.directors.add(new Director(info));
                break;
            case GENRE_ELEMENT:
                addGenre(info);
                break;
        }
    }
    private void setYear(String info) {
        String originalInfo = info;
        info = info.replaceAll("[a-zA-Z\\s]+", "");
        if (info.matches("\\d+")) {
            currentMovie.year = Integer.parseInt(info);
        } else {
            out.println("Inconsistent year: " + originalInfo);
            currentMovie.year = NA_IDENTIFIER;
        }
    }
    private void addGenre(String info){
        info = Genre.cleanKey(info);
        if(Genre.CODE_TO_GENRE.containsKey(info)){
            currentMovie.genres.add(Genre.CODE_TO_GENRE.get(info));
        }
        else{
            out.println("Inconsistent genre key: " + info);
        }
    }
    protected static String cleanMovieID(String id){
        return id.replace(" ", "");
    }
    public static String cleanKey(String key) {
        // Remove spaces, lowercase everything, and remove everything that is not a letter
        return key.replaceAll("[^a-zA-Z]", "").toLowerCase();
    }
    private boolean handleMovieExistInDatabase(){
        String query = "SELECT * FROM movies WHERE id = ?";
        boolean rv = false;
        try (PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setString(1, currentMovie.id);
            ResultSet rs = statement.executeQuery();
            if(rs.next()){
                currentMovie.id = rs.getString("id");
                currentMovie.year = rs.getInt("year");
                currentMovie.title = rs.getString("title");
                currentMovie.directors.clear();
                currentMovie.directors.add(new Director(rs.getString("director")));
                rv = true;

            }
            rs.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return rv;
    }
    public static void main(String[] args) {


        try(Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "mytestuser", "My6$Password");){


        } catch (Exception e) {
            throw new RuntimeException(e);
        }



    }





}
