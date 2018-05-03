<%@ page language="java" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8" isELIgnored="false" import="org.concordiainternational.competition.ui.generators.*,org.concordiainternational.competition.ui.*,org.concordiainternational.competition.data.*,org.concordiainternational.competition.data.lifterSort.*,org.concordiainternational.competition.spreadsheet.*,java.util.*"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%
ServletContext sCtx = this.getServletContext();
SessionData groupData = (SessionData)sCtx.getAttribute(SessionData.MASTER_KEY+"Platform");
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

if (currentlifters.get(0).getGender().contains("F")) {
	lifters = sortedWomen;
} else {
	lifters = sortedMen;
}

pageContext.setAttribute("currentFirstName", currentlifters.get(0).getFirstName());
pageContext.setAttribute("currentLastName", currentlifters.get(0).getLastName());
pageContext.setAttribute("lifters", lifters);

%>
<c:forEach var="lifter" items="${lifters}">
<jsp:useBean id="lifter" type="org.concordiainternational.competition.data.Lifter"/>${lifter.club},${lifter.lastName},<%= lifter.getFirstName().substring(0,1) %>,<c:choose>
	<c:when test="${lifter.snatchAttemptsDone == 0}">
		<c:choose>
			<c:when test="${lifter.firstName == currentFirstName && lifter.lastName == currentLastName}">c${lifter.nextAttemptRequestedWeight},</c:when>
			<c:otherwise>r${lifter.nextAttemptRequestedWeight},</c:otherwise>
		</c:choose>
	</c:when>
	<c:when test="${lifter.snatchAttemptsDone > 0 }"><%= lifter.getSnatch1ActualLift() %>,</c:when>
	<c:otherwise>,</c:otherwise>
</c:choose>
<c:choose>
	<c:when test="${lifter.snatchAttemptsDone == 1}">
		<c:choose>
			<c:when test="${lifter.firstName == currentFirstName && lifter.lastName == currentLastName}">c${lifter.nextAttemptRequestedWeight},</c:when>
			<c:otherwise>r${lifter.nextAttemptRequestedWeight},</c:otherwise>
		</c:choose>
	</c:when>
	<c:when test="${lifter.snatchAttemptsDone > 1}"><%= lifter.getSnatch2ActualLift() %>,</c:when>
	<c:otherwise>,</c:otherwise>
</c:choose>
<c:choose>
	<c:when test="${lifter.snatchAttemptsDone == 2}">
		<c:choose>
			<c:when test="${lifter.firstName == currentFirstName && lifter.lastName == currentLastName}">c${lifter.nextAttemptRequestedWeight},</c:when>
			<c:otherwise>r${lifter.nextAttemptRequestedWeight},</c:otherwise>
		</c:choose>
	</c:when>
	<c:when test="${lifter.snatchAttemptsDone > 2}"><%= lifter.getSnatch3ActualLift() %>,</c:when>
	<c:otherwise>,</c:otherwise>
</c:choose>
<c:choose>
	<c:when test="${lifter1.cleanJerkAttemptsDone == 0}">
		<c:choose>
			<c:when test="${lifter.firstName == currentFirstName && lifter.lastName == currentLastName}">c${lifter.nextAttemptRequestedWeight},</c:when>
			<c:otherwise>r${lifter.nextAttemptRequestedWeight},</c:otherwise>
		</c:choose>
	</c:when>
	<c:when test="${lifter.cleanJerkAttemptsDone > 0}"><%= lifter.getCleanJerk1ActualLift() %>,</c:when>
	<c:otherwise>r<%= lifter.getRequestedWeightForAttempt(4) %>,</c:otherwise>
</c:choose>
<c:choose>
	<c:when test="${lifter1.cleanJerkAttemptsDone == 1}">
		<c:choose>
			<c:when test="${lifter.firstName == currentFirstName && lifter.lastName == currentLastName}">c${lifter.nextAttemptRequestedWeight},</c:when>
			<c:otherwise>r${lifter.nextAttemptRequestedWeight},</c:otherwise>
		</c:choose>
	</c:when>
	<c:when test="${lifter.cleanJerkAttemptsDone > 1}"><%= lifter.getCleanJerk2ActualLift() %>,</c:when>
	<c:otherwise>,</c:otherwise>
</c:choose>
<c:choose>
	<c:when test="${lifter1.cleanJerkAttemptsDone == 2}">
		<c:choose>
			<c:when test="${lifter.firstName == currentFirstName && lifter.lastName == currentLastName}">c${lifter.nextAttemptRequestedWeight},</c:when>
			<c:otherwise>r${lifter.nextAttemptRequestedWeight},</c:otherwise>
		</c:choose>
	</c:when>
	<c:when test="${lifter.cleanJerkAttemptsDone > 2}"><%= lifter.getCleanJerk3ActualLift() %>,</c:when>
	<c:otherwise>,</c:otherwise>
</c:choose><%= String.format(locale, "%.4f",lifter.getSinclairFactor()) %>,<c:choose>
	<c:when test="${lifter.sinclair > 0}"><%= String.format(locale, "%.2f",lifter.getSinclair()) %>;</c:when>
	<c:otherwise><%= String.format(locale, "%.2f", lifter.getBestSnatch() * lifter.getSinclairFactor()) %>;</c:otherwise>
</c:choose>
</c:forEach>