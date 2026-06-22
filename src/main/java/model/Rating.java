package model;

public class Rating {
    private int id;
    private int raterId;
    private int ratedId;
    private Integer sessionId;
    private int score;
    private String comment;

    public Rating() {}

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
