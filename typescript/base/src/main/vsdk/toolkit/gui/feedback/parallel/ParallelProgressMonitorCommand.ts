/**
Port of the Java enum `ParallelProgressMonitorCommand`. Members are string
constants so an event survives a structured-clone hop between a worker and the
runtime that owns the console.
*/
export enum ParallelProgressMonitorCommand {
    INIT = "INIT",
    PROCESS_NEXT_ELEMENT = "PROCESS_NEXT_ELEMENT",
    FINISH = "FINISH",
}
