#include <cstring>

#include "model/LabelsModel.h"
LabelsModel::LabelsModel()
    : count(0),
      labelSizeMm(0.0),
      circleHoledRadiusMm(0.0),
      labels(nullptr) {
    outputPdf[0] = '\0';
}

LabelsModel::~LabelsModel() {
}

int LabelsModel::getCount() const {
    return count;
}

double LabelsModel::getLabelSizeMm() const {
    return labelSizeMm;
}

double LabelsModel::getCircleHoledRadiusMm() const {
    return circleHoledRadiusMm;
}

const char* LabelsModel::getOutputPdf() const {
    return outputPdf;
}

java::ArrayList<Label*>* LabelsModel::getLabels() const {
    return labels;
}

void LabelsModel::setCount(int count) {
    this->count = count;
}

void LabelsModel::setLabelSizeMm(double labelSizeMm) {
    this->labelSizeMm = labelSizeMm;
}

void LabelsModel::setCircleHoledRadiusMm(double circleHoledRadiusMm) {
    this->circleHoledRadiusMm = circleHoledRadiusMm;
}

void LabelsModel::setOutputPdf(const char* outputPdf) {
    std::strncpy(this->outputPdf, outputPdf, OUTPUT_PDF_MAX_LENGTH - 1);
    this->outputPdf[OUTPUT_PDF_MAX_LENGTH - 1] = '\0';
}

void LabelsModel::setLabels(java::ArrayList<Label*>* labels) {
    this->labels = labels;
}
