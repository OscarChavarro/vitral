#include "model/LabelsModel.h"

#include <cstring>

LabelsModel::LabelsModel()
    : count_(0),
      labelSizeMm_(0.0),
      circleHoledRadiusMm_(0.0),
      labels_(nullptr) {
    outputPdf_[0] = '\0';
}

LabelsModel::~LabelsModel() {
}

int LabelsModel::getCount() const {
    return count_;
}

double LabelsModel::getLabelSizeMm() const {
    return labelSizeMm_;
}

double LabelsModel::getCircleHoledRadiusMm() const {
    return circleHoledRadiusMm_;
}

const char* LabelsModel::getOutputPdf() const {
    return outputPdf_;
}

java::ArrayList<Label*>* LabelsModel::getLabels() const {
    return labels_;
}

void LabelsModel::setCount(int count) {
    count_ = count;
}

void LabelsModel::setLabelSizeMm(double labelSizeMm) {
    labelSizeMm_ = labelSizeMm;
}

void LabelsModel::setCircleHoledRadiusMm(double circleHoledRadiusMm) {
    circleHoledRadiusMm_ = circleHoledRadiusMm;
}

void LabelsModel::setOutputPdf(const char* outputPdf) {
    std::strncpy(outputPdf_, outputPdf, OUTPUT_PDF_MAX_LENGTH - 1);
    outputPdf_[OUTPUT_PDF_MAX_LENGTH - 1] = '\0';
}

void LabelsModel::setLabels(java::ArrayList<Label*>* labels) {
    labels_ = labels;
}
