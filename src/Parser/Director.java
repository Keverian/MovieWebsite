package Parser;

public class Director {
    private String name;
    public Director(String name){
        if(!name.equals(Integer.toString(ElementTags.NA_IDENTIFIER))){
            this.name = name;
        }

    }
    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }
}
