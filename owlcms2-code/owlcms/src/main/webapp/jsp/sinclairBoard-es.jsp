<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" isELIgnored="false" import="org.concordiainternational.competition.ui.generators.*,org.concordiainternational.competition.ui.*,org.concordiainternational.competition.data.*,org.concordiainternational.competition.data.lifterSort.*,org.concordiainternational.competition.spreadsheet.*,java.util.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<html><!--
/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
 --><head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<%
    String platform = request.getParameter("platformName");
    if (platform == null) {
        out.println("Platform parameter expected. URL must include ?platformName=X");
        return;
    }
    pageContext.setAttribute("platform", platform);

    String style = request.getParameter("style");
    if (style == null) {
        out.println("Style parameter expected. URL must include ?style=X");
        return;
    }
    pageContext.setAttribute("style", style);

    ServletContext sCtx = this.getServletContext();
    SessionData groupData = (SessionData)sCtx.getAttribute(SessionData.MASTER_KEY+platform);
    if (groupData == null) return;
    CompetitionApplication app = (CompetitionApplication)sCtx.getAttribute(SessionData.MASTER_KEY+"AllLifters");
    if (app == null) {
        out.println("Cannot access calling application using attribute "+SessionData.MASTER_KEY+"*");
        return;
    }
    CompetitionApplication.setCurrent(app); // (ouch. global variable of the worst kind.)
    java.util.Locale locale = CompetitionApplication.getCurrentLocale();


    java.util.List<Lifter> lifters = Lifter.getAll(true);
    java.util.List<Lifter> sortedLifters = LifterSorter.resultsOrderCopy(lifters, LifterSorter.Ranking.SINCLAIR);
    LifterSorter.assignSinclairRanksAndPoints(sortedLifters, LifterSorter.Ranking.SINCLAIR);
    java.util.List<Lifter> sortedMen = new java.util.ArrayList<Lifter>(sortedLifters.size());
    java.util.List<Lifter> sortedWomen = new java.util.ArrayList<Lifter>(sortedLifters.size());
    JXLSCompetitionBook.splitByGender(sortedLifters, sortedMen, sortedWomen);

    int minMen = java.lang.Math.min(5,sortedMen.size());
    sortedMen = sortedMen.subList(0,minMen);
    ListIterator<Lifter> iterMen = sortedMen.listIterator();
    while (iterMen.hasNext()) {
	   if (iterMen.next().getSinclairForDelta() <= 0) iterMen.remove();
    }
    Lifter topMan = (sortedMen.size() > 0 ? sortedMen.get(0) : null);
    double topManSinclair = (topMan != null ? topMan.getSinclairForDelta() : 999.0D);

    int minWomen = java.lang.Math.min(5,sortedWomen.size());
    sortedWomen = sortedWomen.subList(0,minWomen);
    ListIterator<Lifter> iterWomen = sortedWomen.listIterator();
    while (iterWomen.hasNext()) {
	   Lifter curLifter = iterWomen.next();
       if (curLifter.getSinclairForDelta() <= 0) {
	        iterWomen.remove();
       }
    }
    Lifter topWoman = (sortedWomen.size() > 0 ? sortedWomen.get(0) : null);
    double topWomanSinclair = (topWoman != null ? topWoman.getSinclairForDelta() : 999.0D);

    if (lifters == null || lifters.size() == 0) {
        out.println("</head><body></body></html>");
        out.flush();
        return;
    }
    pageContext.setAttribute("lifters", lifters);
    pageContext.setAttribute("sortedMen", sortedMen);
    pageContext.setAttribute("sortedWomen", sortedWomen);
    pageContext.setAttribute("isMasters", Competition.isMasters());

    CompetitionSession group = groupData.getCurrentSession();
    pageContext.removeAttribute("groupName");
    pageContext.setAttribute("useGroupName", false);
%>
<title>Top 5 Sinclair Results</title>
<link rel="stylesheet" type="text/css" href="${style}" />
<!--  style type="text/css">
.requestedWeight {
    color: navy;
    font-size: medium;
    font-style: italic;
    font-weight: 400;
    text-align: center;
    width: 7%;
}
</style  -->
</head>
<body>
<div class="title">
        <span class="title">Top 5 Sinclair Results</span>
</div>

<table>
    <thead>
        <tr>
            <th>Name</th>
            <c:choose>
                <c:when test="${isMasters}">
                    <th>Age Gr.</th>
                </c:when>
            </c:choose>
            <th class="cat">Cat.</th>
            <th class='weight'>B.W.</th>
            <th class='club'>Team</th>
            <th colspan="3">Snatch</th>
            <th colspan="3">Clean&amp;Jerk</th>
            <th>Total</th>
            <th>Sinclair</th>
            <th class="cat" style='text-align: center'>Rank</th>
            <th class="cat" style='text-align: center'>kg Needed</th>
        </tr>
    </thead>
    <tbody>
        <% if (sortedWomen.size() > 0) { %>
            <tr>
                <c:choose>
                    <c:when test="${isMasters}">
                        <td colspan="15">Women</td>
                    </c:when>
                    <c:otherwise>
                        <td colspan="14">Women</td>
                    </c:otherwise>
                </c:choose>
            </tr>
        <% } %>
        <c:forEach var="womanLifter" items="${sortedWomen}">
            <jsp:useBean id="womanLifter" type="org.concordiainternational.competition.data.Lifter" />
            <tr>
                <c:choose>
                    <c:when test="${womanLifter.currentLifter}">
                        <td class='name current'><nobr><%= womanLifter.getLastName().toUpperCase() %>, ${womanLifter.firstName}</nobr></td>
                    </c:when>
                    <c:otherwise>
                        <td class='name'><nobr><%= womanLifter.getLastName().toUpperCase() %>, ${womanLifter.firstName}</nobr></td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${isMasters}">
                        <td class='club'><nobr>${womanLifter.mastersAgeGroup}</nobr></td>
                    </c:when>
                </c:choose>
                <td class="cat" ><nobr>${womanLifter.shortCategory}</nobr></td>
                <td class='narrow'><%= WeightFormatter.formatBodyWeight(womanLifter.getBodyWeight()) %></td>
                <td class='club'><nobr>${womanLifter.club}</nobr></td>
                <!--  td class="weight">&nbsp;</td>  -->
                <c:choose>
                    <c:when test="${womanLifter.snatchAttemptsDone == 0}">
                        <c:choose>
                            <c:when test="${womanLifter.currentLifter}">
                                <td class='currentWeight'>${womanLifter.nextAttemptRequestedWeight}</td>
                            </c:when>
                            <c:otherwise>
                                <td class='requestedWeight'>${womanLifter.nextAttemptRequestedWeight}</td>
                            </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:when test="${womanLifter.snatchAttemptsDone > 0 }">
                        <%= WeightFormatter.htmlFormatWeight(womanLifter.getSnatch1ActualLift()) %>
                    </c:when>
                    <c:otherwise>
                        <td class='weight'></td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${womanLifter.snatchAttemptsDone == 1}">
                        <c:choose>
                            <c:when test="${womanLifter.currentLifter}">
                                <td class='currentWeight'>${womanLifter.nextAttemptRequestedWeight}</td>
                            </c:when>
                            <c:otherwise>
                                <td class='requestedWeight'>${womanLifter.nextAttemptRequestedWeight}</td>
                            </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:when test="${womanLifter.snatchAttemptsDone > 1}">
                        <%= WeightFormatter.htmlFormatWeight(womanLifter.getSnatch2ActualLift()) %>
                    </c:when>
                    <c:otherwise>
                        <td class='weight'></td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${womanLifter.snatchAttemptsDone == 2}">
                        <c:choose>
                            <c:when test="${womanLifter.currentLifter}">
                                <td class='currentWeight'>${womanLifter.nextAttemptRequestedWeight}</td>
                            </c:when>
                            <c:otherwise>
                                <td class='requestedWeight'>${womanLifter.nextAttemptRequestedWeight}</td>
                            </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:when test="${womanLifter.snatchAttemptsDone > 2}">
                        <%= WeightFormatter.htmlFormatWeight(womanLifter.getSnatch3ActualLift()) %>
                    </c:when>
                    <c:otherwise>
                        <td class='weight'></td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${womanLifter.attemptsDone == 3}">
                        <c:choose>
                            <c:when test="${womanLifter.currentLifter}">
                                <td class='currentWeight'>${womanLifter.nextAttemptRequestedWeight}</td>
                            </c:when>
                            <c:otherwise>
                                <td class='requestedWeight'>${womanLifter.nextAttemptRequestedWeight}</td>
                            </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:when test="${womanLifter.cleanJerkAttemptsDone > 0}">
                        <%= WeightFormatter.htmlFormatWeight(womanLifter.getCleanJerk1ActualLift()) %>
                    </c:when>
                    <c:otherwise>
                        <td class='requestedWeight'><%= womanLifter.getRequestedWeightForAttempt(4) %></td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${womanLifter.cleanJerkAttemptsDone == 1}">
                        <c:choose>
                            <c:when test="${womanLifter.currentLifter}">
                                <td class='currentWeight'>${womanLifter.nextAttemptRequestedWeight}</td>
                            </c:when>
                            <c:otherwise>
                                <td class='requestedWeight'>${womanLifter.nextAttemptRequestedWeight}</td>
                            </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:when test="${womanLifter.cleanJerkAttemptsDone > 1}">
                        <%= WeightFormatter.htmlFormatWeight(womanLifter.getCleanJerk2ActualLift()) %>
                    </c:when>
                    <c:otherwise>
                        <td class='weight'></td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${womanLifter.cleanJerkAttemptsDone == 2}">
                        <c:choose>
                            <c:when test="${womanLifter.currentLifter}">
                                <td class='currentWeight'>${womanLifter.nextAttemptRequestedWeight}</td>
                            </c:when>
                            <c:otherwise>
                                <td class='requestedWeight'>${womanLifter.nextAttemptRequestedWeight}</td>
                            </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:when test="${womanLifter.cleanJerkAttemptsDone > 2}">
                        <%= WeightFormatter.htmlFormatWeight(womanLifter.getCleanJerk3ActualLift()) %>
                    </c:when>
                    <c:otherwise>
                        <td class='weight'></td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${womanLifter.total > 0}">
                        <td class='weight'>${womanLifter.total}</td>
                    </c:when>
                    <c:otherwise>
                        <td class='weight'>&ndash;</td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${womanLifter.sinclair > 0}">
                        <td class='cat'><%= String.format(locale, "%.3f",womanLifter.getSinclair()) %></td>
                    </c:when>
                    <c:otherwise>
                        <td class='cat'>&ndash;</td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${womanLifter.sinclairRank > 0}">
                        <td class='cat'>${womanLifter.sinclairRank}</td>
                    </c:when>
                    <c:otherwise>
                        <td class='cat'>&ndash;</td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${womanLifter.sinclairForDelta > 0}">
                        <td class='cat'>
                        <% double womanNeeded = Math.ceil(
                            (topWomanSinclair - womanLifter.getSinclairForDelta())
                            / womanLifter.getSinclairFactor()
                            );
                           out.println((womanNeeded > 0 ? String.format(locale, "%.0f", womanNeeded) : "&ndash;"));
                        %></td>
                    </c:when>
                    <c:otherwise>
                        <td class='cat'>&ndash;</td>
                    </c:otherwise>
                </c:choose>
            </tr>
        </c:forEach>

        <% if (sortedMen.size() > 0) { %>
            <tr>
                <c:choose>
                    <c:when test="${isMasters}">
                        <td colspan="15">Men</td>
                    </c:when>
                    <c:otherwise>
                        <td colspan="14">Men</td>
                    </c:otherwise>
                </c:choose>
            </tr>
        <% } %>

        <c:forEach var="manLifter" items="${sortedMen}">
            <jsp:useBean id="manLifter" type="org.concordiainternational.competition.data.Lifter" />
            <tr>
                <c:choose>
                    <c:when test="${manLifter.currentLifter}">
                        <td class='name current'><nobr><%= manLifter.getLastName().toUpperCase() %>, ${manLifter.firstName}</nobr></td>
                    </c:when>
                    <c:otherwise>
                        <td class='name'><nobr><%= manLifter.getLastName().toUpperCase() %>, ${manLifter.firstName}</nobr></td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${isMasters}">
                        <td class='club'><nobr>${manLifter.mastersAgeGroup}</nobr></td>
                    </c:when>
                </c:choose>
                <td class="cat" ><nobr>${manLifter.shortCategory}</nobr></td>
                <td class='narrow'><%= WeightFormatter.formatBodyWeight(manLifter.getBodyWeight()) %></td>
                <td class='club'><nobr>${manLifter.club}</nobr></td>
                <!--  td class="weight">&nbsp;</td>  -->
                <c:choose>
                    <c:when test="${manLifter.snatchAttemptsDone == 0}">
                        <c:choose>
                            <c:when test="${manLifter.currentLifter}">
                                <td class='currentWeight'>${manLifter.nextAttemptRequestedWeight}</td>
                            </c:when>
                            <c:otherwise>
                                <td class='requestedWeight'>${manLifter.nextAttemptRequestedWeight}</td>
                            </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:when test="${manLifter.snatchAttemptsDone > 0 }">
                        <%= WeightFormatter.htmlFormatWeight(manLifter.getSnatch1ActualLift()) %>
                    </c:when>
                    <c:otherwise>
                        <td class='weight'></td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${manLifter.snatchAttemptsDone == 1}">
                        <c:choose>
                            <c:when test="${manLifter.currentLifter}">
                                <td class='currentWeight'>${manLifter.nextAttemptRequestedWeight}</td>
                            </c:when>
                            <c:otherwise>
                                <td class='requestedWeight'>${manLifter.nextAttemptRequestedWeight}</td>
                            </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:when test="${manLifter.snatchAttemptsDone > 1}">
                        <%= WeightFormatter.htmlFormatWeight(manLifter.getSnatch2ActualLift()) %>
                    </c:when>
                    <c:otherwise>
                        <td class='weight'></td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${manLifter.snatchAttemptsDone == 2}">
                        <c:choose>
                            <c:when test="${manLifter.currentLifter}">
                                <td class='currentWeight'>${manLifter.nextAttemptRequestedWeight}</td>
                            </c:when>
                            <c:otherwise>
                                <td class='requestedWeight'>${manLifter.nextAttemptRequestedWeight}</td>
                            </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:when test="${manLifter.snatchAttemptsDone > 2}">
                        <%= WeightFormatter.htmlFormatWeight(manLifter.getSnatch3ActualLift()) %>
                    </c:when>
                    <c:otherwise>
                        <td class='weight'></td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${manLifter.attemptsDone == 3}">
                        <c:choose>
                            <c:when test="${manLifter.currentLifter}">
                                <td class='currentWeight'>${manLifter.nextAttemptRequestedWeight}</td>
                            </c:when>
                            <c:otherwise>
                                <td class='requestedWeight'>${manLifter.nextAttemptRequestedWeight}</td>
                            </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:when test="${manLifter.cleanJerkAttemptsDone > 0}">
                        <%= WeightFormatter.htmlFormatWeight(manLifter.getCleanJerk1ActualLift()) %>
                    </c:when>
                    <c:otherwise>
                        <td class='requestedWeight'><%= manLifter.getRequestedWeightForAttempt(4) %></td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${manLifter.cleanJerkAttemptsDone == 1}">
                        <c:choose>
                            <c:when test="${manLifter.currentLifter}">
                                <td class='currentWeight'>${manLifter.nextAttemptRequestedWeight}</td>
                            </c:when>
                            <c:otherwise>
                                <td class='requestedWeight'>${manLifter.nextAttemptRequestedWeight}</td>
                            </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:when test="${manLifter.cleanJerkAttemptsDone > 1}">
                        <%= WeightFormatter.htmlFormatWeight(manLifter.getCleanJerk2ActualLift()) %>
                    </c:when>
                    <c:otherwise>
                        <td class='weight'></td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${manLifter.cleanJerkAttemptsDone == 2}">
                        <c:choose>
                            <c:when test="${manLifter.currentLifter}">
                                <td class='currentWeight'>${manLifter.nextAttemptRequestedWeight}</td>
                            </c:when>
                            <c:otherwise>
                                <td class='requestedWeight'>${manLifter.nextAttemptRequestedWeight}</td>
                            </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:when test="${manLifter.cleanJerkAttemptsDone > 2}">
                        <%= WeightFormatter.htmlFormatWeight(manLifter.getCleanJerk3ActualLift()) %>
                    </c:when>
                    <c:otherwise>
                        <td class='weight'></td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${manLifter.total > 0}">
                        <td class='weight'>${manLifter.total}</td>
                    </c:when>
                    <c:otherwise>
                        <td class='weight'>&ndash;</td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${manLifter.sinclair > 0}">
                        <td class='cat'><%= String.format(locale,"%.3f",manLifter.getSinclair()) %></td>
                    </c:when>
                    <c:otherwise>
                        <td class='cat'>&ndash;</td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${manLifter.sinclairRank > 0}">
                        <td class='cat'>${manLifter.sinclairRank}</td>
                    </c:when>
                    <c:otherwise>
                        <td class='cat'>&ndash;</td>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${manLifter.sinclairForDelta > 0}">
                        <td class='cat'>
                        <% double manNeeded = Math.ceil(
                            (topManSinclair - manLifter.getSinclairForDelta())
                            / manLifter.getSinclairFactor()
                            );
                           out.println(manNeeded > 0 ? String.format(locale, "%.0f", manNeeded) : "&ndash;");
                        %></td>
                    </c:when>
                    <c:otherwise>
                        <td class='cat'>&ndash;</td>
                    </c:otherwise>
                </c:choose>
            </tr>
        </c:forEach>
    </tbody>
</table>

</body>
</html>