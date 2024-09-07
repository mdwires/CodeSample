package logging;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class MockLogger extends Logger {

    private List<LogRecord> logEntries;

    /**
     *
     * Mock logger for capturing and validating log messages.
     *
     * @param name               A name for the logger.  This should
     *                           be a dot-separated name and should normally
     *                           be based on the package name or class name
     *                           of the subsystem, such as java.net
     *                           or javax.swing.  It may be null for anonymous Loggers.
     */
    public MockLogger(String name) {
        super(name, null);
        logEntries = new ArrayList<>();
    }

    @Override
    public void log(LogRecord record) {
        logEntries.add(record);
    }

    public List<LogRecord> getLogEntries() {
        return logEntries;
    }

}
