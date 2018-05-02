<%@ page language="java" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8" isELIgnored="false" import="org.concordiainternational.competition.ui.generators.*,org.concordiainternational.competition.ui.*,org.concordiainternational.competition.data.*,org.concordiainternational.competition.data.lifterSort.*,org.concordiainternational.competition.spreadsheet.*,java.util.*"%>
<!DOCTYPE html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<html>
<!--
/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
-->
<head>
<meta content="text/html; charset=utf-8" http-equiv="Content-Type">

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
java.util.List<Lifter> sortedLifters = LifterSorter.teamRankingOrderCopy(lifters, LifterSorter.Ranking.CUSTOM);
LifterSorter.assignSinclairRanksAndPoints(sortedLifters, LifterSorter.Ranking.SINCLAIR);

java.util.List<Lifter> sortedMen = new java.util.ArrayList<Lifter>(sortedLifters.size());
java.util.List<Lifter> sortedWomen = new java.util.ArrayList<Lifter>(sortedLifters.size());
JXLSCompetitionBook.splitByGender(sortedLifters, sortedMen, sortedWomen);

if (lifters == null || lifters.size() == 0) {
	out.println("</head><body></body></html>");
	out.flush();
	return;
}

pageContext.setAttribute("sortedMen", sortedMen);
pageContext.setAttribute("sortedWomen", sortedWomen);

java.util.List<Lifter> currentlifters = groupData.getCurrentLiftingOrder();
if (currentlifters == null || currentlifters.size() == 0) {
	out.flush();
	return;
}

Integer liftersPerTeam;

if (currentlifters.get(0).getGender().contains("F")) {
	lifters = sortedWomen;
	liftersPerTeam = 4;
} else {
	lifters = sortedMen;
	liftersPerTeam = 6;
}

pageContext.setAttribute("lifters", lifters);
pageContext.setAttribute("liftersPerTeam", liftersPerTeam);

%>

<title>Lag-resultat</title>

<link href="${style}" rel="stylesheet" type="text/css">

<style type="text/css">
	body {
		zoom: 1.5;
	}
	.requestedWeight {
		color: navy;
		font-size: medium;
		font-style: italic;
		font-weight: 400;
		text-align: center;
		width: 8%;
	}
	.success, .fail {
		font-size: medium;
		font-weight: 400;
		width: 8%;
	}
	.name {width: 30%;}
	.name, .weight, .cat, .noborder {
		font-size: medium;
		font-weight: 400;
	}
	.weight, .cat {
		width: 11%;
	}
	.noborder {
		border: none;
	}
	.team {
		border: none;
		font-size: x-large;
		text-align: center;
	}
	.totalSinclair {
		border: none;
		text-align: right;
		font-size: x-large;
		font-weight: bold;
	}
	.outer {
		border: none;
		padding-left: 30px;
		padding-right: 30px;
	}
</style>
</head>
<body>
<br>
<div>
<table>
<tr>
<td class="outer">
<table id="upperLeftTable">
	<thead>
		<tr>
			<th>Namn</th>
			<th colspan="3">Ryck</th>
			<th colspan="3">Stöt</th>
			<th>Koeff.</th>
			<th>Sinclair</th>
		</tr>
	</thead>
	<tbody>
		<% if (lifters.size() > 0) { %>
		<tr>
			<td class="noborder"></td>
		</tr>
		<tr>
			<td class="noborder"></td>
		</tr>
		<tr>
			<td class="team" colspan="9">${lifters[0].club}</td>
		</tr>
		<% } %>
		<c:forEach var="lifter1" items="${lifters}" begin="0" end="${liftersPerTeam - 1}">
		<jsp:useBean id="lifter1" type="org.concordiainternational.competition.data.Lifter"/>
		<tr>
			<c:choose>
				<c:when test="${lifter1.currentLifter}">
					<td class='name current'><nobr>${lifter1.lastName}, <%= lifter1.getFirstName().substring(0,1) %></nobr></td>
				</c:when>
				<c:otherwise>
					<td class='name'><nobr>${lifter1.lastName}, <%= lifter1.getFirstName().substring(0,1) %><nobr></td>
				</c:otherwise>
			</c:choose>
			<c:choose>
			<c:when test="${lifter1.snatchAttemptsDone == 0}">
				<c:choose>
					<c:when test="${lifter1.currentLifter}">
						<td class='currentWeight'>${lifter1.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter1.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter1.snatchAttemptsDone > 0 }">
				<%= WeightFormatter.htmlFormatWeight(lifter1.getSnatch1ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter1.snatchAttemptsDone == 1}">
				<c:choose>
					<c:when test="${lifter1.currentLifter}">
						<td class='currentWeight'>${lifter1.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter1.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter1.snatchAttemptsDone > 1}">
				<%= WeightFormatter.htmlFormatWeight(lifter1.getSnatch2ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter1.snatchAttemptsDone == 2}">
				<c:choose>
					<c:when test="${lifter1.currentLifter}">
						<td class='currentWeight'>${lifter1.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
							<td class='requestedWeight'>${lifter1.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter1.snatchAttemptsDone > 2}">
				<%= WeightFormatter.htmlFormatWeight(lifter1.getSnatch3ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter1.attemptsDone == 3}">
				<c:choose>
					<c:when test="${lifter1.currentLifter}">
						<td class='currentWeight'>${lifter1.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter1.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter1.cleanJerkAttemptsDone > 0}">
				<%= WeightFormatter.htmlFormatWeight(lifter1.getCleanJerk1ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='requestedWeight'><%= lifter1.getRequestedWeightForAttempt(4) %></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter1.cleanJerkAttemptsDone == 1}">
				<c:choose>
					<c:when test="${lifter1.currentLifter}">
						<td class='currentWeight'>${lifter1.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter1.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter1.cleanJerkAttemptsDone > 1}">
				<%= WeightFormatter.htmlFormatWeight(lifter1.getCleanJerk2ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter1.cleanJerkAttemptsDone == 2}">
				<c:choose>
					<c:when test="${lifter1.currentLifter}">
						<td class='currentWeight'>${lifter1.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter1.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter1.cleanJerkAttemptsDone > 2}">
				<%= WeightFormatter.htmlFormatWeight(lifter1.getCleanJerk3ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<td class='weight'><%= String.format(locale, "%.4f",lifter1.getSinclairFactor()) %></td>
		<c:choose>
			<c:when test="${lifter1.sinclair > 0}">
				<td class='cat'><%= String.format(locale, "%.2f",lifter1.getSinclair()) %></td>
			</c:when>
			<c:otherwise>
				<td class='cat'><%= String.format(locale, "%.2f", lifter1.getBestSnatch() * lifter1.getSinclairFactor()) %></td>
			</c:otherwise>
		</c:choose>
	</tr>
</c:forEach>
<tr>
	<td class="noborder" colspan="8"></td>
	<td class="totalSinclair"></td>
</tr>
</tbody>
</table>
</td>
<td class="outer">
<table id="upperRightTable">
	<thead>
		<tr>
			<th>Namn</th>
			<th colspan="3">Ryck</th>
			<th colspan="3">Stöt</th>
			<th>Koeff.</th>
			<th>Sinclair</th>
		</tr>
	</thead>
	<tbody>
		<% if (lifters.size() > 0) { %>
		<tr>
			<td class="noborder"></td>
		</tr>
		<tr>
			<td class="noborder"></td>
		</tr>
		<tr>
			<td class="team" colspan="9">${lifters[liftersPerTeam].club}</td>
		</tr>
		<% } %>
		<c:forEach var="lifter2" items="${lifters}" begin="${liftersPerTeam}" end="${liftersPerTeam * 2 - 1}">
		<jsp:useBean id="lifter2" type="org.concordiainternational.competition.data.Lifter"/>
		<tr>
			<c:choose>
				<c:when test="${lifter2.currentLifter}">
					<td class='name current'><nobr>${lifter2.lastName}, <%= lifter2.getFirstName().substring(0,1) %></nobr></td>
				</c:when>
				<c:otherwise>
					<td class='name'><nobr>${lifter2.lastName}, <%= lifter2.getFirstName().substring(0,1) %><nobr></td>
				</c:otherwise>
			</c:choose>
			<c:choose>
			<c:when test="${lifter2.snatchAttemptsDone == 0}">
				<c:choose>
					<c:when test="${lifter2.currentLifter}">
						<td class='currentWeight'>${lifter2.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter2.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter2.snatchAttemptsDone > 0 }">
				<%= WeightFormatter.htmlFormatWeight(lifter2.getSnatch1ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter2.snatchAttemptsDone == 1}">
				<c:choose>
					<c:when test="${lifter2.currentLifter}">
						<td class='currentWeight'>${lifter2.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter2.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter2.snatchAttemptsDone > 1}">
				<%= WeightFormatter.htmlFormatWeight(lifter2.getSnatch2ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter2.snatchAttemptsDone == 2}">
				<c:choose>
					<c:when test="${lifter2.currentLifter}">
						<td class='currentWeight'>${lifter2.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
							<td class='requestedWeight'>${lifter2.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter2.snatchAttemptsDone > 2}">
				<%= WeightFormatter.htmlFormatWeight(lifter2.getSnatch3ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter2.attemptsDone == 3}">
				<c:choose>
					<c:when test="${lifter2.currentLifter}">
						<td class='currentWeight'>${lifter2.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter2.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter2.cleanJerkAttemptsDone > 0}">
				<%= WeightFormatter.htmlFormatWeight(lifter2.getCleanJerk1ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='requestedWeight'><%= lifter2.getRequestedWeightForAttempt(4) %></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter2.cleanJerkAttemptsDone == 1}">
				<c:choose>
					<c:when test="${lifter2.currentLifter}">
						<td class='currentWeight'>${lifter2.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter2.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter2.cleanJerkAttemptsDone > 1}">
				<%= WeightFormatter.htmlFormatWeight(lifter2.getCleanJerk2ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter2.cleanJerkAttemptsDone == 2}">
				<c:choose>
					<c:when test="${lifter2.currentLifter}">
						<td class='currentWeight'>${lifter2.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter2.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter2.cleanJerkAttemptsDone > 2}">
				<%= WeightFormatter.htmlFormatWeight(lifter2.getCleanJerk3ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<td class='weight'><%= String.format(locale, "%.4f",lifter2.getSinclairFactor()) %></td>
		<c:choose>
			<c:when test="${lifter2.sinclair > 0}">
				<td class='cat'><%= String.format(locale, "%.2f",lifter2.getSinclair()) %></td>
			</c:when>
			<c:otherwise>
				<td class='cat'><%= String.format(locale, "%.2f", lifter2.getBestSnatch() * lifter2.getSinclairFactor()) %></td>
			</c:otherwise>
		</c:choose>
	</tr>
</c:forEach>
<tr>
	<td class="noborder" colspan="8"></td>
	<td class="totalSinclair"></td>
</tr>
</tbody>
</table>
</td>
</tr>
<tr>
</tr>
<tr>
<td class="outer">
<table id="lowerLeftTable">
	<tbody>
		<% if (lifters.size() > 0) { %>
		<tr>
			<td class="noborder"></td>
		</tr>
		<tr>
			<td class="noborder"></td>
		</tr>
		<tr>
			<td class="team" colspan="9">${lifters[liftersPerTeam * 2].club}</td>
		</tr>
		<% } %>
		<c:forEach var="lifter3" items="${lifters}" begin="${liftersPerTeam * 2}" end="${liftersPerTeam * 3 - 1}">
		<jsp:useBean id="lifter3" type="org.concordiainternational.competition.data.Lifter"/>
		<tr>
			<c:choose>
				<c:when test="${lifter3.currentLifter}">
					<td class='name current'><nobr>${lifter3.lastName}, <%= lifter3.getFirstName().substring(0,1) %></nobr></td>
				</c:when>
				<c:otherwise>
					<td class='name'><nobr>${lifter3.lastName}, <%= lifter3.getFirstName().substring(0,1) %><nobr></td>
				</c:otherwise>
			</c:choose>
			<c:choose>
			<c:when test="${lifter3.snatchAttemptsDone == 0}">
				<c:choose>
					<c:when test="${lifter3.currentLifter}">
						<td class='currentWeight'>${lifter3.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter3.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter3.snatchAttemptsDone > 0 }">
				<%= WeightFormatter.htmlFormatWeight(lifter3.getSnatch1ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter3.snatchAttemptsDone == 1}">
				<c:choose>
					<c:when test="${lifter3.currentLifter}">
						<td class='currentWeight'>${lifter3.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter3.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter3.snatchAttemptsDone > 1}">
				<%= WeightFormatter.htmlFormatWeight(lifter3.getSnatch2ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter3.snatchAttemptsDone == 2}">
				<c:choose>
					<c:when test="${lifter3.currentLifter}">
						<td class='currentWeight'>${lifter3.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
							<td class='requestedWeight'>${lifter3.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter3.snatchAttemptsDone > 2}">
				<%= WeightFormatter.htmlFormatWeight(lifter3.getSnatch3ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter3.attemptsDone == 3}">
				<c:choose>
					<c:when test="${lifter3.currentLifter}">
						<td class='currentWeight'>${lifter3.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter3.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter3.cleanJerkAttemptsDone > 0}">
				<%= WeightFormatter.htmlFormatWeight(lifter3.getCleanJerk1ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='requestedWeight'><%= lifter3.getRequestedWeightForAttempt(4) %></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter3.cleanJerkAttemptsDone == 1}">
				<c:choose>
					<c:when test="${lifter3.currentLifter}">
						<td class='currentWeight'>${lifter3.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter3.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter3.cleanJerkAttemptsDone > 1}">
				<%= WeightFormatter.htmlFormatWeight(lifter3.getCleanJerk2ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter3.cleanJerkAttemptsDone == 2}">
				<c:choose>
					<c:when test="${lifter3.currentLifter}">
						<td class='currentWeight'>${lifter3.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter3.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter3.cleanJerkAttemptsDone > 2}">
				<%= WeightFormatter.htmlFormatWeight(lifter3.getCleanJerk3ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<td class='weight'><%= String.format(locale, "%.4f",lifter3.getSinclairFactor()) %></td>
		<c:choose>
			<c:when test="${lifter3.sinclair > 0}">
				<td class='cat'><%= String.format(locale, "%.2f",lifter3.getSinclair()) %></td>
			</c:when>
			<c:otherwise>
				<td class='cat'><%= String.format(locale, "%.2f", lifter3.getBestSnatch() * lifter3.getSinclairFactor()) %></td>
			</c:otherwise>
		</c:choose>
	</tr>
</c:forEach>
<tr>
	<td class="noborder" colspan="8"></td>
	<td class="totalSinclair"></td>
</tr>
</tbody>
</table>
</td>
<td class="outer">
<table id="lowerRightTable">
	<tbody>
		<% if (lifters.size() > 0) { %>
		<tr>
			<td class="noborder"></td>
		</tr>
		<tr>
			<td class="noborder"></td>
		</tr>
		<tr>
			<td class="team" colspan="9">${lifters[liftersPerTeam * 3].club}</td>
		</tr>
		<% } %>
		<c:forEach var="lifter4" items="${lifters}" begin="${liftersPerTeam * 3}" end="${liftersPerTeam * 4 - 1}">
		<jsp:useBean id="lifter4" type="org.concordiainternational.competition.data.Lifter"/>
		<tr>
			<c:choose>
				<c:when test="${lifter4.currentLifter}">
					<td class='name current'><nobr>${lifter4.lastName}, <%= lifter4.getFirstName().substring(0,1) %></nobr></td>
				</c:when>
				<c:otherwise>
					<td class='name'><nobr>${lifter4.lastName}, <%= lifter4.getFirstName().substring(0,1) %><nobr></td>
				</c:otherwise>
			</c:choose>
			<c:choose>
			<c:when test="${lifter4.snatchAttemptsDone == 0}">
				<c:choose>
					<c:when test="${lifter4.currentLifter}">
						<td class='currentWeight'>${lifter4.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter4.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter4.snatchAttemptsDone > 0 }">
				<%= WeightFormatter.htmlFormatWeight(lifter4.getSnatch1ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter4.snatchAttemptsDone == 1}">
				<c:choose>
					<c:when test="${lifter4.currentLifter}">
						<td class='currentWeight'>${lifter4.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter4.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter4.snatchAttemptsDone > 1}">
				<%= WeightFormatter.htmlFormatWeight(lifter4.getSnatch2ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter4.snatchAttemptsDone == 2}">
				<c:choose>
					<c:when test="${lifter4.currentLifter}">
						<td class='currentWeight'>${lifter4.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
							<td class='requestedWeight'>${lifter4.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter4.snatchAttemptsDone > 2}">
				<%= WeightFormatter.htmlFormatWeight(lifter4.getSnatch3ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter4.attemptsDone == 3}">
				<c:choose>
					<c:when test="${lifter4.currentLifter}">
						<td class='currentWeight'>${lifter4.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter4.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter4.cleanJerkAttemptsDone > 0}">
				<%= WeightFormatter.htmlFormatWeight(lifter4.getCleanJerk1ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='requestedWeight'><%= lifter4.getRequestedWeightForAttempt(4) %></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter4.cleanJerkAttemptsDone == 1}">
				<c:choose>
					<c:when test="${lifter4.currentLifter}">
						<td class='currentWeight'>${lifter4.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter4.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter4.cleanJerkAttemptsDone > 1}">
				<%= WeightFormatter.htmlFormatWeight(lifter4.getCleanJerk2ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${lifter4.cleanJerkAttemptsDone == 2}">
				<c:choose>
					<c:when test="${lifter4.currentLifter}">
						<td class='currentWeight'>${lifter4.nextAttemptRequestedWeight}</td>
					</c:when>
					<c:otherwise>
						<td class='requestedWeight'>${lifter4.nextAttemptRequestedWeight}</td>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:when test="${lifter4.cleanJerkAttemptsDone > 2}">
				<%= WeightFormatter.htmlFormatWeight(lifter4.getCleanJerk3ActualLift()) %>
			</c:when>
			<c:otherwise>
				<td class='weight'></td>
			</c:otherwise>
		</c:choose>
		<td class='weight'><%= String.format(locale, "%.4f",lifter4.getSinclairFactor()) %></td>
		<c:choose>
			<c:when test="${lifter4.sinclair > 0}">
				<td class='cat'><%= String.format(locale, "%.2f",lifter4.getSinclair()) %></td>
			</c:when>
			<c:otherwise>
				<td class='cat'><%= String.format(locale, "%.2f", lifter4.getBestSnatch() * lifter4.getSinclairFactor()) %></td>
			</c:otherwise>
		</c:choose>
	</tr>
</c:forEach>
<tr>
	<td class="noborder" colspan="8"></td>
	<td class="totalSinclair"></td>
</tr>
</tbody>
</table>
</td>
</tr>
</table>
</div>
<script>
let tables = document.getElementsByClassName('outer');
for (let i = 0; i < tables.length; i++) {
	let Sinclairs = tables[i].getElementsByClassName('cat');
	let totalSinclair = 0.00;
	for (let j = 0; j < Sinclairs.length; j++) {
		let Sinclair = Sinclairs[j].innerHTML;
		if (Sinclair != '') totalSinclair += parseFloat(Sinclair);
	}
	let td = tables[i].getElementsByClassName('totalSinclair');
	td[0].appendChild(document.createTextNode(totalSinclair.toFixed(2)));
}
</script>
</body>
</html>