/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.data.lifterSort;

import java.util.Comparator;
import java.util.Date;

import org.concordiainternational.competition.data.Competition;
import org.concordiainternational.competition.data.CompetitionSession;
import org.concordiainternational.competition.data.Lifter;
import org.concordiainternational.competition.data.lifterSort.LifterSorter.Ranking;
import org.concordiainternational.competition.webapp.WebApplicationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Medal ordering.
 *
 * @author jflamy
 *
 */
public class WinningOrderComparator extends AbstractLifterComparator implements Comparator<Lifter> {

    final static Logger logger = LoggerFactory.getLogger(WinningOrderComparator.class);

    private Ranking rankingType;

    public WinningOrderComparator(Ranking rankingType) {
        this.rankingType = rankingType;
    }

    @Override
    public int compare(Lifter lifter1, Lifter lifter2) {
        int compare = 0;

        switch (rankingType) {
        case SNATCH:
            return compareSnatchResultOrder(lifter1, lifter2);
        case CLEANJERK:
            return compareCleanJerkResultOrder(lifter1, lifter2);
        case TOTAL:
            return compareTotalResultOrder(lifter1, lifter2);
        case CUSTOM:
            return compareCustomResultOrder(lifter1, lifter2);
        case SINCLAIR:
            if (Competition.isMasters()) {
                return compareSmmResultOrder(lifter1, lifter2);
            } else {
                if (WebApplicationConfiguration.isUseCategorySinclair()) {
                    return compareCategorySinclairResultOrder(lifter1, lifter2);
                } else {
                    return compareSinclairResultOrder(lifter1, lifter2);
                }
            }

        }

        return compare;
    }

    /**
     * Determine who ranks first. If the body weights are the same, the lifter who reached total first is ranked first.
     *
     * @param lifter1
     * @param lifter2
     * @return
     */
    public int compareTotalResultOrder(Lifter lifter1, Lifter lifter2) {
        int compare = 0;

        if (Competition.isMasters()) {
            compare = compareGender(lifter1, lifter2);
            if (compare != 0)
                return compare;

            compare = compareAgeGroup(lifter1, lifter2);
            if (compare != 0)
                return -compare;
        }

        if (WebApplicationConfiguration.isUseRegistrationCategory()) {
            compare = compareRegistrationCategory(lifter1, lifter2);
        } else {
            compare = compareCategory(lifter1, lifter2);
        }
        if (compare != 0)
            return compare;

        compare = compareTotal(lifter1, lifter2);
        traceComparison("compareTotal", lifter1, lifter2, compare);
        if (compare != 0) {
            return -compare; // we want reverse order - smaller comes after
        }


        return tieBreak(lifter1, lifter2, WebApplicationConfiguration.isUseOldBodyWeightTieBreak());
    }

    public int compareSnatchResultOrder(Lifter lifter1, Lifter lifter2) {
        boolean trace = false;
        int compare = 0;

        if (trace)
            logger.trace("lifter1 {};  lifter2 {}", lifter1.getFirstName(), lifter2.getFirstName());

        if (WebApplicationConfiguration.isUseRegistrationCategory()) {
            compare = compareRegistrationCategory(lifter1, lifter2);
        } else {
            compare = compareCategory(lifter1, lifter2);
        }
        if (trace)
            logger.trace("compareCategory {}", compare);
        if (compare != 0)
            return compare;

        compare = compareBestSnatch(lifter1, lifter2);
        if (trace)
            logger.trace("compareBestSnatch {}", compare);
        if (compare != 0)
            return -compare; // smaller snatch is less good

        compare = compareCompetitionSessionTime(lifter1,lifter2);
        traceComparison("compareCompetitionSessionTime", lifter1, lifter2, compare);
        if (compare != 0) {
            return compare; // earlier group time wins
        }

        if (WebApplicationConfiguration.isUseOldBodyWeightTieBreak()) {
            compare = compareBodyWeight(lifter1, lifter2);
            if (trace)
                logger.trace("compareBodyWeight {}", compare);
            if (compare != 0)
                return compare; // smaller lifter wins
        }

//        if (Competition.isMasters()) {
//            compare = compareBirthDate(lifter1, lifter2);
//            if (compare != 0) return -compare; // oldest wins
//        }

        compare = compareBestSnatchAttemptNumber(lifter1, lifter2);
        if (trace)
            logger.trace("compareBestSnatchAttemptNumber {}", compare);
        if (compare != 0)
            return compare; // earlier best attempt wins

        compare = comparePreviousAttempts(lifter1.getBestSnatchAttemptNumber(), false, lifter1, lifter2);
        if (trace)
            logger.trace("comparePreviousAttempts {}", compare);
        if (compare != 0)
            return compare; // compare attempted weights (prior to
                            // best attempt), smaller first

        compare = compareLotNumber(lifter1, lifter2);
        if (trace)
            logger.trace("compareLotNumber {}", compare);
        if (compare != 0)
            return compare; // if equality within a group,
                            // smallest lot number wins

        return compare;
    }

    public int compareCleanJerkResultOrder(Lifter lifter1, Lifter lifter2) {
        int compare = 0;

        if (WebApplicationConfiguration.isUseRegistrationCategory()) {
            compare = compareRegistrationCategory(lifter1, lifter2);
        } else {
            compare = compareCategory(lifter1, lifter2);
        }
        if (compare != 0)
            return compare;

        compare = compareBestCleanJerk(lifter1, lifter2);
        if (compare != 0)
            return -compare; // smaller is less good

        return tieBreak(lifter1, lifter2, WebApplicationConfiguration.isUseOldBodyWeightTieBreak());
    }

    /**
     * Determine who ranks first. If the body weights are the same, the lifter who reached total first is ranked first.
     *
     * @param lifter1
     * @param lifter2
     * @return
     */
    public int compareSinclairResultOrder(Lifter lifter1, Lifter lifter2) {
        int compare = 0;

        compare = compareSinclair(lifter1, lifter2);
        if (compare != 0)
            return compare;

        // for sinclair, lighter lifter that acheives same sinclair is better
        return tieBreak(lifter1, lifter2, true);
    }

    /**
     * Determine who ranks first. If the body weights are the same, the lifter who reached total first is ranked first.
     *
     * @param lifter1
     * @param lifter2
     * @return
     */
    public int compareCategorySinclairResultOrder(Lifter lifter1, Lifter lifter2) {
        int compare = 0;

        compare = compareCategorySinclair(lifter1, lifter2);
        if (compare != 0)
            return compare;

        return tieBreak(lifter1, lifter2, true);
    }

    /**
     * Determine who ranks first. If the body weights are the same, the lifter who reached total first is ranked first.
     *
     * @param lifter1
     * @param lifter2
     * @return
     */
    public int compareSmmResultOrder(Lifter lifter1, Lifter lifter2) {
        int compare = 0;

        compare = compareSmm(lifter1, lifter2);
        if (compare != 0)
            return compare;

        return tieBreak(lifter1, lifter2, true);
    }

    /**
     * Processing shared between all coefficient-based rankings
     *
     * @param lifter1
     * @param lifter2
     * @return
     */
    private int tieBreak(Lifter lifter1, Lifter lifter2, boolean bodyWeightTieBreak) {
        int compare;

        compare = compareCompetitionSessionTime(lifter1,lifter2);
        traceComparison("compareCompetitionSessionTime", lifter1, lifter2, compare);
        if (compare != 0) {
            return compare; // earlier group time wins
        }

        if (bodyWeightTieBreak) {
            compare = compareBodyWeight(lifter1, lifter2);
            traceComparison("compareBodyWeight", lifter1, lifter2, compare);
            if (compare != 0)
                return compare; // smaller lifter wins
        }

        // for total, must compare best clean and jerk value and smaller is better
        // because the total was reached earlier.
        // if this routine called to tiebreak cj ranking, the result will be 0 so this test is harmless
        compare = compareBestCleanJerk(lifter1, lifter2);
        traceComparison("compareBestCleanJerk", lifter1, lifter2, compare);
        if (compare != 0)
            return compare; // smaller cj, when total is the same, means total was reached earlier.

        // same clean and jerk, earlier attempt wins
        compare = compareBestCleanJerkAttemptNumber(lifter1, lifter2);
        traceComparison("compareBestCleanJerkAttemptNumber", lifter1, lifter2, compare);
        if (compare != 0)
            return compare; // earlier best attempt wins

        // determine who lifted best clean and jerk first
        compare = comparePreviousAttempts(lifter1.getBestCleanJerkAttemptNumber(), true, lifter1, lifter2);
        traceComparison("comparePreviousAttempts", lifter1, lifter2, compare);
        if (compare != 0)
            return compare; // compare attempted weights (prior to best attempt), smaller first

        // if equality within a group, smallest lot number wins (same session, same category, same weight, same attempt) -- smaller lot lifted first.
        compare = compareLotNumber(lifter1, lifter2);
        return compare;


    }

    private void traceComparison(String where, Lifter lifter1, Lifter lifter2, int compare) {
        if (logger.isTraceEnabled()) {
            logger.trace("{} {} {} {}",
                    where,
                    lifter1,
                    (compare < 0
                            ? "<"
                            :(compare == 0
                                 ? "="
                                 : ">")),
                    lifter2);
        }
    }

    /**
     * Compare competition session start times for two athletes.
     * A null session time is considered to be at the beginning of time, earlier than any non-null time.
     * @param lifter1
     * @param lifter2
     * @return -1 if lifter1 was part of earlier group, 0 if same group, 1 if lifter1 lifted in later group
     */
    private int compareCompetitionSessionTime(Lifter lifter1, Lifter lifter2) {
        CompetitionSession group1 = lifter1.getCompetitionSession();
        CompetitionSession group2 = lifter2.getCompetitionSession();
        if (group1 == null && group2 == null)
            return 0;
        if (group1 == null)
            return -1;
        if (group2 == null)
            return 1;
        Date competitionTime1 = group1.getCompetitionTime();
        Date competitionTime2 = group2.getCompetitionTime();
        if (competitionTime1 == null && competitionTime2 == null)
            return 0;
        if (competitionTime1 == null)
            return -1;
        if (competitionTime2 == null)
            return 1;
        return competitionTime1.compareTo(competitionTime2);
    }

    /**
     * Determine who ranks first. If the body weights are the same, the lifter who reached total first is ranked first.
     *
     * This variant allows judges to award a score based on a formula, with bonuses or penalties, manually. Used for the under-12
     * championship in Quebec.
     *
     * @param lifter1
     * @param lifter2
     * @return
     */
    public int compareCustomResultOrder(Lifter lifter1, Lifter lifter2) {
        return compareStartNumber(lifter1, lifter2);
    }

}
