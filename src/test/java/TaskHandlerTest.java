import exceptions.InvalidTaskDateException;
import exceptions.TaskHandlerException;
import logging.MockLogger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TaskHandlerTest {

    TaskHandler taskHandler;
    DateTimeFormatter dtFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss zzz");
    MockLogger mockLogger = new MockLogger("TestLogger");

    String invalidTaskDateExceptionExpected = "Expected a InvalidTaskDateException but none was thrown";
    String taskHandlerExceptionExpected = "Expected a TaskDateHandlerException but none was thrown";
    String invalidTaskDateLogMessage = "Invalid start date provided";
    String invalidTaskLengthLogMessage = "Invalid task length provided";

    @BeforeEach
    public void Setup() {
        taskHandler = new TaskHandler(mockLogger);
    }

    //region Validation Tests
    @Test
    public void taskDateIsValid() {
        // Work day start
        ZonedDateTime startDate1 = ZonedDateTime.parse("09/06/2024 09:00:00 EST", dtFormat);
        assertDoesNotThrow(() -> taskHandler.calculateDueDate(new Task(startDate1, 10)),
                invalidTaskDateExceptionExpected);
        // Work day end
        ZonedDateTime startDate2 = ZonedDateTime.parse("09/06/2024 17:00:00 EST", dtFormat);
        assertDoesNotThrow(() -> taskHandler.calculateDueDate(new Task(startDate2, 10)),
                invalidTaskDateExceptionExpected);
        // Mid work day
        ZonedDateTime startDate3 = ZonedDateTime.parse("09/06/2024 12:50:30 EST", dtFormat);
        assertDoesNotThrow(() -> taskHandler.calculateDueDate(new Task(startDate3, 10)),
                invalidTaskDateExceptionExpected);

        //Validate Logging
        long logCount = mockLogger.getLogEntries().stream()
                .filter(l -> (l.getLevel() == Level.SEVERE && l.getMessage().contains(invalidTaskDateLogMessage))).count();
        Assertions.assertEquals(0, logCount);

    }

    @Test
    public void taskDateInvalidDay() {
        ZonedDateTime startDate = ZonedDateTime.parse("09/07/2024 16:50:30 EST", dtFormat);
        assertThrows(InvalidTaskDateException.class,
                () -> taskHandler.calculateDueDate(new Task(startDate, 10)),
                invalidTaskDateExceptionExpected);

        //Validate Logging
        long logCount = mockLogger.getLogEntries().stream()
                .filter(l -> (l.getLevel() == Level.SEVERE && l.getMessage().contains(invalidTaskDateLogMessage))).count();
        Assertions.assertEquals(1, logCount);
    }

    @Test
    public void taskDateInvalidTimeHour() {
        // Test after working hours
        ZonedDateTime startDate1 = ZonedDateTime.parse("09/06/2024 18:00:00 EST", dtFormat);
        assertThrows(InvalidTaskDateException.class,
                () -> taskHandler.calculateDueDate(new Task(startDate1, 10)),
                invalidTaskDateExceptionExpected);

        // Test before working hours
        ZonedDateTime startDate2 = ZonedDateTime.parse("09/06/2024 08:00:00 EST", dtFormat);
        assertThrows(InvalidTaskDateException.class,
                () -> taskHandler.calculateDueDate(new Task(startDate2, 10)),
                invalidTaskDateExceptionExpected);

        //Validate Logging
        long logCount = mockLogger.getLogEntries().stream()
                .filter(l -> (l.getLevel() == Level.SEVERE && l.getMessage().contains(invalidTaskDateLogMessage))).count();
        Assertions.assertEquals(2, logCount);
    }

    @Test
    public void taskDateInvalidTimeMinute() {
        // Test after working hours (minutes)
        ZonedDateTime startDate = ZonedDateTime.parse("09/06/2024 17:12:00 EST", dtFormat);
        assertThrows(InvalidTaskDateException.class,
                () -> taskHandler.calculateDueDate(new Task(startDate, 10)),
                invalidTaskDateExceptionExpected);

        //Validate Logging
        long logCount = mockLogger.getLogEntries().stream()
                .filter(l -> (l.getLevel() == Level.SEVERE && l.getMessage().contains(invalidTaskDateLogMessage))).count();
        Assertions.assertEquals(1, logCount);

    }

    @Test
    public void taskDateInvalidTimeSeconds() {
      // Test after working hours (minutes)
        ZonedDateTime startDate = ZonedDateTime.parse("09/06/2024 17:00:01 EST", dtFormat);
        assertThrows(InvalidTaskDateException.class,
                () -> taskHandler.calculateDueDate(new Task(startDate, 10)),
                invalidTaskDateExceptionExpected);

        //Validate Logging
        long logCount = mockLogger.getLogEntries().stream()
                .filter(l -> (l.getLevel() == Level.SEVERE && l.getMessage().contains(invalidTaskDateLogMessage))).count();
        Assertions.assertEquals(1, logCount);

    }

    @Test
    public void taskLengthInvalid()  {
        ZonedDateTime startDate = ZonedDateTime.parse("09/06/2024 15:00:00 EST", dtFormat);

        assertThrows(TaskHandlerException.class,
                () -> taskHandler.calculateDueDate(new Task(startDate, 0)),
                taskHandlerExceptionExpected);

        assertThrows(TaskHandlerException.class,
                () -> taskHandler.calculateDueDate(new Task(startDate, -3)),
                taskHandlerExceptionExpected);

        //Validate Logging
        long logCount = mockLogger.getLogEntries().stream()
                .filter(l -> (l.getLevel() == Level.SEVERE && l.getMessage().contains(invalidTaskLengthLogMessage))).count();
        Assertions.assertEquals(2, logCount);
    }
    //endregion

    //region Incrementation Tests
    @Test
    public void incrementOneHour() throws TaskHandlerException, InvalidTaskDateException {
        ZonedDateTime startDate = ZonedDateTime.parse("09/05/2024 15:34:23 EST", dtFormat);
        ZonedDateTime expectedDueDate = ZonedDateTime.parse("09/05/2024 16:34:23 EST", dtFormat);
        ZonedDateTime resultDate = taskHandler.calculateDueDate(new Task(startDate, 1));
        Assertions.assertEquals(expectedDueDate, resultDate);
    }

    @Test
    public void incrementOneWorkDay() throws TaskHandlerException, InvalidTaskDateException {
        ZonedDateTime startDate = ZonedDateTime.parse("09/05/2024 16:34:23 EST", dtFormat);
        ZonedDateTime expectedDueDate = ZonedDateTime.parse("09/06/2024 09:34:23 EST", dtFormat);
        ZonedDateTime resultDate = taskHandler.calculateDueDate(new Task(startDate, 1));
        Assertions.assertEquals(expectedDueDate, resultDate);
    }

    @Test
    public void incrementOneWorkDayOverWeekend() throws TaskHandlerException, InvalidTaskDateException {
        ZonedDateTime startDate = ZonedDateTime.parse("09/06/2024 16:34:23 EST", dtFormat);
        ZonedDateTime expectedDueDate = ZonedDateTime.parse("09/09/2024 09:34:23 EST", dtFormat);
        ZonedDateTime resultDate = taskHandler.calculateDueDate(new Task(startDate, 1));
        Assertions.assertEquals(expectedDueDate, resultDate);
    }

    @Test
    public void incrementOneWeek() throws TaskHandlerException, InvalidTaskDateException {
        ZonedDateTime startDate = ZonedDateTime.parse("09/06/2024 16:34:23 EST", dtFormat);
        ZonedDateTime expectedDueDate = ZonedDateTime.parse("09/13/2024 16:34:23 EST", dtFormat);
        ZonedDateTime resultDate = taskHandler.calculateDueDate(new Task(startDate, 40));
        Assertions.assertEquals(expectedDueDate, resultDate);
    }

    @Test
    public void incrementFourWeeks() throws TaskHandlerException, InvalidTaskDateException {
        ZonedDateTime startDate = ZonedDateTime.parse("09/06/2024 16:34:23 EST", dtFormat);
        ZonedDateTime expectedDueDate = ZonedDateTime.parse("10/04/2024 16:34:23 EST", dtFormat);
        ZonedDateTime resultDate = taskHandler.calculateDueDate(new Task(startDate, 160));
        Assertions.assertEquals(expectedDueDate, resultDate);
    }

    @Test
    public void incrementLeapYearShortLength() throws TaskHandlerException, InvalidTaskDateException {
        ZonedDateTime startDate = ZonedDateTime.parse("03/28/2024 16:34:23 EST", dtFormat);
        ZonedDateTime expectedDueDate = ZonedDateTime.parse("03/29/2024 09:34:23 EST", dtFormat);
        ZonedDateTime resultDate = taskHandler.calculateDueDate(new Task(startDate, 1));
        Assertions.assertEquals(expectedDueDate, resultDate);
    }

    @Test
    public void incrementLeapYearLongLength() throws TaskHandlerException, InvalidTaskDateException {
        int taskLength = (52*5+1)*8; //Work hours in a leap year (2024)
        ZonedDateTime startDate = ZonedDateTime.parse("01/01/2024 16:34:23 EST", dtFormat);
        ZonedDateTime expectedDueDate = ZonedDateTime.parse("12/31/2024 16:34:23 EST", dtFormat);
        ZonedDateTime resultDate = taskHandler.calculateDueDate(new Task(startDate, taskLength));
        Assertions.assertEquals(expectedDueDate, resultDate);
    }

    @Test
    public void incrementNonLeapYearLongLength() throws TaskHandlerException, InvalidTaskDateException {
        int taskLength = (52*5)*8; //Work hours in a non leap year
        ZonedDateTime startDate = ZonedDateTime.parse("01/01/2025 16:34:23 EST", dtFormat);
        ZonedDateTime expectedDueDate = ZonedDateTime.parse("12/31/2025 16:34:23 EST", dtFormat);
        ZonedDateTime resultDate = taskHandler.calculateDueDate(new Task(startDate, taskLength));
        Assertions.assertEquals(expectedDueDate, resultDate);
    }

    @Test
    public void incrementCheckYearRollover() throws TaskHandlerException, InvalidTaskDateException {
        int taskLength = (52*5)*8; //Work hours in a non leap year
        ZonedDateTime startDate = ZonedDateTime.parse("01/01/2025 16:34:23 EST", dtFormat);
        ZonedDateTime expectedDueDate = ZonedDateTime.parse("01/01/2026 09:34:23 ET", dtFormat);
        ZonedDateTime resultDate = taskHandler.calculateDueDate(new Task(startDate, taskLength + 1));
        Assertions.assertEquals(expectedDueDate, resultDate);
    }

    @Test
    public void incrementUTCWithRollover() throws TaskHandlerException, InvalidTaskDateException {
        int taskLength = (52*5)*8; //Work hours in a non leap year
        ZonedDateTime startDate = ZonedDateTime.parse("01/01/2025 16:34:23 UTC", dtFormat);
        ZonedDateTime expectedDueDate = ZonedDateTime.parse("01/01/2026 09:34:23 UTC", dtFormat);
        ZonedDateTime resultDate = taskHandler.calculateDueDate(new Task(startDate, taskLength + 1));
        Assertions.assertEquals(expectedDueDate, resultDate);
    }
    //endregion


}
