/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.data;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.concordiainternational.competition.tests.AllTests;
import org.concordiainternational.competition.ui.CompetitionApplication;
import org.concordiainternational.competition.webapp.WebApplicationConfiguration;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
public class Competition implements Serializable {
    private static final long serialVersionUID = -2817516132425565754L;

    private static final Logger logger = LoggerFactory.getLogger(Competition.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    String competitionName;
    String competitionSite;
    Date competitionDate = new Date();
    String competitionCity;
    String competitionOrganizer;
    String resultTemplateFileName;
    String protocolFileName;

    Integer invitedIfBornBefore;
    Boolean masters;
    private Boolean enforce15_20KgRule;

    String federation;
    String federationAddress;
    String federationWebSite;
    String federationEMail;

    public String getCompetitionName() {
        return competitionName;
    }

    public void setCompetitionName(String competitionName) {
        this.competitionName = competitionName;
    }

    public String getCompetitionSite() {
        return competitionSite;
    }

    public void setCompetitionSite(String competitionSite) {
        this.competitionSite = competitionSite;
    }

    public Date getCompetitionDate() {
        return competitionDate;
    }

    public void setCompetitionDate(Date competitionDate) {
        this.competitionDate = competitionDate;
    }

    public String getCompetitionCity() {
        return competitionCity;
    }

    public void setCompetitionCity(String competitionCity) {
        this.competitionCity = competitionCity;
    }

    public Boolean getMasters() {
        return masters;
    }

    public void setMasters(Boolean masters) {
        this.masters = masters;
    }

    public String getFederation() {
        return federation;
    }

    public void setFederation(String federation) {
        this.federation = federation;
    }

    public String getFederationAddress() {
        return federationAddress;
    }

    public void setFederationAddress(String federationAddress) {
        this.federationAddress = federationAddress;
    }

    public String getFederationWebSite() {
        return federationWebSite;
    }

    public void setFederationWebSite(String federationWebSite) {
        this.federationWebSite = federationWebSite;
    }

    public String getFederationEMail() {
        return federationEMail;
    }

    public void setFederationEMail(String federationEMail) {
        this.federationEMail = federationEMail;
    }

    public Long getId() {
        return id;
    }

    public Integer getInvitedIfBornBefore() {
        if (invitedIfBornBefore == null)
            return 0;
        return invitedIfBornBefore;
    }

    public void setInvitedIfBornBefore(Integer invitedIfBornBefore) {
        this.invitedIfBornBefore = invitedIfBornBefore;
    }

    public String getCompetitionOrganizer() {
        return competitionOrganizer;
    }

    public void setCompetitionOrganizer(String competitionOrganizer) {
        this.competitionOrganizer = competitionOrganizer;
    }


    static Integer invitedThreshold = null;

    @SuppressWarnings("unchecked")
    public static int invitedIfBornBefore() {
        if (invitedThreshold != null)
            return invitedThreshold;
        final CompetitionApplication currentApp = CompetitionApplication.getCurrent();
        final Session hbnSession = currentApp.getHbnSession();
        List<Competition> competitions = hbnSession.createCriteria(Competition.class).list();
        if (competitions.size() > 0) {
            final Competition competition = competitions.get(0);
            invitedThreshold = competition.getInvitedIfBornBefore();
        }
        if (invitedThreshold == null)
            return 0;
        return invitedThreshold;
    }

    @SuppressWarnings("unchecked")
    static public List<Competition> getAll() {
        final List<Competition> list = CompetitionApplication.getCurrent().getHbnSession().createCriteria(Competition.class).list();
        return list;
    }

    static Boolean isMasters = null;
    private static Boolean isEnforce15_20rule = null;

    public static boolean isMasters() {
        if (isMasters != null)
            return isMasters;
        Competition competition = getCompetition();
        isMasters = competition.getMasters();
        if (isMasters == null) {
            isMasters = false;
            return false;
        } else {
            return isMasters;
        }
    }

    public static boolean isEnforce15_20rule() {
        if (isEnforce15_20rule != null)
            return isEnforce15_20rule;
        Competition competition = getCompetition();
        isEnforce15_20rule = competition.getEnforce15_20KgRule();
        if (isEnforce15_20rule == null) {
            return false;
        } else {
            return isEnforce15_20rule;
        }
    }

    public static Competition getCompetition() {
        final CompetitionApplication currentApp = CompetitionApplication.getCurrent();
        final Session hbnSession = (currentApp != null ? currentApp.getHbnSession() : AllTests.getSessionManager().getHbnSession());
        @SuppressWarnings("unchecked")
        List<Competition> competitions = hbnSession.createCriteria(Competition.class).list();
        Competition competition = null;
        if (competitions.size() > 0) {
            competition = competitions.get(0);
        }
        return competition;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((competitionName == null) ? 0 : competitionName.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Competition other = (Competition) obj;
        if (competitionName == null) {
            if (other.competitionName != null)
                return false;
        } else if (!competitionName.equals(other.competitionName))
            return false;
        return true;
    }

    public String getProtocolFileName() throws IOException {
        logger.debug("protocolFileName = {}", protocolFileName);
        String str = File.separator+"protocolSheet";
        int protocolPos = protocolFileName.indexOf(str);
        if (protocolPos != -1) {
            // make file relative
            String substring = protocolFileName.substring(protocolPos+1);
            logger.debug("relative protocolFileName = {}",substring);
            return CompetitionApplication.getCurrent().getResourceFileName(substring);
        } else {
            logger.debug("could not find {}", str);
        }
        return CompetitionApplication.getCurrent().getResourceFileName(protocolFileName);
    }

    public void setProtocolFileName(String protocolFileName) {
        this.protocolFileName = protocolFileName;
    }

    public String getResultTemplateFileName() throws IOException {
        logger.debug("competitionBookFileName = {}", resultTemplateFileName);
        String str = File.separator+"competitionBook";
        int protocolPos = resultTemplateFileName.indexOf(str);
        if (protocolPos != -1) {
            // make file relative
            String substring = resultTemplateFileName.substring(protocolPos+1);
            logger.debug("relative competitionBookFileName = {}",substring);
            return CompetitionApplication.getCurrent().getResourceFileName(substring);
        } else {
            logger.debug("could not find {}", str);
        }
        return CompetitionApplication.getCurrent().getResourceFileName(resultTemplateFileName);
    }

    public void setResultTemplateFileName(String resultTemplateFileName) {
        this.resultTemplateFileName = resultTemplateFileName;
    }

    public static Boolean getEnforce15_20rule() {
        return isEnforce15_20rule;
    }

    public static void setEnforce15_20rule(Boolean isEnforce15_20rule) {
        Competition.isEnforce15_20rule = isEnforce15_20rule;
    }

    public Boolean getEnforce15_20KgRule() {
        return enforce15_20KgRule;
    }

    public void setEnforce15_20KgRule(Boolean enforce15_20KgRule) {
        this.enforce15_20KgRule = enforce15_20KgRule;
    }

    public boolean getUseRegistrationCategory() {
        return WebApplicationConfiguration.isUseRegistrationCategory();
    }

}
