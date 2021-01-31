package com.androidexam.printshare;

public class User {

    private String username;
    private String position;

    User(){}

    User(String username, String position){
        this.username = username;
        this.position = position;
    }

    public String getUsername(){return this.username;};
    public String getPosition(){return this.position;};

    public void setUsername(String username){this.username = username;};
    public void setPosition(String position){this.position = position;};
}
