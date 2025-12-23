package Parser;

import java.util.ArrayList;

public class Cast {
    protected Actor actor;
    protected Movie movie;
    protected static ArrayList<Cast> castList = new ArrayList<>();
    protected static ArrayList<Cast> rejectedCastList = new ArrayList<>();
    public Cast(){
        actor = null;
        movie = null;
    }

    @Override
    public String toString() {
        String actorString = "Actor: Null";
        if(actor != null){
            actorString = actor.toString();
        }
        String movieString = "Movie: Null";
        if(movie != null){
            movieString = movie.toString();
        }
        return "Cast: ["+ actorString + "\n" + movieString + "]";
    }
    protected static void printCastList(){
        for(Cast cast : castList){
            System.out.println(cast.toString());
        }
        System.out.println("Cast List Size: " + Integer.toString(castList.size()));
        return;
    }
    protected static void printRejectedCastList(){
        for(Cast cast : rejectedCastList){
            System.out.println(cast.toString());
        }
        System.out.println("Cast List Size: " + Integer.toString(rejectedCastList.size()));
        return;
    }
}
