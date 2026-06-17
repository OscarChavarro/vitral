#include "java/util/ArrayList.txx"
#include "render/OpenCVMarkerGroupRenderer.hpp"
#include <opencv2/imgproc.hpp>
OpenCVMarkerGroupRenderer::OpenCVMarkerGroupRenderer(MarkersModel* model)
    : model_(model) {}

bool OpenCVMarkerGroupRenderer::project(const Vector3Dd& p, double fx, double fy, double cx, double cy, cv::Point2f* out) const {
    if (out == nullptr) return false;
    if (p.z() <= 1e-6) return false;
    out->x = static_cast<float>(fx * p.x() / p.z() + cx);
    out->y = static_cast<float>(fy * p.y() / p.z() + cy);
    return true;
}

double OpenCVMarkerGroupRenderer::computeAxisLength(const MarkerGroup& group) const {
    if (group.markers.size() <= 0) return 0.02;
    double sum = 0.0;
    for (long i = 0; i < group.markers.size(); ++i) {
        Marker m = group.markers.get(i);
        sum += m.position.length();
    }
    double avg = sum / static_cast<double>(group.markers.size());
    if (avg <= 1e-9) return 0.02;
    return avg;
}

void OpenCVMarkerGroupRenderer::drawGroupGizmos(cv::Mat& canvas,
                                                 const java::ArrayList<MarkerGroupPose>& groups,
                                                 double fx, double fy, double cx, double cy) {
    for (long i = 0; i < groups.size(); ++i) {
        const MarkerGroupPose& gp = groups.get(i);

        MarkerGroup group;
        bool found = false;
        for (long j = 0; j < model_->getMarkerGroups().size(); ++j) {
            MarkerGroup g = model_->getMarkerGroups().get(j);
            if (g.label == gp.label) {
                group = g;
                found = true;
                break;
            }
        }
        if (!found) continue;

        double axisLength = computeAxisLength(group);

        Vector3Dd origin3 = gp.position;
        Vector3Dd x3 = gp.position.add(gp.rotation.rotate(Vector3Dd(axisLength, 0.0, 0.0)));
        Vector3Dd y3 = gp.position.add(gp.rotation.rotate(Vector3Dd(0.0, axisLength, 0.0)));
        Vector3Dd z3 = gp.position.add(gp.rotation.rotate(Vector3Dd(0.0, 0.0, axisLength)));

        cv::Point2f origin2, x2, y2, z2;
        if (!project(origin3, fx, fy, cx, cy, &origin2)) continue;

        cv::circle(canvas, origin2, 5, cv::Scalar(0, 0, 255), -1);
        if (project(x3, fx, fy, cx, cy, &x2)) {
            cv::line(canvas, origin2, x2, cv::Scalar(0, 0, 255), 2);
        }
        if (project(y3, fx, fy, cx, cy, &y2)) {
            cv::line(canvas, origin2, y2, cv::Scalar(0, 255, 0), 2);
        }
        if (project(z3, fx, fy, cx, cy, &z2)) {
            cv::line(canvas, origin2, z2, cv::Scalar(255, 0, 0), 2);
        }
    }
}
