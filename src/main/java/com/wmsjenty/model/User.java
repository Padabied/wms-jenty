package com.wmsjenty.model;

public class User {
    private int id;

    public User(int id, String name, String login, String role) {
        this.id = id;
        this.name = name;
        this.login = login;
        this.role = role;
    }

    private String name;
    private String login;
    private String role;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
