package edu.mit.broad.modules.heatmap.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *  BrowserLauncher is a class that provides one static method, openURL, which
 *  opens the default web browser for the current user of the system to the
 *  given URL. It may support other protocols depending on the system -- mailto,
 *  ftp, etc. -- but that has not been rigorously tested and is not guaranteed
 *  to work. <p>
 *
 *  Yes, this is platform-specific code, and yes, it may rely on classes on
 *  certain platforms that are not part of the standard JDK. What we're trying
 *  to do, though, is to take something that's frequently desirable but
 *  inherently platform-specific -- opening a default browser -- and allow
 *  programmers (you, for example) to do so without worrying about dropping into
 *  native code or doing anything else similarly evil. <p>
 *
 *  Anyway, this code is completely in Java and will run on all JDK
 *  1.1-compliant systems without modification or a need for additional
 *  libraries. All classes that are required on certain platforms to allow this
 *  to run are dynamically loaded at runtime via reflection and, if not found,
 *  will not cause this to do anything other than returning an error when
 *  opening the browser. <p>
 *
 *  There are certain system requirements for this class, as it's running
 *  through Runtime.exec(), which is Java's way of making a native system call.
 *  Currently, this requires that a Macintosh have a Finder which supports the
 *  GURL event, which is true for Mac OS 8.0 and 8.1 systems that have the
 *  Internet Scripting AppleScript dictionary installed in the Scripting
 *  Additions folder in the Extensions folder (which is installed by default as
 *  far as I know under Mac OS 8.0 and 8.1), and for all Mac OS 8.5 and later
 *  systems. On Windows, it only runs under Win32 systems (Windows 95, 98, and
 *  NT 4.0, as well as later versions of all). On other systems, this drops back
 *  from the inherently platform-sensitive concept of a default browser and
 *  simply attempts to launch Netscape via a shell command. <p>
 *
 *  This code is Copyright 1999-2001 by Eric Albert (ejalbert@cs.stanford.edu)
 *  and may be redistributed or modified in any form without restrictions as
 *  long as the portion of this comment from this paragraph through the end of
 *  the comment is not removed. The author requests that he be notified of any
 *  application, applet, or other binary that makes use of this code, but that's
 *  more out of curiosity than anything and is not required. This software
 *  includes no warranty. The author is not repsonsible for any loss of data or
 *  functionality or any adverse or unexpected effects of using this software.
 *  <p>
 *
 *  Credits: <br>
 *  Steven Spencer, JavaWorld magazine (<a
 *  href="http://www.javaworld.com/javaworld/javatips/jw-javatip66.html">Java
 *  Tip 66</a> ) <br>
 *  Thanks also to Ron B. Yeh, Eric Shapiro, Ben Engber, Paul Teitlebaum, Andrea
 *  Cantatore, Larry Barowski, Trevor Bedzek, Frank Miedrich, and Ron Rabakukk
 *
 * @author     Eric Albert (<a href="mailto:ejalbert@cs.stanford.edu">
 *      ejalbert@cs.stanford.edu</a> )
 * @created    April 19, 2004
 * @version    1.4b1 (Released June 20, 2001)
 */
public class BrowserLauncher {

	/**
	 *  The Java virtual machine that we are running on. Actually, in most cases we
	 *  only care about the operating system, but some operating systems require us
	 *  to switch on the VM.
	 */
	private static int jvm;

	/**  The browser for the system */
	private static Object browser;

	/**
	 *  Caches whether any classes, methods, and fields that are not part of the
	 *  JDK and need to be dynamically loaded at runtime loaded successfully. <p>
	 *
	 *  Note that if this is <code>false</code>, <code>openURL()</code> will always
	 *  return an IOException.
	 */
	private static boolean loadedWithoutErrors;

	/**  JVM constant for Mac OSX */
	private final static int MAC = 4;

	/**  JVM constant for any Windows NT JVM */
	private final static int WINDOWS_NT = 5;

	/**  JVM constant for any Windows 9x JVM */
	private final static int WINDOWS_9x = 6;

	/**  JVM constant for any other platform */
	private final static int OTHER = -1;

	/**
	 *  The first parameter that needs to be passed into Runtime.exec() to open the
	 *  default web browser on Windows.
	 */
	private final static String FIRST_WINDOWS_PARAMETER = "/c";

	/**  The second parameter for Runtime.exec() on Windows. */
	private final static String SECOND_WINDOWS_PARAMETER = "start";

	/**
	 *  The third parameter for Runtime.exec() on Windows. This is a "title"
	 *  parameter that the command line expects. Setting this parameter allows URLs
	 *  containing spaces to work.
	 */
	private final static String THIRD_WINDOWS_PARAMETER = "\"\"";

	/**
	 *  The shell parameters for Netscape that opens a given URL in an already-open
	 *  copy of Netscape on many command-line systems.
	 */
	private final static String NETSCAPE_REMOTE_PARAMETER = "-remote";
	private final static String NETSCAPE_OPEN_PARAMETER_START = "'openURL(";
	private final static String NETSCAPE_OPEN_PARAMETER_END = ")'";

	/**
	 *  The message from any exception thrown throughout the initialization
	 *  process.
	 */
	private static String errorMessage;


	/**  This class should be never be instantiated; this just ensures so.  */
	private BrowserLauncher() { }


	/**
	 *  Attempts to open the default web browser to the given URL.
	 *
	 * @param  url           The URL to open
	 * @throws  IOException  If the web browser could not be located or does not
	 *      run
	 */
	public static void openURL(String url) throws IOException {
		if(!loadedWithoutErrors) {
			throw new IOException("Exception in finding browser: " + errorMessage);
		}
		Object browser = locateBrowser();
		if(browser == null) {
			throw new IOException("Unable to locate browser: " + errorMessage);
		}

		switch (jvm) {
			case MAC:
				url = java.net.URLEncoder.encode(url, "UTF-8");
				url = url.substring("http%3A%2F%2Fwww".length(), url.length());
				url = "http://www" + url;
				System.out.println("here " + browser + " " + url);
				
				Process p = Runtime.getRuntime().exec(new String[]{(String) browser, url});
				break;
			case WINDOWS_NT:
			case WINDOWS_9x:
				// Add quotes around the URL to allow ampersands and other special
				// characters to work.
				Process process = Runtime.getRuntime().exec(new String[]{(String) browser,
						FIRST_WINDOWS_PARAMETER,
						SECOND_WINDOWS_PARAMETER,
						THIRD_WINDOWS_PARAMETER,
						'"' + url + '"'});
				// This avoids a memory leak on some versions of Java on Windows.
				// That's hinted at in <http://developer.java.sun.com/developer/qow/archive/68/>.
				try {
					process.waitFor();
					process.exitValue();
				} catch(InterruptedException ie) {
					throw new IOException("InterruptedException while launching browser: " + ie.getMessage());
				}
				break;
			case OTHER:
				// Assume that we're on Unix and that Netscape is installed

				// First, attempt to open the URL in a currently running session of Netscape
				process = Runtime.getRuntime().exec(new String[]{(String) browser,
						NETSCAPE_REMOTE_PARAMETER,
						NETSCAPE_OPEN_PARAMETER_START +
						url +
						NETSCAPE_OPEN_PARAMETER_END});
				try {
					int exitCode = process.waitFor();
					if(exitCode != 0) {// if Netscape was not open
						Runtime.getRuntime().exec(new String[]{(String) browser, url});
					}
				} catch(InterruptedException ie) {
					throw new IOException("InterruptedException while launching browser: " + ie.getMessage());
				}
				break;
			default:
				// This should never occur, but if it does, we'll try the simplest thing possible
				Runtime.getRuntime().exec(new String[]{(String) browser, url});
				break;
		}
	}


	/**
	 *  Attempts to locate the default web browser on the local system. Caches
	 *  results so it only locates the browser once for each use of this class per
	 *  JVM instance.
	 *
	 * @return    The browser for the system. Note that this may not be what you
	 *      would consider to be a standard web browser; instead, it's the
	 *      application that gets called to open the default web browser. In some
	 *      cases, this will be a non-String object that provides the means of
	 *      calling the default browser.
	 */
	private static Object locateBrowser() {
		if(browser != null) {
			return browser;
		}
		switch (jvm) {
			case MAC:
				browser =  "open";
				break;
			case WINDOWS_NT:
				browser = "cmd.exe";
				break;
			case WINDOWS_9x:
				browser = "command.com";
				break;
			case OTHER:
			default:
				browser = "netscape";
				break;
		}
		return browser;
	}

	/**
	 *  An initialization block that determines the operating system and loads the
	 *  necessary runtime data.
	 */
	static {
		loadedWithoutErrors = true;
		String osName = System.getProperty("os.name");
		if(osName.startsWith("Mac OS")) {
			//System.getProperty("mrj.version") != null && javax.swing.UIManager.getSystemLookAndFeelClassName().equals(javax.swing.UIManager.getLookAndFeel().getClass().getName());
			jvm = MAC;
		} else if(osName.startsWith("Windows")) {
			if(osName.indexOf("9") != -1) {
				jvm = WINDOWS_9x;
			} else {
				jvm = WINDOWS_NT;
			}
		} else {
			jvm = OTHER;
		}
	}

}

