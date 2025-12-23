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
import java.sql.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;

public class ActorParser extends DefaultHandler{
    private static Connection conn;

    private StringBuilder characterBuffer;
    private Actor currentActor;
    protected static final int ACTOR_ELEMENT = ElementTags.ACTOR_ELEMENT;
    protected static final int ACTOR_NAME_ELEMENT = ElementTags.ACTOR_NAME_ELEMENT;
    protected static final int ACTOR_BIRTH_YEAR_ELEMENT = ElementTags.ACTOR_BIRTH_YEAR_ELEMENT;
    protected static final int NA_IDENTIFIER = ElementTags.NA_IDENTIFIER;
    protected PrintWriter out;


    protected Deque<Integer> elementStack;
    private static final HashMap<String, Integer> ElementNameToTagMap = new HashMap<>();
    private static final HashMap<Integer, String> ElementTagToNameMap = new HashMap<>();
    protected int testCounter;
    protected String lastRecognizedTag;
    private static void initializeElementNameToTagMap(){
        ElementNameToTagMap.put(cleanKey("actor"), ACTOR_ELEMENT);
        ElementNameToTagMap.put(cleanKey("stagename"), ACTOR_NAME_ELEMENT);
        ElementNameToTagMap.put(cleanKey("dob"), ACTOR_BIRTH_YEAR_ELEMENT);

        ElementTagToNameMap.put(ACTOR_ELEMENT, "actor");
        ElementTagToNameMap.put(ACTOR_NAME_ELEMENT, "stagename");
        ElementTagToNameMap.put(ACTOR_BIRTH_YEAR_ELEMENT, "dob");
    }

    static{
        initializeElementNameToTagMap();
    }

    public ActorParser(Connection conn, PrintWriter out){
        this.out = out;
        this.conn = conn;
        characterBuffer = new StringBuilder();
        elementStack = new ArrayDeque<>();
        testCounter = 0;
        lastRecognizedTag = "none";
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
            sp.parse("stanford-movies/actors63.xml", this);

        } catch (SAXException se) {
            se.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }


//  Stack must start with a ACTOR_ELEMENT.
//
//  Child elements (e.g., title, year, etc.) can appear after ACTOR_ELEMENT.
//
//  No nested ACTOR_ELEMENTs are allowed — if one appears, the stack is reset.
//
//  Invalid children (i.e., those outside the movie context or unclosed) are cleaned up.

//    Then set correct element tag

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

//        System.out.println("Counter: " + Integer.toString(testCounter));
//        if(testCounter > 10){
//            Actor.printActors();
//            throw new RuntimeException("hit counter limit");
//        }
        String elementName = cleanKey(qName);
        lastRecognizedTag = elementName;
        if (!ElementNameToTagMap.containsKey(elementName)) {
            return; // Skip unknown element
        }

        int currentElementTag = ElementNameToTagMap.get(elementName);

        if (elementStack.isEmpty()) {
            if (currentElementTag == ACTOR_ELEMENT) {
                testCounter += 1;
                initializeNewActorParse();
            }
            return;
        }

        // If the current context is not a valid movie root, reset
        if (elementStack.peekFirst() != ACTOR_ELEMENT) {
            elementStack.clear();
            return;
        }

        // If trying to start a new movie while already inside one, reset
        if (currentElementTag == ACTOR_ELEMENT) {
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
        if (ElementNameToTagMap.containsKey(lastRecognizedTag) && !elementStack.isEmpty() && elementStack.getFirst() == ACTOR_ELEMENT && elementStack.getLast() != ACTOR_ELEMENT && ElementTagToNameMap.containsKey(elementStack.getLast())) {
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

        if (elementStack.peekLast() == currentElementTag && currentElementTag != ACTOR_ELEMENT) {
            setAttribute(currentElementTag);
            elementStack.removeLast();
        }
        else if (currentElementTag == ACTOR_ELEMENT && elementStack.size() == 1 && elementStack.peekLast() == ACTOR_ELEMENT) {
            elementStack.removeLast(); // Clean end of movie
            finalizeCurrentActorParse();      // Optional finalization hook
        }
    }



    private void finalizeCurrentActorParse(){
        if(currentActor.name.equals(Integer.toString(NA_IDENTIFIER))){
            Actor.REJECTED_ACTOR_LIST.add(currentActor);
            currentActor = null;
            return;
        }
        //TODO: check if actor name and birth year appear in existing movie base
        boolean actorExistInDatabase = assignActorId();

        if(actorExistInDatabase){
            Actor.EXIST_IN_DATABASE_ACTOR_LIST.put(currentActor.name, currentActor);
        }
        else{
            Actor.NEW_ACTOR_LIST.put(currentActor.name, currentActor);
        }
        currentActor = null;
        return;
    }

    private boolean assignActorId() {
        boolean actorExist = false;
        String query = (currentActor.birthYear == NA_IDENTIFIER)
                ? "SELECT id FROM stars WHERE name = ?"
                : "SELECT id FROM stars WHERE name = ? AND birthYear = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, currentActor.name);
            if (currentActor.birthYear != NA_IDENTIFIER) {
                stmt.setInt(2, currentActor.birthYear);
            }
            ResultSet rs = stmt.executeQuery();
            actorExist = handleIfActorExistResultSet(rs); //assign database retrieved id if exist
            rs.close();
            if(!actorExist){
                setGeneratedId(conn, currentActor);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return actorExist;
    }
    private boolean handleIfActorExistResultSet(ResultSet rs) throws SQLException{
        int count = 0;
        String tempID = currentActor.id;
        while(count < 2 && rs.next()){
            count += 1;
            tempID = rs.getString("id");
        }
        if(count == 1){
            currentActor.id = tempID;
            return true;
        }
        return false;
    }

    protected static void setGeneratedId(Connection conn, Actor actor){
        String tempId = actor.generateId();
        String query = "SELECT COUNT(*) FROM stars WHERE id = ?";
        try(PreparedStatement statement = conn.prepareStatement(query)){
            while(true){
                statement.setString(1, tempId);
                ResultSet rs = statement.executeQuery();
                rs.next();
                int count = rs.getInt(1);
                rs.close();
                if (count == 0) {
                    break; // ID is unique
                } else {
                    tempId = actor.generateId(); // Try again
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        actor.id = tempId;
    }

    private void initializeNewActorParse(){
        elementStack.clear();
        elementStack.push(ACTOR_ELEMENT);
        currentActor = new Actor();
        characterBuffer.setLength(0);
    }


    private void setAttributeNA(int elementTag){
        String info = characterBuffer.toString();
        characterBuffer.setLength(0);
        switch (elementTag){
            case ACTOR_NAME_ELEMENT:
                currentActor.name = Integer.toString(NA_IDENTIFIER);
                break;
            case ACTOR_BIRTH_YEAR_ELEMENT:
                currentActor.birthYear = NA_IDENTIFIER;
                break;
        }
    }
    private void setAttribute(int elementTag){
        String info = characterBuffer.toString();
        characterBuffer.setLength(0);

        switch (elementTag){
            case ACTOR_NAME_ELEMENT:
                setName(info);
                break;
            case ACTOR_BIRTH_YEAR_ELEMENT:
                setYear(info);
                break;
        }
    }
    private void setYear(String info){

        String originalInfo = info;
        info = info.replaceAll("[a-zA-Z\\s]+", "");
        if (info.matches("\\d+")) {
            currentActor.birthYear = Integer.parseInt(info);
        } else if (!info.isEmpty()) {
            out.println("Inconsistent birth year: " + originalInfo);
            currentActor.birthYear = NA_IDENTIFIER;
        }
        else{
            currentActor.birthYear = NA_IDENTIFIER;
        }
    }



    public static String cleanKey(String key) {
        // Remove spaces, lowercase everything, and remove everything that is not a letter
        return key.replaceAll("[^a-zA-Z]", "").toLowerCase();
    }
    protected static String formatName(String name){
        return name.trim().replaceAll("\\s+", " ");
    }
    protected void setName(String info){
        if(info.isEmpty()){
            out.println("Inconsistent Name: " + info);
            currentActor.name = Integer.toString(NA_IDENTIFIER);
            return;
        }
        currentActor.name = formatName(info);
    }




}



