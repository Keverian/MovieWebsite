package Parser;
import com.mysql.cj.protocol.Resultset;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
public class CastParser extends DefaultHandler {
    private static Connection conn;

    private StringBuilder characterBuffer;
    private Cast currentCast;
    protected static final int ROOT_ELEMENT = ElementTags.CAST_ROW_ROOT_ELEMENT;
    protected static final int ACTOR_NAME_ELEMENT = ElementTags.CAST_ACTOR_NAME_ELEMENT;
    protected static final int CAST_MOVIE_ID_ELEMENT = ElementTags.CAST_MOVIE_ID_ELEMENT;
    protected static final int NA_IDENTIFIER = ElementTags.NA_IDENTIFIER;


    protected Deque<Integer> elementStack;
    private static final HashMap<String, Integer> ElementNameToTagMap = new HashMap<>();
    private static final HashMap<Integer, String> ElementTagToNameMap = new HashMap<>();
    protected int testCounter;
    protected String lastRecognizedTag;
    protected PrintWriter out;

    protected int rejectedCount = 0;
    protected boolean rejectedOccur = false;
    public CastParser(Connection conn, PrintWriter out){
        this.out = out;
        this.conn = conn;
        characterBuffer = new StringBuilder();
        elementStack = new ArrayDeque<>();
        testCounter = 0;
        lastRecognizedTag = "none";
    }

    private static void initializeElementNameToTagMap(){
        ElementNameToTagMap.put(cleanKey("m"), ROOT_ELEMENT);
        ElementNameToTagMap.put(cleanKey("a"), ACTOR_NAME_ELEMENT);
        ElementNameToTagMap.put(cleanKey("f"), CAST_MOVIE_ID_ELEMENT);

        ElementTagToNameMap.put(ROOT_ELEMENT, "m");
        ElementTagToNameMap.put(ACTOR_NAME_ELEMENT, "a");
        ElementTagToNameMap.put(CAST_MOVIE_ID_ELEMENT, "f");
    }

    public static String cleanKey(String key) {
        // Remove spaces, lowercase everything, and remove everything that is not a letter
        return key.replaceAll("[^a-zA-Z]", "").toLowerCase();
    }
    static{
        initializeElementNameToTagMap();
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
            sp.parse("stanford-movies/casts124.xml", this);

        } catch (SAXException se) {
            se.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

//        System.out.println("Counter: " + Integer.toString(testCounter));
//        if(testCounter > 100){
//            printCastList();
//            throw new RuntimeException("hit counter limit");
//        }
        String elementName = cleanKey(qName);
        lastRecognizedTag = elementName;
        if (!ElementNameToTagMap.containsKey(elementName)) {
            return; // Skip unknown element
        }

        int currentElementTag = ElementNameToTagMap.get(elementName);

        if (elementStack.isEmpty()) {
            if (currentElementTag == ROOT_ELEMENT) {
                testCounter += 1;
                initializeNewActorParse();
            }
            return;
        }

        // If the current context is not a valid movie root, reset
        if (elementStack.peekFirst() != ROOT_ELEMENT) {
            elementStack.clear();
            return;
        }

        // If trying to start a new movie while already inside one, reset
        if (currentElementTag == ROOT_ELEMENT) {
            elementStack.clear();
            initializeNewActorParse();
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
        if (ElementNameToTagMap.containsKey(lastRecognizedTag) && !elementStack.isEmpty() && elementStack.getFirst() == ROOT_ELEMENT && elementStack.getLast() != ROOT_ELEMENT && ElementTagToNameMap.containsKey(elementStack.getLast())) {
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

        if (elementStack.peekLast() == currentElementTag && currentElementTag != ROOT_ELEMENT) {
            setAttribute(currentElementTag);
            elementStack.removeLast();
        }
        else if (currentElementTag == ROOT_ELEMENT && elementStack.size() == 1 && elementStack.peekLast() == ROOT_ELEMENT) {
            elementStack.removeLast(); // Clean end of movie
            finalizeCurrentCasParse();      // Optional finalization hook
        }
    }

    private void initializeNewActorParse(){
        elementStack.clear();
        elementStack.push(ROOT_ELEMENT);
        currentCast = new Cast();
        characterBuffer.setLength(0);
    }

    private void setAttributeNA(int elementTag){
        String info = characterBuffer.toString();
        characterBuffer.setLength(0);
        switch (elementTag){
            case ACTOR_NAME_ELEMENT:
                currentCast.actor = null;
                break;
            case CAST_MOVIE_ID_ELEMENT:
                currentCast.movie = null;
                break;
        }
    }

    private void setAttribute(int elementTag){
        String info = characterBuffer.toString();
        characterBuffer.setLength(0);
        if(info.isEmpty()){
            return;
        }
        switch (elementTag){
            case ACTOR_NAME_ELEMENT:
                setActor(info);
                break;
            case CAST_MOVIE_ID_ELEMENT:
                setMovie(info);
                break;
        }
    }
    private void finalizeCurrentCasParse(){
        boolean earlyBreak = false;
        if(currentCast.actor == null || currentCast.movie == null){
            rejectedCount += 1;
            Cast.rejectedCastList.add(currentCast);
            out.println("FOR REJECTED CAST: " + currentCast.toString());
            return;
        }

        //TODO: check if actor name and birth year appear in existing movie base
        Cast.castList.add(currentCast);
        currentCast = null;
        return;
    }

    private void setActor(String castActorStageName){
        castActorStageName = ActorParser.formatName(castActorStageName);
        currentCast.actor = Actor.getActorFromList(castActorStageName);
        if(currentCast.actor == null && !findActorFromDataBase(castActorStageName)){//find in database
            //make a new actor
            currentCast.actor = new Actor();
            currentCast.actor.name = castActorStageName;
            ActorParser.setGeneratedId(conn, currentCast.actor);
            Actor.NEW_ACTOR_LIST.put(currentCast.actor.name, currentCast.actor);
        }
        if(currentCast.actor == null){
            out.println("ACTOR: " + castActorStageName + " DO NOT EXIST");
        }
    }
    private void setMovie(String movieID){
        movieID = MovieParser.cleanMovieID(movieID);
        currentCast.movie = Movie.getMovieFromList(movieID);
        if(currentCast.movie == null){
            findMovieInDatabse(movieID);
        }
        if( currentCast.movie == null){
            out.println("MOVIE ID: " + movieID + " DO NOT EXIST");
        }
    }
    protected boolean findMovieInDatabse(String movieId){
        String query = "SELECT * FROM movies WHERE id = ?";
        try (PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setString(1, movieId);
            ResultSet rs = statement.executeQuery();
            int count = 0;
            if(rs.next()){
                currentCast.movie = new Movie();
                currentCast.movie.id = rs.getString("id");
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
//    findMovieFromDataBase(String movieID){
//
//    }

    private boolean findActorFromDataBase(String castActorStageName){//return false if there are duplicate actors
        String query = "SELECT id FROM stars WHERE name = ?";
        boolean actorExistInDatabase = false;
        try (PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setString(1, castActorStageName);
            ResultSet rs = statement.executeQuery();
            int count = 0;
            Actor tempActor = new Actor();
            tempActor.name = castActorStageName;
            while(count < 2 && rs.next()){
                tempActor.id = rs.getString("id");
                count += 1;
            }
            if(count == 0 || count > 1){
                return false;
            }
            rs.close();
            currentCast.actor = tempActor;


        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
    protected void printCastList(){
        for(Cast cast : Cast.castList){
            System.out.println(cast.toString());
        }
        System.out.println("Cast List Size: " + Integer.toString(Cast.castList.size()));
        return;
    }



}
