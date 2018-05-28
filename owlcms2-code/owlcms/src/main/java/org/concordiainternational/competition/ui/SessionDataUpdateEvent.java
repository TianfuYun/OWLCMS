package org.concordiainternational.competition.ui;

import java.util.EventObject;

import org.concordiainternational.competition.data.Lifter;

/**
 * SessionData events all derive from this.
 */
public class SessionDataUpdateEvent extends EventObject {
    private static final long serialVersionUID = -126644150054472005L;
    private Lifter currentLifter;
    private boolean refreshRequest;
    private boolean isAnnounce;

    /**
     * Constructs a new event with a specified source component.
     *
     * @param source
     *            the source component of the event.
     * @param needToAnnounce
     * @param propertyIds
     *            that have been updated.
     */
    public SessionDataUpdateEvent(SessionData source, Lifter currentLifter, boolean needToAnnounce) {
        super(source);
        setCurrentLifter(currentLifter);
        setAnnounce(!needToAnnounce);
    }

    public SessionDataUpdateEvent(SessionData source) {
        super(source);
    }

    public static SessionDataUpdateEvent refreshEvent(SessionData source) {
        SessionDataUpdateEvent sd = new SessionDataUpdateEvent(source);
        sd.refreshRequest = true;
        return sd;
    }

    public static SessionDataUpdateEvent announceEvent(SessionData source) {
        SessionDataUpdateEvent sd = new SessionDataUpdateEvent(source);
        sd.isAnnounce = true;
        return sd;
    }

    public void setCurrentLifter(Lifter lifter) {
        this.currentLifter = lifter;
    }

    public Lifter getCurrentLifter() {
        return currentLifter;
    }

    public boolean getForceRefresh() {
        return refreshRequest;
    }

    public boolean isAnnounce() {
        return isAnnounce;
    }

    public void setAnnounce(boolean isAnnounce) {
        this.isAnnounce = isAnnounce;
    }

}