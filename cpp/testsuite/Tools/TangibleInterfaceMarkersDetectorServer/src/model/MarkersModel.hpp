#ifndef __MARKERS_MODEL__
#define __MARKERS_MODEL__

#include "java/util/ArrayList.h"
#include "model/MarkerGroup.hpp"
#include "model/PreviewOperationMode.hpp"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include <cmath>

class MarkerTracker;

class MarkersModel {
public:
    MarkersModel();

    void setMarkerTracker(MarkerTracker* markerTracker) { this->markerTracker = markerTracker; }
    MarkerTracker* getMarkerTracker() const { return markerTracker; }

    bool isRunning() const { return running; }
    void setRunning(bool running) { this->running = running; }

    void addMarkerGroup(const MarkerGroup& group) { markerGroups.add(group); }
    const java::ArrayList<MarkerGroup>& getMarkerGroups() const { return markerGroups; }
    PreviewOperationMode getPreviewOperationMode() const { return previewOperationMode; }
    void cyclePreviewOperationMode() {
        previewOperationMode = (previewOperationMode == SINGLE_MARKER) ? MARKER_GROUP : SINGLE_MARKER;
    }

    bool findGroupByMarkerId(int markerId, MarkerGroup* outGroup) const {
        if (outGroup == nullptr) return false;
        for (long i = 0; i < markerGroups.size(); ++i) {
            MarkerGroup g = markerGroups.get(i);
            if (g.containsMarkerId(markerId)) {
                *outGroup = g;
                return true;
            }
        }
        return false;
    }

    int getYawTest() const { return yawTest; }
    int getPitchTest() const { return pitchTest; }
    int getRollTest() const { return rollTest; }
    int getMarkerIdTest() const { return markerIdTest; }
    void cycleYawTest() { yawTest = cycleAngle90(yawTest); }
    void cyclePitchTest() { pitchTest = cycleAngle90(pitchTest); }
    void cycleRollTest() { rollTest = cycleAngle90(rollTest); }
    void cycleMarkerIdTest() {
        if (markerGroups.size() <= 0) return;
        MarkerGroup g0 = markerGroups.get(0);
        if (g0.markers.size() <= 0) return;
        long currentIdx = -1;
        for (long i = 0; i < g0.markers.size(); ++i) {
            Marker m = g0.markers.get(i);
            if (m.id == markerIdTest) {
                currentIdx = i;
                break;
            }
        }
        if (currentIdx < 0) {
            markerIdTest = g0.markers.get(0).id;
            return;
        }
        long nextIdx = (currentIdx + 1) % g0.markers.size();
        markerIdTest = g0.markers.get(nextIdx).id;
        syncTestsFromSelectedMarker();
    }

    void initializeTestsFromFirstMarkerGroup() {
        if (markerGroups.size() <= 0) return;
        MarkerGroup g0 = markerGroups.get(0);
        if (g0.markers.size() <= 0) return;
        markerIdTest = g0.markers.get(0).id;
        syncTestsFromSelectedMarker();
    }

private:
    double normalizeDeg(double deg) const {
        while (deg < 0.0) deg += 360.0;
        while (deg >= 360.0) deg -= 360.0;
        return deg;
    }

    int snapToRightAngle(double deg) const {
        const double nd = normalizeDeg(deg);
        const int candidates[4] = {0, 90, 180, 270};
        int best = 0;
        double bestDist = 1e9;
        for (int i = 0; i < 4; ++i) {
            double d = std::fabs(nd - static_cast<double>(candidates[i]));
            d = std::fmin(d, 360.0 - d);
            if (d < bestDist) {
                bestDist = d;
                best = candidates[i];
            }
        }
        return best;
    }

    void syncTestsFromSelectedMarker() {
        if (markerGroups.size() <= 0) return;
        MarkerGroup g0 = markerGroups.get(0);
        for (long i = 0; i < g0.markers.size(); ++i) {
            Marker m = g0.markers.get(i);
            if (m.id != markerIdTest) continue;
            Matrix4x4d r = Matrix4x4d().importFromQuaternion(m.rotation.normalized());
            const double toDeg = 180.0 / 3.14159265358979323846;
            yawTest = snapToRightAngle(r.obtainEulerYawAngle() * toDeg);
            pitchTest = snapToRightAngle(r.obtainEulerPitchAngle() * toDeg);
            rollTest = snapToRightAngle(r.obtainEulerRollAngle() * toDeg);
            return;
        }
    }

    int cycleAngle90(int angle) const {
        if (angle == 0) return 90;
        if (angle == 90) return 180;
        if (angle == 180) return 270;
        return 0;
    }

    MarkerTracker* markerTracker;
    bool running;
    PreviewOperationMode previewOperationMode;
    int yawTest;
    int pitchTest;
    int rollTest;
    int markerIdTest;
    java::ArrayList<MarkerGroup> markerGroups;
};

#endif
