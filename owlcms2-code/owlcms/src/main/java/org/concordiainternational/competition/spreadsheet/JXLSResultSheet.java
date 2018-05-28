/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.spreadsheet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
import org.concordiainternational.competition.data.Competition;
import org.concordiainternational.competition.data.CompetitionSession;
import org.concordiainternational.competition.data.Lifter;
import org.concordiainternational.competition.data.lifterSort.LifterSorter;
import org.concordiainternational.competition.data.lifterSort.LifterSorter.Ranking;
import org.concordiainternational.competition.ui.CompetitionApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSResultSheet extends JXLSWorkbookStreamSource {

    Logger logger = LoggerFactory.getLogger(JXLSResultSheet.class);

    private Competition competition;

    public JXLSResultSheet() {
        super(true);
    }

    public JXLSResultSheet(boolean excludeNotWeighed) {
        super(excludeNotWeighed);
    }


    @Override
    protected void init() {
        //System.err.println("JXLSResultSheet init");
        super.init();
        competition = Competition.getAll().get(0);
        getReportingBeans().put("competition", competition);
        getReportingBeans().put("session", app.getCurrentCompetitionSession());
        //System.err.println("masters = "+getReportingBeans().get("masters"));
    }

    @Override
    public InputStream getTemplate() throws IOException {
        String protocolTemplateFileName = competition.getProtocolFileName();
        if (protocolTemplateFileName != null) {
            File templateFile = new File(protocolTemplateFileName);
            if (templateFile.exists()) {
                FileInputStream resourceAsStream = new FileInputStream(templateFile);
                return resourceAsStream;
            }
            // can't happen unless system is misconfigured.
            throw new IOException("resource not found: " + protocolTemplateFileName); //$NON-NLS-1$
        } else {
            throw new RuntimeException("Protocol sheet template not defined.");
        }
    }

    @Override
    protected void getSortedLifters() {
        final CompetitionSession currentGroup = ((CompetitionApplication) app).getCurrentCompetitionSession();
        if (currentGroup != null) {
            // LifterContainer is used to ensure filtering to current group
            this.lifters = LifterSorter.teamRankingOrderCopy(Lifter.getAll(isExcludeNotWeighed()),Ranking.CUSTOM);
            splitByGender(this.lifters, this.sortedMen, this.sortedWomen);
        } else {
            this.lifters = LifterSorter.teamRankingOrderCopy(Lifter.getAll(isExcludeNotWeighed()),Ranking.CUSTOM);
            splitByGender(this.lifters, this.sortedMen, this.sortedWomen);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.concordiainternational.competition.spreadsheet.JXLSWorkbookStreamSource#postProcess(org.apache.poi.ss.usermodel.Workbook)
     */
    @Override
    protected void postProcess(Workbook workbook) {
        if (Competition.invitedIfBornBefore() <= 0) {
            zapCellPair(workbook, 3, 17);
        }
        final CompetitionSession currentCompetitionSession = app.getCurrentCompetitionSession();
        if (currentCompetitionSession == null) {
            zapCellPair(workbook, 3, 9);
        }
    }

    public static void splitByGender(List<Lifter> sortedLifters, List<Lifter> sortedMen, List<Lifter> sortedWomen) {
        for (Lifter l : sortedLifters) {
            if ("m".equalsIgnoreCase(l.getGender())) {
                sortedMen.add(l);
            } else {
                sortedWomen.add(l);
            }
        }
    }

}
