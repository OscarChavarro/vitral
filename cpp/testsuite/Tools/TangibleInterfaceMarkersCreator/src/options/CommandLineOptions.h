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
    int startId_;
    double markerSizeMm_;
    std::string outputPdfArgument_;
    mutable std::string generatedOutputPdf_;
};

#endif
