import java.time.ZonedDateTime;

public class Task {
    private ZonedDateTime startDate;
    private int taskDuration;

    public ZonedDateTime getStartDate() {
        return startDate;
    }

    public int getTaskDuration() {
        return taskDuration;
    }

    public Task(ZonedDateTime startDate, int taskDuration) {
        this.startDate = startDate;
        this.taskDuration = taskDuration;
    }


}
