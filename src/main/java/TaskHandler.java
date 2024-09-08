import exceptions.InvalidTaskDateException;
import exceptions.TaskHandlerException;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskHandler {

    private static final int NON_WORK_HOURS = 16; // 24 hours - 8 work hours = 16 non work hours
    private static final int WORKDAY_END_HOUR = 17; // 17 = 17:00 - 5:00PM
    private static final int WORKDAY_START_HOUR = 9; // 9 = 09:00 - 9:00AM
    Logger logger;

    /**
     * Handler for all task related operations
     */
    public TaskHandler() {
        this.logger = Logger.getLogger(TaskHandler.class.getName());
    }

    /**
     * Handler for all task related operations
     * @param logger used to log at runtime
     */
    public TaskHandler(Logger logger) {
        this.logger = logger;
    }

    /**
     * Calulates the due date of a task.
     * Only accounts for working hours (9am - 5pm) Mon-Fri, ignores weekends and holidays
     * @param task the task you wish to operate on
     * @return the calculated due date of the task
     */
    public ZonedDateTime calculateDueDate(Task task) throws InvalidTaskDateException, TaskHandlerException {
        // Validate start date
        ZonedDateTime startDate = task.getStartDate();
        Calendar calendarDate = formatDate(startDate);

        if (!validateDateForWorkingHours(calendarDate)) {
            logger.log(Level.SEVERE, String.format("Invalid start date provided: %s", startDate));
            throw new InvalidTaskDateException(String.format("The provided date was invalid: %s. Please ensure date is withing working hours 9am-5pm Mon-Fri.", startDate));
        }

        // Validate task length
        int taskLength = task.getTaskDuration();
        if (taskLength <= 0) {
            logger.log(Level.SEVERE, String.format("Invalid task length provided: %d", taskLength));
            throw new TaskHandlerException(String.format("Invalid task length: %d. Value must be greater than zero.", taskLength));
        }

        return incrementTaskDueDate(calendarDate, taskLength).toInstant().atZone(startDate.getZone());
    }

    // Recursive method used for calculating the task due date.
    private Calendar incrementTaskDueDate(Calendar dueDate, int hoursRemain) throws TaskHandlerException {
        // Sanity check
        if (hoursRemain < 0) {
            logger.log(Level.SEVERE, "Date has been miscalculated, please investigate!");
            throw new TaskHandlerException("Error in due date calculation");
        }

        // Weekend Check
        int dayOfWeek = dueDate.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY) {
            dueDate.add(Calendar.DAY_OF_YEAR, 2); // Skip to monday
            return incrementTaskDueDate(dueDate, hoursRemain);
        }
        // Should never hit this
        if (dayOfWeek == Calendar.SUNDAY) {
            dueDate.add(Calendar.DAY_OF_YEAR, 1); //Skip to monday
            return incrementTaskDueDate(dueDate, hoursRemain);
        }

        // Base case
        if (hoursRemain == 0) {
            if (validateDateForWorkingHours(dueDate)) { // Confirm we have a valid date
                return dueDate;
            } else { // If we land here it means we need to adjust forward to the next workday as we are outside working hours
                dueDate.add(Calendar.HOUR_OF_DAY, NON_WORK_HOURS); //Increment to next workday (Weekends handled above)
                return incrementTaskDueDate(dueDate, 0);
            }
        } else {  // continue to increment
            int currentHour = dueDate.get(Calendar.HOUR_OF_DAY);
            int hoursLeftInDay = WORKDAY_END_HOUR - currentHour;
            if (hoursRemain > hoursLeftInDay) {
                dueDate.add(Calendar.HOUR_OF_DAY, hoursLeftInDay + NON_WORK_HOURS); //hours consume today + shift forward to next workday
                return incrementTaskDueDate(dueDate, hoursRemain - hoursLeftInDay);
            } else {
                dueDate.add(Calendar.HOUR_OF_DAY, hoursRemain);
                return incrementTaskDueDate(dueDate, 0); //Recur again in case we roll into a weekend
            }
        }
    }

    // This method is used for validating task inputs and calculation results
    // UPDATE WITH CARE
    private boolean validateDateForWorkingHours(Calendar targetDate) {
        //Check if Weekend
        int dayOfWeek = targetDate.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false;
        }

        //Check time, ignoring millis
        int hours = targetDate.get(Calendar.HOUR_OF_DAY);
        int minutes = targetDate.get(Calendar.MINUTE);
        int seconds = targetDate.get(Calendar.SECOND);
        if (hours < WORKDAY_START_HOUR || // before work hours
                hours > WORKDAY_END_HOUR || // after work hours
                (hours == WORKDAY_END_HOUR && minutes > 0) || // after work (minutes)
                (hours == WORKDAY_END_HOUR && minutes == 0 && seconds > 0)) // after work (seconds)
            {
            return false;
        }
        return true;
    }

    // Creates a calendar clearing Millis
    private Calendar formatDate(ZonedDateTime targetDate) {
        Calendar resultCalendar = GregorianCalendar.from(targetDate);
        resultCalendar.clear(Calendar.MILLISECOND);
        return resultCalendar;
    }
}
