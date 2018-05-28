/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.ui;

import java.net.MalformedURLException;

import org.concordiainternational.competition.data.CompetitionSession;
import org.concordiainternational.competition.data.Lifter;
import org.concordiainternational.competition.data.RuleViolationException;
import org.concordiainternational.competition.decision.DecisionEvent;
import org.concordiainternational.competition.decision.DecisionEventListener;
import org.concordiainternational.competition.i18n.Messages;
import org.concordiainternational.competition.publicAddress.IntermissionTimerEvent;
import org.concordiainternational.competition.publicAddress.IntermissionTimerEvent.IntermissionTimerListener;
import org.concordiainternational.competition.timer.CountdownTimer;
import org.concordiainternational.competition.timer.CountdownTimerListener;
import org.concordiainternational.competition.ui.SessionData.SessionDataUpdateEventListener;
import org.concordiainternational.competition.ui.components.ApplicationView;
import org.concordiainternational.competition.ui.components.DecisionLights;
import org.concordiainternational.competition.ui.components.RefereeActionDispatcher;
import org.concordiainternational.competition.ui.generators.TimeFormatter;
import org.concordiainternational.competition.utils.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;

public class CountdownDisplay extends VerticalLayout implements
        ApplicationView,
        CountdownTimerListener,
        DecisionEventListener,
        IntermissionTimerListener,
        CloseListener
{
    public final static Logger logger = LoggerFactory.getLogger(CountdownDisplay.class);
    private static final long serialVersionUID = 1437157542240297372L;

    private String platformName;
    private SessionData masterData;
    private Label timeDisplay = new Label();
    private int lastTimeRemaining;
    private String viewName;
    private Window popUp = null;
    private DecisionLights decisionLights;
    private SessionDataUpdateEventListener sessionDataUpdateEventListener;
    protected boolean shown;

    @SuppressWarnings("unused")
    private boolean breakTimerShown = false;
    private Label title;
    private CompetitionApplication app;
    protected Thread robot;
    private RefereeActionDispatcher refereeActionDispatcher;

    public CountdownDisplay(boolean initFromFragment, String fragment, String viewName, CompetitionApplication app) {
        this.app = app;
        if (initFromFragment) {
            setParametersFromFragment(fragment);
        } else {
            setViewName(viewName);
        }
        LoggerUtils.mdcPut(LoggerUtils.LoggingKeys.view, getLoggingId());

        if (platformName == null) {
            // get the default platform name
            platformName = CompetitionApplicationComponents.initPlatformName();
        } else if (app.getPlatform() == null) {
            app.setPlatformByName(platformName);
        }

        synchronized (app) {
            boolean prevDisabled = app.getPusherDisabled();
            try {
                app.setPusherDisabled(true);
                masterData = app.getMasterData(platformName);
                create(app, platformName);
                registerAsListener();
            } finally {
                app.setPusherDisabled(prevDisabled);
            }
        }
    }

    private void registerAsGroupDataListener(final String platformName1, final SessionData masterData1) {
        // locate the current group data for the platformName
        if (masterData1 != null) {
            logger.debug("{} listening to: {}", platformName1, masterData1); //$NON-NLS-1$
            //masterData.addListener(SessionData.UpdateEvent.class, this, "update"); //$NON-NLS-1$

            sessionDataUpdateEventListener = new SessionData.SessionDataUpdateEventListener() {

                @Override
                public void sessionDataUpdateEvent(SessionDataUpdateEvent sessionDataUpdateEvent) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            updateTimeDisplay(platformName1, masterData1);
                        }
                    }).start();
                }

            };
            masterData1.addListener(sessionDataUpdateEventListener); //$NON-NLS-1$

        } else {
            logger.debug("{} NOT listening to:  = {}", platformName1, masterData1); //$NON-NLS-1$
        }
        LoggerUtils.mdcSetup(getLoggingId(),masterData1);
    }

    /**
     * @param app1
     * @param platformName1
     * @throws MalformedURLException
     */
    private void create(CompetitionApplication app1, String platformName1) {
        this.setSizeFull();
        this.addStyleName("largeCountdownBackground");

        title = new Label("");
        this.addComponent(title);
        title.setVisible(false);
        title.addStyleName("title");

        timeDisplay = createTimeDisplay();
        decisionLights = new DecisionLights(false, app1, false);
        showTimeDisplay();

    }

    private void showDecisionLights() {
        synchronized(app) {
            this.removeAllComponents();
            this.addComponent(decisionLights);
            this.setExpandRatio(decisionLights, 100);
            this.setComponentAlignment(decisionLights, Alignment.MIDDLE_CENTER);
        }
        app.push();
    }

    private void showTimeDisplay() {
        synchronized(app) {
                updateTimeDisplay(platformName, masterData);
                this.removeAllComponents();
                this.addComponent(timeDisplay);
                this.setExpandRatio(timeDisplay, 100);
                this.setComponentAlignment(timeDisplay, Alignment.MIDDLE_CENTER);
        }
        app.push();
    }

    /**
     *
     */
    private Label createTimeDisplay() {
        Label timeDisplay1 = new Label();
        timeDisplay1.setSizeUndefined();
        // timeDisplay1.setHeight("600px");
        timeDisplay1.addStyleName("largeCountdown");
        return timeDisplay1;
    }

    /**
     * @param platformName1
     * @param masterData1
     * @throws RuntimeException
     */
    private void updateTimeDisplay(final String platformName1, final SessionData masterData1) throws RuntimeException {
        final Lifter currentLifter = masterData1.getCurrentLifter();
        logger.trace("currentLifter = {}", currentLifter);
        if (currentLifter != null) {
            boolean done = fillLifterInfo(currentLifter);
            logger.trace("done = {}", done);
            updateTime(masterData1);
            timeDisplay.setVisible(!done);
            timeDisplay.removeStyleName("intermission");
        } else {
            timeDisplay.setValue(""); //$NON-NLS-1$
        }

    }

    @Override
    public void refresh() {
        logger.trace("refresh");
        showTimeDisplay();
    }

    public boolean fillLifterInfo(Lifter lifter) {
        final int currentTry = 1 + (lifter.getAttemptsDone() >= 3 ? lifter.getCleanJerkAttemptsDone() : lifter
                .getSnatchAttemptsDone());
        boolean done = currentTry > 3;

        return done;
    }

    /**
     * @param groupData
     */
    private void updateTime(final SessionData groupData) {
        // we set the value to the time remaining for the current lifter as
        // computed by groupData
        int timeRemaining = groupData.getDisplayTime();
        logger.trace("updateTime {}", timeRemaining);
        pushTime(timeRemaining);
    }

    @Override
    public void finalWarning(int timeRemaining) {
        normalTick(timeRemaining);
    }

    @Override
    public void forceTimeRemaining(int timeRemaining, CompetitionApplication originatingApp, InteractionNotificationReason reason) {
        pushTime(timeRemaining);
    }

    @Override
    public void initialWarning(int timeRemaining) {
        normalTick(timeRemaining);
    }

    @Override
    public void noTimeLeft(int timeRemaining) {
        normalTick(timeRemaining);
    }

    @Override
    public void normalTick(int timeRemaining) {
        pushTime(timeRemaining);
    }

    /**
     * @param timeRemaining
     */
    private void pushTime(int timeRemaining) {
        if (timeDisplay == null)
            return;

        // do not update if no visible change
        if (TimeFormatter.getSeconds(timeRemaining) == TimeFormatter.getSeconds(lastTimeRemaining)) {
            lastTimeRemaining = timeRemaining;
            return;
        } else {
            lastTimeRemaining = timeRemaining;
        }

        synchronized (app) {
            timeDisplay.setValue(TimeFormatter.formatAsSeconds(timeRemaining));
        }
        app.push();
    }

    @Override
    public void pause(int timeRemaining, CompetitionApplication originatingApp, InteractionNotificationReason reason) {
    }

    @Override
    public void start(int timeRemaining) {
    }

    @Override
    public void stop(int timeRemaining, CompetitionApplication originatingApp, InteractionNotificationReason reason) {
    }

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
        String[] params = frag.split("/");
        if (params.length >= 1) {
            viewName = params[0];
        } else {
            throw new RuleViolationException("Error.ViewNameIsMissing");
        }
        if (params.length >= 2) {
            platformName = params[1];
        } else {
            platformName = CompetitionApplicationComponents.initPlatformName();
        }
    }

    @Override
    public void updateEvent(final DecisionEvent updateEvent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (app) {
                    switch (updateEvent.getType()) {
                    case DOWN:
                        logger.debug("received DOWN event");
                        showLights(updateEvent);
                        break;

                    case SHOW:
                        // if window is not up, show it.
                        shown = true;
                        logger.debug("received SHOW event {}", shown);
                        showLights(updateEvent);
                        break;

                    case RESET:
                        // we are done
                        logger.debug("received RESET event (hiding decision lights)");
                        hideLights(updateEvent);
                        shown = false;
                        break;

                    case WAITING:
                        logger.debug("ignoring WAITING event");
                        break;

                    case UPDATE:
                        logger.debug("received UPDATE event {}", shown);
                        // we need to show that referees have changed their mind.
                        if (shown) {
                            showLights(updateEvent);
                        }
                        break;

                    case BLOCK:
                        logger.debug("received BLOCK event {}", shown);
                        showLights(updateEvent);
                        break;
                    }
                }
                app.push();
            }
        }).start();
    }

    /**
     * Make sure decision lights are shown, and relay the event to the display component.
     *
     * @param updateEvent
     */
    private void showLights(DecisionEvent updateEvent) {
        // relay the event
        logger.trace("relaying");
        decisionLights.updateEvent(updateEvent);
        logger.debug("updating display");
        showDecisionLights();

    }

    /**
     * Hide the decision lights.
     *
     * @param updateEvent
     */
    private void hideLights(DecisionEvent updateEvent) {
        // relay the event (just in case)
        if (decisionLights != null) {
            decisionLights.updateEvent(updateEvent);
        }

        logger.debug("removing decision lights");
        showTimeDisplay();
    }

    /**
     * Resister to all necessary listening events
     */
    @Override
    public void registerAsListener() {
        LoggerUtils.mdcSetup(getLoggingId(), masterData);
        Window mainWindow = app.getMainWindow();
        mainWindow.addListener((CloseListener) this);

        // listen to changes in the competition data
        logger.debug("listening to session data updates.");
        registerAsGroupDataListener(platformName, masterData);

        // listen to intermission timer events
        masterData.addBlackBoardListener(this);
        logger.debug("listening to intermission timer events.");

        // listen to decisions
        DecisionEventListener decisionListener = (DecisionEventListener) this;
        logger.debug("adding decision listener {}", decisionListener);
        masterData.getRefereeDecisionController().addListener(decisionListener);

        // listen to main timer events
        final CountdownTimer timer = masterData.getTimer();
        timer.setCountdownDisplay(this);
        // should add itself to regular listeners also so we can have multiple
        timer.addListener(this);

        refereeActionDispatcher = new RefereeActionDispatcher(masterData, mainWindow);
        refereeActionDispatcher.addActions();
        logger.debug("added action handler");
    }

    /**
     * Undo what registerAsListener did.
     */
    @Override
    public void unregisterAsListener() {
        logger.debug("unregisterAsListener");
        Window mainWindow = app.getMainWindow();
        if (popUp != null) {
            mainWindow.removeWindow(popUp);
            popUp = null;
        }

        // stop listening to intermission timer events
        removeIntermissionTimer();
        masterData.removeBlackBoardListener(this);
        logger.debug("stopped listening to intermission timer events");

        mainWindow.removeListener((CloseListener) this);
        masterData.removeListener(sessionDataUpdateEventListener);

        DecisionEventListener decisionListener = (DecisionEventListener) this;
        logger.debug("removing decision listener {}", decisionListener);
        masterData.getRefereeDecisionController().removeListener(decisionListener);
        final CountdownTimer timer = masterData.getTimer();
        if (timer.getCountdownDisplay() == this) {
            timer.setCountdownDisplay(null);
        }
        timer.removeListener(this);

        if (refereeActionDispatcher != null) refereeActionDispatcher.removeActions();
        refereeActionDispatcher = null;
        logger.debug("removed action handler");
    }

    //
    // @Override
    // public DownloadStream handleURI(URL context, String relativeUri) {
    // registerAsListener();
    // return null;
    // }

    @Override
    public void windowClose(CloseEvent e) {
        unregisterAsListener();
    }

    public DecisionLights getDecisionLights() {
        return decisionLights;
    }

    public void setDecisionLights(DecisionLights decisionLights) {
        this.decisionLights = decisionLights;
    }



    @Override
    public void intermissionTimerUpdate(IntermissionTimerEvent event) {
        Integer remainingMilliseconds = event.getRemainingMilliseconds();
        if (remainingMilliseconds != null && remainingMilliseconds > 0) {
            displayIntermissionTimer(remainingMilliseconds);
        } else {
            removeIntermissionTimer();
        }

    }

    /**
     * Hide the break timer
     */
    private void removeIntermissionTimer() {
        logger.debug("removing intermission timer");
        breakTimerShown = false;
        title.setVisible(false);
        // title.setHeight("0%");
        // timeDisplay.setHeight("100%");

        // force update
        lastTimeRemaining = 0;
        refresh();
    }

    /**
     * Display the break timer
     *
     * @param remainingMilliseconds
     */
    private void displayIntermissionTimer(Integer remainingMilliseconds) {
        synchronized (app) {
            breakTimerShown = true;

            title.setVisible(true);
            title.addStyleName("title");
            title.setValue(Messages.getString("AttemptBoard.Pause", CompetitionApplication.getCurrentLocale()));
            // title.setHeight("15%");

            timeDisplay.setVisible(true);
            timeDisplay.addStyleName("intermission");
            timeDisplay.setValue(TimeFormatter.formatAsSeconds(remainingMilliseconds));
            // timeDisplay.setHeight("85%");

            decisionLights.setVisible(false);
        }
        app.push();
    }

    @Override
    public void showInteractionNotification(CompetitionApplication originatingApp, InteractionNotificationReason reason) {
        // do nothing - notifications are meant for technical officials
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
