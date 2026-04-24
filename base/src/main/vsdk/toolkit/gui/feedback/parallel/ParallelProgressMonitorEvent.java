package vsdk.toolkit.gui.feedback.parallel;

public class ParallelProgressMonitorEvent {
    private final ParallelProgressMonitorCommand commandType;
    private final long numberOfElementsToProcess;

    public ParallelProgressMonitorEvent(
        ParallelProgressMonitorCommand commandType,
        long numberOfElementsToProcess)
    {
        this.commandType = commandType;
        this.numberOfElementsToProcess = numberOfElementsToProcess;
    }

    public ParallelProgressMonitorCommand getCommandType() {
        return commandType;
    }

    public long getNumberOfElementsToProcess() {
        return numberOfElementsToProcess;
    }
}
