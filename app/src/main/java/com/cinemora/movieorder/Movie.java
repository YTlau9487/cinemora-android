package com.cinemora.movieorder;

import java.util.ArrayList;
import java.util.List;

/**
 * Movie model aligned with Firestore 'movies' collection schema.
 * All time fields stored as Unix timestamps (seconds).
 * All cost fields stored as integers in HKD.
 */
public class Movie {
    private String id;                      // Firestore document ID
    private String movieName;               // Display name
    private int cost;                       // Unit cost in HKD (integer)
    private int rating;                     // Range: 0-5
    private List<String> genres;            // e.g., ["Action", "Adventure"]
    private int duration;                   // Minutes
    private long releaseDate;               // Unix timestamp in seconds
    private String overview;                // Movie description
    private String director;
    private List<String> cast;              // Array of actor names
    private String language;
    private List<String> subtitles;         // Array of subtitle languages
    private String resolution;              // e.g., "1080p"
    private int saleCount;                  // Used for bestseller ranking
    private String posterUrl;               // Image URL
    private long createdAt;                 // Unix timestamp in seconds
    private long updatedAt;                 // Unix timestamp in seconds

    public Movie() {
        // Required for Firestore deserialization
        this.genres = new ArrayList<>();
        this.cast = new ArrayList<>();
        this.subtitles = new ArrayList<>();
    }

    public Movie(String id, String movieName, int cost, int rating, List<String> genres,
                 int duration, long releaseDate, String overview, String director,
                 List<String> cast, String language, List<String> subtitles, String resolution,
                 int saleCount, String posterUrl, long createdAt, long updatedAt) {
        this.id = id;
        this.movieName = movieName;
        this.cost = cost;
        this.rating = rating;
        this.genres = genres != null ? genres : new ArrayList<>();
        this.duration = duration;
        this.releaseDate = releaseDate;
        this.overview = overview;
        this.director = director;
        this.cast = cast != null ? cast : new ArrayList<>();
        this.language = language;
        this.subtitles = subtitles != null ? subtitles : new ArrayList<>();
        this.resolution = resolution;
        this.saleCount = saleCount;
        this.posterUrl = posterUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMovieName() { return movieName; }
    public void setMovieName(String movieName) { this.movieName = movieName; }

    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public long getReleaseDate() { return releaseDate; }
    public void setReleaseDate(long releaseDate) { this.releaseDate = releaseDate; }

    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }

    public List<String> getCast() { return cast; }
    public void setCast(List<String> cast) { this.cast = cast; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public List<String> getSubtitles() { return subtitles; }
    public void setSubtitles(List<String> subtitles) { this.subtitles = subtitles; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public int getSaleCount() { return saleCount; }
    public void setSaleCount(int saleCount) { this.saleCount = saleCount; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // Utility methods for display
    public String getGenresString() {
        if (genres == null || genres.isEmpty()) return "N/A";
        return String.join(", ", genres);
    }

    public String getCastString() {
        if (cast == null || cast.isEmpty()) return "N/A";
        return String.join(", ", cast);
    }

    public String getSubtitlesString() {
        if (subtitles == null || subtitles.isEmpty()) return "N/A";
        return String.join(", ", subtitles);
    }
}
