package com.bolyndevelopment.owner.runlogger2;

/**
 * Created by Bobby Jones on 1/31/2018.
 */

public class ListItem {
    int calories;
    float distance;
    String date = null, time = null, cType;

    @Override
    public String toString() {
        return "cType: " + cType + ", calories: " + calories + ", distance: " +
                distance + ", date: " + date + ", time: " + time;
    }
}
