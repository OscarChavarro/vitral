#ifndef __OPEN_CV_MARKERS_RENDERER__
#define __OPEN_CV_MARKERS_RENDERER__

#include <opencv2/core.hpp>

#include <apriltag.h>
#include "java/util/ArrayList.h"
#include "webservice/Protocol.hpp"

class MarkersModel;
class OpenCVHudRenderer;

class OpenCVMarkersRenderer {
public:
    explicit OpenCVMarkersRenderer(MarkersModel* model);

    void render(const cv::Mat& frame, zarray_t* detections, const java::ArrayList<MarkerGroupPose>& groups);
    void displayFrame(const char* windowName);

private:
    void drawDetections(const cv::Mat& frame, zarray_t* detections);

    MarkersModel* model_;
    OpenCVHudRenderer* hudRenderer_;
    cv::Mat preview_;
};

#endif
