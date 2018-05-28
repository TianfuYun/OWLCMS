/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.ui.list;


import org.concordiainternational.competition.data.CategoryLookup;
import org.concordiainternational.competition.i18n.Messages;
import org.concordiainternational.competition.ui.CompetitionApplication;
import org.concordiainternational.competition.ui.generators.CommonFieldFactory;
import org.concordiainternational.competition.ui.generators.FieldTable;

import com.vaadin.data.hbnutil.HbnContainer.HbnSessionManager;
import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;

@SuppressWarnings("serial")
public abstract class GenericList<T> extends ToolbarView {

    public Table table;
    protected Class<T> parameterizedClass;

    protected Button toggleEditModeButton;
    protected Button addRowButton;

    public GenericList(CompetitionApplication app, Class<T> parameterizedClass, String caption) {
        super();
        this.app = app;
        this.parameterizedClass = parameterizedClass;
        this.tableCaption = caption;
    }

    /**
	 *
	 */
    protected void positionTable() {
        // make table consume all extra space
        this.setSizeFull();
        this.setExpandRatio(table, 1);
        table.setSizeFull();
    }

    @Override
    protected void createBottom() {
        populateAndConfigureTable();

        this.addComponent(table);
        positionTable();
        setButtonVisibility();
    }

    @Override
    public void refresh() {
        Component oldTable = table;
        CategoryLookup.getSharedInstance().reload();
        this.populateAndConfigureTable();
        this.replaceComponent(oldTable, table);
        this.positionTable();
    }

    /**
     * Guess expansion ratios. Empirical; there is probably a better way.
     */
    protected void setExpandRatios() {
        Object[] visibleColumns = table.getVisibleColumns();
        for (int i = 0; i < visibleColumns.length; i++) {
            Object columnId = visibleColumns[i];
            if (columnId.equals("lastName") || columnId.equals("firstName")) {
                table.setColumnExpandRatio(columnId, 1.0F);
            } else {
                table.setColumnExpandRatio(columnId, 0.0F);
            }

        }
    }

    /**
     *
     */
    protected void populateAndConfigureTable() {

        // FieldTable always generates fields to ensure consistent formatting.
        table = new FieldTable();

        table.setWidth("100%"); //$NON-NLS-1$
        table.setSelectable(true);
        table.setImmediate(true);
        table.setColumnCollapsingAllowed(true);

        // load the data (this enables the table to know what fields are
        // available).
        loadData(); // table.getContainerDataSource() returns the datasource

        if (table.getContainerDataSource().size() > 0) {
            // enhance table
            setTableFieldFactory();
            addGeneratedColumns(); // add editing buttons in a generated cell.
            addRowContextMenu();

            // set visibility and ordering
            setVisibleColumns();
            setColumnHeaders();

            // hide editing buttons if not editable
            if (!table.isEditable()) {
                table.removeGeneratedColumn("actions"); //$NON-NLS-1$
            }
            table.setPageLength(30);
            table.setCacheRate(2.0);
        } else {
            table.setVisibleColumns(new Object[] {});
        }
    }

    protected void addGeneratedColumns() {
    }

    /**
     * Handles the creation of fields when the table is built in editable mode. By default, the same factory used by default for Form items
     * is used.
     */
    protected void setTableFieldFactory() {
        table.setTableFieldFactory(new CommonFieldFactory((HbnSessionManager) app));
    }

    /**
     * Add context menu to the table rows.
     */
    protected void addRowContextMenu() {
        // add context menus for rows
        table.addActionHandler(new Handler() {
            private static final long serialVersionUID = -6577539154616303085L;

            Action add = new Action(Messages.getString("Common.addRow", app.getLocale())); //$NON-NLS-1$
            Action remove = new Action(Messages.getString("Common.deleteThisRow", app.getLocale())); //$NON-NLS-1$
            Action[] actions = new Action[] { add, remove };

            @Override
            public Action[] getActions(Object target, Object sender) {
                return actions;
            }

            @Override
            public void handleAction(Action action, Object sender, Object targetId) {
                if (action == add) {
                    newItem();
                } else if (action == remove) {
                    deleteItem(targetId);
                }
            }
        });
    }

    /**
     * @param targetId
     */
    public void deleteItem(Object targetId) {
        table.removeItem(targetId);
    }

    /**
     * Adds new row to Table and selects new row. Table will delegate Item creation to its container.
     */
    public Object newItem() {
        Object newItemId = table.addItem();
        // open in announcerLiftEditor window unless table is in content
        // editable mode
        if (!table.isEditable()) {
            table.setValue(newItemId);
        }
        return newItemId;
    }

    /**
     * @param tableToolbar1
     */
    @Override
    protected void createToolbarButtons(HorizontalLayout tableToolbar1) {
    }

    /**
     * This method is used in response to a button click.
     */
    public void toggleEditable() {
        table.setEditable(!table.isEditable());
        setButtonVisibility();
    }

    @Override
    protected abstract void setButtonVisibility();

    protected abstract void clearCache();

    protected abstract void loadData();

    public boolean isEditable() {
        return table.isEditable();
    }

    abstract protected String[] getColHeaders();

    abstract protected String[] getColOrder();

    /**
     * Defines which fields from the container and the generated columns will actually be visible, and in which order they will be
     * displayed.
     */
    protected void setVisibleColumns() {
        final String[] colOrder = getColOrder();
        table.setVisibleColumns(colOrder);
    }

    /**
     * Defines the readable headers for the table columns
     *
     * @return
     */
    protected void setColumnHeaders() {
        final String[] colHeaders = getColHeaders();
        table.setColumnHeaders(colHeaders);
    }


}
