package org.concordiainternational.competition.ui.list;

import java.util.Iterator;

import org.concordiainternational.competition.ui.CompetitionApplication;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public abstract class ToolbarView extends VerticalLayout {

    private static final long serialVersionUID = -3014859089271158928L;
    protected CompetitionApplication app;
    protected Component tableToolbar;
    protected String tableCaption;

    public ToolbarView() {
        super();
    }

    /**
     * @param app
     * @param parameterizedClass
     * @param caption
     */
    protected void init() {
        buildView();
    }

    /**
     * Builds a simple view for application with Table and a row of buttons below it.
     */
    protected void buildView() {

        // we synchronize because specializations of this class may do all sorts of
        // event-based things in their construction, and we don't want them to call
        // the push() method while in the constructor (this causes the session to drop.)
        synchronized (app) {
            boolean prevDisabled = app.getPusherDisabled();
            try {
                app.setPusherDisabled(true);

                this.setSizeFull();
                this.setMargin(true);

                tableToolbar = createTableToolbar();
                this.addComponent(tableToolbar);

                createBottom();
            } finally {
                app.setPusherDisabled(prevDisabled);
            }
        }

    }

    abstract protected void createBottom();

    /**
     * Create a "toolbar" above the table that contains a caption, and some buttons.
     */
    protected Component createTableToolbar() {
        HorizontalLayout tableToolbar1 = new HorizontalLayout();

        tableToolbar1.setStyleName("tableWithButtons"); //$NON-NLS-1$
        tableToolbar1.setMargin(true);
        tableToolbar1.setSpacing(true);

        createToolbarButtons(tableToolbar1);

        for (Iterator<?> iterator = tableToolbar1.getComponentIterator(); iterator.hasNext();) {
            Component component = (Component) iterator.next();
            tableToolbar1.setComponentAlignment(component, Alignment.MIDDLE_LEFT);
        }

        // add the caption first
        if (getTableCaption() != null) {
            final HorizontalLayout hl = new HorizontalLayout();
            final Label cap = new Label(getTableCaption());
            cap.setHeight("1.2em"); //$NON-NLS-1$
            hl.setStyleName("title"); //$NON-NLS-1$
            hl.addComponent(cap);
            hl.setComponentAlignment(cap, Alignment.MIDDLE_LEFT);
            tableToolbar1.addComponent(hl, 0);
            tableToolbar1.setComponentAlignment(hl, Alignment.MIDDLE_LEFT);
        }

        return tableToolbar1;
    }

    /**
     * @param tableToolbar1
     */
    protected void createToolbarButtons(HorizontalLayout tableToolbar1) {
    }

    protected abstract void setButtonVisibility();

    /**
     * @param tableToolbar
     *            the tableToolbar to set
     */
    public void setTableToolbar(Component tableToolbar) {
        this.replaceComponent(this.tableToolbar, tableToolbar);
        this.tableToolbar = tableToolbar;
    }

    /**
     * @return the tableToolbar
     */
    public Component getTableToolbar() {
        return tableToolbar;
    }

    public void refresh() {
    }

    public boolean needsBlack() {
        return false;
    }

    /**
     * @param tableCaption
     *            the tableCaption to set
     */
    public void setTableCaption(String tableCaption) {
        this.tableCaption = tableCaption;
    }

    /**
     * @return the tableCaption
     */
    public String getTableCaption() {
        return tableCaption;
    }

}