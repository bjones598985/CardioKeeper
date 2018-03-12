package com.q29ideas.cardiokeeper;

public class ListItem {
    int calories;
    float distance;
    String date = null, time = null, cType;
    private int itemId;

    private static int id = 0;

    ListItem() {
        itemId = ++id;
    }

    public int getId() {
        return itemId;
    }

    @Override
    public String toString() {
        return "cType: " + cType + ", calories: " + calories + ", distance: " +
                distance + ", date: " + date + ", time: " + time;
    }


}
