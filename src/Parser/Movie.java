package Parser;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
//CREATE TABLE IF NOT EXISTS movies (
//        id VARCHAR(10) PRIMARY KEY,
//title VARCHAR(100) NOT NULL,
//year INT NOT NULL,
//director  VARCHAR(100) NOT NULL
//    );
public class Movie {
    protected static final TreeMap<String, Movie> MOVIE_LIST = new TreeMap<>();
    protected static final TreeMap<String, Movie> DATABASE_EXIST_MOVIE_LIST = new TreeMap<>();
    protected static final ArrayList<Movie> REJECTED_MOVIE_LIST = new ArrayList<>();

    protected String id;
    protected String title;
    protected int year;

    protected ArrayList<Director> directors;
    protected ArrayList<Genre> genres;
    protected ArrayList<Actor> casts;
    public Movie(){
        this.id = "";
        this.title = "";
        this.year = 0;
        directors = new ArrayList<>();
        genres = new ArrayList<>();
        casts = new ArrayList<>();
    }
    public Movie(String id, String title, int year) {
        this.id = id;
        this.title = title;
        this.year = year;
        directors = new ArrayList<>();
    }
    public Movie(String id, String title, int year, ArrayList<Director> directors) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.directors = directors;
    }
    public String getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public int getYear() {
        return year;
    }
    public ArrayList<Director> getDirectors() {
        return directors;
    }

    public ArrayList<Genre> getGenres() {
        return genres;
    }
    public ArrayList<Actor> getCasts() {
        return casts;
    }
    public static TreeMap<String, Movie> getMovieList() {
        return MOVIE_LIST;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Movie{id='").append(id)
                .append("', title='").append(title)
                .append("', year=").append(year)
                .append(", directors=").append(directors != null ? directors : "[]")
                .append(", genres=").append(genres != null ? genres : "[]")
                .append("}");
        return sb.toString();
    }

    public static void printMovies() {
        for (Map.Entry<String, Movie> entry : MOVIE_LIST.entrySet()) {
            System.out.println("Key: " + entry.getKey() + " -> " + entry.getValue());
        }
        for (Map.Entry<String, Movie> entry : DATABASE_EXIST_MOVIE_LIST.entrySet()) {
            System.out.println("Key: " + entry.getKey() + " -> " + entry.getValue());
        }
    }
    public static void printSize(){
        System.out.println("Movies: " + MOVIE_LIST.size());
        System.out.println("Movies in Database: " + DATABASE_EXIST_MOVIE_LIST.size());
        System.out.println("Total Movies: " + (MOVIE_LIST.size() + DATABASE_EXIST_MOVIE_LIST.size()));
    }
    protected static Movie getMovieFromList(String id){
        Movie rv = MOVIE_LIST.get(id);
        if(rv == null){
            rv = DATABASE_EXIST_MOVIE_LIST.get(id);
        }
        return rv;
    }
    protected static boolean listContains(String id){
        return MOVIE_LIST.containsKey(id) || DATABASE_EXIST_MOVIE_LIST.containsKey(id);
    }
}
