#include <cstdio>
#include <cstdlib>
#include <cstring>

#include "options/CommandLineOptions.h"
CommandLineOptions::CommandLineOptions(int argc, char** argv)
    : labelSizeMm_(40.0),
      circleHoledRadiusMm_(-1.0) {
    for (int i = 1; i < argc; ++i) {
        if (strcmp(argv[i], "-size") == 0 && i + 1 < argc) {
            labelSizeMm_ = atof(argv[++i]);
        } else if (strcmp(argv[i], "-circleHoleRadius") == 0 && i + 1 < argc) {
            circleHoledRadiusMm_ = atof(argv[++i]);
        } else if (outputPdfArgument_.empty()) {
            outputPdfArgument_ = argv[i];
        }
    }
}

CommandLineOptions::~CommandLineOptions() {
}

double CommandLineOptions::getLabelSizeMm() const {
    return labelSizeMm_;
}

double CommandLineOptions::getCircleHoledRadiusMm() const {
    if (circleHoledRadiusMm_ > 0.0) {
        return circleHoledRadiusMm_;
    }
    return labelSizeMm_ * 0.1;
}

const char* CommandLineOptions::getOutputPdf() const {
    if (!outputPdfArgument_.empty()) {
        return outputPdfArgument_.c_str();
    }

    char buffer[256];
    snprintf(buffer, sizeof(buffer), "labels_%.0fmm_a4.pdf", labelSizeMm_);
    generatedOutputPdf_ = buffer;
    return generatedOutputPdf_.c_str();
}
