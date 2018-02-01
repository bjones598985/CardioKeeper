package com.bolyndevelopment.owner.runlogger2;

/**
 * Created by Bobby Jones on 1/31/2018.
 */

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
