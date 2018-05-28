/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.publicAddress;

import org.concordiainternational.competition.i18n.Messages;
import org.concordiainternational.competition.publicAddress.IntermissionTimerEvent.IntermissionTimerListener;
import org.concordiainternational.competition.ui.CompetitionApplication;
import org.concordiainternational.competition.ui.EditingView;
import org.concordiainternational.competition.ui.components.ISO8601DateField;
import org.concordiainternational.competition.ui.generators.TimeFormatter;
import org.concordiainternational.competition.utils.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addon.customfield.CustomField;

import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItem;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.DateField;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class CountdownCombo extends CustomField implements IntermissionTimerListener {

    private static Logger logger = LoggerFactory.getLogger(CountdownCombo.class);

    private IntermissionTimer intermissionTimer;
    private Label remainingSecondsDisplay;
    private DateField endTime;
    private DurationField requestedSecondsField;
    private BeanItem<IntermissionTimer> timerItem;
    private CompetitionApplication app;

    private EditingView parentView;

    public CountdownCombo(EditingView parentView) {
        app = CompetitionApplication.getCurrent();
        this.parentView = parentView;
        setCompositionRoot(createLayout());
        setInternalValue(null);
    }

    /**
     * @param item
     */
    private Layout createLayout() {
        GridLayout grid = new GridLayout(4, 3);
        addIntermissionTimerToLayout(grid, 0);
        addEndTimeToLayout(grid, 1);
        addRequestedSecondsToLayout(grid, 2);
        grid.setSpacing(true);
        return grid;
    }

    /**
     * @param grid
     * @param row
     *            which row of the grid
     */
    private void addIntermissionTimerToLayout(GridLayout grid, int row) {
        grid.addComponent(new Label(Messages.getString("Field.CountdownField.runningTimer", app.getLocale())), 0, row);
        remainingSecondsDisplay = new Label();
        grid.addComponent(remainingSecondsDisplay, 1, row);
        HorizontalLayout timerButtons = new HorizontalLayout();
        Button start = new Button(
                "", //Messages.getString("Field.CountdownField.start", app.getLocale()),
                new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        LoggerUtils.mdcSetup(getLoggingId(),app.getMasterData());
                        intermissionTimer.restart(); // start from the current remaining number, or from scratch if none.
                    }
                });
        start.setIcon(new ThemeResource("icons/16/playTriangle.png"));
        timerButtons.addComponent(start);
        Button stop = new Button(
                "", //Messages.getString("Field.CountdownField.stop", app.getLocale()),
                new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        LoggerUtils.mdcSetup(getLoggingId(),app.getMasterData());
                        intermissionTimer.pause();
                    }
                });
        stop.setIcon(new ThemeResource("icons/16/pause.png"));
        timerButtons.addComponent(stop);
        Button clear = new Button(
                Messages.getString("Field.CountdownField.clear", app.getLocale()),
                new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        LoggerUtils.mdcSetup(getLoggingId(),app.getMasterData());
                        intermissionTimer.clear();
                    }
                });
        timerButtons.addComponent(clear);
        grid.addComponent(timerButtons, 2, row);
    }

    /**
     * @param grid
     * @param row
     *            which row of the grid
     */
    private void addEndTimeToLayout(GridLayout grid, int row) {
        Label label = new Label(Messages.getString("Field.CountdownField.endTime", app.getLocale()));
        label.setDescription(Messages.getString("Field.CountdownField.endTimeDescription", app.getLocale()));
        grid.addComponent(label, 0, row);
        endTime = new ISO8601DateField();

        //TODO: round time to next full multiple of 5 minutes.

        endTime.setReadOnly(false);
        endTime.setResolution(DateField.RESOLUTION_MIN);
        endTime.setImmediate(false);
        endTime.setWriteThrough(true);
        grid.addComponent(endTime, 1, row);

        HorizontalLayout buttons = new HorizontalLayout();
        Button set = new Button(Messages.getString("Field.CountdownField.set", app.getLocale()), new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                LoggerUtils.mdcSetup(getLoggingId(),app.getMasterData());
                endTime.commit(); // write to the underlying bean.
            }
        });
        buttons.addComponent(set);
        grid.addComponent(buttons, 2, row);
    }

    /**
     * @param grid
     * @param row
     *            which row of the grid
     */
    private void addRequestedSecondsToLayout(GridLayout grid, int row) {
        Label label = new Label(Messages.getString("Field.CountdownField.requestedSeconds", app.getLocale()));
        label.setDescription(Messages.getString("Field.CountdownField.requestedSecondsDescription", app.getLocale()));
        grid.addComponent(label, 0, row);

        // wrap a text field to handle the hh:mm:ss and mm:ss formats, returning milliseconds
        final TextField rawField = new TextField();
        requestedSecondsField = new DurationField(rawField, Integer.class);
        requestedSecondsField.setImmediate(true);
        requestedSecondsField.setWriteThrough(true);
        grid.addComponent(requestedSecondsField, 1, row);

        HorizontalLayout buttons = new HorizontalLayout();
        Button set = new Button(Messages.getString("Field.CountdownField.set", app.getLocale()), new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                LoggerUtils.mdcSetup(getLoggingId(),app.getMasterData());
                logger.debug("requestedSeconds prior to commit {}, rawfield={}", requestedSecondsField.getValue(), rawField.getValue());
                requestedSecondsField.commit(); // write to the underlying bean.
            }
        });

        Button five = new Button("5", new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                LoggerUtils.mdcSetup(getLoggingId(),app.getMasterData());
                requestedSecondsField.setValue(5 * 60);
                logger.debug("requestedSeconds prior to commit {}, rawfield={}", requestedSecondsField.getValue(), rawField.getValue());
                requestedSecondsField.commit(); // write to the underlying bean.
            }
        });
        Button ten = new Button("10", new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                LoggerUtils.mdcSetup(getLoggingId(),app.getMasterData());
                requestedSecondsField.setValue(10 * 60);
                logger.debug("requestedSeconds prior to commit {}, rawfield={}", requestedSecondsField.getValue(), rawField.getValue());
                requestedSecondsField.commit(); // write to the underlying bean.
            }
        });
        Button fifteen = new Button("15", new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                LoggerUtils.mdcSetup(getLoggingId(),app.getMasterData());
                requestedSecondsField.setValue(15 * 60);
                logger.debug("requestedSeconds prior to commit {}, rawfield={}", requestedSecondsField.getValue(), rawField.getValue());
                requestedSecondsField.commit(); // write to the underlying bean.
            }
        });

        buttons.addComponent(set);
        buttons.addComponent(five);
        buttons.addComponent(ten);
        buttons.addComponent(fifteen);
        grid.addComponent(buttons, 2, row);
    }

    @Override
    public void setInternalValue(Object newValue) throws ReadOnlyException,
            ConversionException {
        // use the official timer -- in our case no one should override.
//        timer = (newValue instanceof IntermissionTimer) ?
//                (IntermissionTimer) newValue
//                : CompetitionApplication.getCurrent().getMasterData().getIntermissionTimer();

        intermissionTimer = CompetitionApplication.getCurrent().getMasterData().getIntermissionTimer();
        logger.trace("intermissiontimer = {}",System.identityHashCode(intermissionTimer));
        super.setInternalValue(intermissionTimer);
        timerItem = new BeanItem<IntermissionTimer>(intermissionTimer);

        Property endTimeProperty = timerItem.getItemProperty("endTime");
        endTime.setPropertyDataSource(endTimeProperty);

        Property requestedSecondsProperty = timerItem.getItemProperty("requestedSeconds");
        requestedSecondsField.setPropertyDataSource(requestedSecondsProperty);


        int remainingMilliseconds = intermissionTimer.getRemainingMilliseconds();
        logger.debug("this={} display={}", this.toString(), remainingMilliseconds);
        remainingSecondsDisplay.setValue(TimeFormatter.formatAsSeconds(remainingMilliseconds));
    }

    @Override
    public Object getValue() {
        return intermissionTimer;
    }

    @Override
    public Class<?> getType() {
        return IntermissionTimer.class;
    }

    @Override
    public String toString() {
        return TimeFormatter.formatAsSeconds(intermissionTimer.getRemainingMilliseconds());
    }

    @Override
    public void intermissionTimerUpdate(IntermissionTimerEvent event) {
        Integer remainingMilliseconds = event.getRemainingMilliseconds();
        int seconds = TimeFormatter.getSeconds(remainingMilliseconds);
        logger.debug("received update this={} event={}", this.toString(), seconds);
        synchronized (app) {
            remainingSecondsDisplay.setValue(TimeFormatter.formatAsSeconds(remainingMilliseconds));
        }
        app.push();
    }

    private static int classCounter = 0; // per class
    private final int instanceId = classCounter++; // per instance

    public String getInstanceId() {
        return Long.toString(instanceId);
    }

    public String getLoggingId() {
        return getViewName(); //+ getInstanceId();
    }

    public String getViewName() {
        return ((EditingView) parentView).getViewName();
    }
}
