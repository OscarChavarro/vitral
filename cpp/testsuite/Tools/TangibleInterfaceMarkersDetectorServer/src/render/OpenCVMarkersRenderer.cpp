#include <cstdio>

#include "java/util/ArrayList.txx"
#include "model/MarkersModel.hpp"
#include "render/OpenCVHudRenderer.hpp"
#include "render/OpenCVMarkersRenderer.hpp"
#include <opencv2/highgui.hpp>
#include <opencv2/imgproc.hpp>
OpenCVMarkersRenderer::OpenCVMarkersRenderer(MarkersModel* model)
    : model_(model), hudRenderer_(new OpenCVHudRenderer(model)) {}

void OpenCVMarkersRenderer::render(const cv::Mat& frame, zarray_t* detections, const java::ArrayList<MarkerGroupPose>& groups) {
    preview_ = frame.clone();
    drawDetections(frame, detections);
    hudRenderer_->drawDetectedGroups(preview_, groups);
}

void OpenCVMarkersRenderer::displayFrame(const char* windowName) {
    cv::imshow(windowName, preview_);
}

void OpenCVMarkersRenderer::drawDetections(const cv::Mat& frame, zarray_t* detections) {
    const int n = zarray_size(detections);
    for (int i = 0; i < n; ++i) {
        apriltag_detection_t* det;
        zarray_get(detections, i, &det);

        for (int j = 0; j < 4; ++j) {
            cv::Point2f p1(static_cast<float>(det->p[j][0]),
                           static_cast<float>(det->p[j][1]));
            cv::Point2f p2(static_cast<float>(det->p[(j+1)%4][0]),
                           static_cast<float>(det->p[(j+1)%4][1]));
            cv::line(preview_, p1, p2, cv::Scalar(0, 255, 0), 2);
        }

        cv::Point2f center(static_cast<float>(det->c[0]),
                           static_cast<float>(det->c[1]));
        cv::circle(preview_, center, 6, cv::Scalar(0, 0, 255), -1);
        cv::circle(preview_, center, 8, cv::Scalar(0, 0, 255), 2);

        char label[16];
        std::snprintf(label, sizeof(label), "ID:%d", det->id);
        cv::putText(preview_, label, center + cv::Point2f(8, -8),
                    cv::FONT_HERSHEY_SIMPLEX, 1.2, cv::Scalar(0, 0, 255), 2);
    }
}
