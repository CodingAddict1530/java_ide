package com.project.utility;

public class LanguageStatusParams {

    private String type;
    private String message;

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "LanguageStatusParams{" +
                "type='" + type + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

}
