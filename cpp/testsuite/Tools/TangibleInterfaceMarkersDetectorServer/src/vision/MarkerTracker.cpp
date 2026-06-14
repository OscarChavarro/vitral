#include <cmath>
#include <cstdio>

#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/videoio.hpp>
#include <opencv2/calib3d.hpp>
#include <opencv2/highgui.hpp>

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include <apriltag.h>
#include <tag36h11.h>
#include <apriltag_pose.h>
#include <common/matd.h>
#include "java/util/ArrayList.txx"

#include "vision/MarkerTracker.hpp"
#include "model/MarkersModel.hpp"
#include "render/OpenCVMarkerGroupRenderer.hpp"

Matrix4x4d MarkerTracker::buildAprilToModelRotation() const {
    // Maps model marker frame (normal +X, up +Z) into AprilTag local frame
    // (x right, y down, z into tag).
    Matrix4x4d r;
    r = r.withVal(0,0, 0.0).withVal(0,1, 1.0).withVal(0,2, 0.0).withVal(0,3, 0.0)
         .withVal(1,0, 0.0).withVal(1,1, 0.0).withVal(1,2,-1.0).withVal(1,3, 0.0)
         .withVal(2,0,-1.0).withVal(2,1, 0.0).withVal(2,2, 0.0).withVal(2,3, 0.0)
         .withVal(3,0, 0.0).withVal(3,1, 0.0).withVal(3,2, 0.0).withVal(3,3, 1.0);
    return r;
}

MarkerTracker::MarkerTracker(const MarkerTrackerConfig& cfg, MarkersModel* model, MarkerEventBus* bus)
    : cfg_(cfg), model_(model), bus_(bus),
      fx_(600), fy_(600), cx_(320), cy_(240),
      calibrated_(false), thread_(0), running_(false) {
    dist_.clear();
    for (int i = 0; i < 5; ++i) {
        dist_.add(0.0);
    }
}

MarkerTracker::~MarkerTracker() { stop(); }

double MarkerTracker::resolveTagSize(int markerId) const {
    MarkerGroup group;
    if (model_ != nullptr && model_->findGroupByMarkerId(markerId, &group)) {
        return group.physicalSideLength;
    }
    return cfg_.markerSize;
}

bool MarkerTracker::loadCalibration() {
    if (cfg_.calibFile.empty()) return false;
    cv::FileStorage fs(cfg_.calibFile.c_str(), cv::FileStorage::READ);
    if (fs.isOpened()) {
        cv::Mat K, D;
        fs["camera_matrix"] >> K;
        fs["distortion_coefficients"] >> D;
        if (!K.empty() && K.rows == 3 && K.cols == 3) {
            fx_ = K.at<double>(0,0); fy_ = K.at<double>(1,1);
            cx_ = K.at<double>(0,2); cy_ = K.at<double>(1,2);
            dist_.clear();
    for (int i = 0; i < 5; ++i) {
        dist_.add(0.0);
    }
            for (int i = 0; i < (int)D.total() && i < 5; ++i)
                dist_[i] = D.at<double>(i);
            return true;
        }
    }
    FILE* in = std::fopen(cfg_.calibFile.c_str(), "r");
    if (in) {
        double k1=0,k2=0,p1=0,p2=0,k3=0;
        if (std::fscanf(in, "%lf %lf %lf %lf %lf %lf %lf %lf %lf", &fx_, &fy_, &cx_, &cy_, &k1, &k2, &p1, &p2, &k3) == 9) {
            dist_[0]=k1; dist_[1]=k2; dist_[2]=p1; dist_[3]=p2; dist_[4]=k3;
            std::fclose(in);
            return true;
        }
        std::fclose(in);
    }
    std::fprintf(stderr, "[marker_tracker] could not parse %s; using defaults\n", cfg_.calibFile.c_str());
    return false;
}

bool MarkerTracker::start() {
    calibrated_ = loadCalibration();
    running_ = true;
    if (pthread_create(&thread_, NULL, &MarkerTracker::threadEntry, this) != 0) {
        running_ = false;
        return false;
    }
    return true;
}

void MarkerTracker::stop() {
    if (running_) {
        running_ = false;
        pthread_join(thread_, NULL);
        thread_ = 0;
    }
}

void* MarkerTracker::threadEntry(void* self) {
    static_cast<MarkerTracker*>(self)->loop();
    return NULL;
}

void MarkerTracker::configureCapture(cv::VideoCapture& cap) {
    const double targetWidth = 640.0;
    const double targetHeight = 480.0;
    const double targetFps = 120.0;

    const bool widthSet = cap.set(cv::CAP_PROP_FRAME_WIDTH, targetWidth);
    const bool heightSet = cap.set(cv::CAP_PROP_FRAME_HEIGHT, targetHeight);
    const bool fpsSet = cap.set(cv::CAP_PROP_FPS, targetFps);
    const bool autofocusSet = cap.set(cv::CAP_PROP_AUTOFOCUS, 0.0);
    const double actualWidth = cap.get(cv::CAP_PROP_FRAME_WIDTH);
    const double actualHeight = cap.get(cv::CAP_PROP_FRAME_HEIGHT);
    const double actualFps = cap.get(cv::CAP_PROP_FPS);
    const double actualAutofocus = cap.get(cv::CAP_PROP_AUTOFOCUS);

    std::fprintf(
        stderr,
        "[marker_tracker] capture request width=%.0f(ok=%d) height=%.0f(ok=%d) fps=%.0f(ok=%d) autofocus=off(ok=%d)\n",
        targetWidth, widthSet ? 1 : 0,
        targetHeight, heightSet ? 1 : 0,
        targetFps, fpsSet ? 1 : 0,
        autofocusSet ? 1 : 0);
    std::fprintf(
        stderr,
        "[marker_tracker] capture actual  width=%.0f height=%.0f fps=%.2f autofocus=%.0f\n",
        actualWidth, actualHeight, actualFps, actualAutofocus);
    if (actualFps + 0.5 < targetFps) {
        std::fprintf(
            stderr,
            "[marker_tracker] warning requested fps=%.0f but actual fps=%.2f (camera/backend limit at this resolution)\n",
            targetFps, actualFps);
    }
}

void MarkerTracker::loop() {
    cv::VideoCapture cap(cfg_.cameraIndex);
    if (!cap.isOpened()) {
        std::fprintf(stderr, "[marker_tracker] cannot open camera %d\n", cfg_.cameraIndex);
        running_ = false;
        return;
    }
    configureCapture(cap);

    apriltag_family_t* tf = tag36h11_create();
    apriltag_detector_t* td = apriltag_detector_create();
    apriltag_detector_add_family(td, tf);
    td->quad_decimate = 1.0f;
    td->nthreads = 1;

    cv::Mat frame, gray;
    while (running_) {
        if (!cap.read(frame) || frame.empty()) continue;
        cv::cvtColor(frame, gray, cv::COLOR_BGR2GRAY);

        image_u8_t im = { gray.cols, gray.rows, (int32_t)gray.step, gray.data };
        zarray_t* dets = apriltag_detector_detect(td, &im);

        java::ArrayList<MarkerPose> markers;
        const int n = zarray_size(dets);
        for (int i = 0; i < n; ++i) {
            apriltag_detection_t* det;
            zarray_get(dets, i, &det);

            apriltag_detection_info_t info;
            info.det     = det;
            info.tagsize = resolveTagSize(det->id);
            info.fx = fx_; info.fy = fy_;
            info.cx = cx_; info.cy = cy_;

            apriltag_pose_t pose;
            estimate_tag_pose(&info, &pose);

            double tx = matd_get(pose.t, 0, 0);
            double ty = matd_get(pose.t, 1, 0);
            double tz = matd_get(pose.t, 2, 0);

            double* r = pose.R->data;
            double tr = r[0] + r[4] + r[8];
            double qw, qx, qy, qz;
            if (tr > 0) {
                double s = std::sqrt(tr + 1.0) * 2.0;
                qw = 0.25*s; qx=(r[7]-r[5])/s; qy=(r[2]-r[6])/s; qz=(r[3]-r[1])/s;
            } else if (r[0]>r[4] && r[0]>r[8]) {
                double s = std::sqrt(1.0+r[0]-r[4]-r[8])*2.0;
                qw=(r[7]-r[5])/s; qx=0.25*s; qy=(r[1]+r[3])/s; qz=(r[2]+r[6])/s;
            } else if (r[4]>r[8]) {
                double s = std::sqrt(1.0+r[4]-r[0]-r[8])*2.0;
                qw=(r[2]-r[6])/s; qx=(r[1]+r[3])/s; qy=0.25*s; qz=(r[5]+r[7])/s;
            } else {
                double s = std::sqrt(1.0+r[8]-r[0]-r[4])*2.0;
                qw=(r[3]-r[1])/s; qx=(r[2]+r[6])/s; qy=(r[5]+r[7])/s; qz=0.25*s;
            }

            Quaterniond qApril(Vector3Dd(qx, qy, qz), qw);
            Matrix4x4d tCameraApril = Matrix4x4d().importFromQuaternion(qApril.normalized())
                .withTranslation(Vector3Dd(tx, ty, tz));
            Matrix4x4d tAprilModel = buildAprilToModelRotation();
            Matrix4x4d tCameraModel = tCameraApril.multiply(tAprilModel);
            Quaterniond qModel = tCameraModel.withoutTranslation().exportToQuaternion().normalized();
            Vector3Dd pModel = tCameraModel.extractTranslation();

            MarkerPose mp;
            mp.markerId = det->id;
            mp.decisionMargin = det->decision_margin;
            mp.position  = Vector3Df(static_cast<float>(pModel.x()),
                                     static_cast<float>(pModel.y()),
                                     static_cast<float>(pModel.z()));
            mp.rotation  = Quaternionf(
                Vector3Df(static_cast<float>(qModel.direction().x()),
                         static_cast<float>(qModel.direction().y()),
                         static_cast<float>(qModel.direction().z())),
                static_cast<float>(qModel.magnitude())).normalized();
            mp.viewDot   = std::fabs(static_cast<float>(r[8]));

            matd_destroy(pose.R);
            matd_destroy(pose.t);
            markers.add(mp);
        }

        apriltag_detections_destroy(dets);

        java::ArrayList<MarkerGroupPose> groups =
            poser_.estimate(markers, model_->getMarkerGroups(),
                            cfg_.decisionMarginThreshold,
                            cfg_.viewAngleCosThreshold);
        bus_->publish(groups);
    }

    apriltag_detector_destroy(td);
    tag36h11_destroy(tf);
}

void MarkerTracker::runPreviewLoop() {
    cv::VideoCapture cap(cfg_.cameraIndex);
    if (!cap.isOpened()) {
        std::fprintf(stderr, "[marker_tracker] cannot open camera %d\n", cfg_.cameraIndex);
        return;
    }
    configureCapture(cap);
    std::fprintf(stderr, "[marker_tracker] preview mode - press 'q' or ESC to exit\n");

    apriltag_family_t* tf = tag36h11_create();
    apriltag_detector_t* td = apriltag_detector_create();
    apriltag_detector_add_family(td, tf);
    td->quad_decimate = 1.0f;
    td->nthreads = 1;

    loadCalibration();

    cv::Mat frame, gray;
    OpenCVMarkerGroupRenderer markerGroupRenderer(model_);
    int frameCount = 0;
    bool shouldExit = false;

    while (!shouldExit) {
        if (!cap.read(frame) || frame.empty()) continue;
        cv::cvtColor(frame, gray, cv::COLOR_BGR2GRAY);

        image_u8_t im = { gray.cols, gray.rows, (int32_t)gray.step, gray.data };
        zarray_t* dets = apriltag_detector_detect(td, &im);

        java::ArrayList<MarkerPose> markers;
        const int n = zarray_size(dets);
        for (int i = 0; i < n; ++i) {
            apriltag_detection_t* det;
            zarray_get(dets, i, &det);

            apriltag_detection_info_t info;
            info.det     = det;
            info.tagsize = resolveTagSize(det->id);
            info.fx = fx_; info.fy = fy_;
            info.cx = cx_; info.cy = cy_;

            apriltag_pose_t pose;
            estimate_tag_pose(&info, &pose);

            double tx = matd_get(pose.t, 0, 0);
            double ty = matd_get(pose.t, 1, 0);
            double tz = matd_get(pose.t, 2, 0);

            double* r = pose.R->data;
            double tr = r[0] + r[4] + r[8];
            double qw, qx, qy, qz;
            if (tr > 0) {
                double s = std::sqrt(tr + 1.0) * 2.0;
                qw = 0.25*s; qx=(r[7]-r[5])/s; qy=(r[2]-r[6])/s; qz=(r[3]-r[1])/s;
            } else if (r[0]>r[4] && r[0]>r[8]) {
                double s = std::sqrt(1.0+r[0]-r[4]-r[8])*2.0;
                qw=(r[7]-r[5])/s; qx=0.25*s; qy=(r[1]+r[3])/s; qz=(r[2]+r[6])/s;
            } else if (r[4]>r[8]) {
                double s = std::sqrt(1.0+r[4]-r[0]-r[8])*2.0;
                qw=(r[2]-r[6])/s; qx=(r[1]+r[3])/s; qy=0.25*s; qz=(r[5]+r[7])/s;
            } else {
                double s = std::sqrt(1.0+r[8]-r[0]-r[4])*2.0;
                qw=(r[3]-r[1])/s; qx=(r[2]+r[6])/s; qy=(r[5]+r[7])/s; qz=0.25*s;
            }

            Quaterniond qApril(Vector3Dd(qx, qy, qz), qw);
            Matrix4x4d tCameraApril = Matrix4x4d().importFromQuaternion(qApril.normalized())
                .withTranslation(Vector3Dd(tx, ty, tz));
            Matrix4x4d tAprilModel = buildAprilToModelRotation();
            Matrix4x4d tCameraModel = tCameraApril.multiply(tAprilModel);
            Quaterniond qModel = tCameraModel.withoutTranslation().exportToQuaternion().normalized();
            Vector3Dd pModel = tCameraModel.extractTranslation();

            MarkerPose mp;
            mp.markerId = det->id;
            mp.decisionMargin = det->decision_margin;
            mp.position  = Vector3Df(static_cast<float>(pModel.x()),
                                     static_cast<float>(pModel.y()),
                                     static_cast<float>(pModel.z()));
            mp.rotation  = Quaternionf(
                Vector3Df(static_cast<float>(qModel.direction().x()),
                         static_cast<float>(qModel.direction().y()),
                         static_cast<float>(qModel.direction().z())),
                static_cast<float>(qModel.magnitude())).normalized();
            mp.viewDot   = std::fabs(static_cast<float>(r[8]));

            matd_destroy(pose.R);
            matd_destroy(pose.t);
            markers.add(mp);
        }

        java::ArrayList<MarkerGroup> effectiveGroups = model_->getMarkerGroups();
        if (effectiveGroups.size() > 0) {
            MarkerGroup g0 = effectiveGroups.get(0);
            const int markerIdTest = model_->getMarkerIdTest();
            for (long mi = 0; mi < g0.markers.size(); ++mi) {
                Marker marker = g0.markers.get(mi);
                if (marker.id == markerIdTest) {
                    const double toRad = 3.14159265358979323846 / 180.0;
                    Matrix4x4d r = Matrix4x4d().eulerAnglesRotation(
                        model_->getYawTest() * toRad,
                        model_->getPitchTest() * toRad,
                        model_->getRollTest() * toRad);
                    marker.rotation = r.exportToQuaternion().normalized();
                    g0.markers.set(mi, marker);
                    break;
                }
            }
            effectiveGroups.set(0, g0);
        }

        java::ArrayList<MarkerGroupPose> groups =
            poser_.estimate(markers, effectiveGroups,
                            cfg_.decisionMarginThreshold,
                            cfg_.viewAngleCosThreshold);
        bus_->publish(groups);
        cv::Mat preview = frame.clone();
        PreviewOperationMode mode = model_->getPreviewOperationMode();
        if (mode == SINGLE_MARKER) {
            for (int i = 0; i < n; ++i) {
                apriltag_detection_t* det;
                zarray_get(dets, i, &det);
                MarkerGroup group;
                bool foundGroup = model_->findGroupByMarkerId(det->id, &group);
                cv::Scalar color(84, 84, 84);
                if (foundGroup) {
                    color = cv::Scalar(
                        static_cast<int>(group.color.b() * 255.0),
                        static_cast<int>(group.color.g() * 255.0),
                        static_cast<int>(group.color.r() * 255.0));
                }

                for (int j = 0; j < 4; ++j) {
                    cv::Point2f p1(static_cast<float>(det->p[j][0]),
                                   static_cast<float>(det->p[j][1]));
                    cv::Point2f p2(static_cast<float>(det->p[(j+1)%4][0]),
                                   static_cast<float>(det->p[(j+1)%4][1]));
                    cv::line(preview, p1, p2, color, 2);
                }

                cv::Point2f center(static_cast<float>(det->c[0]),
                                   static_cast<float>(det->c[1]));
                cv::circle(preview, center, 4, color, -1);

                char label[16];
                std::snprintf(label, sizeof(label), "%d", det->id);
                int baseline = 0;
                cv::Size textSize = cv::getTextSize(label, cv::FONT_HERSHEY_SIMPLEX, 1.2, 2, &baseline);
                cv::Point2f labelPos(center.x - static_cast<float>(textSize.width) * 0.5f, center.y - 8.0f);
                cv::putText(preview, label, labelPos, cv::FONT_HERSHEY_SIMPLEX, 1.2, color, 2);
            }

        } else {
            for (int i = 0; i < n; ++i) {
                apriltag_detection_t* det;
                zarray_get(dets, i, &det);
                MarkerGroup group;
                bool foundGroup = model_->findGroupByMarkerId(det->id, &group);
                if (foundGroup) {
                    continue;
                }
                cv::Scalar color(84, 84, 84);
                for (int j = 0; j < 4; ++j) {
                    cv::Point2f p1(static_cast<float>(det->p[j][0]),
                                   static_cast<float>(det->p[j][1]));
                    cv::Point2f p2(static_cast<float>(det->p[(j+1)%4][0]),
                                   static_cast<float>(det->p[(j+1)%4][1]));
                    cv::line(preview, p1, p2, color, 2);
                }
                cv::Point2f center(static_cast<float>(det->c[0]),
                                   static_cast<float>(det->c[1]));
                cv::circle(preview, center, 4, color, -1);
                char label[16];
                std::snprintf(label, sizeof(label), "%d", det->id);
                int baseline = 0;
                cv::Size textSize = cv::getTextSize(label, cv::FONT_HERSHEY_SIMPLEX, 1.2, 2, &baseline);
                cv::Point2f labelPos(center.x - static_cast<float>(textSize.width) * 0.5f, center.y - 8.0f);
                cv::putText(preview, label, labelPos, cv::FONT_HERSHEY_SIMPLEX, 1.2, color, 2);
            }

            markerGroupRenderer.drawGroupGizmos(preview, groups, fx_, fy_, cx_, cy_);
        }

        int hudY = 32;
        for (long i = 0; i < groups.size(); ++i) {
            const MarkerGroupPose& gp = groups.get(i);
            MarkerGroup group;
            bool foundGroupColor = false;
            for (long j = 0; j < model_->getMarkerGroups().size(); ++j) {
                MarkerGroup g = model_->getMarkerGroups().get(j);
                if (g.label == gp.label) {
                    group = g;
                    foundGroupColor = true;
                    break;
                }
            }
            cv::Scalar hudColor(84, 84, 84);
            if (foundGroupColor) {
                hudColor = cv::Scalar(
                    static_cast<int>(group.color.b() * 255.0),
                    static_cast<int>(group.color.g() * 255.0),
                    static_cast<int>(group.color.r() * 255.0));
            }
            cv::putText(preview, gp.label.c_str(), cv::Point2f(16.0f, static_cast<float>(hudY)),
                        cv::FONT_HERSHEY_SIMPLEX, 0.9, hudColor, 2);
            hudY += 28;
        }

        cv::imshow("tangibleInterfaceServer - Preview (press 'q' to quit)", preview);
        int key = cv::waitKey(1) & 0xFF;
        if (key == 'q' || key == 27) {
            MarkerGroupPose exitPose;
            exitPose.label = "exit";
            java::ArrayList<MarkerGroupPose> exitMsg;
            exitMsg.add(exitPose);
            bus_->publish(exitMsg);
            usleep(100000);
            shouldExit = true;
            cv::destroyAllWindows();
        } else if (key == ' ') {
            model_->cyclePreviewOperationMode();
            std::printf("[preview] mode switched to %s\n",
                        model_->getPreviewOperationMode() == SINGLE_MARKER ? "SINGLE_MARKER" : "MARKER_GROUP");
        } else if (key == '1') {
            model_->cycleYawTest();
            std::printf("<%d, %d, %d>\n",
                        model_->getYawTest(), model_->getPitchTest(), model_->getRollTest());
        } else if (key == '2') {
            model_->cyclePitchTest();
            std::printf("<%d, %d, %d>\n",
                        model_->getYawTest(), model_->getPitchTest(), model_->getRollTest());
        } else if (key == '3') {
            model_->cycleRollTest();
            std::printf("<%d, %d, %d>\n",
                        model_->getYawTest(), model_->getPitchTest(), model_->getRollTest());
        } else if (key == '4') {
            model_->cycleMarkerIdTest();
            std::printf("[test] markerIdTest=%d\n", model_->getMarkerIdTest());
        }

        if (model_->getPreviewOperationMode() == SINGLE_MARKER && frameCount % 30 == 0 && n > 0) {
            std::printf("[preview] frame %d: detected %d markers\n", frameCount, n);
            for (int i = 0; i < n; ++i) {
                apriltag_detection_t* det;
                zarray_get(dets, i, &det);
                std::printf("  [%d] id=%d margin=%.1f\n", i, det->id, det->decision_margin);
            }
        }

        apriltag_detections_destroy(dets);
    }

    cap.release();
    apriltag_detector_destroy(td);
    tag36h11_destroy(tf);
}
