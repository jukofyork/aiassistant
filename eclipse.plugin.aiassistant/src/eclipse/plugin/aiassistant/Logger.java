package eclipse.plugin.aiassistant;

import org.eclipse.core.runtime.ILog;

/**
 * The Logger class provides a simple interface to log messages to the Eclipse
 * log. It uses the default log provided by the Activator class.
 */
public final class Logger {

	private static final ILog defaultLog = Activator.getDefault().getLog();

	/**
	 * Private constructor to prevent instantiation of the class.
	 */
	private Logger() {
	}

	/**
	 * Returns the default log provided by the Activator class.
	 * 
	 * @return The default log.
	 */
	public static ILog getDefault() {
		return defaultLog;
	}

	/**
	 * Logs an informational message to the default log.
	 * 
	 * @param message The message to be logged.
	 */
	public static void info(String message) {
		defaultLog.info(message);
	}

	/**
	 * Logs a warning message to the default log.
	 * 
	 * @param message The message to be logged.
	 */
	public static void warning(String message) {
		defaultLog.warn("WARNING: " + message);
	}

	/**
	 * Logs an error message to the default log.
	 * 
	 * @param message The message to be logged.
	 */
	public static void error(String message) {
		defaultLog.error("ERROR: " + message);
	}

	/**
	 * Logs an informational message with an associated exception to the default
	 * log.
	 * 
	 * @param message The message to be logged.
	 * @param e       The exception to be logged.
	 */
	public static void info(String message, Exception e) {
		defaultLog.info(message, e);
	}

	/**
	 * Logs a warning message with an associated exception to the default log.
	 * 
	 * @param message The message to be logged.
	 * @param e       The exception to be logged.
	 */
	public static void warning(String message, Exception e) {
		defaultLog.warn("WARNING: " + message, e);
	}

	/**
	 * Logs an error message with an associated exception to the default log.
	 * 
	 * @param message The message to be logged.
	 * @param e       The exception to be logged.
	 */
	public static void error(String message, Exception e) {
		defaultLog.error("ERROR: " + message, e);
	}

}