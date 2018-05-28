/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.tests;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.concordiainternational.competition.data.CategoryLookup;
import org.concordiainternational.competition.data.Lifter;
import org.concordiainternational.competition.spreadsheet.CSVHelper;
import org.concordiainternational.competition.webapp.WebApplicationConfiguration;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.hbnutil.HbnContainer.HbnSessionManager;

/**
 * @author jflamy
 *
 */
public class CSVTest {

    HbnSessionManager hbnSessionManager = AllTests.getSessionManager();
    Logger logger = LoggerFactory.getLogger(CSVTest.class);

    @Before
    public void setupTest() {
        Assert.assertNotNull(hbnSessionManager);
        Assert.assertNotNull(hbnSessionManager.getHbnSession());
        hbnSessionManager.getHbnSession().beginTransaction();
        CategoryLookup categoryLookup = CategoryLookup.getSharedInstance(hbnSessionManager);
        categoryLookup.reload();
    }

    @After
    public void tearDownTest() {
        hbnSessionManager.getHbnSession().close();
    }

    /**
     * @param args
     * @throws Throwable
     */
    @Test
    public void fullBirthDate() throws Throwable {

        HbnSessionManager sessionManager = AllTests.getSessionManager();
        Session hbnSession = sessionManager.getHbnSession();

        boolean isUseBirthYear = WebApplicationConfiguration.isUseBirthYear();
        try {
            WebApplicationConfiguration.setUseBirthYear(false);

            InputStream is = AllTests.class.getResourceAsStream("/testData/fullBirthDate.csv"); //$NON-NLS-1$

            CSVHelper iCSV = new CSVHelper(sessionManager);
            List<Lifter> lifters1 = iCSV.getAllLifters(is, hbnSession);

            assertEquals(2L,(long)lifters1.size());

            Calendar cal = GregorianCalendar.getInstance();
            cal.setTime(lifters1.get(0).getFullBirthDate());
            int month = cal.get(Calendar.MONTH);
            // month is 0-based, subtract 1
            assertEquals(12-1,month);

            System.out.println(AllTests.longDump(lifters1));
        } finally {
            WebApplicationConfiguration.setUseBirthYear(isUseBirthYear);
        }

    }

    @Test
    public void yearOfBirth() throws Throwable {

        HbnSessionManager sessionManager = AllTests.getSessionManager();
        Session hbnSession = sessionManager.getHbnSession();

        boolean isUseBirthYear = WebApplicationConfiguration.isUseBirthYear();
        try {
            WebApplicationConfiguration.setUseBirthYear(true);

            InputStream is = AllTests.class.getResourceAsStream("/testData/yearOfBirth.csv"); //$NON-NLS-1$

            CSVHelper iCSV = new CSVHelper(sessionManager);
            List<Lifter> lifters1 = iCSV.getAllLifters(is, hbnSession);

            assertEquals(2L,(long)lifters1.size());

            Calendar cal = GregorianCalendar.getInstance();
            cal.setTime(lifters1.get(0).getFullBirthDate());
            int month = cal.get(Calendar.MONTH);
            // month is 0-based, subtract 1
            assertEquals(1-1,month);
            int year = cal.get(Calendar.YEAR);
            assertEquals(1961,year);

            System.out.println(AllTests.longDump(lifters1));
        } finally {
            WebApplicationConfiguration.setUseBirthYear(isUseBirthYear);
        }
    }
}
