/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.ui;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map.Entry;

import javax.naming.OperationNotSupportedException;

import org.concordiainternational.competition.data.Competition;
import org.concordiainternational.competition.data.CompetitionSession;
import org.concordiainternational.competition.data.Platform;
import org.concordiainternational.competition.data.RuleViolationException;
import org.concordiainternational.competition.decision.DecisionEvent;
import org.concordiainternational.competition.decision.DecisionEvent.Type;
import org.concordiainternational.competition.decision.DecisionEventListener;
import org.concordiainternational.competition.decision.IDecisionController;
import org.concordiainternational.competition.i18n.Messages;
import org.concordiainternational.competition.publicAddress.PublicAddressForm;
import org.concordiainternational.competition.spreadsheet.JXLSCompetitionBook;
import org.concordiainternational.competition.spreadsheet.JXLSResultSheet;
import org.concordiainternational.competition.spreadsheet.JXLSTimingStats;
import org.concordiainternational.competition.spreadsheet.JXLSWorkbookStreamSource;
import org.concordiainternational.competition.ui.SessionData.SessionDataUpdateEventListener;
import org.concordiainternational.competition.ui.components.ApplicationView;
import org.concordiainternational.competition.ui.components.ResultFrame;
import org.concordiainternational.competition.ui.components.SessionSelect;
import org.concordiainternational.competition.ui.components.Stylable;
import org.concordiainternational.competition.ui.list.ToolbarView;
import org.concordiainternational.competition.utils.ItemAdapter;
import org.concordiainternational.competition.utils.LoggerUtils;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import com.vaadin.data.Item;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.SystemError;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;
import com.vaadin.ui.Window.Notification;

public class SecretaryResultsView extends ToolbarView implements
ApplicationView,
CloseListener,
DecisionEventListener,
Stylable,
Bookmarkable
{

    private static final long serialVersionUID = 5562100583893230718L;

    protected boolean editable = false;

    private CompetitionApplication app;

    Logger logger = (Logger) LoggerFactory.getLogger(SecretaryResultsView.class);
    Logger listenerLogger = (Logger) LoggerFactory.getLogger("listeners." + ResultFrame.class.getSimpleName()); //$NON-NLS-1$

    private String viewName;
    private String platformName;
    private String groupName;
    private Embedded iframe;
    private String urlString;
    private SessionData groupData;
    private SessionData masterData;
    private String appUrlString;
    private SessionDataUpdateEventListener updateListener;
    private EditingView parentView = null;

    public SecretaryResultsView(boolean initFromFragment, String fragment, String viewName, String platformNameParam, String urlString, String stylesheetName, CompetitionApplication app) {
        setViewName(viewName);
        //logger.setLevel(Level.WARN);

        this.app = app;
        this.platformName = platformNameParam;
        this.urlString = urlString;

        LoggerUtils.mdcPut(LoggerUtils.LoggingKeys.view, getLoggingId());

        if (initFromFragment) {
            setParametersFromFragment(fragment);
            setStylesheetName("tv");
        } else {
            setViewName(viewName);
            setStylesheetName(stylesheetName);
        }

        init();
        registerAsListener();
    }

    @Override
    protected void init() {
        setGroupAndMasterData(app, groupName);

        getAppUrlString();

        try {
            createLayout();
            display(platformName, groupName, masterData);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        LoggerUtils.mdcPut(LoggerUtils.LoggingKeys.currentGroup, getLoggingId());
        logger.debug("groupData on platform {}, group {}", platformName, groupName);

        // we cannot call push() at this point
        synchronized (app) {
            boolean prevDisabled = app.getPusherDisabled();
            try {
                app.setPusherDisabled(true);
                app.getUriFragmentUtility().setFragment(getFragment(), false);
                display(platformName, groupName, groupData);
            } finally {
                app.setPusherDisabled(prevDisabled);
            }
            // logger.debug("browser panel: push disabled = {}", app.getPusherDisabled());
        }
    }

    /**
     * if group currently lifting, groupData and masterData are set to the correct SessionData (dynamically updated)
     * if the groupName is defined, but group is not currently lifting, an independent (not updated) groupData is setup, and masterData is null.
     * @param app1
     */
    private void setGroupAndMasterData(CompetitionApplication app1, String groupName1) {
        logger.trace("findMasterData  groupName1="+groupName1+" groupName="+groupName);
        if (masterData != null) {
            unregisterAsMasterDataListener();
        }

        boolean found = false;
        masterData = null;
        groupName = groupName1;
        if (groupName1 != null) {
            // group name given, check platforms to see if active.
            for (Entry<String, Platform> e : Platform.getPlatformMap().entrySet()) {
                String platformName1 = e.getKey();
                Platform platform1 = e.getValue();
                groupData = app1.getMasterData(platformName1);
                if (groupData != null) {
                    final CompetitionSession currentPlatformGroup = groupData.getCurrentSession();
                    if (currentPlatformGroup != null && groupName1.equals(currentPlatformGroup.getName())) {
                        found = true;
                        masterData = groupData;
                        app1.setCurrentGroup(currentPlatformGroup);
                        app1.setPlatform(platform1);
                        logger.debug("new current group {}/{}", platformName, currentPlatformGroup.getName()); //$NON-NLS-1$
                        app.getUriFragmentUtility().setFragment(getFragment(), false);

                        registerAsMasterDataListener();
                        break;
                    }
                }
            }
        }
        if (groupName1 == null || !found) {
            // get the session data -- no update will take place; if no groupName then all lifters.
            masterData = null;
            groupData = SessionData.getIndependentInstance(groupName1);
            app1.setCurrentGroup(groupData.getCurrentSession());
            app1.setPlatform(null);
            logger.debug("new current group {}/{}", "notActive", groupName1); //$NON-NLS-1$
        }
        this.groupName = groupName1;
    }

    /**
     * Compute where we think the jsp file ought to be.
     */
    private void getAppUrlString() {
        appUrlString = app.getURL().toExternalForm();
        int lastSlash = appUrlString.lastIndexOf("/");
        if (lastSlash == appUrlString.length() - 1) {
            // go back one more slash, the string ended with /
            lastSlash = appUrlString.lastIndexOf("/", lastSlash - 1);
        }
        appUrlString = appUrlString.substring(0, lastSlash + 1);
        // System.err.println("appUrlString with slash="+appUrlString);
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
                            CompetitionSession currentSession = masterData1.getCurrentSession();
                            //logger.trace("updateEvent {} currentGroup={} newGroup={}", SecretaryResultsView.this, groupName, (currentSession != null ?currentSession.getName(): "*"));
                            sessionSelect.select(currentSession.getId());
                            display(platformName, groupName, masterData1);
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

        tableToolbar = createTableToolbar();
        this.addComponent(tableToolbar);

        iframe = new Embedded();
        iframe.setType(Embedded.TYPE_BROWSER);
        iframe.setSizeFull();
        this.addComponent(iframe);

        this.setExpandRatio(iframe, 100);
    }

    /**
     * @param platformName1
     * @return
     * @throws RuntimeException
     */
    protected URL computeUrl(final String platformName1, final String groupName1) throws RuntimeException {
        URL url;
        String encodedPlatformName = null;
        if (platformName1 != null) {
            try {
                encodedPlatformName = "platformName=" + URLEncoder.encode(platformName1, "UTF-8");
                // System.err.println(encodedPlatformName);
            } catch (UnsupportedEncodingException e1) {
                throw new RuntimeException(e1);
            }
        }
        String styleSheet = getStylesheetName();
        if (styleSheet == null || styleSheet.isEmpty()) {
            styleSheet = "";
        } else {
            styleSheet = "&style=" + getStylesheetName() + ".css";
        }
        String encodedGroupName = "";
        if (groupName1 != null) {
            try {
                encodedGroupName = "groupName=" + URLEncoder.encode(groupName1, "UTF-8");
            } catch (UnsupportedEncodingException e1) {
                throw new RuntimeException(e1);
            }
        } else {
            try {
                encodedGroupName = "groupName=" + URLEncoder.encode("*", "UTF-8");
            } catch (UnsupportedEncodingException e1) {
                throw new RuntimeException(e1);
            }
        }

        final String spec = appUrlString + urlString + (masterData != null ? (encodedPlatformName != null ? encodedPlatformName : "") : encodedGroupName) + styleSheet + "&time=" + System.currentTimeMillis(); //$NON-NLS-1$
        try {
            url = new URL(spec);
            // logger.debug("url={} {}", url.toExternalForm(), this);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e); // can't happen.
        }
        return url;
    }

    /**
     * @param platformName1
     * @param masterData1
     * @throws RuntimeException
     */
    protected void display(final String platformName1, final String groupName1, final SessionData masterData1) throws RuntimeException {
        synchronized (app) {
            URL url = computeUrl(platformName1, groupName1);
            logger.trace("display {}", url, getStylesheetName());
            // LoggerUtils.traceBack(logger,"display()");
            iframe.setSource(new ExternalResource(url));
        }
        // logger.debug("prior to display push disabled={}", app.getPusherDisabled());
        app.push();
    }

    @Override
    public void refresh() {
        CompetitionSession nSession = app.getCurrentCompetitionSession();
        CompetitionSession oSession = groupData.getCurrentSession();

        if (nSession != null && !(nSession.equals(oSession))) {
            setGroupAndMasterData(app, nSession.getName());
            logger.trace("switch to different group - platform="+platformName+" groupName="+groupName + " nSession="+(nSession != null ? nSession.getName() : "null")+" masterData="+masterData+" groupData="+groupData);
            if (logger.isTraceEnabled()) LoggerUtils.traceBack(logger,"switch1");
            display(platformName, nSession.getName(), (masterData != null ? masterData : groupData));
        } else if (nSession == null) {
            setGroupAndMasterData(app, null);
            System.err.println("switch to null - platform="+platformName+" groupName="+groupName + " nSession="+(nSession != null ? nSession.getName() : "null")+" masterData="+masterData+" groupData="+groupData);
            if (logger.isTraceEnabled()) LoggerUtils.traceBack(logger,"switch1");
            display(platformName, null, (masterData != null ? masterData : groupData));
        }  else {
            System.err.println("switch to same group - platform="+platformName+" groupName="+groupName + " nSession="+(nSession != null ? nSession.getName() : "null")+" masterData="+masterData+" groupData="+groupData);
            if (logger.isTraceEnabled()) LoggerUtils.traceBack(logger,"switch2");
            display(platformName, groupName, (masterData != null ? masterData : groupData));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.concordiainternational.competition.ui.components.ApplicationView#needsMenu()
     */
    @Override
    public boolean needsMenu() {
        return true;
    }

    /**
     * @return
     */
    @Override
    public String getFragment() {
        String fragment;
        if (masterData != null) {
            platformName = masterData.getPlatform().getName();
            fragment = viewName + "/" + safeEncode(platformName == null ? "" : platformName);
        } else if (groupData != null) {
            CompetitionSession groupSession = groupData.getCurrentSession();
            groupName = groupSession != null ? groupSession.getName() : "*";
            fragment = viewName + "//" +safeEncode(groupName);
        } else {
            fragment = viewName + "//*";
        }
        return fragment;
    }

    public String safeEncode(String fragment) {
        try {
            return URLEncoder.encode(fragment, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
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
        logger.debug("fragment={} {}",frag, params.length);
        if (params.length >= 2) {
            platformName = params[1];
            if (platformName.trim().isEmpty()) platformName = null;
        } else {
            platformName = CompetitionApplicationComponents.initPlatformName();
        }
        if (params.length >= 3) {
            groupName = params[2];
        } else {
            groupName = null;
        }
    }

    @Override
    public void registerAsListener() {
//        LoggerUtils.traceBack(listenerLogger);

        registerAsMasterDataListener();

        // listen to close events
        app.getMainWindow().addListener(this);
        listenerLogger.debug("{} listening to window close events.", this);
    }

    private void registerAsMasterDataListener() {
        if (masterData != null) {
            LoggerUtils.mdcSetup(getLoggingId(), masterData);
            // listen to changes in the competition data
            if (updateListener == null) {
                updateListener = createUpdateSessionUpdateListener(platformName, masterData);
            }
            masterData.addListener(updateListener);
            listenerLogger.debug("{} listening to session data updates.", updateListener);

            // listen to decisions
            IDecisionController decisionController = masterData.getRefereeDecisionController();
            if (decisionController != null) {
                decisionController.addListener(this);
                listenerLogger.debug("{} listening to decision events.", this);
            }
        }
    }

    @Override
    public void unregisterAsListener() {
        if (logger.isTraceEnabled()) LoggerUtils.traceBack(listenerLogger);
        LoggerUtils.mdcPut(LoggerUtils.LoggingKeys.view, getLoggingId());

        unregisterAsMasterDataListener();

        // stop listening to close events
        app.getMainWindow().removeListener(this);
        listenerLogger.debug("{} stopped listening to window close events..", this);
    }

    private void unregisterAsMasterDataListener() {
        // stop listening to changes in the competition data
        if (updateListener != null && masterData != null) {
            masterData.removeListener(updateListener);
            listenerLogger.debug("{} stopped listening session data updates", updateListener);


        // stop listening to decisions
            IDecisionController decisionController = masterData.getRefereeDecisionController();
            if (decisionController != null && masterData != null) {
                decisionController.removeListener(this);
                listenerLogger.debug("{} stopped listening to decision events.", this);
            }
        }
    }

    @Override
    public void windowClose(CloseEvent e) {
        unregisterAsListener();
    }

    @Override
    public boolean needsBlack() {
        return false;
    }

    private static int classCounter = 0; // per class
    private final int instanceId = classCounter++; // per instance

    private String stylesheetName;

    private SessionSelect sessionSelect;

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
        CompetitionSession oldSession = masterData.getCurrentSession();
        boolean switching = oldSession != newSession;

        if (switching) {
            logger.debug("=============== switching from {} to group {}", oldSession, newSession); //$NON-NLS-1$
            logger.debug("=============== modifying group data {}", masterData, (newSession != null ? newSession.getName() : null)); //$NON-NLS-1$
            masterData.setCurrentSession(newSession);
        }

        CompetitionSession currentCompetitionSession = masterData.getCurrentSession();
        if (currentCompetitionSession != null) {
            groupName = currentCompetitionSession.getName();
        } else {
            groupName = "";
        }

        if (switching) {
            app.getUriFragmentUtility().setFragment(getFragment(), false);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.concordiainternational.competition.ui.components.Stylable#setStylesheet(java.lang.String)
     */
    @Override
    public void setStylesheetName(String stylesheetName) {
        this.stylesheetName = stylesheetName;
    }

    @Override
    public String getStylesheetName() {
        return stylesheetName;
    }

    @Override
    public void updateEvent(DecisionEvent updateEvent) {
        Type type = updateEvent.getType();
        if (type == DecisionEvent.Type.SHOW || type == DecisionEvent.Type.UPDATE) {
            logger.trace("decision given accepted={}",updateEvent.isAccepted());
            display(platformName, groupName, (masterData != null ? masterData : groupData));
        }
    }

    @Override
    protected void createBottom() {
        throw new RuntimeException(new OperationNotSupportedException());
    }

    @Override
    protected void setButtonVisibility() {
        // Intentionally empty, no edit mode
    }

    @Override
    protected void createToolbarButtons(HorizontalLayout tableToolbar1) {
        // we do not call super because the default buttons are inappropriate.
        final Locale locale = app.getLocale();
        sessionSelect = new SessionSelect((CompetitionApplication) app, locale, this);
        tableToolbar1.addComponent(sessionSelect);

        {
            final Button resultSpreadsheetButton = new Button(Messages.getString("ResultList.ResultSheet", locale)); //$NON-NLS-1$
            final Button.ClickListener listener = new Button.ClickListener() { //$NON-NLS-1$
                private static final long serialVersionUID = -8473648982746209221L;

                @Override
                public void buttonClick(ClickEvent event) {
                    resultSpreadsheetButton.setComponentError(null);

                    if (!Competition.isMasters()) {
                        regularCompetition(locale);
                    } else {
                        mastersCompetition(locale);
                    }
                }

                /**
                 * @param locale1
                 * @throws RuntimeException
                 */
                private void regularCompetition(final Locale locale1) throws RuntimeException {
                    final JXLSWorkbookStreamSource streamSource = new JXLSResultSheet();
                    // final OutputSheetStreamSource<ResultSheet> streamSource = new OutputSheetStreamSource<ResultSheet>(
                    // ResultSheet.class, (CompetitionApplication) app, true);
                    if (streamSource.size() == 0) {
                        setComponentError(new SystemError(Messages.getString("ResultList.NoResults", locale1))); //$NON-NLS-1$
                        throw new RuntimeException(Messages.getString("ResultList.NoResults", locale1)); //$NON-NLS-1$
                    }

                    String now = new SimpleDateFormat("yyyy-MM-dd_HHmmss") //$NON-NLS-1$
                            .format(new Date());
                    ((UserActions) app).openSpreadsheet(streamSource, Messages.getString("ResultList.ResultsPrefix", locale) + now); //$NON-NLS-1$
                }

                /**
                 * @param locale1
                 * @throws RuntimeException
                 */
                private void mastersCompetition(final Locale locale1) throws RuntimeException {
                    regularCompetition(locale1);
                }
            };
            resultSpreadsheetButton.addListener(listener);
            tableToolbar1.addComponent(resultSpreadsheetButton);
        }

        {
            final Button teamResultSpreadsheetButton = new Button(Messages.getString("ResultList.TeamResultSheet", locale)); //$NON-NLS-1$
            final Button.ClickListener teamResultClickListener = new Button.ClickListener() { //$NON-NLS-1$
                private static final long serialVersionUID = -8473648982746209221L;

                @Override
                public void buttonClick(ClickEvent event) {

//                    int maxCount = 2500;  // for debugging -- competitionBook was causing table locks
//                    for (int repeatCount = 0; repeatCount < maxCount; repeatCount++) {
//                        logger.debug("step {}",repeatCount);
                        teamResultSpreadsheetButton.setComponentError(null);

                        final JXLSWorkbookStreamSource streamSource = new JXLSCompetitionBook();
                        if (streamSource.size() == 0) {
                            setComponentError(new SystemError(Messages.getString("ResultList.NoResults", locale))); //$NON-NLS-1$
                            throw new RuntimeException(Messages.getString("ResultList.NoResults", locale)); //$NON-NLS-1$
                        }

                        String now = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date()); //$NON-NLS-1$
                        ((UserActions) app).openSpreadsheet(streamSource, Messages.getString("ResultList.TeamPrefix", locale) + now); //$NON-NLS-1$

                    }
//                }
            };
            teamResultSpreadsheetButton.addListener(teamResultClickListener);
            tableToolbar1.addComponent(teamResultSpreadsheetButton);
        }

        {
            final Button timingStatsButton = new Button(Messages.getString("ResultList.TimingStats", locale)); //$NON-NLS-1$
            final Button.ClickListener teamResultClickListener = new Button.ClickListener() { //$NON-NLS-1$
                private static final long serialVersionUID = -8473648982746209221L;

                @Override
                public void buttonClick(ClickEvent event) {
                    timingStatsButton.setComponentError(null);

                    final JXLSWorkbookStreamSource streamSource = new JXLSTimingStats();
                    if (streamSource.size() == 0) {
                        setComponentError(new SystemError(Messages.getString("ResultList.NoResults", locale))); //$NON-NLS-1$
                        throw new RuntimeException(Messages.getString("ResultList.NoResults", locale)); //$NON-NLS-1$
                    }

                    String now = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date()); //$NON-NLS-1$
                    ((UserActions) app).openSpreadsheet(streamSource, Messages.getString("ResultList.TimingStatsPrefix", locale) + now); //$NON-NLS-1$

                }
            };
            timingStatsButton.addListener(teamResultClickListener);
            tableToolbar1.addComponent(timingStatsButton);
        }

        final Button refreshButton = new Button(Messages.getString("ResultList.Refresh", locale)); //$NON-NLS-1$
        final Button.ClickListener refreshClickListener = new Button.ClickListener() { //$NON-NLS-1$
            private static final long serialVersionUID = 7744958942977063130L;

            @Override
            public void buttonClick(ClickEvent event) {
//                CompetitionApplication current = (CompetitionApplication)app;
//                SessionData masterData1 = current.getMasterData(current.getPlatformName());
//                LoggerUtils.mdcSetup(getLoggingId(), masterData1);
                logger.debug("reloading"); //$NON-NLS-1$

                refresh();
            }
        };
        refreshButton.addListener(refreshClickListener);
        tableToolbar1.addComponent(refreshButton);

        final Button editButton = new Button(Messages.getString("ResultList.edit", locale)); //$NON-NLS-1$
        final Button.ClickListener editClickListener = new Button.ClickListener() { //$NON-NLS-1$
            private static final long serialVersionUID = 7744958942977063130L;

            @Override
            public void buttonClick(ClickEvent event) {
                editCompetitionSession(sessionSelect.getSelectedId(), sessionSelect.getSelectedItem());
            }
        };
        editButton.addListener(editClickListener);
        tableToolbar1.addComponent(editButton);

        final Button publicAddressButton = new Button(Messages.getString("LiftList.publicAddress", app.getLocale())); //$NON-NLS-1$
        final Button.ClickListener publicAddressClickListener = new Button.ClickListener() { //$NON-NLS-1$
            private static final long serialVersionUID = 7744958942977063130L;

            @Override
            public void buttonClick(ClickEvent event) {
                SessionData masterData1 = app.getMasterData(app.getPlatformName());
                LoggerUtils.mdcSetup(parentView.getLoggingId(), masterData1);
                PublicAddressForm.editPublicAddress(null, masterData1,parentView);
            }
        };
        publicAddressButton.addListener(publicAddressClickListener);
        tableToolbar1.addComponent(publicAddressButton);
    }

    protected void editCompetitionSession(Object itemId, Item item) {
        if (itemId == null) {
            app.getMainWindow().showNotification(
                    Messages.getString("ResultList.sessionNotSelected", CompetitionApplication.getCurrentLocale()),
                    Notification.TYPE_ERROR_MESSAGE);
            return;
        }
        SessionForm form = new SessionForm(app);

        form.setItemDataSource(item);
        form.setReadOnly(false);

        CompetitionSession competitionSession = (CompetitionSession) ItemAdapter.getObject(item);
        // logger.debug("retrieved session {} {}",System.identityHashCode(competitionSession), competitionSession.getReferee3());
        Window editingWindow = new Window(competitionSession.getName());
        form.setWindow(editingWindow);
        form.setParentList(null);
        editingWindow.getContent().addComponent(form);
        app.getMainWindow().addWindow(editingWindow);
        editingWindow.setWidth("40em");
        editingWindow.center();
    }

    public void setCurrentSession(CompetitionSession newSession) {
        switchGroup(newSession);
    }

}
