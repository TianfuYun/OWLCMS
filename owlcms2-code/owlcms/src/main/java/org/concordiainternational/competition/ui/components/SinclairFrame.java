package org.concordiainternational.competition.ui.components;

import java.net.MalformedURLException;
import java.net.URL;

import org.concordiainternational.competition.data.Lifter;
import org.concordiainternational.competition.ui.CompetitionApplication;
import org.concordiainternational.competition.ui.SessionData;

import com.vaadin.terminal.ExternalResource;

public class SinclairFrame extends ResultFrame {

    private static final long serialVersionUID = -2824024595096405022L;

    public SinclairFrame(boolean initFromFragment, String fragment, String viewName, String urlString, String stylesheetName, CompetitionApplication app) throws MalformedURLException {
        super(initFromFragment, fragment, viewName, urlString, stylesheetName, app);
    }


    /**
     * @param platformName1
     * @param masterData1
     * @throws RuntimeException
     */
    @Override
    protected void display(final String platformName1, final SessionData masterData1) throws RuntimeException {
        synchronized (app) {
            URL url = computeUrl(platformName1);
            logger.trace("display {}", url, getStylesheetName());

            @SuppressWarnings("unused")
            final Lifter currentLifter = masterData1.getCurrentLifter();
            iframe.setSource(new ExternalResource(url));

            resetTop();

            boolean displayBreakTimer = isBreak();

            if (displayBreakTimer) {
                showBreakTimer();
            } else {
                name.setValue(getWaitingMessage()); //$NON-NLS-1$
            }
            top.addComponent(name, "name"); //$NON-NLS-1$

            displayDecision(true);

            attempt.setValue(""); //$NON-NLS-1$
            logger.trace("adding attempt");
            top.addComponent(attempt, "attempt"); //$NON-NLS-1$
            attempt.setWidth(ATTEMPT_WIDTH); //$NON-NLS-1$

            if (showTimer && !displayBreakTimer) {
                timeDisplay.setValue(""); //$NON-NLS-1$
                logger.trace("adding timeDisplay");
                top.addComponent(timeDisplay, "timeDisplay"); //$NON-NLS-1$
            }

            weight.setValue(""); //$NON-NLS-1$
            weight.setWidth("4em"); //$NON-NLS-1$
            logger.trace("adding weight");
            top.addComponent(weight, "weight"); //$NON-NLS-1$
        }

        app.push();
    }
}
