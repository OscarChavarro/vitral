#ifndef MARKER_TRACKER_HPP
#define MARKER_TRACKER_HPP

#include <pthread.h>
#include "java/lang/String.h"
#include <opencv2/core.hpp>
#include <opencv2/videoio.hpp>
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "webservice/Protocol.hpp"
#include "model/MarkerEventBus.hpp"
#include "processing/MarkerPoser.hpp"

class MarkersModel;

struct MarkerTrackerConfig {
    int cameraIndex = 0;
    double markerSize = 0.04;
    java::String calibFile;
    double decisionMarginThreshold = 30.0;
    double viewAngleCosThreshold = 0.5;
    bool debugMode = false;
    java::String debugDir = "debug";
    bool previewMode = false;
};

class MarkerTracker {
public:
    MarkerTracker(const MarkerTrackerConfig& cfg, MarkersModel* model, MarkerEventBus* bus);
    ~MarkerTracker();

    bool start();
    void stop();

    void runPreviewLoop();

private:
    static void* threadEntry(void* self);
    void loop();
    bool loadCalibration();
    void configureCapture(cv::VideoCapture& cap);
    double resolveTagSize(int markerId) const;
    Matrix4x4d buildAprilToModelRotation() const;

    MarkerTrackerConfig cfg_;
    MarkersModel* model_;
    MarkerPoser poser_;
    MarkerEventBus* bus_;
    double fx_, fy_, cx_, cy_;
    java::ArrayList<double> dist_;
    bool calibrated_;

    pthread_t thread_;
    bool running_;
};
#endif
