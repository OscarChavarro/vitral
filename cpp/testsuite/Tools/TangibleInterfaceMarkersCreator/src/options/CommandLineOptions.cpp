#include "options/CommandLineOptions.h"

#include <cstdlib>
#include <cstring>

CommandLineOptions::CommandLineOptions(int argc, char** argv)
    : startId_(0),
      markerSizeMm_(40.0) {
    for (int i = 1; i < argc; ++i) {
        if (strcmp(argv[i], "-start") == 0 && i + 1 < argc) {
            startId_ = atoi(argv[++i]);
        } else if (strcmp(argv[i], "-size") == 0 && i + 1 < argc) {
            // Accepts both "40" and "40mm" (atof stops at the unit suffix).
            markerSizeMm_ = atof(argv[++i]);
        } else if (outputPdfArgument_.empty()) {
            outputPdfArgument_ = argv[i];
        }
    }
}

CommandLineOptions::~CommandLineOptions() {
}

int CommandLineOptions::getStartId() const {
    return startId_;
}

double CommandLineOptions::getMarkerSizeMm() const {
    return markerSizeMm_;
}

const char* CommandLineOptions::getOutputPdf(int endId) const {
    if (!outputPdfArgument_.empty()) {
        return outputPdfArgument_.c_str();
    }

    char buffer[256];
    snprintf(buffer, sizeof(buffer), "markers_%d_%d_a4.pdf", startId_, endId);
    generatedOutputPdf_ = buffer;
    return generatedOutputPdf_.c_str();
}
