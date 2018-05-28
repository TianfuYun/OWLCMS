/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.concordiainternational.competition.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.concordiainternational.competition.data.CompetitionSession;
import org.concordiainternational.competition.ui.SessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class LoggerUtils {
    static Logger thisLogger = LoggerFactory.getLogger(LoggerUtils.class);

    public enum LoggingKeys {
        view,
        currentGroup
    }

    public static void traceException(Logger logger, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        logger.trace(sw.toString());
    }

    public static void debugException(Logger logger, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        logger.debug(sw.toString());
    }

    public static void warnException(Logger logger, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        logger.warn(sw.toString());
    }

    public static void infoException(Logger logger, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        logger.info(sw.toString());
    }

    public static void errorException(Logger logger, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        logger.error(sw.toString());
    }

    public static void mdcPut(LoggingKeys key, String value) {
//        if (key != null && (value == null || value.trim().isEmpty())) {
//          LoggerUtils.traceBack(thisLogger);
//        }
        MDC.put(key.name(), value);
    }

    public static String mdcGet(String key) {
        return MDC.get(key);
    }

    public static void mdcSetup(String loggingId, final SessionData groupData) {
        if (groupData == null) {
//            LoggerUtils.mdcPut(LoggingKeys.view, app.getCurrentView().getLoggingId());
            LoggerUtils.mdcPut(LoggingKeys.view, loggingId);
            LoggerUtils.mdcPut(LoggingKeys.currentGroup, "n/a");
        } else {
            CompetitionSession currentSession = groupData.getCurrentSession();
            LoggerUtils.mdcPut(LoggingKeys.currentGroup, currentSession != null ? currentSession.getName() : "");
//            CompetitionApplication app = CompetitionApplication.getCurrent();
//            LoggerUtils.mdcPut(LoggingKeys.view, app.getCurrentView().getLoggingId());
            LoggerUtils.mdcPut(LoggingKeys.view, loggingId);
        }
    }

    public static String getCallerClassName() {
        @SuppressWarnings({ "deprecation" })
        Class<?> callerClass = sun.reflect.Reflection.getCallerClass(5);
        return callerClass.getSimpleName();
    }

//    public static void buttonSetup() {
//        LoggerUtils.mdcPut(LoggingKeys.currentGroup, "*");
//        LoggerUtils.mdcPut(LoggingKeys.view, CompetitionApplication.getCurrent().getCurrentView().getLoggingId());
//    }

    public static void traceBack(Logger logger) {
        warnException(logger, new Exception("traceBack"));
    }

    public static void traceBack(Logger logger, String whereFrom) {
        warnException(logger, new Exception(whereFrom));
    }

}
