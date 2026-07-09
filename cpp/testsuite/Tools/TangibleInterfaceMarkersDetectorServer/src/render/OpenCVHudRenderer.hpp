#ifndef __OPEN_CV_HUD_RENDERER__
#define __OPEN_CV_HUD_RENDERER__

#include <opencv2/core.hpp>
#include "java/util/ArrayList.h"
#include "webservice/Protocol.hpp"

class MarkersModel;

class OpenCVHudRenderer {
public:
    explicit OpenCVHudRenderer(MarkersModel* model);

    void drawDetectedGroups(cv::Mat& canvas, const java::ArrayList<MarkerGroupPose>& groups);

private:
    MarkersModel* model_;
};

#endif
