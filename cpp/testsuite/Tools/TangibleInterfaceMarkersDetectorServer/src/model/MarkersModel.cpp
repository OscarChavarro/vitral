#include "java/util/ArrayList.txx"
#include "model/MarkersModel.hpp"
MarkersModel::MarkersModel()
    : tracker_(nullptr), running_(false), previewOperationMode_(SINGLE_MARKER),
      yawTest_(0), pitchTest_(0), rollTest_(0), markerIdTest_(3) {}
