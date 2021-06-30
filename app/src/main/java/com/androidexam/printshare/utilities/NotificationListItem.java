package com.androidexam.printshare.utilities;

public class NotificationListItem {

    private String type;
    private String from_user;
    private String content;

    public NotificationListItem(String type, String from_user, String content) {
        this.type = type;
        this.from_user = from_user;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public String getFrom_user() {
        return from_user;
    }

    public String getContent(){return content;}

}
