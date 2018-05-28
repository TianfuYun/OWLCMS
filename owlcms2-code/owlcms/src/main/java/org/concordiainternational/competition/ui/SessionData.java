/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.ui;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.ObjectUtils;
import org.concordiainternational.competition.data.Competition;
import org.concordiainternational.competition.data.CompetitionSession;
import org.concordiainternational.competition.data.Lifter;
import org.concordiainternational.competition.data.LifterContainer;
import org.concordiainternational.competition.data.Platform;
import org.concordiainternational.competition.data.lifterSort.LifterSorter;
import org.concordiainternational.competition.data.lifterSort.LifterSorter.Ranking;
import org.concordiainternational.competition.decision.Decision;
import org.concordiainternational.competition.decision.IDecisionController;
import org.concordiainternational.competition.decision.JuryDecisionController;
import org.concordiainternational.competition.decision.RefereeDecisionController;
import org.concordiainternational.competition.i18n.Messages;
import org.concordiainternational.competition.publicAddress.IntermissionTimer;
import org.concordiainternational.competition.publicAddress.IntermissionTimerEvent;
import org.concordiainternational.competition.publicAddress.IntermissionTimerEvent.IntermissionTimerListener;
import org.concordiainternational.competition.publicAddress.PublicAddressMessageEvent;
import org.concordiainternational.competition.publicAddress.PublicAddressMessageEvent.MessageDisplayListener;
import org.concordiainternational.competition.timer.CountdownTimer;
import org.concordiainternational.competition.ui.PlatesInfoEvent.PlatesInfoListener;
import org.concordiainternational.competition.ui.components.ApplicationView;
import org.concordiainternational.competition.utils.EventHelper;
import org.concordiainternational.competition.utils.IdentitySet;
import org.concordiainternational.competition.utils.NotificationManager;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import com.github.wolfie.blackboard.Blackboard;
import com.github.wolfie.blackboard.Event;
import com.github.wolfie.blackboard.Listener;
import com.vaadin.data.Item;
import com.vaadin.data.hbnutil.HbnContainer.HbnSessionManager;
import com.vaadin.event.EventRouter;
import com.vaadin.ui.Component;

/**
 * Data about a competition group.
 * <p>
 * Manages the lifting order, keeps tabs of which lifters have been called, who is entitled to two minutes, etc. Also holds the master timer
 * for the group.
 * </p>
 *
 * @author jflamy
 */
public class SessionData implements Lifter.UpdateEventListener, Serializable {

    private static final long serialVersionUID = -7621561459948739065L;
    public static final String MASTER_KEY = "GroupData_"; //$NON-NLS-1$

    private static XLogger logger = XLoggerFactory.getXLogger(SessionData.class);
    private static final Logger timingLogger = LoggerFactory.getLogger("timing." + SessionData.class.getSimpleName()); //$NON-NLS-1$
    private static Logger listenerLogger = LoggerFactory.getLogger("listeners." + SessionData.class.getSimpleName()); //$NON-NLS-1$

    public List<Lifter> lifters;
    /**
     * list of currently displayed lifters that, if updated, will notify us. We use an IdentitySet because the same lifter can appear in two
     * windows, as two occurrences that are != but equals.
     */
    public Set<Object> notifiers = (new IdentitySet(5));
    private List<Lifter> liftTimeOrder;
    private List<Lifter> displayOrder;
    private List<Lifter> resultOrder;
    //private CompetitionApplication app;

    private CompetitionSession currentSession;
    private Lifter currentLifter;

    private List<Lifter> currentDisplayOrder;
    private List<Lifter> currentLiftingOrder;
    private List<Lifter> currentResultOrder;

    private NotificationManager<SessionData, Lifter, Component> notificationManager;
    private int timeAllowed;
    private int liftsDone;

    private RefereeDecisionController refereeDecisionController = null;
    private JuryDecisionController juryDecisionController = null;

    boolean allowAll = false; // allow null group to mean all lifters.
    // will be set to true if the Timekeeping button is pressed.
    private boolean timeKeepingInUse = Competition.isMasters();

    private Lifter priorLifter;
    private Integer priorRequest;
    private Integer priorRequestNum;
    Blackboard blackBoardEventRouter = new Blackboard();

    public int getLiftsDone() {
        return liftsDone;
    }

    private SessionData(String platformName) {
        lifters = new ArrayList<Lifter>();
        platform = Platform.getByName(platformName);

        notificationManager = new NotificationManager<SessionData, Lifter, Component>(this);
        refereeDecisionController = new RefereeDecisionController(this);
        juryDecisionController = new JuryDecisionController(this);
        init();
    }

    /**
     * This constructor is only meant for unit tests.
     *
     * @param lifters
     */
    public SessionData(List<Lifter> lifters) {
        this.lifters = lifters;
        notificationManager = new NotificationManager<SessionData, Lifter, Component>(this);
        refereeDecisionController = new RefereeDecisionController(this);
        juryDecisionController = new JuryDecisionController(this);
        updateListsForLiftingOrderChange(null,true, false);
        init();
    }

    /**
     * This constructor is meant to create an independent instance
     *
     * @param lifters
     */
    public SessionData(CompetitionSession cg, List<Lifter> lifters) {
        this(lifters);
        this.currentSession = cg;
        this.platform = CompetitionApplication.getCurrent().getPlatform();
    }

    static private final Map<String, SessionData> platformToSessionData = new HashMap<String, SessionData>();

    public static SessionData getSingletonForPlatform(String platformName) {
        SessionData groupDataSingleton = platformToSessionData.get(platformName);

        if (groupDataSingleton == null) {
            groupDataSingleton = new SessionData(platformName);
            platformToSessionData.put(platformName, groupDataSingleton);
            groupDataSingleton.registerAsMasterData(platformName);
        }
        //logger.debug("groupData = {}", groupDataSingleton); //$NON-NLS-1$
        return groupDataSingleton;
    }

    /**
     * @return information about a session, not connected to a platform.
     */
    public static SessionData getIndependentInstance() {
        SessionData independentData = new SessionData("");
        //logger.debug("independentData = {}", independentData); //$NON-NLS-1$
        return independentData;
    }

    /**
     * @return
     */
    private void init() {
        blackBoardEventRouter.register(MessageDisplayListener.class, PublicAddressMessageEvent.class);
        blackBoardEventRouter.register(IntermissionTimerListener.class, IntermissionTimerEvent.class);
        blackBoardEventRouter.register(PlatesInfoListener.class, PlatesInfoEvent.class);
    }

    /**
     * This method reloads the underlying data. Beware that only "master" views are meant to do this, such as AnnouncerView when mode =
     * ANNOUNCER, or the results view to edit results after a group is over.
     *
     * "slave" views such as the MARSHAL, TIMEKEEPER views should never call this method.
     */
    void loadData() {
        currentSession = this.getCurrentSession();
        if (currentSession == null && !allowAll) {
            // make it so we have to select a group
            lifters = new ArrayList<Lifter>();
            logger.debug("current group is empty"); //$NON-NLS-1$
        } else {
            CompetitionApplication current = CompetitionApplication.getCurrent();
            logger.debug("loading data for group {}", currentSession); //$NON-NLS-1$
            final LifterContainer hbnCont = new LifterContainer(current);
            // hbnCont will filter automatically to application.getCurrentGroup

            // TODO : avoid using hbnCont -- get from database directly.
            lifters = hbnCont.getAllPojos();
        }
    }

    /**
     * @return the lifter who lifted most recently
     */
    public Lifter getPreviousLifter() {
        if (getLiftTimeOrder() == null) {
            setLiftTimeOrder(LifterSorter.LiftTimeOrderCopy(lifters));
        }
        if (getLiftTimeOrder().size() == 0)
            return null;
        // if (logger.isDebugEnabled())
        // System.err.println(AllTests.longDump(liftTimeOrder));

        Lifter lifter = getLiftTimeOrder().get(0);
        if (lifter.getPreviousLiftTime() == null)
            return null;
        return lifter;

        // // we want the most recent, who will be at the end.
        // // skip people who have not lifted.
        // Lifter lifter = null;
        // for (int index = lifters.size()-1; index >= 0; index--) {
        // lifter = liftTimeOrder.get(index);
        // if (lifter.getLastLiftTime() != null) break;
        // }
        // return lifter;
    }

    /**
     * Saves changes made to object to Hibernate Session. Note that run is most likely detached due session-per-request patterns so we'll
     * use merge. Actual database update will happen by Vaadin's transaction listener in the end of request.
     *
     * If one wanted to make sure that this operation will be successful a (Hibernate) transaction commit and error checking ought to be
     * done.
     *
     * @param object
     */
    public void persistPojo(Object object) {
        try {
            CompetitionApplication current = CompetitionApplication.getCurrent();
            ((HbnSessionManager) current).getHbnSession().merge(object);
        } catch (StaleObjectStateException e) {
            throw new RuntimeException(Messages.getString("SessionData.UserHasBeenDeleted", CompetitionApplication
                    .getCurrentLocale()));
        }
    }

    /**
     * Sort the various lists to reflect new lifting order.
     * @param automaticProgression
     * @param letClockRun
     */
    public void updateListsForLiftingOrderChange(Lifter updatedLifter, boolean automaticProgression, boolean letClockRun) {
        logger.debug("updateListsForLiftingOrderChange next = {} change for = {}", currentLifter, updatedLifter); //$NON-NLS-1$

        final CountdownTimer timer2 = getTimer();
        if (timer2 != null) {
            // athlete that was set to lift made a change
            Lifter timerOwner = timer2.getOwner();
            logger.debug("updateListsForLiftingOrderChange next = {} timerOwner = {} updatedLifter={} declarationSameAsAutomatic={}", currentLifter, timerOwner, updatedLifter, letClockRun); //$NON-NLS-1$

            if (currentLifter != null && updatedLifter == currentLifter) {
                if (automaticProgression) {
                    // automatic progression or initial declaration, don't notify announcer
                    // stop the timer if it was running, and make sure event is broadcast
                    timer2.pause();
                } else if (! letClockRun){
                    // the declared weight is different from what was automatically requested
                    // stop the timer if it was running, and make sure event is broadcast
                    timer2.pause(InteractionNotificationReason.CURRENT_LIFTER_CHANGE_DONE);
                }

            }
        }

        boolean needToAnnounce = sortLists(letClockRun);
        publishListsToServletContext();
        notifyListeners(needToAnnounce);

    }

    /**
     * @param b
     *
     */
    private boolean sortLists(boolean letClockRun) {
        logger.debug("sortLists"); //$NON-NLS-1$

        displayOrder = LifterSorter.displayOrderCopy(lifters);
        setLiftTimeOrder(LifterSorter.LiftTimeOrderCopy(lifters));
        setResultOrder(LifterSorter.resultsOrderCopy(lifters, Ranking.TOTAL));
        LifterSorter.assignCategoryRanks(getResultOrder(), Ranking.TOTAL);
        this.liftsDone = LifterSorter.countLiftsDone(lifters);

        LifterSorter.liftingOrder(lifters);
        currentLifter = LifterSorter.markCurrentLifter(lifters);

        Integer currentRequest = (currentLifter != null ? currentLifter.getNextAttemptRequestedWeight() : null);
        Integer currentRequestNum = (currentLifter != null ? currentLifter.getAttemptsDone() : null);

        boolean sameLifter = currentLifter == priorLifter;
        boolean sameWeightRequest = ObjectUtils.compare(priorRequest, currentRequest) == 0;
        boolean sameAttemptNo = ObjectUtils.compare(priorRequestNum, currentRequestNum) == 0;
        boolean needToAnnounce = ! (sameLifter && sameWeightRequest && sameAttemptNo);



        logger.debug("needToAnnounce={} : new/old {}/{}  {}/{}  {}/{} {}", //$NON-NLS-1$
                needToAnnounce, currentLifter, priorLifter, currentRequest, priorRequest, currentRequestNum,
                priorRequestNum, letClockRun);

        if (needToAnnounce) {
//            setAnnounced(false);
            // stop the timer if it was running, and make sure event is broadcast
            final CountdownTimer timer2 = getTimer();
            if (timer2 != null && ! letClockRun) {
                // This also broadcasts an event to all listeners
                timer2.pause();
            }
            setTimeAllowed(timeAllowed(currentLifter));

            logger.trace("timeAllowed={}, timeRemaining={}", timeAllowed, timer2.getTimeRemaining()); //$NON-NLS-1$
        }

        if (currentLifter != null) {
            // copy values from current lifter.
            priorLifter = currentLifter;
            priorRequest = (currentLifter != null ? currentLifter.getNextAttemptRequestedWeight() : null);
            priorRequestNum = (currentLifter != null ? currentLifter.getAttemptsDone() : null);
        } else {
            priorLifter = null;
            priorRequest = null;
            priorRequestNum = null;
        }

        return needToAnnounce;
    }

    /**
     * Notify the listeners that the lifting order has changed.
     * @param needToAnnounce
     */
    void notifyListeners(boolean needToAnnounce) {
        // notify listeners to pick up the new information.
        final Lifter firstLifter = lifters.size() > 0 ? lifters.get(0) : null;
        logger.debug("notifyListeners() firing event, first lifter={}", firstLifter); //$NON-NLS-1$
        notifyListeners(new SessionDataUpdateEvent(this, firstLifter, needToAnnounce));
    }

    /**
     * Notify the listeners that the lifting order has changed.
     */
    void notifyListeners(SessionDataUpdateEvent e) {
        // notify listeners to pick up the new information.
        final Lifter firstLifter = lifters.size() > 0 ? lifters.get(0) : null;
        e.setCurrentLifter(firstLifter);
        fireEvent(e);
    }

    /**
     * Make the lists visible to all (including JSPs)
     */
    void publishListsToServletContext() {
        // make results available to all (including JSPs)
        final CompetitionApplication current = CompetitionApplication.getCurrent();
        final String platformName = current.getPlatformName();
        final CompetitionSession currentGroup = current.getCurrentCompetitionSession();
        String name = (currentGroup != null ? (String) currentGroup.getName() : null);
        ServletContext sCtx = current.getServletContext();
        if (sCtx != null) {
            logger.debug("current group for platformName " + platformName + " = " + name); //$NON-NLS-1$ //$NON-NLS-2$
            currentLiftingOrder = getAttemptOrder();
            currentDisplayOrder = getDisplayOrder();
            currentResultOrder = getResultOrder();
            sCtx.setAttribute("groupData_" + platformName, this); //$NON-NLS-1$
        }
    }

    public List<Lifter> getLifters() {
        return lifters;
    }

    /**
     * @return lifters in standard display order
     */
    public List<Lifter> getDisplayOrder() {
        return displayOrder;
    }

    /**
     * @return lifters in lifting order
     */
    public List<Lifter> getAttemptOrder() {
        return lifters;
    }


    /**
     * Check if lifter is following himself, and that no other lifter has been announced since (if time starts running for another lifter,
     * then the two minute privilege is lost).
     *
     * @param lifter
     *
     */
    public int timeAllowed(Lifter lifter) {
        logger.trace("timeAllowed start"); //$NON-NLS-1$
        // if clock was running for the current lifter, return the remaining
        // time.
        if (getTimer().getOwner() == lifter) {
            logger.trace("timeAllowed current lifter {} was running.", lifter); //$NON-NLS-1$
            int timeRemaining = getTimer().getTimeRemaining();
            if (timeRemaining < 0)
                timeRemaining = 0;
            // if the decision was not entered, and timer has run to 0, we still
            // want to see 0
            // if (timeRemaining > 0
            // //&& timeRemaining != 60000 && timeRemaining != 120000
            // ) {
            logger.info("resuming time for lifter {}: {} ms remaining", lifter, timeRemaining); //$NON-NLS-1$
            return timeRemaining;
            // }
        }
        logger.trace("not current lifter"); //$NON-NLS-1$
        final Lifter previousLifter = getPreviousLifter();
        if (previousLifter == null) {
            logger.trace("A one minute (first lifter): previousLifter null: startedLifters={} lifter={}", //$NON-NLS-1$
                    new Object[] { getTimer().getOwner(), lifter });
            return 60000;
        } else if (lifter.getAttemptsDone() % 3 == 0) {
            // no 2 minutes if starting snatch or starting c-jerk
            logger.trace("B one minute (first lifter): first attempt lifter={}", lifter); //$NON-NLS-1$
            return 60000;
        } else if (getTimer().getOwner() == null) {
            if (lifter.equals(previousLifter)) {
                logger.trace("C two minutes (same, timer did not start): startedLifters={} lifter={} previousLifter={}", //$NON-NLS-1$
                        new Object[] { getTimer().getOwner(), lifter, previousLifter });
                return 120000;
            } else {
                logger.trace("D one minute (not same): startedLifters={} lifter={} previousLifter={}", //$NON-NLS-1$
                        new Object[] { getTimer().getOwner(), lifter, previousLifter });
                return 60000;
            }
        } else {
            logger.trace("E one minute (same, timer started for someone else) : startedLifters={} lifter={} previousLifter={}", //$NON-NLS-1$
                    new Object[] { getTimer().getOwner(), lifter, previousLifter });
            return 60000;
        }
    }

    /**
     * @param lifter
     */
    @SuppressWarnings("unused")
    private void setTimerForTwoMinutes(Lifter lifter) {
        logger.info("setting timer owner to {}", lifter);
        getTimer().stop();
        getTimer().setOwner(lifter); // so time is kept for this lifter after
                                     // switcheroo
        getTimer().setTimeRemaining(120000);
    }

    private boolean forcedByTimekeeper = false;

    // public class LifterCall {
    // public Date callTime;
    // public Lifter lifter;
    //
    // LifterCall(Date callTime, Lifter lifter) {
    // this.callTime = callTime;
    // this.lifter = lifter;
    // }
    //
    // @Override
    // public String toString() {
    //            return lifter.toString() + "_" + callTime.toString(); //$NON-NLS-1$
    // }
    // }

    public void callLifter(Lifter lifter) {
        // beware: must call timeAllowed *before* setLifterAnnounced.

        CountdownTimer timer2 = getTimer();
        final int timeRemaining = timer2.getTimeRemaining();
        Long runningTimeRemaining = timer2.getRunningTimeRemaining();

        if (timeExpiredForCurrentLifter(lifter, timer2, timeRemaining, runningTimeRemaining))
            return;

        if (timer2.isRunning()) {
            logger.info("TIMER RUNNING! call of lifter {} :  - {}ms remaining", lifter, runningTimeRemaining); //$NON-NLS-1$
        } else if (isForcedByTimekeeper() && (timeRemaining == 120000 || timeRemaining == 60000)) {
            setForcedByTimekeeper(true, timeRemaining);
            logger.info("call of lifter {} : {}ms FORCED BY TIMEKEEPER", lifter, timeRemaining); //$NON-NLS-1$
        } else {
            if (!getTimeKeepingInUse()) {
                int allowed = getTimeAllowed();
                timer2.setTimeRemaining(allowed);
                logger.info("call of lifter {} : {}ms allowed", lifter, allowed); //$NON-NLS-1$
            } else {
                logger.info("call of lifter {} : {}ms remaining", lifter, timeRemaining); //$NON-NLS-1$
            }
            setForcedByTimeKeeper(false);
        }

        refereeDecisionController.reset();
        juryDecisionController.reset();
        announced = false;

        if (startTimeAutomatically) {
            startUpdateModel();
        } else if (!getTimeKeepingInUse()) {
            logger.info("setting lifter {} as owner", lifter);
            timer2.setOwner(lifter);
        }

        // we just did the announce.
        setAnnounced(true);
        SessionDataUpdateEvent e = SessionDataUpdateEvent.announceEvent(this);
        notifyListeners(e);
        return;
    }

    public boolean timeExpiredForCurrentLifter(Lifter lifter, CountdownTimer timer2, final int timeRemaining, Long runningTimeRemaining) {
        // time expired for lifter
        boolean timeExpiredForCurrentLifter = false;
        if (lifter == timer2.getOwner()) {
            if (timeRemaining <= 0 || (runningTimeRemaining != null && runningTimeRemaining <= 0)) {
                timer2.forceTimeRemaining(0, InteractionNotificationReason.CLOCK_EXPIRED);
                announced = true;
                timeExpiredForCurrentLifter = true;
            }
        }
        return timeExpiredForCurrentLifter;
    }

    /**
     * @param b
     */
    private void setForcedByTimeKeeper(boolean b) {
        this.forcedByTimekeeper = b;
    }

    public List<Lifter> getLiftTimeOrder() {
        return liftTimeOrder;
    }

    public void liftDone(Lifter lifter, boolean success) {
        logger.debug("lift done: notifiers={}", notifiers); //$NON-NLS-1$
        final CountdownTimer timer2 = getTimer();
        timer2.setOwner(null);
        timer2.stop(); // in case timekeeper has failed to stop it.
        timer2.setTimeRemaining(0);
//        setAnnounced(false); // now done in caller
        setTimerStarted(false);
    }

    CountdownTimer timer;

    public CountdownTimer getTimer() {
        if (timer == null) {
            timer = new CountdownTimer();
        }
        ;
        return timer;
    }

    /**
     * @param forcedByTimekeeper
     *            the forcedByTimekeeper to set
     * @param timeRemaining
     */
    public void setForcedByTimekeeper(boolean forcedByTimekeeper, int timeRemaining) {
        getTimer().forceTimeRemaining(timeRemaining);
        setForcedByTimeKeeper(forcedByTimekeeper);
    }

    /**
     * @return the forcedByTimekeeper
     */
    public boolean isForcedByTimekeeper() {
        return forcedByTimekeeper;
    }

    public void setStartTimeAutomatically(boolean b) {
        // if we start time automatically, announcing a lifter is the same as starting
        // the clock
        startTimeAutomatically = b;
    }

    /**
     * Remember which view is the current announcer view.
     *
     * @param announcerView
     */
    public void setAnnouncerView(ApplicationView announcerView) {
        this.announcerView = announcerView;
    }

    public ApplicationView getAnnouncerView() {
        return announcerView;
    }

    /**
     * @return the currentDisplayOrder
     */
    public List<Lifter> getCurrentDisplayOrder() {
        return currentDisplayOrder;
    }

    /**
     * @return the currentDisplayOrder
     */
    public List<Lifter> getCurrentResultOrder() {
        return currentResultOrder;
    }

    /**
     * @return the currentLiftingOrder
     */
    public List<Lifter> getCurrentLiftingOrder() {
        return currentLiftingOrder;
    }

    /* *********************************************************************************
     * Interactions with the context.
     */

    /**
     * Change the underlying session. Change the underlying session. When sessionData object has many browsers listening to it, it is
     * simpler to change the session than to recreate a new SessionData object. This method should only be used by AnnouncerView, when the
     * announcer.
     *
     * @param newCurrentSession
     *            the currentSession to set
     */
    void setCurrentSession(CompetitionSession newCurrentSession) {
        CompetitionSession oldCompetitionSession = getCurrentSession();

        // do this first, in case we get called us recursively
        this.currentSession = newCurrentSession;
        if (oldCompetitionSession != newCurrentSession) { // ok
            // synchronize with the application (if we were not called from there)
            logger.info("{} setting application group to {} (was {})", this,
                    (newCurrentSession != null ? newCurrentSession.getName() : "nil"),
                    (oldCompetitionSession != null ? oldCompetitionSession.getName() : "nil")
                    ); //$NON-NLS-1$
            CompetitionApplication current = CompetitionApplication.getCurrent();
            ApplicationView currentView = current.getCurrentView();
            if (currentView != null) currentView.switchGroup(newCurrentSession);
            current.setCurrentCompetitionSession(newCurrentSession);
        }


        loadData();
        sortLists(false);
        publishListsToServletContext();
        setTimeKeepingInUse(false); // will switch to true if Start/stop is used.
        getTimer().forceTimeRemaining(getDisplayTime());
        // tell listeners to refresh.
        fireEvent(SessionDataUpdateEvent.refreshEvent(this));
    }

    /**
     * @return the currentSession
     */
    public CompetitionSession getCurrentSession() {
        // if (currentSession != null) {
        // final String name = currentSession.getName();
        // LoggerUtils.mdcPut(LoggerUtils.LoggingKeys.currentGroup, ">"+name);
        // }
        return currentSession;
    }

    public void setMasterApplication(CompetitionApplication app2) {
        logger.debug("setting as master application {}", app2);
        this.masterApplication = app2;
    }

    public CompetitionApplication getMasterApplication() {
        return masterApplication;
    }

    public Platform getPlatform() {
        Platform oPlatform = platform;
        // truly braindead way to fetch the current platform in the cache
        try {
            platform = Platform.getByName(platform.getName());
            logger.debug("getting platform for name {} = {}", platform.getName(), System.identityHashCode(platform));
        } catch (Exception e) {
            return oPlatform; // return the current platform (required for tests)
        }
        return platform;
    }

    public void setPlatform(Platform platform) {
        // braindead way to fetch the current platform in the cache, as opposed to a stale copy.
        this.platform = Platform.getByName(platform.getName());
    }

    /* *********************************************************************************
     * UpdateEvent framework.
     */

    private EventRouter eventRouter = new EventRouter();
    private boolean startTimeAutomatically = false;
    private ApplicationView announcerView;
    private CompetitionApplication masterApplication;
    private boolean announced = true;
    public Item publicAddressItem;
    private IntermissionTimer publicAddressTimer = new IntermissionTimer(this);
    private Platform platform;
    private boolean timerStarted;

    public boolean getAnnouncerEnabled() {
        return announced;
    }

    /**
     * Listener interface for receiving <code>SessionData.UpdateEvent</code>s.
     */
    public interface SessionDataUpdateEventListener extends java.util.EventListener {

        /**
         * This method will be invoked when a SessionData.UpdateEvent is fired.
         *
         * @param sessionDataUpdateEvent
         *            the event that has occured.
         */
        public void sessionDataUpdateEvent(SessionDataUpdateEvent sessionDataUpdateEvent);
    }

    /**
     * This method is the Java object for the method in the Listener interface. It allows the framework to know how to pass the event
     * information.
     */

    private static final Method LIFTER_EVENT_METHOD = EventHelper.findMethod(
            SessionDataUpdateEvent.class, // when receiving this type of event
            SessionData.SessionDataUpdateEventListener.class, // an object implementing this interface...
            "sessionDataUpdateEvent"); // ... will be called with this method. //$NON-NLS-1$;

    /**
     * Broadcast a SessionData.event to all registered listeners
     *
     * @param sessionDataUpdateEvent
     *            contains the source (ourself) and the list of properties to be refreshed.
     */
    protected void fireEvent(SessionDataUpdateEvent sessionDataUpdateEvent) {
        // logger.trace("SessionData: firing event from groupData"+System.identityHashCode(this)+" first="+updateEvent.getCurrentLifter()+" eventRouter="+System.identityHashCode(eventRouter));
        // logger.trace("                        listeners"+eventRouter.dumpListeners(this));
        if (eventRouter != null) {
            eventRouter.fireEvent(sessionDataUpdateEvent);
        }

    }

    /**
     * Register a new SessionData.Listener object with a SessionData in order to be informed of updates.
     *
     * @param listener
     */
    public void addListener(SessionDataUpdateEventListener listener) {
        String id = "";
        if (listener instanceof EditingView)
            id = ((EditingView) listener).getLoggingId();
        listenerLogger.debug("add listener {} {}", listener, id); //$NON-NLS-1$
        getEventRouter().addListener(SessionDataUpdateEvent.class, listener, LIFTER_EVENT_METHOD);
    }

    /**
     * Remove a specific SessionData.Listener object
     *
     * @param listener
     */
    public void removeListener(SessionDataUpdateEventListener listener) {

        String id = "";
        if (listener instanceof EditingView)
            id = ((EditingView) listener).getLoggingId();
        if (eventRouter != null) {
            listenerLogger.debug("remove listener {} {}", listener, id); //$NON-NLS-1$
            eventRouter.removeListener(SessionDataUpdateEvent.class, listener, LIFTER_EVENT_METHOD);
        }
    }

    /*
     * General event framework: we implement the com.vaadin.event.MethodEventSource interface which defines how a notifier can call a method
     * on a listener to signal an event an event occurs, and how the listener can register/unregister itself.
     */

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.event.MethodEventSource#addListener(java.lang.Class, java.lang.Object, java.lang.reflect.Method)
     */
    @SuppressWarnings("rawtypes")
    public void addListener(Class eventType, Object object, Method method) {
        getEventRouter().addListener(eventType, object, method);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.event.MethodEventSource#addListener(java.lang.Class, java.lang.Object, java.lang.String)
     */
    @SuppressWarnings("rawtypes")
    public void addListener(Class eventType, Object object, String methodName) {
        getEventRouter().addListener(eventType, object, methodName);
    }

    /**
     * @return the object's event router.
     */
    private EventRouter getEventRouter() {
        if (eventRouter == null) {
            eventRouter = new EventRouter();
            logger
                    .trace("new event router for groupData " + System.identityHashCode(this) + " = " + System.identityHashCode(eventRouter)); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return eventRouter;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.event.MethodEventSource#removeListener(java.lang.Class, java.lang.Object)
     */
    @SuppressWarnings("rawtypes")
    public void removeListener(Class eventType, Object target) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.event.MethodEventSource#removeListener(java.lang.Class, java.lang.Object, java.lang.reflect.Method)
     */
    @SuppressWarnings("rawtypes")
    public void removeListener(Class eventType, Object target, Method method) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target, method);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.event.MethodEventSource#removeListener(java.lang.Class, java.lang.Object, java.lang.String)
     */
    @SuppressWarnings("rawtypes")
    public void removeListener(Class eventType, Object target, String methodName) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target, methodName);
        }
    }

    public void removeAllListeners() {
        if (eventRouter != null) {
            eventRouter.removeAllListeners();
        }
    }

    /**
     * Change who the lift list is listening to, unless the notifier being removed is the top in the list.
     *
     * @param lifter
     * @param editor
     * @param firstLifter
     */
    public void stopListeningTo(final Lifter lifter, Component editor) {
        if (lifter == null)
            return;
        notificationManager.removeEditor(lifter, editor);
    }

    /**
     * Change who the lift list is listening to.
     *
     * @param lifter
     * @param editor
     */
    public void listenToLifter(final Lifter lifter, Component editor) {
        if (lifter == null)
            return;
        notificationManager.addEditor(lifter, editor);
    }

    /**
     * Makes this class visible to other sessions so they can call addListener .
     *
     * @param platformName
     */
    void registerAsMasterData(String platformName) {
        // make ourselves visible to other parts of the web application (e.g.
        // JSP pages).
        CompetitionApplication current = CompetitionApplication.getCurrent();
        final ServletContext servletContext = ((CompetitionApplication) current).getServletContext();
        if (servletContext != null) {
            servletContext.setAttribute(SessionData.MASTER_KEY + platformName, this);
            servletContext.setAttribute(SessionData.MASTER_KEY + "AllLifters", current);
            logger.info("Master data registered for platform {}={}", platformName, this); //$NON-NLS-1$ //$NON-NLS-2$
            //LoggerUtils.traceBack(logger);
        }
    }

    /**
     * Makes this class visible to other sessions so they can call addListener .
     *
     * @param platformName
     */
    static void publishAsGroupData(String groupName, SessionData sd) {
        // make ourselves visible to other parts of the web application (e.g.
        // JSP pages).
        CompetitionApplication current = CompetitionApplication.getCurrent();
        final ServletContext servletContext = ((CompetitionApplication) current).getServletContext();
        if (servletContext != null) {
            String key = SessionData.MASTER_KEY + (groupName == null ? "*" : groupName);
            servletContext.setAttribute(key, sd);
            logger.info("Group data registered {}={} {} lifters", key, sd, sd.getLifters().size()); //$NON-NLS-1$ //$NON-NLS-2$
            //LoggerUtils.traceBack(logger);
        }
    }

    /**
     * Copied from interface. React to lifter changes by recomputing the lists.
     *
     * @see org.concordiainternational.competition.data.Lifter.UpdateEventListener#updateEvent(org.concordiainternational.competition.data.Lifter.UpdateEvent)
     */
    @Override
    public void updateEvent(Lifter.UpdateEvent updateEvent) {
        List<String> propertyIds = updateEvent.getPropertyIds();
        logger.debug("lifter {}, changed {}", updateEvent.getSource(), propertyIds); //$NON-NLS-1$
        boolean automaticProgression = false;
        if (propertyIds != null) automaticProgression = propertyIds.contains("automatic");
        boolean declaration = false;
        for (String propertyId: propertyIds) {
            declaration = propertyId.endsWith("Declaration");
            if (declaration) break;
        }
        Lifter source = (Lifter) updateEvent.getSource();

        // we don't want to stop the clock if the coach declares the same weight as was automatically determined
        boolean declarationSameAsAutomatic = false;
        if (declaration) {
            Object currentAutomatic = source.getCurrentAutomatic();
            // if the automatic declaration is changed after other data has been entered (e.g. correcting typo), stop clock.
            String currentChange1 = source.getCurrentChange1();
            declarationSameAsAutomatic = Objects.equals(source.getCurrentDeclaration(),currentAutomatic) && (currentChange1 != null && currentChange1.isEmpty());
        }

        updateListsForLiftingOrderChange(source, automaticProgression, declarationSameAsAutomatic);
        persistPojo(updateEvent.getSource());
    }

    public Lifter getCurrentLifter() {
        return currentLifter;
    }

    /**
     * @param liftTimeOrder
     *            the liftTimeOrder to set
     */
    void setLiftTimeOrder(List<Lifter> liftTimeOrder) {
        this.liftTimeOrder = liftTimeOrder;
    }

    /**
     * @param resultOrder
     *            the resultOrder to set
     */
    void setResultOrder(List<Lifter> resultOrder) {
        this.resultOrder = resultOrder;
    }

    /**
     * @return the resultOrder
     */
    List<Lifter> getResultOrder() {
        return resultOrder;
    }

    /**
     * Register the fact that component comp is now editing newLifter instead of previousLifter
     *
     * @param newLifter
     * @param previousLifter
     * @param comp
     */
    public void trackEditors(Lifter newLifter, Lifter previousLifter, Component comp) {
        logger.trace("previousLifter = {}, lifter = {}", previousLifter, newLifter);; //$NON-NLS-1$
        if (previousLifter != newLifter) {
            // stopListeningTo actually waits until no editor is left to stop
            // listening
            stopListeningTo(previousLifter, comp);
        }
        if (newLifter != null) {
            listenToLifter(newLifter, comp);
        }
    }

    /**
     * @param timeAllowed
     *            the timeAllowed to set
     */
    private void setTimeAllowed(int timeAllowed) {
        this.timeAllowed = timeAllowed;
    }

    /**
     * @return the timeAllowed
     */
    public int getTimeAllowed() {
        return timeAllowed;
    }

    public int getTimeRemaining() {
        if (timer != null) {
            Long runningTimeRemaining = timer.getRunningTimeRemaining();
            return runningTimeRemaining != null ? runningTimeRemaining.intValue() : timer.getTimeRemaining() ;
        } else {
            return timeAllowed;
        }

    }

    public boolean getAllowAll() {
        return allowAll;
    }

    public void setAllowAll(boolean allowAll) {
        this.allowAll = allowAll;
    }

    public IDecisionController getRefereeDecisionController() {
        return refereeDecisionController;
    }

    public IDecisionController getJuryDecisionController() {
        return juryDecisionController;
    }

    public void majorityDecision(Decision[] refereeDecisions) {
        final Lifter currentLifter2 = getCurrentLifter();
        int pros = 0;
        for (int i = 0; i < refereeDecisions.length; i++) {
            if (refereeDecisions[i] != null && refereeDecisions[i].accepted)
                pros++;
        }
        final boolean success = pros >= 2;
        liftDone(currentLifter2, success);
        if (success) {
            logger.info("Referee decision for {}: GOOD lift",currentLifter2);
            if (currentLifter2 != null)
                currentLifter2.successfulLift();
        } else {
            logger.info("Referee decision for {}: NO lift",currentLifter2);
            if (currentLifter2 != null)
                currentLifter2.failedLift();
        }

        // record the decision.
        if (currentLifter2 != null) {
            saveLifter(currentLifter2);
        } else {
            logger.warn("No current lifter.");
        }
    }

    /**
     * @param currentLifter2
     */
    public void saveLifter(final Lifter currentLifter2) {
        CompetitionApplication current = CompetitionApplication.getCurrent();
        Session session = current.getHbnSession();
        session.merge(currentLifter2);
        session.flush();
        try {
            session.getTransaction().commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void downSignal() {
//        final CountdownDisplay countDownDisplay = (CountdownDisplay) getTimer().getCountdownDisplay();
//        if (countDownDisplay != null) {
//            DecisionLights dl = countDownDisplay.getDecisionLights();
//            if (dl != null) {
//                logger.debug("down signal from session data");
//                dl.doDown();
//            } else {
//                logger.error("decision lights is null");
//            }
//        }
        notifyPrematureDecision();
    }

    /**
	 *
	 */
    synchronized public void notifyPrematureDecision() {
        CountdownTimer timer2 = getTimer();
        if (!isAnnounced()) {
            timer2.stop(InteractionNotificationReason.NOT_ANNOUNCED);
        } else if (timeKeepingInUse && timer2.isRunning()) {
            timer2.stop(InteractionNotificationReason.REFEREE_DECISION);
        } else if (timeKeepingInUse && !isTimerStarted()) {
            timer2.stop(InteractionNotificationReason.NO_TIMER);
        }
    }

    public void setAnnounced(boolean b) {
        //logger.debug("announced = {}",b);
        announced = b;
//            LoggerUtils.traceBack(logger, "setAnnounced");
    }

    public boolean isAnnounced() {
        return announced;
    }

    /**
     * @param timeKeepingInUse
     *            the timeKeepingInUse to set
     */
    public void setTimeKeepingInUse(boolean timeKeepingInUse) {
        this.timeKeepingInUse = timeKeepingInUse;
    }

    /**
     * @return the timeKeepingInUse
     */
    public boolean getTimeKeepingInUse() {
        return timeKeepingInUse;
    }

    // /**
    // * @param needToAnnounce
    // * the needToAnnounce to set
    // */
    // public void setNeedToAnnounce(boolean needToAnnounce) {
    // this.needToAnnounce = needToAnnounce;
    // }
    //
    // /**
    // * @return the needToAnnounce
    // */
    // public boolean getNeedToAnnounce() {
    // return needToAnnounce;
    // }

    /**
     * @return
     */
    public boolean getStartTimeAutomatically() {
        return startTimeAutomatically;
    }

    /**
     * @param lifter
     * @param groupData
     */
    public void manageTimerOwner(Lifter lifter, SessionData groupData, CountdownTimer timing) {
        // first time we use the timekeeper button or that we announce
        // with the automatic start determines that
        // there is a timekeeper and that timekeeper runs clock.
        groupData.setTimeKeepingInUse(true);

        if (lifter != timing.getOwner()) {
            if (!isForcedByTimekeeper()) {
                final int remaining = groupData.getTimeAllowed();
                timing.setTimeRemaining(remaining);
            } else {
                logger.info("forced by timekeeper: {} remaining", getTimeRemaining());
            }
            timing.setOwner(lifter); // enforce rule 6.6.8
            logger.debug("timekeeping in use, setting lifter {} as owner", lifter);
        }
    }

    // /**
    // * @param lifter
    // * @param groupData
    // */
    // private void startTimer(Lifter lifter, SessionData groupData,CountdownTimer timing) {
    // manageTimerOwner(lifter,groupData, timing);
    // timing.restart();
    // }

    public Item getPublicAddressItem() {
        return publicAddressItem;
    }

    public void setPublicAddressItem(Item publicAddressItem) {
        this.publicAddressItem = publicAddressItem;
    }

    public void clearPublicAddressDisplay() {
        PublicAddressMessageEvent event = new PublicAddressMessageEvent();
        // more intuitive if hiding the display does not stop the timer.
        // publicAddressTimer.stop();
        event.setHide(true);
        fireBlackBoardEvent(event);
    }

    public void displayPublicAddress() {
        IntermissionTimer timer1 = (IntermissionTimer) publicAddressItem.getItemProperty("remainingSeconds").getValue();
        int remainingMilliseconds = timer1.getRemainingMilliseconds();

        // tell the registered browsers to pop-up the message area
        PublicAddressMessageEvent messageEvent = new PublicAddressMessageEvent();
        messageEvent.setHide(false);
        messageEvent.setTitle((String) publicAddressItem.getItemProperty("title").getValue());
        messageEvent.setMessage((String) publicAddressItem.getItemProperty("message").getValue());
        messageEvent.setRemainingMilliseconds(remainingMilliseconds);
        fireBlackBoardEvent(messageEvent);

        // tell the message areas to display the initial time
        IntermissionTimerEvent timerEvent = new IntermissionTimerEvent();
        timerEvent.setRemainingMilliseconds(remainingMilliseconds);
        fireBlackBoardEvent(timerEvent);

    }

    /**
     * @param event
     */
    public void fireBlackBoardEvent(Event event) {
        blackBoardEventRouter.fire(event);
    }

    public void addBlackBoardListener(Listener listener) {
        blackBoardEventRouter.addListener(listener);
    }

    public void removeBlackBoardListener(Listener listener) {
        blackBoardEventRouter.removeListener(listener);
    }

    public IntermissionTimer getIntermissionTimer() {
        return publicAddressTimer;
    }

    void noCurrentLifter() {
        // most likely completely obsolete.
        // getTimer().removeAllListeners();
    }

    public void refresh(boolean isMaster) {
        setCurrentSession(this.getCurrentSession());
        if (isMaster) {
            // get current platform back from database
            // Note: should use entity refresh
            Platform curPlatform = this.getPlatform();
            if (curPlatform != null) {
                String platformName = curPlatform.getName();
                Platform refreshedPlatform = Platform.getByName(platformName);
                // setPlatform forces the audio to switch
                this.setPlatform(refreshedPlatform);
            }

        }
    }

    public int getDisplayTime() {
        if (currentLifter != timer.getOwner()) {
            return getTimeAllowed();
        } else {
            return getTimeRemaining();
        }
    }

    public void startUpdateModel() {
        final CountdownTimer timer1 = this.getTimer();
        final Lifter lifter = getCurrentLifter();

        CountdownTimer timer2 = getTimer();
        final int timeRemaining = timer2.getTimeRemaining();
        Long runningTimeRemaining = timer2.getRunningTimeRemaining();

        if (timeExpiredForCurrentLifter(lifter, timer2, timeRemaining, runningTimeRemaining))
            return;

        manageTimerOwner(lifter, this, timer1);
        final boolean running = timer1.isRunning();
        timingLogger.debug("start timer.isRunning()={}", running); //$NON-NLS-1$
        timer1.restart();
        setTimerStarted(true);
        getRefereeDecisionController().setBlocked(false);
    }

    public void stopUpdateModel() {
        getTimer().pause(); // pause() does not clear the associated lifter
    }

    public void oneMinuteUpdateModel() {
        if (getTimer().isRunning()) {
            timer.forceTimeRemaining(60000); // pause() does not clear the associated lifter
        }
        setForcedByTimekeeper(true, 60000);
    }

    public void twoMinuteUpdateModel() {
        if (getTimer().isRunning()) {
            timer.forceTimeRemaining(120000); // pause() does not clear the associated lifter
        }
        setForcedByTimekeeper(true, 120000);
    }

    public void okLiftUpdateModel() {
        Lifter currentLifter2 = getCurrentLifter();
        liftDone(currentLifter2, true);
        currentLifter2.successfulLift();
    }

    public void noLiftUpdateModel() {
        Lifter currentLifter2 = getCurrentLifter();
        liftDone(currentLifter2, false);
        currentLifter2.failedLift();
    }

    private boolean isTimerStarted() {
        return timerStarted;
    }

    private void setTimerStarted(boolean timerStarted) {
        this.timerStarted = timerStarted;
    }

    public static SessionData getIndependentInstance(String groupName1) {
        // TODO finish this.
        CompetitionSession currentGroup = null;
        List<CompetitionSession> allGroups = CompetitionSession.getAll();
        if (groupName1 != null && groupName1.trim().length() >0) {
            // locate by name
            for (CompetitionSession cg : allGroups) {
                if (groupName1.equals(cg.getName())) {
                    currentGroup = cg;
                    break;
                }
            }
        }
        // create a new sessionData instance by brute force.
        SessionData sd = null;
        if (currentGroup != null) {
            sd = new SessionData(currentGroup, Lifter.getAllCurrentGroup(currentGroup, true));
        } else {
            sd = new SessionData(null, Lifter.getAll(true));
        }
        publishAsGroupData(groupName1, sd);
        return sd;
    }

}
