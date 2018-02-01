package com.bolyndevelopment.owner.runlogger2;

import org.junit.Test;

import java.io.Console;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;
/**
 * Created by Bobby Jones on 1/30/2018.
 */

public class ListDisplayFragTest {

    public List<ListItem> getFakeList() {
        ListItem item1 = new ListItem();
        item1.calories = 500;
        item1.distance = 5.2f;
        item1.date = "2018-01-01";
        item1.cType = "Biking";
        item1.time = "01:00:00";
        ListItem item2 = new ListItem();
        item2.calories = 300;
        item2.distance = 3.0f;
        item2.date = "2018-01-20";
        item2.cType = "Exercise Bike";
        item2.time = "25:15";
        ListItem item3 = new ListItem();
        item3.calories = 100;
        item3.distance = 1.29f;
        item3.date = "2018-10-18";
        item3.cType = "Treadmill";
        item3.time = "01:06:59";
        List<ListItem> list = new ArrayList<>();

        list.add(item1);
        list.add(item2);
        list.add(item3);
        return list;
    }

    @Test
    public void sort() {
        ListItem item1 = new ListItem();
        item1.calories = 500;
        item1.distance = 5.2f;
        item1.date = "2018-01-01";
        item1.cType = "Biking";
        item1.time = "01:00:00";
        ListItem item2 = new ListItem();
        item2.calories = 300;
        item2.distance = 3.0f;
        item2.date = "2018-01-10";
        item2.cType = "Exercise Bike";
        item2.time = "25:15";
        ListItem item3 = new ListItem();
        item3.calories = 100;
        item3.distance = 1.29f;
        item3.date = "2018-01-18";
        item3.cType = "Treadmill";
        item3.time = "01:06:59";
        List<ListItem> list = new ArrayList<>();
        List<ListItem> compList = new ArrayList<>();

        list.add(item1);
        list.add(item2);
        list.add(item3);
        compList.addAll(list);

        for (ListItem item : list) {
            System.out.println("Base: \n" + item.toString());
        }
        ListDisplayFragment.ListSorter.sortAlphabetic(list, ListDisplayFragment.ASCENDING);
        assertArrayEquals(compList.toArray(), list.toArray());
        ListDisplayFragment.ListSorter.sortAlphabetic(list, ListDisplayFragment.DESCENDING);
        compList.clear();
        compList.add(item3);
        compList.add(item2);
        compList.add(item1);
        assertArrayEquals(compList.toArray(), list.toArray());
        //compList.clear();
        ListDisplayFragment.ListSorter.sortByCalories(list, ListDisplayFragment.ASCENDING);
        assertArrayEquals(compList.toArray(), list.toArray());
        ListDisplayFragment.ListSorter.sortByCalories(list, ListDisplayFragment.DESCENDING);
        compList.clear();
        compList.add(item1);
        compList.add(item2);
        compList.add(item3);
        assertArrayEquals(compList.toArray(), list.toArray());
        ListDisplayFragment.ListSorter.sortByDate(list, ListDisplayFragment.ASCENDING);
        assertArrayEquals(compList.toArray(), list.toArray());
        ListDisplayFragment.ListSorter.sortByDate(list, ListDisplayFragment.DESCENDING);
        compList.clear();
        compList.add(item3);
        compList.add(item2);
        compList.add(item1);
        assertArrayEquals(compList.toArray(), list.toArray());
        ListDisplayFragment.ListSorter.sortByTime(list, ListDisplayFragment.ASCENDING);
        compList.clear();
        compList.add(item2);
        compList.add(item1);
        compList.add(item3);
        assertArrayEquals(compList.toArray(), list.toArray());
        ListDisplayFragment.ListSorter.sortByTime(list, ListDisplayFragment.DESCENDING);
        compList.clear();
        compList.add(item3);
        compList.add(item1);
        compList.add(item2);
        assertArrayEquals(compList.toArray(), list.toArray());
        ListDisplayFragment.ListSorter.sortByDistance(list, ListDisplayFragment.ASCENDING);
        compList.clear();
        compList.add(item3);
        compList.add(item2);
        compList.add(item1);
        assertArrayEquals(compList.toArray(), list.toArray());
        ListDisplayFragment.ListSorter.sortByDistance(list, ListDisplayFragment.DESCENDING);
        compList.clear();
        compList.add(item1);
        compList.add(item2);
        compList.add(item3);
        assertArrayEquals(compList.toArray(), list.toArray());

    }

    @Test
    public void dateSort() {
        List<ListItem> list = getFakeList();
        Comparator<ListItem> comp = ListDisplayFragment.ListSorter.getDateComparator(ListDisplayFragment.DESCENDING);
        Collections.sort(list, comp);
        System.out.println("After:");
        for (ListItem i : list) {
            System.out.println(i.toString());
        }
    }

}
