#ifndef __PARALLELPROGRESSMONITOREVENT__
#define __PARALLELPROGRESSMONITOREVENT__

#include "vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorCommand.h"
class ParallelProgressMonitorEvent {
private:
    ParallelProgressMonitorCommand commandType;
    long long numberOfElementsToProcess;

public:
    ParallelProgressMonitorEvent()
        : commandType(FINISH), numberOfElementsToProcess(0) {}

    ParallelProgressMonitorEvent(ParallelProgressMonitorCommand commandType, long long numberOfElementsToProcess)
        : commandType(commandType), numberOfElementsToProcess(numberOfElementsToProcess) {}

    ParallelProgressMonitorCommand getCommandType() const { return commandType; }
    long long getNumberOfElementsToProcess() const { return numberOfElementsToProcess; }
};

#endif
