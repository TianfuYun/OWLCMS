/*
 * Copyright 2009-2012, Jean-François Lamy
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.data.lifterSort;

import java.util.Comparator;

import org.concordiainternational.competition.data.Lifter;

// för lag-sm, world-cup, lottnumret används som gruppnummer
// Ex: gruppen "1-2 Damer" innehåller 4 lyftare med lottnummer 1 och 4 lyftare med lottnummer 2
public class LiftOrderComparator extends AbstractLifterComparator implements Comparator<Lifter> {

    @Override
    public int compare(Lifter lifter1, Lifter lifter2) {
        int compare;

        // a lifter that has the boolean flag "forceAsFirst" collates smallest
        // by definition
        compare = compareForcedAsFirst(lifter1, lifter2);
        if (compare != 0)
            return compare;

        // lifters who are done lifting are shown at bottom, in reverse total
        // number
        compare = compareFinalResults(lifter1, lifter2);
        if (compare != 0)
            return compare;

        // ryck före stöt
        compare = compareLiftType(lifter1, lifter2);
        if (compare != 0)
            return compare;

        // grupp 1 före grupp 2
        compare = compareLotNumber(lifter1, lifter2);
        if (compare != 0)
            return compare;

        // lyftare som gjort färre lyft kör före lyftare som gjort fler
        compare = compareAttemptsDone(lifter1, lifter2);
        if (compare != 0)
            return compare;

        // lägst startnummer kör först, startnumret delas ut efter ingångsryck
        compare = compareStartNumber(lifter1, lifter2);
        if (compare != 0)
            return compare;

        // behövs ej
        compare = compareRequestedWeight(lifter1, lifter2);
        if (compare != 0)
            return compare;

        //behövs ej
        compare = compareProgression(lifter1, lifter2);
        if (compare != 0)
            return compare;

        return compare;
    }

}
