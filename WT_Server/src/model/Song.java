package model;

public class Song {
    private String name;
    private String author;

    public Song(String name, String author) {
        this.name = name;
        this.author = author;
    }

    // getter and setter
    public String getName() { return name;}
    public void setName(String name) { this.name = name;}

    public String getAuthor() { return author;}
    public void setAuthor(String author) { this.author = author;}

    @Override
    public String toString() {
        return this.name + " by " + this.author;
    }
}
