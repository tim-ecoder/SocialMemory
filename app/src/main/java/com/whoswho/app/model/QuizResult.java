package com.whoswho.app.model;

import java.util.List;
import java.util.ArrayList;

public class QuizResult {
    private int totalQuestions;
    private int correctAnswers;
    private List<Person> mistakePeople;

    public QuizResult() {
        this.mistakePeople = new ArrayList<>();
    }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }

    public List<Person> getMistakePeople() { return mistakePeople; }
    public void setMistakePeople(List<Person> mistakePeople) { this.mistakePeople = mistakePeople; }

    public void addMistake(Person person) {
        if (!mistakePeople.contains(person)) {
            mistakePeople.add(person);
        }
    }

    public int getPercentage() {
        if (totalQuestions == 0) return 0;
        return (int) Math.round(100.0 * correctAnswers / totalQuestions);
    }
}
