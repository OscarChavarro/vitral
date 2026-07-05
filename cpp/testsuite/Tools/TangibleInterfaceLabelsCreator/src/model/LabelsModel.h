#ifndef __LABELSMODEL__
#define __LABELSMODEL__

#include <java/util/ArrayList.h>
#include "model/Label.h"
class LabelsModel {
  public:
    LabelsModel();
    ~LabelsModel();

    int getCount() const;
    double getLabelSizeMm() const;
    double getCircleHoledRadiusMm() const;
    const char* getOutputPdf() const;
    java::ArrayList<Label*>* getLabels() const;

    void setCount(int count);
    void setLabelSizeMm(double labelSizeMm);
    void setCircleHoledRadiusMm(double circleHoledRadiusMm);
    void setOutputPdf(const char* outputPdf);
    void setLabels(java::ArrayList<Label*>* labels);

  private:
    static const int OUTPUT_PDF_MAX_LENGTH = 256;

    int count_;
    double labelSizeMm_;
    double circleHoledRadiusMm_;
    char outputPdf_[OUTPUT_PDF_MAX_LENGTH];
    java::ArrayList<Label*>* labels_;
};

#endif
