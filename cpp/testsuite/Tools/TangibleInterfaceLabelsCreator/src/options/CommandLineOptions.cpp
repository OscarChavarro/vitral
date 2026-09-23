#include <cstdio>
#include <cstdlib>
#include <cstring>

#include "options/CommandLineOptions.h"
CommandLineOptions::CommandLineOptions(int argc, char** argv)
    : labelSizeMm(40.0),
      circleHoledRadiusMm(-1.0) {
    for (int i = 1; i < argc; ++i) {
        if (strcmp(argv[i], "-size") == 0 && i + 1 < argc) {
            labelSizeMm = atof(argv[++i]);
        } else if (strcmp(argv[i], "-circleHoleRadius") == 0 && i + 1 < argc) {
            circleHoledRadiusMm = atof(argv[++i]);
        } else if (outputPdfArgument.empty()) {
            outputPdfArgument = argv[i];
        }
    }
}

CommandLineOptions::~CommandLineOptions() {
}

double CommandLineOptions::getLabelSizeMm() const {
    return labelSizeMm;
}

double CommandLineOptions::getCircleHoledRadiusMm() const {
    if (circleHoledRadiusMm > 0.0) {
        return circleHoledRadiusMm;
    }
    return labelSizeMm * 0.1;
}

const char* CommandLineOptions::getOutputPdf() const {
    if (!outputPdfArgument.empty()) {
        return outputPdfArgument.c_str();
    }

    char buffer[256];
    snprintf(buffer, sizeof(buffer), "labels_%.0fmm_a4.pdf", labelSizeMm);
    generatedOutputPdf = buffer;
    return generatedOutputPdf.c_str();
}
