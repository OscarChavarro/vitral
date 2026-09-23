#ifndef __COMMAND_LINE_OPTIONS__
#define __COMMAND_LINE_OPTIONS__

#include <string>
class CommandLineOptions {
  public:
    CommandLineOptions(int argc, char** argv);
    ~CommandLineOptions();

    double getLabelSizeMm() const;
    double getCircleHoledRadiusMm() const;
    const char* getOutputPdf() const;

  private:
    double labelSizeMm;
    double circleHoledRadiusMm;
    std::string outputPdfArgument;
    mutable std::string generatedOutputPdf;
};

#endif
