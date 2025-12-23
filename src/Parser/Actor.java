package Parser;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Actor {
    protected String name;
    protected String id;
    protected int birthYear;
    protected static final TreeMap<String, Actor> NEW_ACTOR_LIST = new TreeMap<>();
    protected static final TreeMap<String, Actor> EXIST_IN_DATABASE_ACTOR_LIST = new TreeMap<>();
    protected static final ArrayList<Actor> REJECTED_ACTOR_LIST = new ArrayList<>();

    public Actor(){
        name = "";
        id = "";
        birthYear = ElementTags.NA_IDENTIFIER;
    }

    @Override
    public String toString(){
        return "Actor{id='" + id + "', name='" + name + "', birthYear=" + birthYear + "}";
    }

    protected String generateId(){
        if(name.isEmpty()){
            return Integer.toString(ElementTags.NA_IDENTIFIER);
        }
        String onlyLetters = name.replaceAll("[^a-zA-Z]", "");
        onlyLetters = onlyLetters.substring(0, Math.min(3, onlyLetters.length()));
        String randomNumberString = generateRandomDigits(6);
        return onlyLetters + randomNumberString;
    }
    public static String generateRandomDigits(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            int digit = (int) (Math.random() * 10);
            sb.append(digit);
        }
        return sb.toString();
    }
    public static void printAllActors() {
        System.out.println("New Actors:");
        for (Map.Entry<String, Actor> entry : NEW_ACTOR_LIST.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        System.out.println("\nExisting Actors in Database:");
        for (Map.Entry<String, Actor> entry : EXIST_IN_DATABASE_ACTOR_LIST.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }
    protected boolean listContains(String name){
        return NEW_ACTOR_LIST.containsKey(name) || EXIST_IN_DATABASE_ACTOR_LIST.containsKey(name);
    }
    protected static Actor getActorFromList(String name){
        Actor rv = NEW_ACTOR_LIST.get(name);
        if(rv == null){
            rv = EXIST_IN_DATABASE_ACTOR_LIST.get(name);
        }
        return rv;
    }
    protected static void printSize(){
        System.out.println("New Actors: " + NEW_ACTOR_LIST.size());
        System.out.println("Existing Actors in Database: " + EXIST_IN_DATABASE_ACTOR_LIST.size());
        System.out.println("Total Actors: " + (NEW_ACTOR_LIST.size() + EXIST_IN_DATABASE_ACTOR_LIST.size()));
    }




}
