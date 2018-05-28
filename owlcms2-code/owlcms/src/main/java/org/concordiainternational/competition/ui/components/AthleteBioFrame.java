/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.ui.components;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import org.concordiainternational.competition.data.Competition;
import org.concordiainternational.competition.data.CompetitionSession;
import org.concordiainternational.competition.data.Lifter;
import org.concordiainternational.competition.data.RuleViolationException;
import org.concordiainternational.competition.i18n.Messages;
import org.concordiainternational.competition.ui.CompetitionApplication;
import org.concordiainternational.competition.ui.CompetitionApplicationComponents;
import org.concordiainternational.competition.ui.SessionData;
import org.concordiainternational.competition.ui.SessionData.SessionDataUpdateEventListener;
import org.concordiainternational.competition.ui.SessionDataUpdateEvent;
import org.concordiainternational.competition.ui.generators.TimeFormatter;
import org.concordiainternational.competition.utils.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;

/**
 * Show an WebPage underneath a banner.
 *
 * @author jflamy
 *
 */

public class AthleteBioFrame extends VerticalLayout implements
        ApplicationView,
        Window.CloseListener,
        Stylable
{

    protected static final String ATTEMPT_WIDTH = "6em";
    public final static Logger logger = LoggerFactory.getLogger(AthleteBioFrame.class);
    private static Logger listenerLogger = LoggerFactory.getLogger("listeners." + AthleteBioFrame.class.getSimpleName()); //$NON-NLS-1$

    private static final long serialVersionUID = 1437157542240297372L;
    protected Embedded iframe;
    public String urlString;
    private String platformName;
    private SessionData masterData;
    protected CustomLayout top;
    protected CompetitionApplication app;
    protected Label name = new Label("<br>", Label.CONTENT_XHTML); //$NON-NLS-1$
    protected Label attempt = new Label("", Label.CONTENT_XHTML); //$NON-NLS-1$
    protected Label timeDisplay = new Label("", Label.CONTENT_XHTML);
    protected Label weight = new Label("", Label.CONTENT_XHTML);
    private String appUrlString;
    private SessionDataUpdateEventListener updateListener;
    protected Locale pageLocale;

    public AthleteBioFrame(boolean initFromFragment, String fragment, String viewName, String localeSuffix, CompetitionApplication app) throws MalformedURLException {
        setViewName(viewName);
        this.app = app;
        LoggerUtils.mdcPut(LoggerUtils.LoggingKeys.view, getLoggingId());

        if (initFromFragment) {
            setParametersFromFragment(fragment);
        } else {
            setViewName(viewName);
            setStylesheetName(stylesheetName);
        }


        this.pageLocale = app.getLocale();

        boolean prevDisabledPush = app.getPusherDisabled();
        try {
            app.setPusherDisabled(true);
            if (platformName == null) {
                // get the default platform name
                platformName = CompetitionApplicationComponents.initPlatformName();
            }
            app.setPlatformByName(platformName);

            urlString = getAppUrlString();

            createLayout();
            masterData = app.getMasterData(platformName);
            LoggerUtils.mdcPut(LoggerUtils.LoggingKeys.currentGroup, getLoggingId());


            // we cannot call push() at this point
            synchronized (app) {
                boolean prevDisabled = app.getPusherDisabled();
                try {
                    app.setPusherDisabled(true);
                    display(platformName, masterData);
                } finally {
                    app.setPusherDisabled(prevDisabled);
                }
                // logger.debug("browser panel: push disabled = {}", app.getPusherDisabled());
            }

            registerAsListener();
        } finally {
            app.setPusherDisabled(prevDisabledPush);
        }
    }

    /**
     * Compute where we think the athlete bio html file ought to be.
     */
    private String getAppUrlString() {
        appUrlString = app.getURL().toExternalForm();
        int lastSlash = appUrlString.lastIndexOf("/");
        if (lastSlash == appUrlString.length() - 1) {
            // go back one more slash, the string ended with /
            lastSlash = appUrlString.lastIndexOf("/", lastSlash - 1);
        }
        appUrlString = appUrlString.substring(0, lastSlash + 1);
        // System.err.println("appUrlString with slash="+appUrlString);
        return appUrlString;
    }

    private SessionDataUpdateEventListener createUpdateSessionUpdateListener(final String platformName1, final SessionData masterData1) {
        // locate the current group data for the platformName
        if (masterData1 != null) {
            SessionData.SessionDataUpdateEventListener listener = new SessionData.SessionDataUpdateEventListener() {

                @Override
                public void sessionDataUpdateEvent(SessionDataUpdateEvent sessionDataUpdateEvent) {
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            LoggerUtils.mdcPut(LoggerUtils.LoggingKeys.view, getLoggingId());
                            logger.debug("updateEvent {}", AthleteBioFrame.this);
                            display(platformName1, masterData1);
                        }
                    }).start();
                }
            };
            return listener;

        } else {
            logger.debug(urlString + "{} NOT listening to:  = {}", platformName1, masterData1); //$NON-NLS-1$
            return null;
        }
    }

    /**
     * @param platformName
     * @throws MalformedURLException
     */
    private void createLayout() throws MalformedURLException {
        this.setSizeFull();
        this.setMargin(true);
        this.setStyleName("athleteBio");

        resetTop(); // create for first time

        iframe = new Embedded(); //$NON-NLS-1$
        iframe.setType(Embedded.TYPE_BROWSER);
        iframe.setSizeFull();
        iframe.setStyleName("bioTop");
        this.addComponent(iframe);

        this.setExpandRatio(iframe, 100);
    }

    protected void resetTop() {
        CustomLayout newTop = new CustomLayout("bioTop"); //$NON-NLS-1$

        if (top == null) {
            // create for first time
            this.addComponent(newTop);
        } else {
            top.removeAllComponents();
            this.replaceComponent(top, newTop);
        }
        top = newTop;
        top.setWidth("100%"); //$NON-NLS-1$
        name.setSizeUndefined();
        attempt.setSizeUndefined();
        timeDisplay.setSizeUndefined();
        weight.setSizeUndefined();
        this.setExpandRatio(top, 0);
    }

    /**
     * @param platformName1
     * @param masterData1
     * @throws RuntimeException
     */
    protected void display(final String platformName1, final SessionData masterData1) throws RuntimeException {
        synchronized (app) {
            final Lifter currentLifter = masterData1.getCurrentLifter();
            URL url = computeUrl(platformName1,currentLifter);
            logger.trace("display {}", url, getStylesheetName());
            // LoggerUtils.traceBack(logger,"display()");
            iframe.setSource(new ExternalResource(url));


            resetTop();

            boolean displayBreakTimer = isBreak();
            if (currentLifter != null) {
                {
                    fillLifterInfo(currentLifter);
                }
            } else {
                logger.trace("lifter null, displayBreakTimer={}",displayBreakTimer);
                {
                    name.setValue(getWaitingMessage()); //$NON-NLS-1$
                }
                logger.trace("adding name {}", name.getParent());
                top.addComponent(name, "name"); //$NON-NLS-1$

                attempt.setValue(""); //$NON-NLS-1$
                logger.trace("adding attempt");
                top.addComponent(attempt, "attempt"); //$NON-NLS-1$
                attempt.setWidth(ATTEMPT_WIDTH); //$NON-NLS-1$

                weight.setValue(""); //$NON-NLS-1$
                weight.setWidth("4em"); //$NON-NLS-1$
                logger.trace("adding weight");
                top.addComponent(weight, "weight"); //$NON-NLS-1$
            }
        }
        // logger.debug("prior to display push disabled={}", app.getPusherDisabled());

        app.push();
    }

    protected boolean isBreak() {
        return masterData.getIntermissionTimer().getRemainingMilliseconds() > 0;
    }


    /**
     * @param platformName1
     * @param currentLifter
     * @return
     * @throws RuntimeException
     */
    protected URL computeUrl(final String platformName1, Lifter currentLifter) throws RuntimeException {
        URL url;
        String relativePath = null;
        String spec = null;

        if (currentLifter == null) {
            relativePath = "athleteBio/empty.html"; //$NON-NLS-1$
            spec = appUrlString + relativePath;
        } else {
            relativePath = "athleteBio/" + currentLifter.getMembership() + ".html"; //$NON-NLS-1$  //$NON-NLS-2$
            String fullPath;
            File bioFile = null;
            try {
                fullPath = app.getResourceFileName(relativePath);
                bioFile = new File(fullPath);
            } catch (IOException e) {
            }

            if (bioFile == null || ! bioFile.exists()) {
                relativePath = "athleteBio/noData.html"; //$NON-NLS-1$
                spec = appUrlString + relativePath + "?time=" + System.currentTimeMillis(); //$NON-NLS-1$
            } else {
                spec = appUrlString + relativePath + "?time=" + System.currentTimeMillis(); //$NON-NLS-1$
            }
        }


        try {
            url = new URL(spec);
            // logger.debug("url={} {}", url.toExternalForm(), this);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e); // can't happen.
        }
        return url;
    }

    /**
     * @return message used when Announcer has not selected a group
     */
    protected String getWaitingMessage() {
        String message = Messages.getString("ResultFrame.Waiting", pageLocale);
        List<Competition> competitions = Competition.getAll();
        if (competitions.size() > 0) {
            message = competitions.get(0).getCompetitionName();
        }
        return message;
    }

    @Override
    public void refresh() {
        display(platformName, masterData);
    }

    public boolean fillLifterInfo(Lifter lifter) {
        final int currentTry = 1 + (lifter.getAttemptsDone() >= 3 ? lifter.getCleanJerkAttemptsDone() : lifter
                .getSnatchAttemptsDone());
        boolean done = currentTry > 3;

        synchronized (app) {
            displayName(lifter, pageLocale, done);
            displayAttemptNumber(lifter, pageLocale, currentTry, done);
            displayRequestedWeight(lifter, pageLocale, done);
        }
        app.push();
        return done;
    }

    /**
     * @param lifter
     * @param alwaysShowName
     * @param sb
     * @param done
     */
    private void displayName(Lifter lifter, final Locale locale, boolean done) {
        // display lifter name and affiliation
        if (!done) {
            final String lastName = lifter.getLastName();
            final String firstName = lifter.getFirstName();
            final String club = lifter.getClub();
            name.setValue(noBr(lastName.toUpperCase()) + " " + noBr(firstName) + " &nbsp;&nbsp; " + noBr(club)); //$NON-NLS-1$ //$NON-NLS-2$
            logger.trace("adding name={}",name.getParent());
            top.addComponent(name, "name"); //$NON-NLS-1$
        } else {
            name.setValue(MessageFormat.format(
                    Messages.getString("ResultFrame.Done", locale), masterData.getCurrentSession().getName())); //$NON-NLS-1$
            logger.trace("adding name {}",name.getParent());
            top.addComponent(name, "name"); //$NON-NLS-1$
        }

    }

    private String noBr(String str) {
        return "<nobr>"+str+"</nobr>";
    }


    /**
     * @param lifter
     * @param sb
     * @param locale
     * @param currentTry
     * @param done
     */
    private void displayAttemptNumber(Lifter lifter, final Locale locale, final int currentTry, boolean done) {
        // display current attempt number
        if (!done) {
            //appendDiv(sb, lifter.getNextAttemptRequestedWeight()+Messages.getString("Common.kg",locale)); //$NON-NLS-1$
            String tryInfo = MessageFormat.format(Messages.getString("ResultFrame.tryNumber", locale), //$NON-NLS-1$
                    currentTry, (lifter.getAttemptsDone() >= 3 ? Messages.getString("Common.shortCleanJerk", locale) //$NON-NLS-1$
                            : Messages.getString("Common.shortSnatch", locale))); //$NON-NLS-1$
            attempt.setValue(tryInfo);
        } else {
            attempt.setValue("");
        }
        attempt.setWidth(ATTEMPT_WIDTH);
        logger.trace("adding attempt");
        top.addComponent(attempt, "attempt"); //$NON-NLS-1$
    }

    /**
     * @param lifter
     * @param sb
     * @param locale
     * @param done
     * @return
     */
    private void displayRequestedWeight(Lifter lifter, final Locale locale, boolean done) {
        // display requested weight
        if (!done) {
            weight.setValue(lifter.getNextAttemptRequestedWeight() + Messages.getString("Common.kg", locale)); //$NON-NLS-1$
        } else {
            weight.setValue(""); //$NON-NLS-1$
        }
        logger.trace("adding weight");
        top.addComponent(weight, "weight"); //$NON-NLS-1$
    }

    /**
     * @param groupData
     */
    protected void updateTime(final SessionData groupData) {
        // we set the value to the time remaining for the current lifter as
        // computed by groupData
        int timeRemaining = groupData.getDisplayTime();
        logger.trace("timeRemaining = {}",timeRemaining);
        timeDisplay.setValue(TimeFormatter.formatAsSeconds(timeRemaining));
    }

    int previousTimeRemaining = 0;
    private String viewName;
    protected boolean shown;
    private String stylesheetName;



    /*
     * (non-Javadoc)
     *
     * @see org.concordiainternational.competition.ui.components.ApplicationView#needsMenu()
     */
    @Override
    public boolean needsMenu() {
        return false;
    }

    /**
     * @return
     */
    @Override
    public String getFragment() {
        return viewName + "/" + platformName;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.concordiainternational.competition.ui.components.ApplicationView#setParametersFromFragment(java.lang.String)
     */
    @Override
    public void setParametersFromFragment(String fragment) {
        String frag = fragment;
        if (frag == null) frag = app.getUriFragmentUtility().getFragment();
        logger.debug(frag);
        String[] params = frag.split("/");
        if (params.length >= 1) {
            viewName = params[0];
        } else {
            throw new RuleViolationException("Error.ViewNameIsMissing");
        }

        if (params.length >= 2) {
            platformName = params[1];
        }
        logger.debug("frag = {}, viewName = {}, platformName = {}",frag,viewName,platformName);
        setViewName(viewName);

    }


    /*
     * Unregister listeners when window is closed.
     *
     * @see com.vaadin.ui.Window.CloseListener#windowClose(com.vaadin.ui.Window.CloseEvent)
     */
    @Override
    public void windowClose(CloseEvent e) {
        unregisterAsListener();
    }

//    @Override
//    public DownloadStream handleURI(URL context, String relativeUri) {
//        listenerLogger.debug("re-registering handlers for {} {}", this, relativeUri);
//        registerAsListener();
//        return null;
//    }

    /*
     * (non-Javadoc)
     *
     * @see org.concordiainternational.competition.ui.components.Stylable#setStylesheet(java.lang.String)
     */
    @Override
    public void setStylesheetName(String stylesheetName) {
        this.stylesheetName = stylesheetName;
        // String callerClassName = LoggerUtils.getCallerClassName();
        // logger.trace("stylesheetName={} ({}) {}", stylesheetName, callerClassName, this);
        // LoggerUtils.traceBack(logger);
    }

    @Override
    public String getStylesheetName() {
        // String callerClassName = LoggerUtils.getCallerClassName();
        // logger.trace("stylesheetName={} ({}) {}", stylesheetName, callerClassName, this);
        return stylesheetName;
    }

    @Override
    public void registerAsListener() {
//        LoggerUtils.traceBack(listenerLogger);
        LoggerUtils.mdcSetup(getLoggingId(), masterData);
        // listen to changes in the competition data
        if (updateListener == null) {
            updateListener = createUpdateSessionUpdateListener(platformName, masterData);
        }
        masterData.addListener(updateListener);
        listenerLogger.debug("{} listening to session data updates for platform {}.", updateListener, platformName);

        // listen to close events
        app.getMainWindow().addListener((CloseListener) this);
        listenerLogger.debug("{} listening to window close events.", this);
    }

    @Override
    public void unregisterAsListener() {
      LoggerUtils.traceBack(listenerLogger);
        LoggerUtils.mdcPut(LoggerUtils.LoggingKeys.view, getLoggingId());

        // stop listening to changes in the competition data
        if (updateListener != null) {
            masterData.removeListener(updateListener);
            listenerLogger.debug("{} stopped listening session data updates", updateListener);
        }

        // stop listening to close events
        app.getMainWindow().removeListener((CloseListener) this);
        listenerLogger.debug("{} stopped listening to window close events..", this);
    }

    @Override
    public boolean needsBlack() {
        return true;
    }

    private static int classCounter = 0; // per class
    private final int instanceId = classCounter++; // per instance


    @Override
    public String getInstanceId() {
        return Long.toString(instanceId);
    }

    @Override
    public String getLoggingId() {
        return getViewName(); //+ getInstanceId();
    }

    @Override
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    @Override
    public String getViewName() {
        return this.viewName;
    }


    @Override
    public void switchGroup(final CompetitionSession newSession) {
    }


}
