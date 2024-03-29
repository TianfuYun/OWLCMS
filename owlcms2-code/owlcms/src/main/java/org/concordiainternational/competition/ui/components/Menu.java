/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.ui.components;

import java.io.File;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.ServletContext;

import org.concordiainternational.competition.data.Platform;
import org.concordiainternational.competition.i18n.Messages;
import org.concordiainternational.competition.ui.CompetitionApplication;
import org.concordiainternational.competition.ui.CompetitionApplicationComponents;
import org.concordiainternational.competition.ui.LoadWindow;
import org.concordiainternational.competition.ui.SessionData;
import org.concordiainternational.competition.utils.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class Menu extends MenuBar implements Serializable {
    private static final long serialVersionUID = -3809346951739483448L;
    protected static final Logger logger = LoggerFactory.getLogger(Menu.class);
    private LoadWindow loadComputerWindow;
    private CompetitionApplication app;

    public Menu(CompetitionApplication app) {
        final Locale locale = app.getLocale();
        this.app = app;

        MenuBar menu = this;
        menu.setWidth("100%"); //$NON-NLS-1$

        MenuItem console = createConsoleMenu(menu, app, locale);
        createAnnouncerMenuItem(console, locale);
        createTimeKeeperMenuItem(console, locale);
        createChangesMenuItem(console, locale);

        MenuItem projectors = createProjectorsMenuItem(menu, app, locale);
        createMainBoardMenuItem(projectors, app, locale, "dlp");
        createMainBoardMenuItem(projectors, app, locale, "lcd");
        createMainBoardMenuItem(projectors, app, locale, "tv");
        projectors.addSeparator();

        createResultBoardMenuItem(projectors, app, locale, "dlp");
        createResultBoardMenuItem(projectors, app, locale, "lcd");
        createResultBoardMenuItem(projectors, app, locale, "tv");
        projectors.addSeparator();

        createSinclairBoardMenuItem(projectors, app, locale, "dlp");
        createSinclairBoardMenuItem(projectors, app, locale, "lcd");
        createSinclairBoardMenuItem(projectors, app, locale, "tv");
        projectors.addSeparator();

        createPublicAttemptBoardMenuItem(projectors, app, locale, "s4_3");
        createPublicAttemptBoardMenuItem(projectors, app, locale, "s720p");
        createPublicAttemptBoardMenuItem(projectors, app, locale, "s1080p");
        projectors.addSeparator();
        createLifterAttemptBoardMenuItem(projectors, app, locale, "s4_3");
        createLifterAttemptBoardMenuItem(projectors, app, locale, "s720p");
        createLifterAttemptBoardMenuItem(projectors, app, locale, "s1080p");
        projectors.addSeparator();

        createAthleteBioMenuItem(projectors, app, locale);
        projectors.addSeparator();

        // createLiftOrderMenuItem(projectors, competitionApplication, locale);
        createSummaryLiftOrderMenuItem(projectors, app, locale);

        createLoadComputerMenuItem(menu, app, locale);

        MenuItem decisions = createDecisionMenuItem(menu, app, locale);
        createCountdownDisplayMenuItem(decisions, app, locale);
        decisions.addSeparator();
        createJuryLightsMenuItem(decisions, app, locale);
//        decisions.addSeparator();
//        createRefereeTestingMenuItem(decisions, competitionApplication, locale);
        decisions.addSeparator();
        createRefereeMenuItem(decisions, app, locale, 0);
        createRefereeMenuItem(decisions, app, locale, 1);
        createRefereeMenuItem(decisions, app, locale, 2);

        MenuItem results = createResultsMenuItem(menu, app, locale);
        createShowResultsMenuItem(results, app, locale);
        createEditResultsMenuItem(results, app, locale);

        createWeighInsMenuItem(menu, app, locale);

        MenuItem administration = createAdminMenuItem(menu, app, locale);
        createCompetitionMenuItem(administration, app, locale);
        createPlatformsMenuItem(administration, app, locale);
        createCategoriesMenuItem(administration, app, locale);
        createGroupsMenuItem(administration, app, locale);
        administration.addSeparator();
        createUploadMenuItem(administration, app, locale);
        createLiftersMenuItem(administration, app, locale);
        administration.addSeparator();
        createRestartMenuItem(administration, app, locale);

        createAboutMenuItem(menu, app, locale);

        if (Platform.getSize() > 1) {
            MenuItem platforms = createPlatformsMenuItem(menu, app, locale);
            createPlatformSelectionMenuItems(platforms, app, locale);
        }

    }



    /**
     * @param competitionApplication
     * @param locale
     */
    private MenuItem createConsoleMenu(MenuBar menu, final CompetitionApplication competitionApplication, final Locale locale) {
        return menu.addItem(Messages.getString("CompetitionApplication.Console", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/document.png"),
                null);
    }

    /**
     * @param competitionApplication
     * @param locale
     */
    private MenuItem createAnnouncerMenuItem(MenuItem menu, final Locale locale) {
        return menu.addItem(Messages.getString("CompetitionApplication.Announcer", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/document.png"),
                new Command() {
                    private static final long serialVersionUID = -547788870764317931L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        app.display(CompetitionApplicationComponents.ANNOUNCER_VIEW);
                    }
                });
    }

    /**
     * @param competitionApplication
     * @param locale
     */
    private MenuItem createChangesMenuItem(MenuItem menu, final Locale locale) {
        return menu.addItem(Messages.getString("CompetitionApplication.Changes", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/document.png"),
                new Command() {
                    private static final long serialVersionUID = -547788870764317931L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        app.display(CompetitionApplicationComponents.CHANGES_VIEW);
                    }
                });
    }

    /**
     * @param competitionApplication
     * @param locale
     */
    private MenuItem createTimeKeeperMenuItem(MenuItem menu, final Locale locale) {
        return menu.addItem(Messages.getString("CompetitionApplication.TimeKeeper", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/document.png"),
                new Command() {
                    private static final long serialVersionUID = -547788870764317931L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        app.display(CompetitionApplicationComponents.TIMEKEEPER_VIEW);
                    }
                });
    }

    /**
     * @param competitionApplication
     * @param locale
     */
    private MenuItem createResultsMenuItem(MenuBar menu, final CompetitionApplication competitionApplication, final Locale locale) {
        return menu.addItem(Messages.getString("CompetitionApplication.Results", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/document.png"),
                null);
    }

    private void createEditResultsMenuItem(MenuItem results, final CompetitionApplication competitionApplication, Locale locale) {
        // CompetitionApplication.Results.Edit
        results.addItem(Messages.getString("CompetitionApplication.Results.Edit", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/document.png"),
                new Command() {
                    private static final long serialVersionUID = 5577281157225515360L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.RESULT_EDIT_VIEW);
                    }
                });
    }

    private void createShowResultsMenuItem(MenuItem results, final CompetitionApplication competitionApplication, Locale locale) {
        // CompetitionApplication.Results.Edit
        results.addItem(Messages.getString("CompetitionApplication.Results.Display", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/document.png"),
                new Command() {
                    private static final long serialVersionUID = 1593459389710222080L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.RESULT_DISPLAY_VIEW,"tv",null);
                    }
                });

//        List<Platform> platforms = Platform.getAll();
//        for (final Platform p : platforms) {
//            final String pName = p.getName() == null ? "?" : p.getName();
//            final String name = (platforms.size() == 1
//                    ? Messages.getString("CompetitionApplication.Results.Display", locale) //$NON-NLS-1$
//                    : MessageFormat.format(Messages.getString("CompetitionApplication.Results.DisplayPlatformResults", locale),pName)); //$NON-NLS-1$
//            results.addItem(name, null,
//                    new Command() {
//                        private static final long serialVersionUID = 3563330867710192233L;
//
//                        @Override
//                        public void menuSelected(MenuItem selectedItem) {
//                            competitionApplication.display(CompetitionApplicationComponents.RESULT_DISPLAY_VIEW,"tv",pName);
//                        }
//                    });
//        }

    }

    /**
     * @param competitionApplication
     * @param locale
     */
    private MenuItem createLoadComputerMenuItem(MenuBar menu, final CompetitionApplication competitionApplication,
            final Locale locale) {
        return menu.addItem(Messages.getString("CompetitionApplication.Load", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/document.png"),
                new Command() {
                    private static final long serialVersionUID = 5577281157225515360L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        if (getLoadComputerWindow() == null) {
                            displayLoadComputerWindow();
                        } else {
                            closeLoadComputerWindow();
                        }
                    }
                });
    }

    /**
     * @param competitionApplication
     * @param locale
     */
    private MenuItem createWeighInsMenuItem(MenuBar menu, final CompetitionApplication competitionApplication, final Locale locale) {
        return menu.addItem(Messages.getString("CompetitionApplication.WeighIn", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/users.png"),
                new Command() {
                    private static final long serialVersionUID = 3563330867710192233L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.WEIGH_IN_LIST);
                    }
                });
    }

    private MenuItem createProjectorsMenuItem(MenuBar menu, CompetitionApplication competitionApplication, Locale locale) {
        return menu.addItem(Messages.getString("CompetitionApplication.Projectors", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/folder-add.png"),
                null);
    }

    /**
     * @param competitionApplication
     * @param locale
     * @return
     */
    private MenuItem createMainBoardMenuItem(MenuItem projectors, final CompetitionApplication competitionApplication,
            final Locale locale, final String stylesheet) {
        return projectors.addItem(
                Messages.getString("CompetitionApplication.MainBoard", locale)//$NON-NLS-1$
                        + " - "
                        + Messages.getString("CompetitionApplication.Display." + stylesheet, locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/folder-add.png"),
                new Command() {
                    private static final long serialVersionUID = -4179990860181438187L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.MAIN_BOARD, stylesheet, null);
                    }
                });
    }

    /**
     * @param competitionApplication
     * @param locale
     * @return
     */
    private MenuItem createResultBoardMenuItem(MenuItem projectors, final CompetitionApplication competitionApplication,
            final Locale locale, final String stylesheet) {
        return projectors.addItem(
                Messages.getString("CompetitionApplication.ResultBoard", locale)//$NON-NLS-1$
                        + " - "
                        + Messages.getString("CompetitionApplication.Display." + stylesheet, locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/folder-add.png"),
                new Command() {
                    private static final long serialVersionUID = -4179990860181438187L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.RESULT_BOARD, stylesheet, null);
                    }
                });
    }


    /**
     * @param competitionApplication
     * @param locale
     * @return
     */
    private MenuItem createSinclairBoardMenuItem(MenuItem projectors, final CompetitionApplication competitionApplication,
            final Locale locale, final String stylesheet) {
        return projectors.addItem(
                Messages.getString("CompetitionApplication.SinclairBoard", locale)//$NON-NLS-1$
                        + " - "
                        + Messages.getString("CompetitionApplication.Display." + stylesheet, locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/folder-add.png"),
                new Command() {
                    private static final long serialVersionUID = -4179990860181438187L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.SINCLAIR_BOARD, stylesheet, null);
                    }
                });
    }

    /**
     * @param competitionApplication
     * @param locale
     */
    private MenuItem createSummaryLiftOrderMenuItem(MenuItem projectors, final CompetitionApplication competitionApplication,
            final Locale locale) {
        return projectors.addItem(Messages.getString("CompetitionApplication.SummaryLiftOrder", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/folder-add.png"),
                new Command() {
                    private static final long serialVersionUID = 5658882232799685230L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.SUMMARY_LIFT_ORDER_VIEW);
                    }
                });
    }

    /**
     * @param competitionApplication
     * @param locale
     * @param style
     */
    private MenuItem createPublicAttemptBoardMenuItem(MenuItem projectors, final CompetitionApplication competitionApplication,
            final Locale locale, final String style) {
        String menuEntryString = Messages.getString("CompetitionApplication.PublicAttemptBoard", locale)//$NON-NLS-1$
                + " - "
                + Messages.getString("CompetitionApplication.Display." + style, locale); //$NON-NLS-1$
        return projectors.addItem(
                menuEntryString,
                null, // new ThemeResource("icons/32/folder-add.png"),
                new Command() {
                    private static final long serialVersionUID = 5658882232799685230L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.PUBLIC_ATTEMPT_BOARD_VIEW, style, null);
                    }
                });
    }

    /**
     * @param competitionApplication
     * @param locale
     * @param style
     */
    private MenuItem createLifterAttemptBoardMenuItem(MenuItem projectors, final CompetitionApplication competitionApplication,
            final Locale locale, final String style) {
        String menuEntryString = Messages.getString("CompetitionApplication.LifterAttemptBoard", locale)//$NON-NLS-1$
                + " - "
                + Messages.getString("CompetitionApplication.Display." + style, locale); //$NON-NLS-1$
        return projectors.addItem(
                menuEntryString,
                null, // new ThemeResource("icons/32/folder-add.png"),
                new Command() {
                    private static final long serialVersionUID = 5658882232799685230L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.LIFTER_ATTEMPT_BOARD_VIEW, style, null);
                    }
                });
    }

    /**
     * @param competitionApplication
     * @param locale
     * @return
     */
    private MenuItem createAthleteBioMenuItem(MenuItem projectors, final CompetitionApplication competitionApplication,
            final Locale locale) {
        return projectors.addItem(
                Messages.getString("CompetitionApplication.AthleteBio", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/folder-add.png"),
                new Command() {
                    private static final long serialVersionUID = 5658882232799685230L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.ATHLETE_BIO_VIEW);
                    }
                });
    }

    private MenuItem createAdminMenuItem(MenuBar menu, CompetitionApplication competitionApplication, Locale locale) {
        return menu.addItem(Messages.getString("CompetitionApplication.Administration", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/folder-add.png"),
                null);
    }

    private MenuItem createPlatformsMenuItem(MenuBar menu, CompetitionApplication competitionApplication, Locale locale) {
        return menu.addItem(Messages.getString("CompetitionApplication.Platforms", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/folder-add.png"),
                null);
    }

    /**
     * @param competitionApplication
     * @param locale
     */
    private MenuItem createPlatformsMenuItem(MenuItem administration, final CompetitionApplication competitionApplication,
            final Locale locale) {
        return administration.addItem(Messages.getString("CompetitionApplication.Platforms", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/folder-add.png"),
                new Command() {
                    private static final long serialVersionUID = -3184587992763328917L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.PLATFORM_LIST);
                    }
                });
    }

    /**
     * @param competitionApplication
     * @param locale
     */
    private MenuItem createCompetitionMenuItem(MenuItem administration, final CompetitionApplication competitionApplication,
            final Locale locale) {
        return administration.addItem(Messages.getString("CompetitionApplication.Competition", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/folder-add.png"),
                new Command() {
                    private static final long serialVersionUID = -3184587992763328917L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.COMPETITION_EDITOR);
                    }
                });
    }

    /**
     * @param competitionApplication
     * @param locale
     */
    private MenuItem createCategoriesMenuItem(MenuItem administration, final CompetitionApplication competitionApplication,
            final Locale locale) {
        return administration.addItem(Messages.getString("CompetitionApplication.Categories", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/folder-add.png"),
                new Command() {
                    private static final long serialVersionUID = -6471211259031643832L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.CATEGORY_LIST);
                    }
                });
    }

    /**
     * @param competitionApplication
     * @param locale
     */
    private MenuItem createGroupsMenuItem(MenuItem administration, final CompetitionApplication competitionApplication,
            final Locale locale) {
        return administration.addItem(Messages.getString("CompetitionApplication.Groups", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/folder-add.png"),
                new Command() {
                    private static final long serialVersionUID = -6740574252795556971L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.GROUP_LIST);
                    }
                });
    }

    private MenuItem createLiftersMenuItem(MenuItem administration,
            final CompetitionApplication competitionApplication, Locale locale) {
        return administration.addItem(Messages.getString("CompetitionApplication.Lifters", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/users.png"),
                new Command() {
                    private static final long serialVersionUID = 3563330867710192233L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.REGISTRATION_LIST);
                    }
                });
    }

    private MenuItem createRestartMenuItem(MenuItem administration,
            final CompetitionApplication competitionApplication, Locale locale) {
        return administration.addItem(Messages.getString("Restart.Restart", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/users.png"),
                new Command() {
                    private static final long serialVersionUID = 3563330867710192233L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        displayRestartConfirmation();
                    }
                });
    }

    protected void restart() {
        ServletContext sCtx = app.getServletContext();
        String configFilePath = sCtx.getRealPath("/WEB-INF/web.xml"); //$NON-NLS-1$
        File configFile = new File(configFilePath);
        logger.info("restarting by touching {}", configFile); //$NON-NLS-1$
        configFile.setLastModified(System.currentTimeMillis());
    }

    private MenuItem createUploadMenuItem(MenuItem administration, final CompetitionApplication competitionApplication,
            Locale locale) {
        return administration.addItem(Messages.getString("CompetitionApplication.Upload", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/users.png"),
                new Command() {
                    private static final long serialVersionUID = 3563330867710192233L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.UPLOAD_VIEW);
                    }
                });

    }

    private void createPlatformSelectionMenuItems(MenuItem platforms,
            final CompetitionApplication competitionApplication, Locale locale) {
        for (final Platform platform : Platform.getAll()) {
            final String name = (platform.getName() == null ? "?" : platform.getName()); //$NON-NLS-1$
            platforms.addItem(name, null, // new
                                          // ThemeResource("icons/32/users.png"),
                    new Command() {
                        private static final long serialVersionUID = 3563330867710192233L;

                        @Override
                        public void menuSelected(MenuItem selectedItem) {
                            Menu.this.setComponentError(null); // erase error
                                                               // marker;
                            competitionApplication.setPlatformByName(name);
                            SessionData masterData = competitionApplication.getMasterData(name);
                            logger.debug("new platform={}, new group = {}", name, masterData.getCurrentSession()); //$NON-NLS-1$
                            competitionApplication.setCurrentCompetitionSession(masterData.getCurrentSession());
                        }
                    });
        }
    }

    private MenuItem createDecisionMenuItem(MenuBar menu, final CompetitionApplication competitionApplication,
            Locale locale) {
        return menu.addItem(Messages.getString("CompetitionApplication.Refereeing", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/document.png"),
                null);
    }

    @SuppressWarnings("unused")
    private MenuItem createRefereeTestingMenuItem(MenuItem item, final CompetitionApplication competitionApplication,
            Locale locale) {
        return item.addItem(Messages.getString("CompetitionApplication.DecisionLights", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/document.png"),
                new Command() {
                    private static final long serialVersionUID = 5577281157225515360L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.REFEREE_TESTING);
                    }
                });
    }

    private MenuItem createJuryLightsMenuItem(MenuItem item, final CompetitionApplication competitionApplication,
            Locale locale) {
        return item.addItem(Messages.getString("CompetitionApplication.JuryLights", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/document.png"),
                new Command() {
                    private static final long serialVersionUID = 5577281157225515360L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.JURY_LIGHTS);
                    }
                });
    }

    private MenuItem createCountdownDisplayMenuItem(MenuItem item, final CompetitionApplication competitionApplication,
            Locale locale) {
        return item.addItem(Messages.getString("CompetitionApplication.CountdownDisplay", locale), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/document.png"),
                new Command() {
                    private static final long serialVersionUID = 5577281157225515360L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.display(CompetitionApplicationComponents.COUNTDOWN_DISPLAY);
                    }
                });
    }

    private MenuItem createRefereeMenuItem(MenuItem item, final CompetitionApplication competitionApplication,
            Locale locale, final int refereeIndex) {
        return item.addItem(Messages.getString("CompetitionApplication.Referee", locale) + " " + (refereeIndex + 1), //$NON-NLS-1$
                null, // new ThemeResource("icons/32/document.png"),
                new Command() {
                    private static final long serialVersionUID = 5577281157225515360L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        competitionApplication.displayRefereeConsole(refereeIndex);
                    }
                });
    }

    public void displayRestartConfirmation() {

        // Create the window...
        final Window subwindow = new Window(Messages.getString("Restart.ConfirmationDialogTitle", getApplication().getLocale())); //$NON-NLS-1$
        // ...and make it modal
        subwindow.setModal(true);
        subwindow.setWidth("10cm"); //$NON-NLS-1$

        // Configure the windws layout; by default a VerticalLayout
        VerticalLayout layout = (VerticalLayout) subwindow.getContent();
        layout.setMargin(true);
        layout.setSpacing(true);

        // Add some content; a label and a close-button
        final Label message = new Label(
                Messages.getString("Restart.Confirmation", getApplication().getLocale()), Label.CONTENT_XHTML); //$NON-NLS-1$
        subwindow.addComponent(message);

        final Button close = new Button(
                Messages.getString("Restart.Cancel", getApplication().getLocale()), new Button.ClickListener() { //$NON-NLS-1$
                    private static final long serialVersionUID = 1L;

                    // inline click-listener
                    @Override
                    public void buttonClick(ClickEvent event) {
                        // close the window by removing it from the main window
                        getApplication().getMainWindow().removeWindow(subwindow);
                    }
                });

        final Button ok = new Button(Messages.getString("Restart.Restart", getApplication().getLocale()));
        ok.addListener(new Button.ClickListener() { //$NON-NLS-1$
            private static final long serialVersionUID = 1L;

            // inline click-listener
            @Override
            public void buttonClick(ClickEvent event) {
                // close the window by removing it from the main window
                restart();
                message.setValue(Messages.getString("Restart.InProgress", getApplication().getLocale()));//$NON-NLS-1$
                // getApplication().getMainWindow().removeWindow(subwindow);
                // close.setVisible(false);
                ok.setVisible(false);
                close.setCaption(Messages.getString("Common.done", getApplication().getLocale()));

            }
        });

        // The components added to the window are actually added to the window's
        // layout; you can use either. Alignments are set using the layout
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.addComponent(ok);
        buttons.addComponent(close);
        buttons.setSpacing(true);
        layout.addComponent(buttons);
        layout.setComponentAlignment(buttons, Alignment.BOTTOM_CENTER);

        getApplication().getMainWindow().addWindow(subwindow);
    }

    public void displayLoadComputerWindow() {
        if (loadComputerWindow != null) {
            loadComputerWindow.setVisible(true);
        } else {
            loadComputerWindow = new LoadWindow(this, app);
            getApplication().getMainWindow().addWindow(loadComputerWindow);
        }
    }

    public void closeLoadComputerWindow() {
        LoadWindow loadComputerWindow2 = getLoadComputerWindow();
        getApplication().getMainWindow().removeWindow(loadComputerWindow2);
        loadComputerWindow2 = getLoadComputerWindow();
        if (loadComputerWindow2 != null) {
            loadComputerWindow2.close();
        }
        setLoadComputerWindow(null);
    }

    /**
     * @param loadComputerWindow
     *            the loadComputerWindow to set
     */
    public void setLoadComputerWindow(LoadWindow loadComputerWindow) {
        this.loadComputerWindow = loadComputerWindow;
    }

    /**
     * @return the loadComputerWindow
     */
    public LoadWindow getLoadComputerWindow() {
        return loadComputerWindow;
    }

    /**
     * @param competitionApplication
     * @param locale
     */
    private MenuItem createAboutMenuItem(MenuBar menu, final CompetitionApplication competitionApplication,
            final Locale locale) {
        return menu.addItem(Messages.getString("About.menu", CompetitionApplication.getCurrentLocale()), //$NON-NLS-1$
                null,
                new Command() {
                    private static final long serialVersionUID = 5577281157225515360L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        if (getLoadComputerWindow() == null) {
                            displayAboutWindow();
                        }
                    }

                    private void displayAboutWindow() {
                        Window window = new Window();
                        window.setIcon(new ThemeResource("icons/16/appIcon.png"));
                        ServletContext servletContext = app.getServletContext();
                        String name = servletContext.getInitParameter("appName");
                        String version = servletContext.getInitParameter("appVersion");
                        String url = servletContext.getInitParameter("appUrl");
                        window.setCaption(" " + name);
                        String pattern = Messages.getString("About.message", CompetitionApplication.getCurrentLocale());
                        String message = MessageFormat.format(pattern, version, "Jean-François Lamy", url, url,
                                "lamyjeanfrancois@gmail.com", getUrls());
                        window.addComponent(new Label(message, Label.CONTENT_XHTML));
                        getApplication().getMainWindow().addWindow(window);
                        window.center();
                    }

                    /**
                     * Try to guess URLs that can reach the system.
                     *
                     * The browser on the master laptop most likely uses "localhost" in its URL.  We can't know which of its available
                     * IP addresses can actually reach the application. We scan the network addresses, and try the URLs one by one,
                     * listing wired interfaces first, and wireless interfaces second (in as much as we can guess).
                     *
                     * We rely on the URL used to reach the "about" screen to know how the application is named, what port is used, and
                     * which protocol works.
                     *
                     * @return HTML ("a" tags) for the various URLs that appear to work.
                     */
                    private String getUrls() {
                        URL requestUrl = app.getURL();
                        logger.debug("request URL = {}", requestUrl);
                        String protocol = requestUrl.getProtocol();
                        String siteString = requestUrl.getFile();
                        int requestPort = requestUrl.getPort();

                        String ip;
                        StringBuilder wireless = new StringBuilder();
                        StringBuilder wired = new StringBuilder();
//                        Set<String> done = new HashSet<String>(10);
                        try {
                            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                            while (interfaces.hasMoreElements()) {
                                NetworkInterface iface = interfaces.nextElement();
                                // filters out 127.0.0.1 and inactive interfaces
                                if (iface.isLoopback() || !iface.isUp())
                                    continue;

                                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                                while(addresses.hasMoreElements()) {
                                    InetAddress addr = addresses.nextElement();
                                    ip = addr.getHostAddress();

                                    String displayName = iface.getDisplayName();
                                    String lowerCase = displayName.toLowerCase();
                                    boolean ipV4 = addr.getAddress().length == 4;

                                    // filter out IPV6 and interfaces to virtual machines
                                    if (!ipV4 || lowerCase.contains("virtual")) continue;

                                    // try reaching the current IP address with the known protocol, port and site.
                                    try {
                                        URL u = new URL(protocol, ip, requestPort, siteString);
                                        String externalForm = u.toExternalForm();
                                        HttpURLConnection huc =  (HttpURLConnection)  u.openConnection();
                                        huc.setRequestMethod("GET");
                                        huc.connect();
                                        int response = huc.getResponseCode();

                                        if (response != 200) {
                                            logger.debug("{} not reachable: {}", externalForm, response);
                                        } else {
                                            logger.debug("{} OK: {}", externalForm, lowerCase);
                                            String urlString = "<a href='"+externalForm+"'>"+externalForm+"</a>";
                                            if (lowerCase.contains("wireless")) {
                                                wireless.append(urlString);
                                                wireless.append("<br/>");
                                            } else {
                                                wired.append(urlString);
                                                wired.append("<br/>");
                                            }
                                            break;
                                        }
                                    } catch (Exception e) {
                                        LoggerUtils.traceException(logger,e);
                                    }
                                }
                            }
                        } catch (SocketException e) {
                            LoggerUtils.debugException(logger,e);
                        }
                        logger.debug("wired = {} {}",wired,wired.length());
                        logger.debug("wireless = {} {}",wireless,wireless.length());
                        if (wired.length() == 0 && wireless.length() == 0) {
                            return Messages.getString("About.urlUnknown", CompetitionApplication.getCurrentLocale());
                        } else {
                            return wired.toString()+wireless.toString();
                        }

                    }

                });
    }
}
