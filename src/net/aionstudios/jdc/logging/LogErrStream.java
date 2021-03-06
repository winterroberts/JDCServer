package net.aionstudios.jdc.logging;

import java.io.PrintStream;

import org.fusesource.jansi.AnsiConsole;

/**
 * Logs errors to the {@link Logger}'s {@link PrintStream}.
 * @author Winter Roberts
 */
public class LogErrStream extends PrintStream {

	/**
	 * Creates a new error stream which streams errors to a log file.
	 */
	public LogErrStream() {
		super(Logger.getStream(), true);
	}
	
	@Override
    public final void print(String s) {
        LogOut.errp(s);
    }
	
	@Override
    public final void println(String s) {
        LogOut.errpl(s);
    }

	@Override
	public final PrintStream printf(String format, Object... args) {
		LogOut.errp(String.format(format, args));
		return this;
	}
	
	@Override
	public final PrintStream format(String format, Object... args) {
		return printf(format, args);
	}
	
}
