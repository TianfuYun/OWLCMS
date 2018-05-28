/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.concordiainternational.competition.data.Competition;
import org.concordiainternational.competition.data.CompetitionSession;
import org.concordiainternational.competition.data.Lifter;
import org.concordiainternational.competition.data.lifterSort.LifterSorter;
import org.concordiainternational.competition.i18n.Messages;
import org.concordiainternational.competition.ui.CompetitionApplication;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSTimingStats extends JXLSWorkbookStreamSource {

    public class SessionStats {

        @Override
        public String toString() {
            double hours = (maxTime.getTime()-minTime.getTime())/1000.0/60.0/60.0;
            return "SessionStats [groupName=" + getGroupName() + ", nbLifters=" + nbLifters + ", minTime=" + minTime + ", maxTime=" + maxTime
                    + ", nbAttemptedLifts=" + nbAttemptedLifts + " Hours=" + hours+ " LiftersPerHour=" + nbLifters/hours+ "]" ;
        }

        String groupName = null;
        int nbLifters;
        Date maxTime = new Date(0L); // forever ago
        Date minTime = new Date(); // now
        int nbAttemptedLifts;

        public SessionStats() {
        }

        public SessionStats(String groupName) {
            this.setGroupName(groupName);
        }

        public Date getMaxTime() {
            return maxTime;
        }

        public Date getMinTime() {
            return minTime;
        }

        public int getNbAttemptedLifts() {
            return nbAttemptedLifts;
        }

        public int getNbLifters() {
            return nbLifters;
        }

        public void setMaxTime(Date maxTime) {
            this.maxTime = maxTime;
        }

        public void setMinTime(Date minTime) {
            this.minTime = minTime;
        };

        public void setNbAttemptedLifts(int nbAttemptedLifts) {
            this.nbAttemptedLifts = nbAttemptedLifts;
        }

        public void setNbLifters(int nbLifters) {
            this.nbLifters = nbLifters;
        }

        public void updateMaxTime(Date newTime) {
            if (this.maxTime.compareTo(newTime) < 0) {
//                System.err.println("updateMaxTime updating "+newTime+" later than "+this.maxTime);
                this.maxTime = newTime;
            } else {
//                System.err.println("updateMaxTime not updating: "+newTime+" earlier than "+this.maxTime);
            }

        }

        public void updateMinTime(Date newTime) {
            if (this.minTime.compareTo(newTime) > 0) {
//                System.err.println("updateMinTime updating: "+newTime+" earlier than "+this.minTime);
                this.minTime = newTime;
            } else {
//                System.err.println("updateMinTime not updating: "+newTime+" later than "+this.minTime);
            }

        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }
    }

    Logger logger = LoggerFactory.getLogger(JXLSTimingStats.class);

    public JXLSTimingStats() {
        super(false);
    }

    public JXLSTimingStats(boolean excludeNotWeighed) {
        super(excludeNotWeighed);
    }

    @Override
    protected void getSortedLifters() {
        HashMap<String, Object> reportingBeans = getReportingBeans();

        this.lifters = LifterSorter.registrationOrderCopy(Lifter.getAll(isExcludeNotWeighed()));
        if (lifters.isEmpty()) {
            // prevent outputting silliness.
            throw new RuntimeException(Messages.getString("OutputSheet.EmptySpreadsheet", CompetitionApplication.getCurrentLocale())); //$NON-NLS-1$
        }

        // extract group stats
        CompetitionSession curGroup = null;
        CompetitionSession prevGroup = null;

        List<SessionStats> sessions = new LinkedList<SessionStats>();

        SessionStats curStat = null;
        for (Lifter curLifter : lifters) {
            curGroup = curLifter.getCompetitionSession();
            if (curGroup == null) {
                continue;  // we simply skip over lifters with no groups
            }
            if (curGroup != prevGroup) {
                processGroup(sessions, curStat);

                String name = curGroup.getName();
                curStat = new SessionStats(name);
            }
            // update stats, min, max.
            curStat.setNbLifters(curStat.getNbLifters() + 1);
            Date minTime = curLifter.getFirstAttemptedLiftTime();
            curStat.updateMinTime(minTime);

            Date maxTime = curLifter.getLastAttemptedLiftTime();
            curStat.updateMaxTime(maxTime);

            int nbAttemptedLifts = curLifter.getAttemptedLifts();
            curStat.setNbAttemptedLifts(curStat.getNbAttemptedLifts() + nbAttemptedLifts);

            prevGroup = curGroup;
        }
        if (curStat.getNbLifters() > 0) {
            processGroup(sessions, curStat);
        }
        reportingBeans.put("groups", sessions);
    }

    @Override
    public InputStream getTemplate() throws IOException {
        String templateName = "/TimingStatsTemplate_" + CompetitionApplication.getCurrentSupportedLocale().getLanguage() + ".xls";
        final InputStream resourceAsStream = app.getResourceAsStream(templateName);
        if (resourceAsStream == null) {
            throw new IOException("resource not found: " + templateName);} //$NON-NLS-1$
        return resourceAsStream;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void init() {
        super.init();

        final Session hbnSession = CompetitionApplication.getCurrent().getHbnSession();
        List<Competition> competitionList = hbnSession.createCriteria(Competition.class).list();
        Competition competition = competitionList.get(0);
        getReportingBeans().put("competition", competition);

    }

    private void processGroup(List<SessionStats> sessions, SessionStats curStat) {
        if (curStat == null) return;
        //System.err.println(curStat.toString());
        sessions.add(curStat);
    }

}
