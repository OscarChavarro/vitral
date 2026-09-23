#include "java/util/ArrayList.txx"
#include "model/MarkersModel.hpp"
MarkersModel::MarkersModel()
    : markerTracker(nullptr), running(false), previewOperationMode(SINGLE_MARKER),
      yawTest(0), pitchTest(0), rollTest(0), markerIdTest(3) {}
