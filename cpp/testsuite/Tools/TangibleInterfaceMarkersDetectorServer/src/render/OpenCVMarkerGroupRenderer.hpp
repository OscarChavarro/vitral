#ifndef __OPEN_CV_MARKER_GROUP_RENDERER__
#define __OPEN_CV_MARKER_GROUP_RENDERER__

#include <opencv2/core.hpp>

#include "java/util/ArrayList.h"
#include "webservice/Protocol.hpp"
#include "model/MarkersModel.hpp"

class OpenCVMarkerGroupRenderer {
public:
    explicit OpenCVMarkerGroupRenderer(MarkersModel* model);

    void drawGroupGizmos(cv::Mat& canvas,
                         const java::ArrayList<MarkerGroupPose>& groups,
                         double fx, double fy, double cx, double cy);

private:
    double computeAxisLength(const MarkerGroup& group) const;
    bool project(const Vector3Dd& p, double fx, double fy, double cx, double cy, cv::Point2f* out) const;

    MarkersModel* model_;
};

#endif
