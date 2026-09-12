import { ParallelProgressMonitorCommand } from "./ParallelProgressMonitorCommand.js";

export class ParallelProgressMonitorEvent {
    private readonly commandType: ParallelProgressMonitorCommand;
    private readonly numberOfElementsToProcess: bigint;

    public constructor(commandType: ParallelProgressMonitorCommand, numberOfElementsToProcess: bigint) {
        this.commandType = commandType;
        this.numberOfElementsToProcess = numberOfElementsToProcess;
    }

    public getCommandType(): ParallelProgressMonitorCommand {
        return this.commandType;
    }

    public getNumberOfElementsToProcess(): bigint {
        return this.numberOfElementsToProcess;
    }
}
