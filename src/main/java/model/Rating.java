package model;

public class Rating {
    private int id;
    private int raterId;
    private int ratedId;
    private Integer sessionId;
    private int score;
    private String comment;

    public Rating() {}

    public Rating(int id, int raterId, int ratedId, Integer sessionId, int score, String comment) {
        this.id = id;
        this.raterId = raterId;
        this.ratedId = ratedId;
        this.sessionId = sessionId;
        this.score = score;
        this.comment = comment;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRaterId() { return raterId; }
    public void setRaterId(int raterId) { this.raterId = raterId; }

    public int getRatedId() { return ratedId; }
    public void setRatedId(int ratedId) { this.ratedId = ratedId; }

    public Integer getSessionId() { return sessionId; }
    public void setSessionId(Integer sessionId) { this.sessionId = sessionId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
