/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.ui.components;

import org.concordiainternational.competition.decision.Decision;
import org.concordiainternational.competition.decision.DecisionEvent;
import org.concordiainternational.competition.decision.DecisionEventListener;
import org.concordiainternational.competition.i18n.Messages;
import org.concordiainternational.competition.ui.CompetitionApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

public class DecisionLights extends HorizontalLayout implements DecisionEventListener {

    private static final long serialVersionUID = 1L;

    Label[] decisionLights = new Label[3];

    private Logger logger = LoggerFactory.getLogger(DecisionLights.class);

    private boolean publicFacing;

    private boolean shown = false;

    private HorizontalLayout lights;

    private Label down;

    private CompetitionApplication app;

    public DecisionLights(boolean publicFacing, CompetitionApplication app, boolean small) {
        this.publicFacing = publicFacing;
        this.app = app;

        lights = createLights(small);
        this.addComponent(lights);
        lights.setStyleName("decisionLights");
        lights.setVisible(true);

        down = new Label("&nbsp;", Label.CONTENT_XHTML);
        down.setSizeFull();
        down.setStyleName("decisionLightsWindow");
        down.addStyleName("down");
        down.setVisible(true);
        this.addComponent(down);

        this.setMargin(false);
        this.setSpacing(false);
        this.setSizeFull();

        resetLights();
    }


    /**
     * Create the red/white display rectangles for decisions.
     */
    private HorizontalLayout createLights(boolean small) {
        logger.debug("createLights");
        HorizontalLayout hl = new HorizontalLayout();
        hl.setSizeFull();
        //hl.setStyleName("decisionLight");

        for (int i = 0; i < decisionLights.length; i++) {
            decisionLights[i] = new Label();
            decisionLights[i].setSizeFull();
            decisionLights[i].setStyleName("decisionLight");
            decisionLights[i].addStyleName("undecided");
            hl.addComponent(decisionLights[i]);
            if (i < decisionLights.length) {
                Label spacer = new Label("&nbsp;",Label.CONTENT_XHTML);
                spacer.addStyleName("spacer");
                if (small) {
                    spacer.setWidth("0.3em");
                } else {
                    spacer.setWidth("1em");
                }

                hl.addComponent(spacer);
            }
            hl.setComponentAlignment(decisionLights[i], Alignment.MIDDLE_CENTER);
            hl.setExpandRatio(decisionLights[i], 50.0F / decisionLights.length);
            if (small) {
                this.setSpacing(false);
                hl.setMargin(false);
            } else {
                this.setSpacing(true);
                hl.setMargin(true);
            }
        }
        return hl;
    }

    @Override
    public void updateEvent(final DecisionEvent updateEvent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (app) {
                    Decision[] decisions = updateEvent.getDecisions();
                    switch (updateEvent.getType()) {
                    case DOWN:
                        logger.trace("received DOWN event {}", app);
                        doDown();

                        for (int i = 0; i < decisions.length; i++) {
                            if (decisions[i].accepted == null) {
                                // do nothing; maybe show in yellow in Jury Mode ?
                            }
                        }
                        break;
                    case WAITING:
                        logger.trace("received WAITING event {}", app);
                        for (int i = 0; i < decisions.length; i++) {
                            if (decisions[i].accepted == null) {
                                // do nothing; maybe show in yellow in Jury Mode ?
                            }
                        }
                        break;
                    case UPDATE:
                        logger.trace("received UPDATE event {}", app);
                        if (shown) {
                            logger.debug("showing shown={}", shown);
                            showLights(decisions);
                        } else {
                            logger.debug("not showing {}", shown);
                        }
                        break;
                    case SHOW:
                        logger.trace("received SHOW event {}", app);
                        showLights(decisions);
                        break;
//                    case BLOCK:
//                        logger.debug("received BLOCK event {}", app);
//                        showLights(decisions);
//                        break;
                    case RESET:
                        logger.trace("received RESET event {}", app);
                        resetLights();
                        break;
                    default:
                        logger.trace("received default");
                        break;
                    }
                }
                app.push();
            }

        }).start();
    }

    /**
     * show down signal in window.
     */
    public void doDown() {
        synchronized(app) {
            this.removeAllComponents();
            this.addComponent(down);
            this.setExpandRatio(down, 100);
            this.setComponentAlignment(down, Alignment.MIDDLE_CENTER);
        }
        app.push();
    }

    /**
     * @param decisions
     */
    private void showLights(Decision[] decisions) {
        for (int i = 0; i < decisionLights.length; i++) {
            decisionLights[i].setStyleName("decisionLight");
            Boolean accepted = null;
            if (publicFacing) {
                accepted = decisions[i].accepted;
            } else {
                // display in reverse order relative to what public sees
                accepted = decisions[decisionLights.length - 1 - i].accepted;
            }

            if (decisions[i] != null && accepted != null) {
                decisionLights[i].addStyleName(accepted ? "lift" : "nolift");
            } else {
                decisionLights[i].addStyleName("undecided");
            }
        }
        shown = true;
        doLights();
    }


    private void doLights() {
        synchronized(app) {
            this.removeAllComponents();
            this.addComponent(lights);
            this.setExpandRatio(lights, 100);
            this.setComponentAlignment(lights, Alignment.MIDDLE_CENTER);
            this.setVisible(true);
        }
        app.push();
    }

    private void resetLights() {
        synchronized (app) {
            for (int i = 0; i < decisionLights.length; i++) {
                decisionLights[i].setStyleName("decisionLight");
                decisionLights[i].addStyleName("undecided");
                decisionLights[i].setContentMode(Label.CONTENT_XHTML);
                decisionLights[i].setValue("&nbsp;");
            }
        }
        shown = false;
        doLights();
    }

    public void refresh() {
    }

    /**
     * @param refereeIndex2
     * @return
     */
    @SuppressWarnings("unused")
    private String refereeLabel(int refereeIndex2) {
        return Messages.getString("ORefereeConsole.Referee", CompetitionApplication.getCurrentLocale()) + " "
                + (refereeIndex2 + 1);
    }

}
