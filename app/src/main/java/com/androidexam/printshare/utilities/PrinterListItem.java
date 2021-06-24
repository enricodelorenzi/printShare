package com.androidexam.printshare.utilities;

public class PrinterListItem {

    private String id;
    String content;
    boolean edit;

    public PrinterListItem(String id, String content, boolean edit) {
        this.content = content;
        this.edit = edit;
        this.id = id;
    }

    public String getId() {return  this.id;}

    public String getContent() {
        return content;
    }

    public void setContent(String printer_model) {
        this.content = printer_model;
    }

    public boolean isEdit() {
        return edit;
    }

    public void setEdit(boolean edit) {
        this.edit = edit;
    }
}
