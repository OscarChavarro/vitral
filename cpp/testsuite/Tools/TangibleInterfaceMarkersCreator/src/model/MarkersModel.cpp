#include <cstring>

#include "model/MarkersModel.h"
MarkersModel::MarkersModel()
    : startId_(0),
      count_(0),
      markerSizeMm_(0.0),
      markers_(nullptr) {
    outputPdf_[0] = '\0';
}

MarkersModel::~MarkersModel() {
}

int MarkersModel::getStartId() const {
    return startId_;
}

int MarkersModel::getCount() const {
    return count_;
}

double MarkersModel::getMarkerSizeMm() const {
    return markerSizeMm_;
}

const char* MarkersModel::getOutputPdf() const {
    return outputPdf_;
}

java::ArrayList<Marker*>* MarkersModel::getMarkers() const {
    return markers_;
}

void MarkersModel::setStartId(int startId) {
    startId_ = startId;
}

void MarkersModel::setCount(int count) {
    count_ = count;
}

void MarkersModel::setMarkerSizeMm(double markerSizeMm) {
    markerSizeMm_ = markerSizeMm;
}

void MarkersModel::setOutputPdf(const char* outputPdf) {
    std::strncpy(outputPdf_, outputPdf, OUTPUT_PDF_MAX_LENGTH - 1);
    outputPdf_[OUTPUT_PDF_MAX_LENGTH - 1] = '\0';
}

void MarkersModel::setMarkers(java::ArrayList<Marker*>* markers) {
    markers_ = markers;
}
