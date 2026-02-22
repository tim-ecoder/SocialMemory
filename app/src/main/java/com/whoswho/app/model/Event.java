package com.whoswho.app.model;

public class Event {
    private long id;
    private String title;
    private long date;
    private String description;
    private long createdAt;
    private int personCount;

    public Event() {
        this.createdAt = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getPersonCount() { return personCount; }
    public void setPersonCount(int personCount) { this.personCount = personCount; }
}
