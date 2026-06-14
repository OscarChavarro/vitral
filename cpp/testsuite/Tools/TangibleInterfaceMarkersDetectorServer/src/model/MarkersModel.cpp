#include "model/MarkersModel.hpp"
#include "java/util/ArrayList.txx"

MarkersModel::MarkersModel()
    : tracker_(nullptr), running_(false), previewOperationMode_(SINGLE_MARKER),
      yawTest_(0), pitchTest_(0), rollTest_(0), markerIdTest_(3) {}
