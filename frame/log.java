// Date: 2019/02/19
// Author: dylan
// Desc: 日志系统

package frame;

import lombok.extern.slf4j.Slf4j;
import java.lang.String;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

@Slf4j
public final class log {

    public static final int DEBUG = 1;
    public static final int INFO = 2;
    public static final int WARN = 3;
    public static final int ERROR = 4;

    public static int level = DEBUG;

    /**
     * Log a message at the DEBUG level.
     *
     * @param msg the message string to be logged
     */
    public static void debug(String msg) {
        if (level <= DEBUG) {
            log.debug(msg);
        }
    }

    /**
     * Log a message at the DEBUG level according to the specified format and
     * argument.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for
     * the DEBUG level.
     * </p>
     *
     * @param format the format string
     * @param arg    the argument
     */
    public static void debug(String format, Object arg) {
        if (level <= DEBUG) {
            log.debug(format, arg);
        }
    }

    /**
     * Log a message at the DEBUG level according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for
     * the DEBUG level.
     * </p>
     *
     * @param format the format string
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public static void debug(String format, Object arg1, Object arg2) {
        if (level <= DEBUG) {
            log.debug(format, arg1, arg2);
        }
    }

    /**
     * Log a message at the DEBUG level according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous string concatenation when the logger is disabled
     * for the DEBUG level. However, this variant incurs the hidden (and relatively
     * small) cost of creating an <code>Object[]</code> before invoking the method,
     * even if this logger is disabled for DEBUG. The variants taking
     * {@link #debug(String, Object) one} and {@link #debug(String, Object, Object)
     * two} arguments exist solely in order to avoid this hidden cost.
     * </p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */
    public static void debug(String format, Object... arguments) {
        if (level <= DEBUG) {
            log.debug(format, arguments);
        }
    }

    /**
     * Log an exception (throwable) at the DEBUG level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    public static void debug(String msg, Throwable t) {
        if (level <= DEBUG) {
            log.debug(msg, t);
        }
    }

    /**
     * Log a Object at the DEBUG level ,This method calls at first String.valueOf(x)
     * to get the log object's string value,
     * 
     * @param x The <code>Object</code> to be log.
     */
    public static void debug(Object x) {
        if (level <= DEBUG) {
            String s = String.valueOf(x);
            debug(s);
        }
    }

    /**
     * Log a message at the INFO level.
     *
     * @param msg the message string to be logged
     */
    public static void info(String msg) {
        if (level <= INFO) {
            log.info(msg);
        }
    }

    /**
     * Log a message at the INFO level according to the specified format and
     * argument.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for
     * the INFO level.
     * </p>
     *
     * @param format the format string
     * @param arg    the argument
     */
    public static void info(String format, Object arg) {
        if (level <= INFO) {
            log.info(format, arg);
        }
    }

    /**
     * Log a message at the INFO level according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for
     * the INFO level.
     * </p>
     *
     * @param format the format string
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public static void info(String format, Object arg1, Object arg2) {
        if (level <= INFO) {
            log.info(format, arg1, arg2);
        }
    }

    /**
     * Log a message at the INFO level according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous string concatenation when the logger is disabled
     * for the INFO level. However, this variant incurs the hidden (and relatively
     * small) cost of creating an <code>Object[]</code> before invoking the method,
     * even if this logger is disabled for INFO. The variants taking
     * {@link #info(String, Object) one} and {@link #info(String, Object, Object)
     * two} arguments exist solely in order to avoid this hidden cost.
     * </p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */
    public static void info(String format, Object... arguments) {
        if (level <= INFO) {
            log.info(format, arguments);
        }
    }

    /**
     * Log an exception (throwable) at the INFO level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    public static void info(String msg, Throwable t) {
        if (level <= INFO) {
            log.info(msg, t);
        }
    }

    /**
     * Log a Object at the INFO level ,This method calls at first String.valueOf(x)
     * to get the log object's string value,
     * 
     * @param x The <code>Object</code> to be log.
     */
    public static void info(Object x) {
        if (level <= INFO) {
            String s = String.valueOf(x);
            info(s);
        }
    }

    /**
     * Log a message at the WARN level.
     *
     * @param msg the message string to be logged
     */
    public static void warn(String msg) {
        if (level <= WARN) {
            log.warn(msg);
        }
    }

    /**
     * Log a message at the WARN level according to the specified format and
     * argument.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for
     * the WARN level.
     * </p>
     *
     * @param format the format string
     * @param arg    the argument
     */
    public static void warn(String format, Object arg) {
        if (level <= WARN) {
            log.warn(format, arg);
        }
    }

    /**
     * Log a message at the WARN level according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for
     * the WARN level.
     * </p>
     *
     * @param format the format string
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public static void warn(String format, Object arg1, Object arg2) {
        if (level <= WARN) {
            log.warn(format, arg1, arg2);
        }
    }

    /**
     * Log a message at the WARN level according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous string concatenation when the logger is disabled
     * for the WARN level. However, this variant incurs the hidden (and relatively
     * small) cost of creating an <code>Object[]</code> before invoking the method,
     * even if this logger is disabled for WARN. The variants taking
     * {@link #warn(String, Object) one} and {@link #warn(String, Object, Object)
     * two} arguments exist solely in order to avoid this hidden cost.
     * </p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */
    public static void warn(String format, Object... arguments) {
        if (level <= WARN) {
            log.warn(format, arguments);
        }
    }

    /**
     * Log an exception (throwable) at the WARN level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    public static void warn(String msg, Throwable t) {
        if (level <= WARN) {
            log.warn(msg, t);
        }
    }

    /**
     * Log a Object at the WARN level ,This method calls at first String.valueOf(x)
     * to get the log object's string value,
     * 
     * @param x The <code>Object</code> to be log.
     */
    public static void warn(Object x) {
        if (level <= WARN) {
            String s = String.valueOf(x);
            warn(s);
        }
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param msg the message string to be logged
     */
    public static void error(String msg) {
        if (level <= ERROR) {
            log.error(msg);
        }
    }

    /**
     * Log a message at the ERROR level according to the specified format and
     * argument.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for
     * the ERROR level.
     * </p>
     *
     * @param format the format string
     * @param arg    the argument
     */
    public static void error(String format, Object arg) {
        if (level <= ERROR) {
            log.error(format, arg);
        }
    }

    /**
     * Log a message at the ERROR level according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for
     * the ERROR level.
     * </p>
     *
     * @param format the format string
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public static void error(String format, Object arg1, Object arg2) {
        if (level <= ERROR) {
            log.error(format, arg1, arg2);
        }
    }

    /**
     * Log a message at the ERROR level according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous string concatenation when the logger is disabled
     * for the ERROR level. However, this variant incurs the hidden (and relatively
     * small) cost of creating an <code>Object[]</code> before invoking the method,
     * even if this logger is disabled for ERROR. The variants taking
     * {@link #error(String, Object) one} and {@link #error(String, Object, Object)
     * two} arguments exist solely in order to avoid this hidden cost.
     * </p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */
    public static void error(String format, Object... arguments) {
        if (level <= ERROR) {
            log.error(format, arguments);
        }
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
    }

    /**
     * Log an exception (throwable) at the ERROR level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    public static void error(String msg, Throwable t) {
        if (level <= ERROR) {
            log.error(msg, t);
        }
    }

    /**
     * Log a Object at the ERROR level ,This method calls at first String.valueOf(x)
     * to get the log object's string value,
     * 
     * @param x The <code>Object</code> to be log.
     */
    public static void error(Object x) {
        if (level <= ERROR) {
            String s = String.valueOf(x);
            error(s);
        }
    }

}