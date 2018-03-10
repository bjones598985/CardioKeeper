package com.bolyndevelopment.owner.runlogger2;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;
/**
 * Created by Bobby Jones on 1/30/2018.
 */

public class ListDisplayFragTest {
    static final int ASCENDING = -1;
    static final int DESCENDING = 1;

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
        ComparatorHolder.sortAlphabetic(list, ASCENDING);
        assertArrayEquals(compList.toArray(), list.toArray());
        ComparatorHolder.sortAlphabetic(list, DESCENDING);
        compList.clear();
        compList.add(item3);
        compList.add(item2);
        compList.add(item1);
        assertArrayEquals(compList.toArray(), list.toArray());
        //compList.clear();
        ComparatorHolder.sortByCalories(list, ASCENDING);
        assertArrayEquals(compList.toArray(), list.toArray());
        ComparatorHolder.sortByCalories(list, DESCENDING);
        compList.clear();
        compList.add(item1);
        compList.add(item2);
        compList.add(item3);
        assertArrayEquals(compList.toArray(), list.toArray());
        ComparatorHolder.sortByDate(list, ASCENDING);
        assertArrayEquals(compList.toArray(), list.toArray());
        ComparatorHolder.sortByDate(list, DESCENDING);
        compList.clear();
        compList.add(item3);
        compList.add(item2);
        compList.add(item1);
        assertArrayEquals(compList.toArray(), list.toArray());
        ComparatorHolder.sortByTime(list, ASCENDING);
        compList.clear();
        compList.add(item2);
        compList.add(item1);
        compList.add(item3);
        assertArrayEquals(compList.toArray(), list.toArray());
        ComparatorHolder.sortByTime(list, DESCENDING);
        compList.clear();
        compList.add(item3);
        compList.add(item1);
        compList.add(item2);
        assertArrayEquals(compList.toArray(), list.toArray());
        ComparatorHolder.sortByDistance(list, ASCENDING);
        compList.clear();
        compList.add(item3);
        compList.add(item2);
        compList.add(item1);
        assertArrayEquals(compList.toArray(), list.toArray());
        ComparatorHolder.sortByDistance(list, DESCENDING);
        compList.clear();
        compList.add(item1);
        compList.add(item2);
        compList.add(item3);
        assertArrayEquals(compList.toArray(), list.toArray());

    }

    @Test
    public void dateSort() {
        List<ListItem> list = getFakeList();
        Comparator<ListItem> comp = ComparatorHolder.getDateComparator(DESCENDING);
        Collections.sort(list, comp);
        System.out.println("After:");
        for (ListItem i : list) {
            System.out.println(i.toString());
        }
    }

    static class ComparatorHolder {

        //verified works
        static void sortAlphabetic(List<ListItem> recordsList, final int direction) {
            Collections.sort(recordsList, new Comparator<ListItem>() {
                @Override
                public int compare(ListItem o1, ListItem o2) {
                    if (direction == ASCENDING) {
                        return o1.cType.compareTo(o2.cType);
                    } else {
                        return o2.cType.compareTo(o1.cType);
                    }
                }
            });
        }

        static Comparator<ListItem> getAlphabeticComparator(int direction) {
            if (direction == ASCENDING) {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        return o1.cType.compareTo(o2.cType);
                    }
                };
            } else {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        return o2.cType.compareTo(o1.cType);
                    }
                };
            }
        }

        //needs some work to make dates sort properly
        static Comparator<ListItem> getDateComparator(int direction) {
            if (direction == ASCENDING) {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        return o1.date.compareTo(o2.date);
                    }
                };
            } else {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        return o2.date.compareTo(o1.date);
                    }
                };
            }
        }

        static Comparator<ListItem> getDistanceComparator(int direction) {
            if (direction == ASCENDING) {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        return Math.round(o1.distance - o2.distance);
                    }
                };
            } else {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        return Math.round(o2.distance - o1.distance);
                    }
                };
            }
        }

        static Comparator<ListItem> getTimeComparator(int direction) {
            if (direction == ASCENDING) {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        long t1, t2;
                        t1 = Long.valueOf(Utils.getTimeMillis(o1.time));
                        t2 = Long.valueOf(Utils.getTimeMillis(o2.time));
                        return (int) (t1 - t2);
                    }
                };
            } else {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        long t1, t2;
                        t1 = Long.valueOf(Utils.getTimeMillis(o1.time));
                        t2 = Long.valueOf(Utils.getTimeMillis(o2.time));
                        return (int) (t2 - t1);
                    }
                };
            }
        }

        static Comparator<ListItem> getCalorieComparator(int direction) {
            if (direction == ASCENDING) {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        return o1.calories - o2.calories;
                    }
                };
            } else {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        return o2.calories - o1.calories;
                    }
                };
            }
        }

        //verified works
        static void sortByDate(List<ListItem> recordsList, final int direction) {
            Collections.sort(recordsList, new Comparator<ListItem>() {
                @Override
                public int compare(ListItem o1, ListItem o2) {
                    if (direction == ASCENDING) {
                        return o1.date.compareTo(o2.date);
                    } else {
                        return o2.date.compareTo(o1.date);
                    }
                }
            });
        }
        //verified works
        static void sortByDistance(List<ListItem> recordsList, final int direction) {
            Collections.sort(recordsList, new Comparator<ListItem>(){
                @Override
                public int compare(ListItem o1, ListItem o2) {
                    if (direction == ASCENDING) {
                        return Math.round(o1.distance - o2.distance);
                    } else {
                        return Math.round(o2.distance - o1.distance);
                    }
                }
            });
        }
        //verified works
        static void sortByTime(List<ListItem> recordsList, final int direction) {
            Collections.sort(recordsList, new Comparator<ListItem>() {
                @Override
                public int compare(ListItem o1, ListItem o2) {
                    String t1, t2;
                    t1 = Utils.getTimeMillis(o1.time);
                    t2 = Utils.getTimeMillis(o2.time);
                    if (direction == ASCENDING) {
                        return t1.compareTo(t2);
                    } else {
                        return t2.compareTo(t1);
                    }
                }
            });
        }

        //verified works
        static void sortByCalories(List<ListItem> recordsList, final int direction) {
            Collections.sort(recordsList, new Comparator<ListItem>() {
                @Override
                public int compare(ListItem o1, ListItem o2) {
                    if (direction == ASCENDING) {
                        return o1.calories - o2.calories;
                    } else {
                        return o2.calories - o1.calories;
                    }
                }
            });
        }
    }
}
