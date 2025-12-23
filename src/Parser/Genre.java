package Parser;
import com.google.gson.JsonObject;
import com.mysql.cj.protocol.Resultset;

import java.sql.*;
import java.util.HashMap;
import java.util.Hashtable;
import javax.sql.DataSource;
import javax.naming.InitialContext;
import java.util.ArrayList;


public class Genre {
    protected String name;
    protected Integer id;
    protected static DataSource dataSource;
    protected static final Hashtable<String, Genre> CODE_TO_GENRE = new Hashtable<>();
    protected static final Hashtable<String, String> GENRE_NAME_TO_CODE = new Hashtable<>();

    public Genre(){
        name = "";
        id = 0;
    }
    public Genre(String name){
        this.name = name;
        this.id = 0;
    }
    public Genre(String name, Integer id){
        this.name = name;
        this.id = id;
    }
    protected void setId(Integer id){
        this.id = id;
    }
    protected String getName(){
        return this.name;
    }
    protected Integer getId(){
        return this.id;
    }
    static{
        initializeMap();
        getDataSource();
        getInfoFromDatabase();

    }
    protected static void initializeMap(){
        //initialize CODE_TO_CATEGORY_MAP
        CODE_TO_GENRE.put(cleanKey("Susp"), new Genre("Thriller"));
        CODE_TO_GENRE.put(cleanKey("CnR"), new Genre("Crime"));
        CODE_TO_GENRE.put(cleanKey("Dram"), new Genre("Drama"));
        CODE_TO_GENRE.put(cleanKey("West"), new Genre("Western"));
        CODE_TO_GENRE.put(cleanKey("Myst"), new Genre("Mystery"));
        CODE_TO_GENRE.put(cleanKey("S.F."), new Genre("Sci-Fi"));
        CODE_TO_GENRE.put(cleanKey("Advt"), new Genre("Adventure"));
        CODE_TO_GENRE.put(cleanKey("Horr"), new Genre("Horror"));
        CODE_TO_GENRE.put(cleanKey("Romt"), new Genre("Romance"));
        CODE_TO_GENRE.put(cleanKey("Comd"), new Genre("Comedy"));
        CODE_TO_GENRE.put(cleanKey("Musc"), new Genre("Musical"));
        CODE_TO_GENRE.put(cleanKey("Docu"), new Genre("Documentary"));
        CODE_TO_GENRE.put(cleanKey("Porn"), new Genre("Adult"));
        CODE_TO_GENRE.put(cleanKey("Noir"), new Genre("Black"));
        CODE_TO_GENRE.put(cleanKey("BioP"), new Genre("Biography"));
        CODE_TO_GENRE.put(cleanKey("TV"), new Genre("TV-show"));
        CODE_TO_GENRE.put(cleanKey("TVs"), new Genre("TV-series"));
        CODE_TO_GENRE.put(cleanKey("TVm"), new Genre("TV-miniseries"));
       // CODE_TO_GENRE.put(cleanKey("N/A"), new Genre("N/A"));

        // Initialize the GENRE_NAME_TO_CODE map with genre names to codes
        GENRE_NAME_TO_CODE.put(cleanKey("Thriller"), "Susp");
        GENRE_NAME_TO_CODE.put(cleanKey("Crime"), "CnR");
        GENRE_NAME_TO_CODE.put(cleanKey("Drama"), "Dram");
        GENRE_NAME_TO_CODE.put(cleanKey("Western"), "West");
        GENRE_NAME_TO_CODE.put(cleanKey("Mystery"), "Myst");
        GENRE_NAME_TO_CODE.put(cleanKey("Sci-Fi"), "S.F.");
        GENRE_NAME_TO_CODE.put(cleanKey("Adventure"), "Advt");
        GENRE_NAME_TO_CODE.put(cleanKey("Horror"), "Horr");
        GENRE_NAME_TO_CODE.put(cleanKey("Romance"), "Romt");
        GENRE_NAME_TO_CODE.put(cleanKey("Comedy"), "Comd");
        GENRE_NAME_TO_CODE.put(cleanKey("Musical"), "Musc");
        GENRE_NAME_TO_CODE.put(cleanKey("Documentary"), "Docu");
        GENRE_NAME_TO_CODE.put(cleanKey("Black"), "Noir");
        GENRE_NAME_TO_CODE.put(cleanKey("Adult"), "Porn");
        GENRE_NAME_TO_CODE.put(cleanKey("Biography"), "BioP");
        GENRE_NAME_TO_CODE.put(cleanKey("TV-show"), "TV");
        GENRE_NAME_TO_CODE.put(cleanKey("TV-series"), "TVs");
        GENRE_NAME_TO_CODE.put(cleanKey("TV-miniseries"), "TVm");
        //GENRE_NAME_TO_CODE.put(cleanKey("N/A"), "N/A");

    }
    @Override
    public String toString() {
        return "Genre{id=" + id + ", name='" + name + "'}";
    }

    //retrieve id from database for genre that exist in database, insert new genre for non-existing genre
    protected static void getDataSource(){
        try {
//            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void getInfoFromDatabase(){
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "mytestuser", "My6$Password");){
            HashMap<String, Integer> existingGenreNameIdMap = getGenreNameIdFromDatabase(conn);
            ArrayList<Genre> notExistInDatabaseGenreList = setKeyToExistingGenreFindNotExistingGenres(existingGenreNameIdMap);
            Connection connTest = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "mytestuser", "My6$Password");
            insertNotExistGenreToDatabase(connTest, notExistInDatabaseGenreList);
            connTest.close();
            conn.close();
        }
        catch (Exception e) {

            e.printStackTrace();
        }
    }

    protected static void insertNotExistGenreToDatabase(Connection conn, ArrayList<Genre> notExistInDatabaseGenreList) throws SQLException{
        String query = "INSERT INTO genres (name) VALUES (?)";
        PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        for(Genre curGenre : notExistInDatabaseGenreList){
            statement.setString(1, curGenre.getName());
            statement.executeUpdate();
            ResultSet generatedKeys = statement.getGeneratedKeys();

            if(generatedKeys.next()){
                String code = GENRE_NAME_TO_CODE.get(cleanKey(curGenre.getName()));
                Genre curGenreInMap = CODE_TO_GENRE.get(cleanKey(code));
                curGenreInMap.setId(generatedKeys.getInt(1));
            }
            generatedKeys.close();
        }
        statement.close();
    }

    protected static ArrayList<Genre> setKeyToExistingGenreFindNotExistingGenres(HashMap<String, Integer> databaseExistGenreMap){
        ArrayList<Genre> notExistInDatabaseGenreList = new ArrayList<>();
        for (String genreName : GENRE_NAME_TO_CODE.keySet()) {
            String curGenreCode = GENRE_NAME_TO_CODE.get(cleanKey(genreName));
            Genre curGenre = CODE_TO_GENRE.get(cleanKey(curGenreCode));
            if (!databaseExistGenreMap.containsKey(cleanKey(genreName))) {
                notExistInDatabaseGenreList.add(curGenre);
            }
            else{
                int genreId = databaseExistGenreMap.get(cleanKey(genreName));
                curGenre.setId(genreId);
            }
        }
        return notExistInDatabaseGenreList;
    }

    protected static HashMap<String, Integer> getGenreNameIdFromDatabase(Connection conn) throws SQLException{
        String query = "SELECT id, name FROM genres";
        PreparedStatement statement = conn.prepareStatement(query);
        ResultSet rs = statement.executeQuery();
        HashMap<String, Integer> rv = new HashMap<>();
        while (rs.next()) {
            rv.put(cleanKey(rs.getString("name")), rs.getInt("id"));
        }
        rs.close();
        statement.close();
        return rv;

    }

    public static String cleanKey(String key) {
        // Remove spaces, lowercase everything, and remove everything that is not a letter
        return key.replaceAll("[^a-zA-Z]", "").toLowerCase();
    }
    public static void printGenres(){
        for (String key : CODE_TO_GENRE.keySet()) {
            Genre genre = CODE_TO_GENRE.get(key);
            System.out.println("Key: " + key + ", Genre: " + genre);
        }
    }

}
