#include <cstring>

#include "model/MarkersModel.h"
MarkersModel::MarkersModel()
    : startId(0),
      count(0),
      markerSizeMm(0.0),
      markers(nullptr) {
    outputPdf[0] = '\0';
}

MarkersModel::~MarkersModel() {
}

int MarkersModel::getStartId() const {
    return startId;
}

int MarkersModel::getCount() const {
    return count;
}

double MarkersModel::getMarkerSizeMm() const {
    return markerSizeMm;
}

const char* MarkersModel::getOutputPdf() const {
    return outputPdf;
}

java::ArrayList<Marker*>* MarkersModel::getMarkers() const {
    return markers;
}

void MarkersModel::setStartId(int startId) {
    this->startId = startId;
}

void MarkersModel::setCount(int count) {
    this->count = count;
}

void MarkersModel::setMarkerSizeMm(double markerSizeMm) {
    this->markerSizeMm = markerSizeMm;
}

void MarkersModel::setOutputPdf(const char* outputPdf) {
    std::strncpy(this->outputPdf, outputPdf, OUTPUT_PDF_MAX_LENGTH - 1);
    this->outputPdf[OUTPUT_PDF_MAX_LENGTH - 1] = '\0';
}

void MarkersModel::setMarkers(java::ArrayList<Marker*>* markers) {
    this->markers = markers;
}
