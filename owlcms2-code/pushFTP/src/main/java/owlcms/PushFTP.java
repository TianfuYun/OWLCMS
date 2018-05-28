package owlcms;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;

public class PushFTP {

    static Properties props = new Properties();
    static Boolean debug = false;
    static String ftpConnect = null;
    static String ftpUser = null;
    static String ftpPass = null;
    static String url = null;
    static String styleURL = null;
    static Long urlRefreshMS = (long) 2500;
    static String remoteName = null;
    private static long ftpServerTimeOut;
    static boolean connected = false;
    static InputStream websiteStream = null;
    private static String ftpWorkingDirectory = "";

    public static void main(String[] args) {
	InputStream scoreBoardStream = null;
	FTPClient ftp = new FTPClient();


	try {
	    // read config and set things up
	    readConfig(args, ftp);
	    setup(ftp);
	    if (connected) copyStyleSheet(ftp);

	    // copy scoreboard if it has changed and wait a bit;
	    // loop until program is killed.
	    byte[] prevSB = null;
	    long lastUpdate = 0L;

	    while (connected) {
		try {
		    long nextUpdateMS = System.currentTimeMillis() + urlRefreshMS;

		    // get current scoreboard
		    scoreBoardStream = new URL(url).openStream();
		    byte[] curSB = IOUtils.toByteArray(scoreBoardStream);

		    // if different from previous or stale, copy the scoreboard to the FTP server
		    // force update right now if waiting one more time would be too late
		    long nextElapsed = System.currentTimeMillis() - lastUpdate + urlRefreshMS;
		    boolean stale = nextElapsed >= (ftpServerTimeOut-1000);
		    boolean changed = !Arrays.equals(prevSB,curSB);
		    if (changed || stale) {
			if (debug) System.err.println("updating of public web site. changed="+changed+" preventTimeOut=" + nextElapsed);
		        copyScoreBoard(ftp, curSB);
		        prevSB = curSB;
		        lastUpdate = System.currentTimeMillis();
		    } else {
		        if (debug) System.err.println("No change, no update of public web site.");
		    }

		    // wait before polling competition server again
		    long delta = nextUpdateMS - System.currentTimeMillis();
		    if (delta > 0) {
		        if (debug) System.err.println("waiting "+delta+"ms "+" nextElapsed="+nextElapsed);
		        Thread.sleep(delta);
		    }
		} catch (IOException e) {
		    System.err.println(e.getLocalizedMessage());
		    try { ftp.disconnect(); } catch (IOException e2) {}
		    connected = false;
		    int reconnectAttemptCount = 0;
		    while (!connected && reconnectAttemptCount < 10) {
			reconnectAttemptCount++;
			ftpConnect(ftp);
			if (!connected) {
			    System.err.println("reconnect attempt "+reconnectAttemptCount +" succeeded");
			} else {
			    System.err.println("reconnect attempt "+reconnectAttemptCount+" failed");
			    try {
				Thread.sleep(5000);  // wait 5d and retry
			    } catch (Exception e3) {
			    }
			}
		    }
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	} finally {
	    try { ftp.disconnect(); } catch (IOException e) {}
	}
    }

    private static void setup(FTPClient ftp) {
	ftpConnect(ftp);

	// connect to server
	try {
	    websiteStream = new URL(url).openStream();
	    connected = true;
	} catch (Exception e4) {
	    System.err.println("could not connect to competition web site scoreboard "+url);
	    e4.printStackTrace();
	    connected = false;
	}
	return;
    }

    private static void ftpConnect(FTPClient ftp) {
	// connect to FTP server
	try {
	    ftp.connect(ftpConnect);
	    ftp.enterLocalPassiveMode();
	    ftp.user(ftpUser);
	    ftp.pass(ftpPass);
	    if (ftpWorkingDirectory.length() > 0) {
		ftp.changeWorkingDirectory(ftpWorkingDirectory );
	    }
	} catch (Exception e4) {
	    System.err.println("could not connect to FTP site "+ftpConnect);
	    e4.printStackTrace();
	    connected = false;
	}
    }

    private static void copyScoreBoard(FTPClient ftp, byte[] curSB)
	    throws IOException {
	boolean ok;
	//if (debug) {System.err.println("copying "+ new String(curSB,"UTF-8"));}
	ok = ftp.storeFile("tmp_"+remoteName, new ByteArrayInputStream(curSB));
	if (!ok) {
	    System.err.println();
	    System.err.println("Error: copying to web site failed: "+ftp.getReplyString());
	    System.exit(1);
	}

	// rename to official name
	ok = ftp.rename("tmp_"+remoteName, remoteName);
	if (!ok) {
	    System.err.println();
	    System.err.println("Error: rename failed "+ftp.getReplyString());
	    System.exit(1);
	}
    }

    private static void copyStyleSheet(FTPClient ftp)
	    throws IOException {
	InputStream styleSheetStream = null;
	try {
	    // copy the style sheet to the server
	    styleSheetStream = new URL(styleURL).openStream();
	} catch (Exception e) {
	    System.err.println();
	    System.err.println("Error: could not connect to web server "+styleURL);
	    System.exit(1);
	}
	String basename = styleURL.replaceFirst("^.*/", "");
	if (styleSheetStream != null)
	    ftp.storeFile(basename, styleSheetStream);
    }

    private static void readConfig(String[] args, FTPClient ftp)
	    throws IOException, FileNotFoundException {
	if (args.length == 0) {
	    File config = new File("./config.properties");
	    if (config.exists()) {
		props.load(new FileReader(config));
	    } else {
		props.load(PushFTP.class.getResourceAsStream("/config.properties"));
	    }
	} else {
	    props.load(new FileReader(args[0]));
	}
	debug = Boolean.valueOf(props.getProperty("debug"));
	ftpConnect = props.getProperty("ftpConnect");
	ftpUser = props.getProperty("ftpUser");
	ftpPass = props.getProperty("ftpPass");
	ftpServerTimeOut = Integer.valueOf(props.getProperty("ftpServerTimeOut", "60000"));
	ftpWorkingDirectory = props.getProperty("ftpWorkingDirectory", "");
	url = props.getProperty("scoreboardURL");
	styleURL = props.getProperty("scoreboardStyleURL");
	urlRefreshMS = Long.valueOf(props.getProperty("scoreboardURLRefreshMS"));
	remoteName = props.getProperty("ftpRemoteFileName");


	// for debugging
	if (debug) {
	    PrintWriter printWriter = new PrintWriter(System.err);
	    ftp.addProtocolCommandListener(new PrintCommandListener(printWriter, true));
	}
    }


}
