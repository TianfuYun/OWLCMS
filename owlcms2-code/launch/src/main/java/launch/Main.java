/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package launch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.catalina.startup.Tomcat;
import org.boris.winrun4j.Log;

/**
 * Launch a web application as a user process.
 * @author jflamy
 *
 */
public class Main {
    static Logger logger = Logger.getLogger("owlcms.Launch");

    /**
     * The directory that contains the web application files and the context
     * are named the same (as if they came from a .war)
     */
    private static final String APPLICATION_NAME = "owlcms";

    public static void main(String[] args) throws Exception {

	// The installation directory can be set through an environment variable OWLCMS_DIR
	// failing that, a system property (owlcms.dir)
	// failing that, the current directory
	//
	// the default installation sets the system property owlcms.dir on the command line
	String programPath = System.getenv("OWLCMS_DIR");
	String programPathOverride = System.getProperty("owlcms.dir");
	if (programPathOverride != null && !programPathOverride.isEmpty()) {
	    programPath = programPathOverride;
	} else {
	    // should not happen
	    programPath = System.getProperty("user.dir");
	}
	// install program gives us extra quotes for nothing
	programPath = programPath.replaceAll("^\"|\"$", "");

	// The web application is one level below.
	File programDir = new File(programPath);
	File webappFile = new File(programDir,APPLICATION_NAME);
	if (!webappFile.exists()) {
	    System.err.println("webapp location not found: "+webappFile);
	    System.exit(-1);
	} else {
	    System.err.println("webapp location found: "+webappFile);
	}

	// The data and configuration directory can be set through an environment variable.
	// failing that, a system property (owlcms.home)
	// failing that, the current directory
	//
	// the default installation sets the system property owlcms.home on the command line.
	String currentDirectory = System.getProperty("user.dir");
	String dataPath = currentDirectory;
	String dataPathOverride = System.getenv("OWLCMS_HOME");
	if (dataPathOverride != null && !dataPathOverride.isEmpty()) {
	    dataPath = dataPathOverride;
	} else {
	    dataPathOverride = System.getProperty("owlcms.home");
	    if (dataPathOverride != null && !dataPathOverride.isEmpty()) {
		// install program gives us extra quotes for nothing
		dataPathOverride = dataPathOverride.replaceAll("^\"|\"$", "");
		dataPath = dataPathOverride;
	    }
	}

	// load application configuration file
	File dataDir = new File(dataPath);
	Properties config = new Properties();
	try {
	    config.load(new FileInputStream(new File(dataDir,"config.properties")));
	} catch (FileNotFoundException e) {
	    // ignore
	}


	// The port that we should run on can be set from an environment variable,
	// failing that, a system property (owlcms.port)
	// or config file entry, in that priority.
	String webPort = "80";
	String portOverride = System.getenv("OWLCMS_PORT");
	if (portOverride != null && !portOverride.isEmpty()) {
	    webPort = portOverride;
	} else {
	    portOverride = (String) System.getProperty("owlcms.port");
	    if (portOverride != null && !portOverride.isEmpty()) {
		webPort = portOverride;
	    } else {
		portOverride = (String) config.get("owlcms.port");
		if (portOverride != null && !portOverride.isEmpty()) {
		    webPort = portOverride;
		}
	    }
	}

	// configure the application and its URL
	Tomcat tomcat = new Tomcat();
	tomcat.setPort(Integer.valueOf(webPort));
	File workDir = new File(dataDir,"/tomcatWork."+webPort);
	workDir.mkdirs();
	tomcat.setBaseDir(workDir.getAbsolutePath());
	tomcat.addWebapp("/"+APPLICATION_NAME, webappFile.getAbsolutePath());
	System.setProperty("net.sf.ehcache.skipUpdateCheck","true");

	Log.info("\n\n\n");
	Log.info("Starting server on port "+webPort);

	// start the server and wait
	tomcat.start();
	Log.info("Server started. Detailed logs found in directory: "+currentDirectory);
	tomcat.getServer().await();

    }
}
