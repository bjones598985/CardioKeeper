package com.bolyndevelopment.owner.runlogger2;

import android.text.TextUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

//Created by Bobby Jones on 10/28/2017.

@RunWith(MockitoJUnitRunner.class)
public class UtilsTest {

    @Test
    public void convertDateToString() {
        assertEquals("12/02/2017", Utils.convertDateToString(new Date(), "MM/dd/yyyy"));
    }

    @Test
    public void convertMillisToHms() {
        assertEquals("0:14", Utils.convertMillisToHms(14000));
    }

    @Test
    public void convertStringToDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals(cal.getTime(), Utils.convertStringToDate("12/02/2017", "MM/dd/yyyy"));
    }

    @Test
    public void convertSecondsToHms() {
        assertEquals("0:14", Utils.convertSecondsToHms(14));
    }

    @Test
    public void convertToMillis() {
        assertEquals(14000L, Utils.convertToMillis(0, 0, 14));
    }

    @Test
    public void getTimeStringMillis() {
        assertEquals("14000", Utils.getTimeStringMillis("0:14"));
    }

    @Test
    public void getCardioIcon() {
        assertEquals(R.drawable.bike_cardio, Utils.getCardioIcon("Biking"));
    }

}