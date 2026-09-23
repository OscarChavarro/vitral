#include <cstdlib>
#include <cstring>

#include "options/CommandLineOptions.h"
CommandLineOptions::CommandLineOptions(int argc, char** argv)
    : startId(0),
      markerSizeMm(40.0) {
    for (int i = 1; i < argc; ++i) {
        if (strcmp(argv[i], "-start") == 0 && i + 1 < argc) {
            startId = atoi(argv[++i]);
        } else if (strcmp(argv[i], "-size") == 0 && i + 1 < argc) {
            // Accepts both "40" and "40mm" (atof stops at the unit suffix).
            markerSizeMm = atof(argv[++i]);
        } else if (outputPdfArgument.empty()) {
            outputPdfArgument = argv[i];
        }
    }
}

CommandLineOptions::~CommandLineOptions() {
}

int CommandLineOptions::getStartId() const {
    return startId;
}

double CommandLineOptions::getMarkerSizeMm() const {
    return markerSizeMm;
}

const char* CommandLineOptions::getOutputPdf(int endId) const {
    if (!outputPdfArgument.empty()) {
        return outputPdfArgument.c_str();
    }

    char buffer[256];
    snprintf(buffer, sizeof(buffer), "markers_%d_%d_a4.pdf", startId, endId);
    generatedOutputPdf = buffer;
    return generatedOutputPdf.c_str();
}
