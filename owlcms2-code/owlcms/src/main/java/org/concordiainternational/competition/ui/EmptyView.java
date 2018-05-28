/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.ui;

import org.concordiainternational.competition.data.CompetitionSession;
import org.concordiainternational.competition.ui.components.ApplicationView;
import org.concordiainternational.competition.utils.LoggerUtils;

import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;

@SuppressWarnings("serial")
public class EmptyView extends VerticalLayout implements ApplicationView {

    private CompetitionApplication app;

    public EmptyView(CompetitionApplication app) {
        setViewName("(empty)");
        this.app = app;
        registerAsListener();
    }

    @Override
    public void refresh() {
    }

    @Override
    public boolean needsMenu() {
        return true;
    }

    @Override
    public void setParametersFromFragment(String fragment) {
    }

    @Override
    public String getFragment() {
        return "";
    }

    @Override
    public void registerAsListener() {
        app.getMainWindow().addListener((CloseListener) this);
        LoggerUtils.mdcSetup(getLoggingId(), null);
    }

    @Override
    public void unregisterAsListener() {
        app.getMainWindow().removeListener((CloseListener) this);
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
    private String viewName;

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
