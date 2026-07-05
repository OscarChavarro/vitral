#ifndef __COMMANDLINEOPTIONS__
#define __COMMANDLINEOPTIONS__

#include <string>
class CommandLineOptions {
  public:
    CommandLineOptions(int argc, char** argv);
    ~CommandLineOptions();

    double getLabelSizeMm() const;
    double getCircleHoledRadiusMm() const;
    const char* getOutputPdf() const;

  private:
    double labelSizeMm_;
    double circleHoledRadiusMm_;
    std::string outputPdfArgument_;
    mutable std::string generatedOutputPdf_;
};

#endif
