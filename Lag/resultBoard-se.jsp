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
pageContext.setAttribute("currentFirstName", currentlifters.get(0).getFirstName());
pageContext.setAttribute("currentLastName", currentlifters.get(0).getLastName());
pageContext.setAttribute("comingFirstName", currentlifters.get(1).getFirstName());
pageContext.setAttribute("comingLastName", currentlifters.get(1).getLastName());
pageContext.setAttribute("comingFirstName2", currentlifters.get(2).getFirstName());
pageContext.setAttribute("comingLastName2", currentlifters.get(2).getLastName());
pageContext.setAttribute("comingFirstName3", currentlifters.get(3).getFirstName());
pageContext.setAttribute("comingLastName3", currentlifters.get(3).getLastName());
pageContext.setAttribute("lifters", lifters);
pageContext.setAttribute("liftersPerTeam", liftersPerTeam);

%>

<title>Lag-resultat</title>

<link href="${style}" rel="stylesheet" type="text/css">

<style type="text/css">
	body {
		zoom: 1.5;
	}
	.requestedWeight, .currentWeight, .comingWeight, .comingWeight2, .comingWeight3{
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
	.current, .currentWeight {
		background-color: #00ffd9;
	}
	.coming, .comingWeight {
		background-color: #66ffe8;
	}
	.coming2, .comingWeight2 {
		background-color: #b2fff3;
	}
	.coming3, .comingWeight3 {
		background-color: #e5fffb;
	}
</style>
</head>
<body>
<br>
<table>
<c:forEach var="lifter" items="${lifters}" varStatus="loop">
<jsp:useBean id="lifter" type="org.concordiainternational.competition.data.Lifter"/>
<!-- 2 teams per row -->
<c:if test="${loop.index % (liftersPerTeam * 2) == 0}"><tr></c:if>
<!-- create outer table cell in which to put the team table in -->
<c:if test="${loop.index % liftersPerTeam == 0}">
<td class="outer">
<table>
	<!-- create 2 table headers above the 2 upper most team tables -->
	<c:if test="${loop.index == 0 || loop.index == liftersPerTeam}">
		<thead>
			<tr>
				<th>Namn</th>
				<th colspan="3">Ryck</th>
				<th colspan="3">Stöt</th>
				<th>Koeff.</th>
				<th>Sinclair</th>
			</tr>
		</thead>
	</c:if>
	<tbody>
		<tr>
			<td class="noborder" colspan="9"></td>
		</tr>
		<tr>
			<td class="noborder" colspan="9"></td>
		</tr>
		<tr>
			<td class="team" colspan="9">${lifter.club}</td>
		</tr>
	</c:if>
		<tr>
			<c:choose>
				<c:when test="${lifter.firstName == currentFirstName && lifter.lastName == currentLastName}">
					<td class='name current'><nobr>${lifter.lastName}, <%= lifter.getFirstName().substring(0,1) %></nobr></td>
					<c:choose>
						<c:when test="${lifter.snatchAttemptsDone == 0}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.snatchAttemptsDone > 0 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getSnatch1ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.snatchAttemptsDone == 1}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.snatchAttemptsDone > 1 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getSnatch2ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.snatchAttemptsDone == 2}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.snatchAttemptsDone > 2 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getSnatch3ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.attemptsDone == 3}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.attemptsDone > 3 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getCleanJerk3ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'><%= lifter.getRequestedWeightForAttempt(4) %></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.cleanJerkAttemptsDone == 1}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.cleanJerkAttemptsDone > 1 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getCleanJerk2ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.cleanJerkAttemptsDone == 2}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.cleanJerkAttemptsDone > 2 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getCleanJerk3ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
				</c:when>
				<c:when test="${lifter.firstName == comingFirstName && lifter.lastName == comingLastName}">
					<td class='name coming'><nobr>${lifter.lastName}, <%= lifter.getFirstName().substring(0,1) %></nobr></td>
					<c:choose>
						<c:when test="${lifter.snatchAttemptsDone == 0}">
							<td class='comingWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.snatchAttemptsDone > 0 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getSnatch1ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.snatchAttemptsDone == 1}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.snatchAttemptsDone > 1 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getSnatch2ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.snatchAttemptsDone == 2}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.snatchAttemptsDone > 2 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getSnatch3ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.attemptsDone == 3}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.attemptsDone > 3 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getCleanJerk3ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'><%= lifter.getRequestedWeightForAttempt(4) %></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.cleanJerkAttemptsDone == 1}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.cleanJerkAttemptsDone > 1 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getCleanJerk2ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.cleanJerkAttemptsDone == 2}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.cleanJerkAttemptsDone > 2 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getCleanJerk3ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
				</c:when>
				<c:when test="${lifter.firstName == comingFirstName2 && lifter.lastName == comingLastName2}">
					<td class='name coming2'><nobr>${lifter.lastName}, <%= lifter.getFirstName().substring(0,1) %></nobr></td>
					<c:choose>
						<c:when test="${lifter.snatchAttemptsDone == 0}">
							<td class='comingWeight2'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.snatchAttemptsDone > 0 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getSnatch1ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.snatchAttemptsDone == 1}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.snatchAttemptsDone > 1 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getSnatch2ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.snatchAttemptsDone == 2}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.snatchAttemptsDone > 2 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getSnatch3ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.attemptsDone == 3}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.attemptsDone > 3 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getCleanJerk3ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'><%= lifter.getRequestedWeightForAttempt(4) %></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.cleanJerkAttemptsDone == 1}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.cleanJerkAttemptsDone > 1 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getCleanJerk2ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.cleanJerkAttemptsDone == 2}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.cleanJerkAttemptsDone > 2 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getCleanJerk3ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
				</c:when>
				<c:when test="${lifter.firstName == comingFirstName3 && lifter.lastName == comingLastName3}">
					<td class='name coming3'><nobr>${lifter.lastName}, <%= lifter.getFirstName().substring(0,1) %></nobr></td>
					<c:choose>
						<c:when test="${lifter.snatchAttemptsDone == 0}">
							<td class='comingWeight3'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.snatchAttemptsDone > 0 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getSnatch1ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.snatchAttemptsDone == 1}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.snatchAttemptsDone > 1 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getSnatch2ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.snatchAttemptsDone == 2}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.snatchAttemptsDone > 2 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getSnatch3ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.attemptsDone == 3}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.attemptsDone > 3 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getCleanJerk3ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'><%= lifter.getRequestedWeightForAttempt(4) %></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.cleanJerkAttemptsDone == 1}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.cleanJerkAttemptsDone > 1 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getCleanJerk2ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.cleanJerkAttemptsDone == 2}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.cleanJerkAttemptsDone > 2 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getCleanJerk3ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
				</c:when>
				<c:otherwise>
					<td class='name'><nobr>${lifter.lastName}, <%= lifter.getFirstName().substring(0,1) %><nobr></td>
					<c:choose>
						<c:when test="${lifter.snatchAttemptsDone == 0}">
							<td class='requestedWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.snatchAttemptsDone > 0 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getSnatch1ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.snatchAttemptsDone == 1}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.snatchAttemptsDone > 1 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getSnatch2ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.snatchAttemptsDone == 2}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.snatchAttemptsDone > 2 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getSnatch3ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.attemptsDone == 3}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.attemptsDone > 3 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getCleanJerk3ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'><%= lifter.getRequestedWeightForAttempt(4) %></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.cleanJerkAttemptsDone == 1}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.cleanJerkAttemptsDone > 1 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getCleanJerk2ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${lifter.cleanJerkAttemptsDone == 2}">
							<td class='currentWeight'>${lifter.nextAttemptRequestedWeight}</td>
						</c:when>
						<c:when test="${lifter.cleanJerkAttemptsDone > 2 }">
							<%= WeightFormatter.htmlFormatWeight(lifter.getCleanJerk3ActualLift()) %>
						</c:when>
						<c:otherwise>
							<td class='weight'></td>
						</c:otherwise>
					</c:choose>
				</c:otherwise>
			</c:choose>
			<td class='weight'><%= String.format(locale, "%.4f",lifter.getSinclairFactor()) %></td>
			<c:choose>
				<c:when test="${lifter.sinclair > 0}">
					<td class='cat'><%= String.format(locale, "%.2f",lifter.getSinclair()) %></td>
				</c:when>
				<c:otherwise>
					<td class='cat'><%= String.format(locale, "%.2f", lifter.getBestSnatch() * lifter.getSinclairFactor()) %></td>
				</c:otherwise>
			</c:choose>
		</tr>
	<!-- put the total Sinclair under each team table, the score is inserted with javascript -->
	<c:if test="${(loop.index + 1) % liftersPerTeam == 0}">
		<tr>
			<td class="noborder" colspan="8"></td>
			<td class="totalSinclair"></td>
		</tr>
	</tbody>
</table>
</td>
</c:if>
<c:if test="${(loop.index + 1) % (liftersPerTeam * 2) == 0}"></tr></c:if>	
</c:forEach>
</table>
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