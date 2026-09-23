#ifndef __COMMAND_LINE_OPTIONS__
#define __COMMAND_LINE_OPTIONS__

#include <string>
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
    std::string outputPdfArgument;
    mutable std::string generatedOutputPdf;
};

#endif
