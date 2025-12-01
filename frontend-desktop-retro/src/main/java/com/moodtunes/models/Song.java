package com.moodtunes.models;

/**
 * Represents a song with its metadata
 */
public class Song {
    
    private String id;
    private String title;
    private String artist;
    private String duration;
    private String url;
    
    public Song(String id, String title, String artist, String duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.url = null;
    }
    
    public Song(String id, String title, String artist, String duration, String url) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.url = url;
    }
    
    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getDuration() { return duration; }
    public String getUrl() { return url; }
    
    // Setters
    public void setUrl(String url) { this.url = url; }
    
    @Override
    public String toString() {
        return title + " - " + artist;
    }
}
