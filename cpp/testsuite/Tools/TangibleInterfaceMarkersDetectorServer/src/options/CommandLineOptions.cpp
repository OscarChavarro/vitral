#include "options/CommandLineOptions.hpp"

#include <cstdio>
#include <cstdlib>
#include "java/lang/String.h"
#include <opencv2/videoio.hpp>

CommandLineOptions::CommandLineOptions(int argc, char** argv)
    : action_(RUN), port_(8090), cameraIndex_(0), markerSize_(0.035),
      decisionMarginThreshold_(30.0), viewAngleCosThreshold_(0.5),
      streamHz_(30), debugMode_(false), debugDir_("debug"),
      previewMode_(false), mappingValid_(true), programName_(argv[0]) {
    parse(argc, argv);
}

void CommandLineOptions::parse(int argc, char** argv) {
    for (int i = 1; i < argc; ++i) {
        java::String arg = argv[i];
        if (arg == "-h" || arg == "--help") {
            action_ = SHOW_HELP;
            showHelp(programName_);
            return;
        } else if (arg == "-list") {
            action_ = LIST_CAMERAS;
            listCameras();
            return;
        } else if (arg == "-preview") {
            previewMode_ = true;
        } else if (arg == "--debug") {
            debugMode_ = true;
        } else if (arg == "-p" || arg == "--port") {
            if (i + 1 < argc) port_ = std::atoi(argv[++i]);
        } else if (arg == "-cam" || arg == "--camera") {
            if (i + 1 < argc) cameraIndex_ = std::atoi(argv[++i]);
        } else if (arg == "--marker-size") {
            if (i + 1 < argc) markerSize_ = std::atof(argv[++i]);
        } else if (arg == "--calib") {
            if (i + 1 < argc) calibFile_ = argv[++i];
        } else if (arg == "--margin") {
            if (i + 1 < argc) decisionMarginThreshold_ = std::atof(argv[++i]);
        } else if (arg == "--view-cos") {
            if (i + 1 < argc) viewAngleCosThreshold_ = std::atof(argv[++i]);
        } else if (arg == "--hz") {
            if (i + 1 < argc) streamHz_ = std::atoi(argv[++i]);
        } else if (arg == "--debug-dir") {
            if (i + 1 < argc) debugDir_ = argv[++i];
        }
    }
}

void CommandLineOptions::showHelp(const char* progName) {
    std::printf("Usage: %s [options]\n\n", progName);
    std::printf("Options:\n");
    std::printf("  -p, --port PORT               WebSocket server port (default: 8090)\n");
    std::printf("  -cam, --camera INDEX          Camera index (default: 0)\n");
    std::printf("  --marker-size SIZE            AprilTag marker size in meters (default: 0.035)\n");
    std::printf("  --calib FILE                  Camera calibration file\n");
    std::printf("  --margin THRESHOLD            Decision margin threshold (default: 30.0)\n");
    std::printf("  --view-cos THRESHOLD          View angle cos threshold (default: 0.5)\n");
    std::printf("  --hz RATE                     Stream rate in Hz (default: 30)\n");
    std::printf("  --debug                       Enable debug mode\n");
    std::printf("  --debug-dir DIR               Debug output directory\n");
    std::printf("  -preview                      Preview mode (show camera with markers)\n");
    std::printf("  -list                         List available cameras and exit\n");
    std::printf("  -h, --help                    Show this help message\n");
}

void CommandLineOptions::listCameras() {
    std::printf("Available cameras:\n");
    int found = 0;
    int consecutive_fails = 0;
    for (int i = 0; i < 32 && consecutive_fails < 3; ++i) {
        cv::VideoCapture cap(i);
        if (cap.isOpened()) {
            std::printf("  [%d] Camera\n", i);
            found++;
            consecutive_fails = 0;
            cap.release();
        } else {
            consecutive_fails++;
        }
    }
    if (found == 0) {
        std::printf("  (no cameras found)\n");
    }
}
