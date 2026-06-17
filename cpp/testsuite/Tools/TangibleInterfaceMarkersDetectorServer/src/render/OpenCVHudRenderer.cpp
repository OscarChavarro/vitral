#include "java/util/ArrayList.txx"
#include "model/MarkersModel.hpp"
#include "render/OpenCVHudRenderer.hpp"
#include <opencv2/imgproc.hpp>
OpenCVHudRenderer::OpenCVHudRenderer(MarkersModel* model)
    : model_(model) {}

void OpenCVHudRenderer::drawDetectedGroups(cv::Mat& canvas, const java::ArrayList<MarkerGroupPose>& groups) {
    int y = 32;
    for (long i = 0; i < groups.size(); ++i) {
        const MarkerGroupPose& gp = groups.get(i);
        cv::Scalar color(84, 84, 84);
        for (long j = 0; j < model_->getMarkerGroups().size(); ++j) {
            MarkerGroup g = model_->getMarkerGroups().get(j);
            if (g.label == gp.label) {
                color = cv::Scalar(
                    static_cast<int>(g.color.b() * 255.0),
                    static_cast<int>(g.color.g() * 255.0),
                    static_cast<int>(g.color.r() * 255.0));
                break;
            }
        }
        cv::putText(canvas, gp.label.c_str(), cv::Point2f(16.0f, static_cast<float>(y)),
                    cv::FONT_HERSHEY_SIMPLEX, 0.9, color, 2);
        y += 28;
    }
}
