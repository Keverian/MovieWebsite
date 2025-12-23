package Parser;

import DatabaseCredentials.DBLoginConstant;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.TreeMap;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DataLoader {
    protected final TreeMap<String, Actor> ACTOR_LIST;
    protected final TreeMap<String, Movie> MOVIE_LIST;
    protected ArrayList<Cast> CAST_LIST;
    public DataLoader(){
        CAST_LIST = Cast.castList;
        MOVIE_LIST = Movie.MOVIE_LIST;
        ACTOR_LIST = Actor.NEW_ACTOR_LIST;
    }


    public static void main(String[] args){
        try(Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb",
                DBLoginConstant.username, DBLoginConstant.password)
            ;){
            File file = new File("rejected-parse.txt"); // create in parent directory
            PrintWriter writer = new PrintWriter(file);

            MovieParser mp = new MovieParser(conn, writer);
            mp.parseDocument();
            ActorParser spe = new ActorParser(conn, writer);
            spe.parseDocument();

            CastParser cp = new CastParser(conn, writer);
            cp.parseDocument();

            System.out.println("Start loading");
            DataLoader loader = new DataLoader();
            loader.loadMovieBatch(1000);
            loader.loadActorBatch(1000);
            loader.loadCastBatch(1000);
            writer.close();


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    protected void loadMovie(){
        ExecutorService executor  = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (Movie movie : Movie.MOVIE_LIST.values()) {
            executor.execute(new MovieQueryTask(movie));
        }
        executor.shutdown();
    }
    protected void loadMovieBatch(int batchSize){
        ArrayList<Movie> movieList = new ArrayList<Movie>(Movie.MOVIE_LIST.values());
        int size = Movie.MOVIE_LIST.size();
        int start = 0;
        int end = Math.min(batchSize, size);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        while(start < size){
            executor.execute(new MovieQueryBatchTask(movieList,start, end));
            start = end;
            end = Math.min(start + batchSize, size);
        }
        executor.shutdown();
        System.out.println("Finished loading movies");
    }
    protected void loadActorBatch(int batchSize){
        ArrayList<Actor> actorList = new ArrayList<Actor>(Actor.NEW_ACTOR_LIST.values());
        int size = Actor.NEW_ACTOR_LIST.size();
        int start = 0;
        int end = Math.min(batchSize, size);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        while(start < size){
            executor.execute(new ActorQueryBatchTask(actorList, start, end));
            start = end;
            end = Math.min(start + batchSize, size);
        }
        executor.shutdown();
        System.out.println("Finished loading actors");
    }
    protected void loadCastBatch(int batchSize){
        int size = Cast.castList.size();
        int start = 0;
        int end = Math.min(batchSize, size);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        while(start < size){
            executor.execute(new CastQueryBatchTask(start, end));
            start = end;
            end = Math.min(start + batchSize, size);
        }
        executor.shutdown();
        System.out.println("Finished loading casts");
    }



}
class MovieQueryTask implements Runnable{
    private Movie movie;

    public MovieQueryTask(Movie movie){
        this.movie = movie;
    }
    protected void loadMovieGenre(Connection conn){
        if(movie.genres.isEmpty()){
            return;
        }
        String query = "INSERT IGNORE INTO genres_in_movies (genreId, movieId) VALUES (?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(query)) {
            for (Genre genre : movie.genres) {
                statement.setInt(1, genre.getId());
                statement.setString(2, movie.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        String query = "INSERT IGNORE INTO movies (id, title, year, director) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "mytestuser", "My6$Password");
             PreparedStatement statement = conn.prepareStatement(query)) {

            statement.setString(1, movie.getId());
            statement.setString(2, movie.getTitle());
            statement.setInt(3, movie.getYear());
            statement.setString(4, movie.directors.get(0).getName());
            statement.executeUpdate();
            loadMovieGenre(conn);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
class MovieQueryBatchTask implements Runnable{
    private int start;
    private int end;
    protected static ArrayList<Movie> movieList;
    public MovieQueryBatchTask(ArrayList<Movie> list, int start, int end){
        movieList = list;
        this.start = start;
        this.end = end;
    }
    protected void loadMovieGenre(Connection conn, Movie movie){
        if(movie.genres.isEmpty()){
            return;
        }
        String query = "INSERT IGNORE INTO genres_in_movies (genreId, movieId) VALUES (?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(query)) {
            for (Genre genre : movie.genres) {
                statement.setInt(1, genre.getId());
                statement.setString(2, movie.getId());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        String query = "INSERT IGNORE INTO movies (id, title, year, director) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "mytestuser", "My6$Password");
             PreparedStatement statement = conn.prepareStatement(query)) {
            for(int i = start; i < end; i++){
                Movie movie = movieList.get(i);
                statement.setString(1, movie.getId());
                statement.setString(2, movie.getTitle());
                statement.setInt(3, movie.getYear());
                statement.setString(4, movie.directors.get(0).getName());
                statement.addBatch();

            }
            statement.executeBatch();
            for(int i = start; i < end; i++){
                Movie movie = movieList.get(i);
                loadMovieGenre(conn, movie);
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
class ActorQueryTask implements Runnable{
    private Actor actor;

    public ActorQueryTask(Actor actor){
        this.actor = actor;
    }

    @Override
    public void run() {
        String query = "INSERT IGNORE INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "mytestuser", "My6$Password");
             PreparedStatement statement = conn.prepareStatement(query)) {

            statement.setString(1, actor.id);
            statement.setString(2, actor.name);
            if(actor.birthYear == ElementTags.NA_IDENTIFIER) {
                statement.setNull(3, java.sql.Types.INTEGER);
            }
            else{
                statement.setInt(3, actor.birthYear);
            }
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
class ActorQueryBatchTask implements Runnable{
    private int start;
    private int end;
    protected static ArrayList<Actor> actorList;
    public ActorQueryBatchTask(ArrayList<Actor> list, int start, int end){
        actorList = list;
        this.start = start;
        this.end = end;
    }
    @Override
    public void run() {
        String query = "INSERT IGNORE INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "mytestuser", "My6$Password");
             PreparedStatement statement = conn.prepareStatement(query)) {
            for(int i = start; i < end; i++){
                Actor actor = actorList.get(i);
                statement.setString(1, actor.id);
                statement.setString(2, actor.name);
                if(actor.birthYear == ElementTags.NA_IDENTIFIER) {
                    statement.setNull(3, java.sql.Types.INTEGER);
                }
                else{
                    statement.setInt(3, actor.birthYear);
                }
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
class CastQueryBatchTask implements Runnable{
    private int start;
    private int end;
    protected static ArrayList<Cast> castList = Cast.castList;
    public CastQueryBatchTask(int start, int end){
        this.start = start;
        this.end = end;
    }
//    CREATE TABLE IF NOT EXISTS stars_in_movies(
//            starId VARCHAR(10) NOT NULL,
//    movieId VARCHAR(10) NOT NULL,
//    FOREIGN KEY (starId) REFERENCES stars(id),
//    FOREIGN KEY (movieId) REFERENCES movies(id)
//            );
    @Override
    public void run() {
        String query = "INSERT IGNORE INTO stars_in_movies (starId, movieId) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "mytestuser", "My6$Password");
             PreparedStatement statement = conn.prepareStatement(query)) {
            for(int i = start; i < end; i++){
                Cast cast = castList.get(i);
                statement.setString(1, cast.actor.id);
                statement.setString(2, cast.movie.id);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}


