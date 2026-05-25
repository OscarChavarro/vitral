#ifndef __VSDK_TOOLKIT_GUI_FEEDBACK_PARALLEL_PARALLELPROGRESSMONITOREVENT_H__
#define __VSDK_TOOLKIT_GUI_FEEDBACK_PARALLEL_PARALLELPROGRESSMONITOREVENT_H__

#include "vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorCommand.h"

class ParallelProgressMonitorEvent {
private:
    ParallelProgressMonitorCommand commandType;
    long long numberOfElementsToProcess;

public:
    ParallelProgressMonitorEvent(ParallelProgressMonitorCommand commandType, long long numberOfElementsToProcess)
        : commandType(commandType), numberOfElementsToProcess(numberOfElementsToProcess) {}

    ParallelProgressMonitorCommand getCommandType() const { return commandType; }
    long long getNumberOfElementsToProcess() const { return numberOfElementsToProcess; }
};

#endif
