/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.webapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.concordiainternational.competition.data.Category;
import org.concordiainternational.competition.data.Competition;
import org.concordiainternational.competition.data.CompetitionSession;
import org.concordiainternational.competition.data.Lifter;
import org.concordiainternational.competition.data.Platform;
import org.concordiainternational.competition.decision.Speakers;
import org.concordiainternational.competition.i18n.LocalizedApplication;
import org.concordiainternational.competition.i18n.Messages;
import org.concordiainternational.competition.utils.LoggerUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.event.def.OverrideMergeEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.hbnutil.HbnContainer.HbnSessionManager;

/**
 * @author jflamy Called when the application is started to initialize the database and other global features (serial communication ports)
 */

public class WebApplicationConfiguration implements HbnSessionManager, ServletContextListener {
    private static Logger logger = LoggerFactory.getLogger(WebApplicationConfiguration.class);

    private static SessionFactory sessionFactory = null;
    private static final boolean TEST_MODE = false;

    public static final boolean ShowLifterImmediately = true;

    public static final boolean DEFAULT_STICKINESS = true;

    private static AnnotationConfiguration cnf;

    private static boolean useCategorySinclair = false;

    private static boolean useRegistrationCategory = false;

    private static boolean useBirthYear = false;

    private static boolean useBrowserLanguage = true;

    private static String localeString = "en_US";

    private static boolean useOld20_15rule = false;

    private static boolean useOldBodyWeightTieBreak = false;

    /**
     * this constructor sets the default values if the full parameterized constructor has not been called first (which is normally the
     * case). If the full constructor has been called first, then the existing factory is returned.
     *
     * @return a Hibernate session factory
     */
    public static SessionFactory getSessionFactory() {
        // this call sets the default values if the parameterized constructor
        // has not
        // been called first (which is normally the case). If the full
        // constructor
        // has been called first, then the existing factory is returned.
        if (sessionFactory != null)
            return sessionFactory;
        else
            throw new RuntimeException("should have called getSessionFactory(testMode,dbPath) first."); //$NON-NLS-1$
    }

    /**
     * Full constructor, normally invoked first.
     *
     * @param testMode
     *            true if the database runs in memory, false is there is a physical database
     * @param dbPath
     * @return
     */
    public static SessionFactory getSessionFactory(boolean testMode, String dbPath) {
        if (sessionFactory == null) {
            try {
                cnf = new AnnotationConfiguration();
                // derbySetup(testMode, dbPath, cnf);
                h2Setup(testMode, dbPath, cnf);
                cnf.setProperty(Environment.USER, "sa"); //$NON-NLS-1$
                cnf.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread"); //$NON-NLS-1$

                // the classes we store in the database.
                cnf.addAnnotatedClass(Lifter.class);
                cnf.addAnnotatedClass(CompetitionSession.class);
                cnf.addAnnotatedClass(Platform.class);
                cnf.addAnnotatedClass(Category.class);
                cnf.addAnnotatedClass(Competition.class);
                cnf.addAnnotatedClass(CompetitionSession.class);

                // cnf.setProperty("hibernate.cache.provider_class", "org.hibernate.cache.EhCacheProvider"); //$NON-NLS-1$

                cnf.setProperty("hibernate.cache.region.factory_class", "net.sf.ehcache.hibernate.SingletonEhCacheRegionFactory"); //$NON-NLS-1$ //$NON-NLS-2$
                cnf.setProperty("hibernate.cache.use_second_level_cache", "true"); //$NON-NLS-1$ //$NON-NLS-2$
                cnf.setProperty("hibernate.cache.use_query_cache", "true"); //$NON-NLS-1$ //$NON-NLS-2$

                // cnf.setProperty(Environment.CACHE_PROVIDER,"org.hibernate.cache.HashtableCacheProvider");

                // the following line is necessary because the Lifter class uses
                // the Lift class several times (one for each lift), which would normally force
                // us to override the column names to ensure they are unique. Hibernate
                // supports this with an extension.
                // cnf.setNamingStrategy(DefaultComponentSafeNamingStrategy.INSTANCE);

                // listeners
                cnf.setListener("merge", new OverrideMergeEventListener()); //$NON-NLS-1$

                sessionFactory = cnf.buildSessionFactory();
                // create the standard categories, etc.
                // if argument is > 0, create sample data as well.
                Session sess = sessionFactory.getCurrentSession();
                sess.beginTransaction();
                Category.insertStandardCategories(sess);
                insertInitialData(5, sess, testMode);
                sess.flush();
                sess.close();
            } catch (Throwable ex) {
                // Make sure you log the exception, as it might be swallowed
                ex.printStackTrace(System.err);
                throw new ExceptionInInitializerError(ex);
            }
        }

        return sessionFactory;
    }

    /**
     * @param testMode
     * @param dbPath
     * @param cnf1
     * @throws IOException
     */
    private static void h2Setup(boolean testMode, String dbPath, AnnotationConfiguration cnf1) throws IOException {
        cnf1.setProperty(Environment.DRIVER, "org.h2.Driver"); //$NON-NLS-1$
        if (testMode) {
            String string = "jdbc:h2:mem:competition;MVCC=TRUE";
            cnf1.setProperty(Environment.URL, string); //$NON-NLS-1$
            cnf1.setProperty(Environment.SHOW_SQL, "false"); //$NON-NLS-1$
            cnf1.setProperty(Environment.HBM2DDL_AUTO, "create-drop"); //$NON-NLS-1$
            logger.info("running in test mode: {}",string);
        } else {
            File file = new File(dbPath).getParentFile(); //$NON-NLS-1$
            if (!file.exists()) {
                boolean status = file.mkdirs();
                if (!status) {
                    throw new RuntimeException("could not create directories for " + dbPath);
                }
            }

            cnf1.setProperty(Environment.SHOW_SQL, "false"); //$NON-NLS-1$
            // cnf1.setProperty(Environment.URL, "jdbc:h2:file:" + dbPath + ";MVCC=TRUE;TRACE_LEVEL_FILE=4"); //$NON-NLS-1$ //$NON-NLS-2$
            cnf1.setProperty(Environment.URL, "jdbc:h2:file:" + dbPath + ";MVCC=TRUE"); //$NON-NLS-1$ //$NON-NLS-2$
            String ddlMode = "create"; //$NON-NLS-1$

            // check legacy format first
            file = new File(dbPath + ".h2.db"); //$NON-NLS-1$
            if (file.exists()) {
                ddlMode = "update"; //$NON-NLS-1$
            } else {
                // try new format if present
                file = new File(dbPath + ".mv.db"); //$NON-NLS-1$
                if (file.exists()) {
                    ddlMode = "update"; //$NON-NLS-1$
                }
            }
            logger.warn(
                    "Using Hibernate mode {} (file {} exists={}", new Object[] { ddlMode, file.getAbsolutePath(), Boolean.toString(file.exists()) }); //$NON-NLS-1$
            cnf1.setProperty(Environment.HBM2DDL_AUTO, ddlMode);
            // throw new
            // ExceptionInInitializerError("Production database configuration not specified");
        }
        cnf1.setProperty(Environment.DIALECT, H2Dialect.class.getName());
    }

    /**
     * @param testMode
     * @param dbPath
     * @param cnf1
     */
    @SuppressWarnings("unused")
    private static void derbySetup(boolean testMode, String dbPath, AnnotationConfiguration cnf1) {
        if (testMode) {
            cnf1.setProperty(Environment.DRIVER, "org.h2.Driver"); //$NON-NLS-1$
            cnf1.setProperty(Environment.URL, "jdbc:h2:mem:competition"); //$NON-NLS-1$
            cnf1.setProperty(Environment.SHOW_SQL, "false"); //$NON-NLS-1$
            cnf1.setProperty(Environment.HBM2DDL_AUTO, "create-drop"); //$NON-NLS-1$
        } else {
            cnf1.setProperty(Environment.DRIVER, "org.apache.derby.jdbc.EmbeddedDriver"); //$NON-NLS-1$
            cnf1.setProperty(Environment.SHOW_SQL, "false"); //$NON-NLS-1$

            String ddlMode = "create"; //$NON-NLS-1$
            String suffix = ";create=true";

            File file = new File(dbPath); //$NON-NLS-1$
            if (file.exists()) {
                ddlMode = "update"; //$NON-NLS-1$
            } else {
                file = new File(dbPath); //$NON-NLS-1$
                if (file.exists()) {
                    ddlMode = "update"; //$NON-NLS-1$
                }
            }
            logger.info(
                    "Using Hibernate mode {} (file {} exists={}",
                    new Object[] { ddlMode, file.getAbsolutePath(), Boolean.toString(file.exists()) }); //$NON-NLS-1$
            cnf1.setProperty(Environment.HBM2DDL_AUTO, ddlMode);
            cnf1.setProperty(Environment.URL, "jdbc:derby:" + dbPath + suffix); //$NON-NLS-1$
            // throw new ExceptionInInitializerError("Production database configuration not specified");
        }
        cnf1.setProperty(Environment.DIALECT, DerbyDialect.class.getName());
    }

    /**
     * Insert initial data if the database is empty.
     *
     * @param liftersToLoad
     * @param sess
     * @param testMode
     */
    public static void insertInitialData(int liftersToLoad, org.hibernate.Session sess, boolean testMode) {
        if (sess.createCriteria(CompetitionSession.class).list().size() == 0) {
            // empty database
            Competition competition = new Competition();

            competition.setCompetitionName          (Messages.getString("Competition.competitionName", getLocale())+" ?");
            competition.setCompetitionCity          (Messages.getString("Competition.competitionCity", getLocale())+" ?");
            competition.setCompetitionDate          (Calendar.getInstance().getTime());
            competition.setCompetitionOrganizer     (Messages.getString("Competition.competitionOrganizer", getLocale())+" ?");
            competition.setCompetitionSite          (Messages.getString("Competition.competitionSite", getLocale())+" ?");

            String federationLabel = Messages.getString("Competition.federation", getLocale())+" ?";
            String defaultFederationKey = "Competition.defaultFederation";
            String defaultFederation = Messages.getString(defaultFederationKey, getLocale());
            // if string is not translated, we get its key back.
            competition.setFederation(defaultFederation.equals(defaultFederationKey) ? federationLabel : defaultFederation );

            String federationAddressLabel = Messages.getString("Competition.federationAddress", getLocale())+" ?";
            String defaultFederationAddressKey = "Competition.defaultFederationAddress";
            String defaultFederationAddress = Messages.getString(defaultFederationAddressKey, getLocale());
            // if string is not translated, we get its key back.
            competition.setFederationAddress(defaultFederationAddress.equals(defaultFederationAddressKey) ? federationAddressLabel : defaultFederationAddress );

            String federationEMailLabel = Messages.getString("Competition.federationEMail", getLocale())+" ?";
            String defaultFederationEMailKey = "Competition.defaultFederationEMail";
            String defaultFederationEMail = Messages.getString(defaultFederationEMailKey, getLocale());
            // if string is not translated, we get its key back.
            competition.setFederationEMail(defaultFederationEMail.equals(defaultFederationEMailKey) ? federationEMailLabel : defaultFederationEMail );

            String federationWebSiteLabel = Messages.getString("Competition.federationWebSite", getLocale())+" ?";
            String defaultFederationWebSiteKey = "Competition.defaultFederationWebSite";
            String defaultFederationWebSite = Messages.getString(defaultFederationWebSiteKey, getLocale());
            // if string is not translated, we get its key back.
            competition.setFederationWebSite(defaultFederationWebSite.equals(defaultFederationWebSiteKey) ? federationWebSiteLabel : defaultFederationWebSite );

            Calendar w = Calendar.getInstance();
            w.set(Calendar.MILLISECOND, 0);
            w.set(Calendar.SECOND, 0);
            w.set(Calendar.MINUTE, 0);
            w.set(Calendar.HOUR_OF_DAY, 8);
            Calendar c = (Calendar) w.clone();
            c.add(Calendar.HOUR_OF_DAY, 2);

            if (testMode) {
                setupTestData(competition, liftersToLoad, sess, w, c);
            } else {
                setupEmptyCompetition(competition, sess);
            }

            sess.save(competition);
        } else {
            // database contains data, leave it alone.
        }

    }

    /**
     * Create an empty competition. Set-up the defaults for using the timekeeping and refereeing features.
     *
     * @param competition
     *
     * @param sess
     */
    protected static void setupEmptyCompetition(Competition competition, org.hibernate.Session sess) {
        Platform platform1 = new Platform("Platform"); //$NON-NLS-1$
        setDefaultMixerName(platform1);
        platform1.setShowDecisionLights(true);
        platform1.setShowTimer(true);
        // collar
        platform1.setNbC_2_5(1);
        // small plates
        platform1.setNbS_0_5(1);
        platform1.setNbS_1(1);
        platform1.setNbS_1_5(1);
        platform1.setNbS_2(1);
        platform1.setNbS_2_5(1);
        // large plates, regulation set-up
        platform1.setNbL_2_5(0);
        platform1.setNbL_5(0);
        platform1.setNbL_10(1);
        platform1.setNbL_15(1);
        platform1.setNbL_20(1);
        platform1.setNbL_25(1);

        // competition template
        File templateFile;
        String defaultLanguage = getDefaultLanguage();
        String templateName;
        if (!defaultLanguage.equals("fr")) {
            templateName = "/templates/protocolSheet/ProtocolSheetTemplate_" + defaultLanguage + ".xls";
        } else {
            // historical kludge for Québec
            templateName = "/templates/protocolSheet/Quebec_" + defaultLanguage + ".xls";
        }
        URL templateUrl = platform1.getClass().getResource(templateName);
        try {
            templateFile = new File(templateUrl.toURI());
            competition.setProtocolFileName(templateFile.getCanonicalPath());
        } catch (URISyntaxException e) {
            templateFile = new File(templateUrl.getPath());
        } catch (IOException e) {
        } catch (Exception e) {
            logger.debug("templateName = {}",templateName);
        }

        // competition book template
        templateUrl = platform1.getClass().getResource(
                "/templates/competitionBook/CompetitionBook_Total_" + defaultLanguage + ".xls");
        try {
            templateFile = new File(templateUrl.toURI());
            competition.setResultTemplateFileName(templateFile.getCanonicalPath());
        } catch (URISyntaxException e) {
            templateFile = new File(templateUrl.getPath());
        } catch (IOException e) {
        } catch (Exception e) {
            logger.debug("templateUrl = {}",templateUrl);
        }

        sess.save(platform1);

        CompetitionSession group;
        group = new CompetitionSession("M1", null, null); //$NON-NLS-1$
        sess.save(group);
        group = new CompetitionSession("M2", null, null); //$NON-NLS-1$#
        sess.save(group);
        group = new CompetitionSession("M3", null, null); //$NON-NLS-1$
        sess.save(group);
        group = new CompetitionSession("M4", null, null); //$NON-NLS-1$
        sess.save(group);
        group = new CompetitionSession("F1", null, null); //$NON-NLS-1$
        sess.save(group);
        group = new CompetitionSession("F2", null, null); //$NON-NLS-1$
        sess.save(group);
        group = new CompetitionSession("F3", null, null); //$NON-NLS-1$
        sess.save(group);

    }

    private static String getDefaultLanguage() {
        // default language as defined in properties file (not the JVM).
        // this will typically be en.
        return getLocale().getLanguage();
    }

    /**
     * @param competition
     * @param liftersToLoad
     * @param sess
     * @param w
     * @param c
     */
    protected static void setupTestData(Competition competition, int liftersToLoad,
            org.hibernate.Session sess, Calendar w, Calendar c) {
        Platform platform1 = new Platform("Gym 1"); //$NON-NLS-1$
        sess.save(platform1);
        Platform platform2 = new Platform("Gym 2"); //$NON-NLS-1$
        sess.save(platform1);
        sess.save(platform2);
        CompetitionSession groupA = new CompetitionSession("A", w.getTime(), c.getTime()); //$NON-NLS-1$
        groupA.setPlatform(platform1);
        CompetitionSession groupB = new CompetitionSession("B", w.getTime(), c.getTime()); //$NON-NLS-1$
        groupB.setPlatform(platform2);
        CompetitionSession groupC = new CompetitionSession("C", w.getTime(), c.getTime()); //$NON-NLS-1$
        groupC.setPlatform(platform1);
        sess.save(groupA);
        sess.save(groupB);
        sess.save(groupC);
        insertSampleLifters(liftersToLoad, sess, groupA, groupB, groupC);
    }

    /**
     * @param platform1
     */
    protected static void setDefaultMixerName(Platform platform1) {
        String mixerName = null;
        try {
            mixerName = Speakers.getOutputNames().get(0);
            platform1.setMixerName(mixerName);
        } catch (Exception e) {
            // leave mixerName null
        }
    }

    private static void insertSampleLifters(int liftersToLoad, org.hibernate.Session sess, CompetitionSession groupA,
            CompetitionSession groupB,
            CompetitionSession groupC) {
        final String[] fnames = { "Peter", "Albert", "Joshua", "Mike", "Oliver", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                "Paul", "Alex", "Richard", "Dan", "Umberto", "Henrik", "Rene", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
                "Fred", "Donald" }; //$NON-NLS-1$ //$NON-NLS-2$
        final String[] lnames = { "Smith", "Gordon", "Simpson", "Brown", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                "Clavel", "Simons", "Verne", "Scott", "Allison", "Gates", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
                "Rowling", "Barks", "Ross", "Schneider", "Tate" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        Random r = new Random(0);

        for (int i = 0; i < liftersToLoad; i++) {
            Lifter p = new Lifter();
            p.setCompetitionSession(groupA);
            p.setFirstName(fnames[r.nextInt(fnames.length)]);
            p.setLastName(lnames[r.nextInt(lnames.length)]);
            p.setBodyWeight(69.0D);
            sess.save(p);
            // System.err.println("group A - "+InputSheetHelper.toString(p));
        }
        for (int i = 0; i < liftersToLoad; i++) {
            Lifter p = new Lifter();
            p.setCompetitionSession(groupB);
            p.setFirstName(fnames[r.nextInt(fnames.length)]);
            p.setLastName(lnames[r.nextInt(lnames.length)]);
            p.setBodyWeight(69.0D);
            sess.save(p);
            // System.err.println("group B - "+InputSheetHelper.toString(p));
        }
        sess.flush();
    }

    private static Properties configProperties = null ;

    private static String homePath;

    private static Locale locale;

    private static boolean autoLoadLifterEditor;

    private ServletContext servletContext;



    /*
     * We implement HbnSessionManager as a convenience; when a domain class needs access to persistance, and we don't want to pass in
     * another HbnSessionManager such as the application, we use this one. (non-Javadoc)
     *
     * @see com.vaadin.data.hbnutil.HbnContainer.HbnSessionManager#getHbnSession()
     */
    @Override
    public Session getHbnSession() {
        return getSessionFactory().getCurrentSession();
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        WebApplicationConfiguration.getSessionFactory().close();
        h2Shutdown();
        logger.debug("contextDestroyed() done"); //$NON-NLS-1$
    }

    /**
     * Try to shutdown H2 cleanly.
     */
    private void h2Shutdown() {
        Connection connection = null;
        try {
            connection = cnf.buildSettings().getConnectionProvider().getConnection();
            Statement stmt = connection.createStatement();
            try {
                stmt.execute("SHUTDOWN"); //$NON-NLS-1$
            } finally {
                stmt.close();
            }
        } catch (HibernateException e) {
            LoggerUtils.infoException(logger, e);
        } catch (SQLException e) {
            LoggerUtils.infoException(logger, e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    LoggerUtils.infoException(logger, e);
                }
            }
            // This manually deregisters JDBC driver, which prevents Tomcat 7 from complaining about memory leaks
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                try {
                    DriverManager.deregisterDriver(driver);
                    logger.debug("deregistering jdbc driver: {}", driver);
                } catch (SQLException e) {
                   logger.warn("Error deregistering driver {}: {}", driver, e);
                }

            }
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sCtxE) {
        setServletContext(sCtxE.getServletContext());
        initHomePath(getServletContext());
        initConfigProperties();
        initLocaleString(getServletContext());
        initSessionFactory();

        setUseBrowserLanguage(getBooleanConfigParameter(getServletContext(), "useBrowserLanguage", "Using browser language", true));
        setUseCategorySinclair(getBooleanConfigParameter(getServletContext(), "useCategorySinclair", "Using Sinclairs computed at category weight for Sinclair rankings", false));
        setUseRegistrationCategory(getBooleanConfigParameter(getServletContext(), "useRegistrationCategory", "Using registration category to compute rankings.", false));
        setUseBirthYear(getBooleanConfigParameter(getServletContext(), "useBirthYear", "Using birth year only instead of full date.", false));
        setAutoLoadLifterEditor(getBooleanConfigParameter(getServletContext(), "autoLoadLifterEditor", "Automatic display of lifter card", true));
        setUseOld20_15rule(getBooleanConfigParameter(getServletContext(), "useOld20_15Rule", "Using old 15/20kg rule", false));
        setUseOldBodyWeightTieBreak(getBooleanConfigParameter(getServletContext(), "useOldBodyWeightTieBreakRule", "Using old bodyweight tiebreaking rule", false));


        Locale.setDefault(LocalizedApplication.getLocaleFromString(getLocaleString()));
        logger.debug("Default JVM Locale set to: {}", Locale.getDefault()); //$NON-NLS-1$

        logger.debug("contextInitialized() done"); //$NON-NLS-1$
    }

    private void initSessionFactory() {
        String dbPath = getStringConfigParameter(getServletContext(),"dbPath", "db/competition"); //$NON-NLS-1$
        File dbFile;
        if (dbPath != null) {
            dbFile = new File(dbPath);
            if (!dbFile.isAbsolute()) {
                dbFile = new File(getHomePath(), dbPath);
            }
        } else {
            dbFile = new File(getHomePath(), "db/" + getServletContext().getServletContextName());
        }
        WebApplicationConfiguration.getSessionFactory(TEST_MODE, dbFile.getAbsolutePath()).getCurrentSession();
    }

    private void initConfigProperties() {
        setConfigProperties(new Properties());
        try {
            getConfigProperties().load(new FileReader(new File(getHomePath(),"owlcms.properties")));
        } catch (FileNotFoundException e) {
            //logger.error(e.getLocalizedMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    private static boolean getBooleanConfigParameter(ServletContext sCtx, String parameterName, String warning, boolean defaultVal) {
        String propertyName = "owlcms."+parameterName;
        String stringVal = null;

        {
            String stringVal2 = System.getProperty(propertyName);
            if (stringVal2 != null) stringVal = stringVal2;
        }
        if (sCtx != null) {
            String stringVal1 = sCtx.getInitParameter(parameterName);
            if (stringVal1 != null) stringVal = stringVal1;
        }
        if (getConfigProperties() != null) {
            String stringVal3 = (String)getConfigProperties().get(propertyName);
            if (stringVal3 != null) stringVal = stringVal3;
        }


        if (stringVal != null) {
            boolean booleanVal = Boolean.parseBoolean(stringVal);
            if (booleanVal != defaultVal) {
                logger.info("{} : {}",warning, booleanVal);
            }
            return booleanVal;
        } else {
            return defaultVal;
        }

    }

    private static String getStringConfigParameter(ServletContext sCtx, String parameterName, String defaultVal) {
        String propertyName = "owlcms."+parameterName;
        String stringVal = null;

        {
            String stringVal2 = System.getProperty(propertyName);
            if (stringVal2 != null) {
                logger.debug("property {} read from system property = {}",propertyName,stringVal2);
                stringVal = stringVal2;
            }
        }
        if (sCtx != null) {
            String stringVal1 = sCtx.getInitParameter(parameterName);
            if (stringVal1 != null) {
                logger.debug("property {} read from system init parameter = {}",propertyName, stringVal1);
                stringVal = stringVal1;
            }
        }
        if (getConfigProperties() != null) {
            String stringVal3 = (String)getConfigProperties().get(propertyName);
            if (stringVal3 != null) {
                logger.debug("property {} read from owlcms.properties = {}",propertyName, stringVal3);
                stringVal = stringVal3;
            }
        }

        if (stringVal != null) {
            logger.debug(parameterName+" = "+stringVal);
            return stringVal;
        } else {
            logger.debug("default value for "+ parameterName +" = "+defaultVal);
            return defaultVal;
        }

    }

    private static void initHomePath(ServletContext servletContext2) {
        String homePath1 = null;

        String parameterName = "home";
        homePath1 = getStringConfigParameter(servletContext2, parameterName, null);
        if (homePath1 != null) {
            // compensate for extra quotes courtesy Advanced Installer...
            homePath1 = homePath1.replaceAll("^\"|\"$", "");
            logger.info("home path set from property owlcms.{} = {}", parameterName, homePath1);
            WebApplicationConfiguration.setHomePath(homePath1);
            return;
        }

        homePath1 = System.getenv("OWLCMS_HOME");
        if (homePath1 != null) {
            logger.info("home path set from OWLCMS_HOME = {}", homePath1);
            WebApplicationConfiguration.setHomePath(homePath1);
            return;
        }

        homePath1 = System.getProperty("user.dir");
        logger.info("home path set as current directory = {}", homePath1);
        WebApplicationConfiguration.setHomePath(homePath1);
    }

    private static void initLocaleString(ServletContext servletContext2) {
        String localeString1 = null;

        localeString1 = getStringConfigParameter(servletContext2, "locale", null);
        if (localeString1 != null) {
            WebApplicationConfiguration.setLocaleString(localeString1);
            logger.info("locale string from properties = {}", localeString1);
            return;
        }

        localeString1 = System.getenv("OWLCMS_LOCALE");
        if (localeString1 != null){
            WebApplicationConfiguration.setLocaleString(localeString1);
            logger.info("locale string from OWLCMS_LOCALE() = {}", localeString1);
            return;
        }

        localeString1 = "en_US";
        logger.info("locale string default = {}", localeString1);
        WebApplicationConfiguration.setLocaleString(localeString1);
    }

    public static boolean isUseCategorySinclair() {
        return useCategorySinclair;
    }

    public static void setUseCategorySinclair(boolean useCategorySinclair) {
        WebApplicationConfiguration.useCategorySinclair = useCategorySinclair;
    }

    public static boolean isUseRegistrationCategory() {
        return useRegistrationCategory;
    }

    public static void setUseRegistrationCategory(boolean useRegistrationCategory) {
        WebApplicationConfiguration.useRegistrationCategory = useRegistrationCategory;
    }

    public static boolean isUseBirthYear() {
        return useBirthYear;
    }

    public static void setUseBirthYear(boolean useBirthYear) {
        WebApplicationConfiguration.useBirthYear = useBirthYear;
    }

    public static boolean isUseBrowserLanguage() {
        return useBrowserLanguage;
    }

    public static void setUseBrowserLanguage(boolean useBrowserLanguage) {
        WebApplicationConfiguration.useBrowserLanguage = useBrowserLanguage;
    }

    public static boolean isUseOld20_15rule() {
        return useOld20_15rule;
    }

    public static void setUseOld20_15rule(boolean useOld20_15rule) {
        WebApplicationConfiguration.useOld20_15rule = useOld20_15rule;
    }

    public static boolean isUseOldBodyWeightTieBreak() {
        return useOldBodyWeightTieBreak;
    }

    public static void setUseOldBodyWeightTieBreak(boolean useOldBodyWeightTieBreak) {
        WebApplicationConfiguration.useOldBodyWeightTieBreak = useOldBodyWeightTieBreak;
    }

    private static void setLocaleString(String localeString) {
        WebApplicationConfiguration.localeString = localeString;
    }

    public static boolean isAutoLoadLifterEditor() {
        return WebApplicationConfiguration.autoLoadLifterEditor;
    }

    private static void setAutoLoadLifterEditor(boolean autoLoadLifterEditor) {
        WebApplicationConfiguration.autoLoadLifterEditor = autoLoadLifterEditor;
    }

    private static String getLocaleString() {
        if (localeString == null) {
            initLocaleString(null);
        }
        return localeString ;
    }

    public static Locale getLocale() {
        if (locale == null) {
            locale = LocalizedApplication.getLocaleFromString(getLocaleString());
        }
        return locale ;
    }

    public static String getHomePath() {
        return homePath;
    }

    private static void setHomePath(String homePath) {
        WebApplicationConfiguration.homePath = homePath;
    }

    private static Properties getConfigProperties() {
        return configProperties;
    }

    private static void setConfigProperties(Properties configProperties) {
        WebApplicationConfiguration.configProperties = configProperties;
    }




}
