#ifndef __COMMAND_LINE_OPTIONS__
#define __COMMAND_LINE_OPTIONS__

#include "java/lang/String.h"
class CommandLineOptions {
public:
    CommandLineOptions(int argc, char** argv);
    ~CommandLineOptions();

    int getStartId() const;
    double getMarkerSizeMm() const;
    const char* getOutputPdf(int endId) const;

private:
    int startId;
    double markerSizeMm;
    java::String outputPdfArgument;
    mutable java::String generatedOutputPdf;
};

#endif
