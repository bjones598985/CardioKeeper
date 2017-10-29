package com.bolyndevelopment.owner.runlogger2;

import static org.junit.Assert.*;

/**
 * Created by Bobby Jones on 10/28/2017.
 */
public class UtilsTest {
    @org.junit.Test
    public void convertMillisToHms() throws Exception {
        assertEquals("14 secs", Utils.convertMillisToHms(14000));
    }

}