#ifndef __MARKERSMODEL_H__
#define __MARKERSMODEL_H__

#include <java/util/ArrayList.h>

#include "model/Marker.h"

class MarkersModel {
  public:
    MarkersModel();
    ~MarkersModel();

    int getStartId() const;
    int getCount() const;
    double getMarkerSizeMm() const;
    const char* getOutputPdf() const;
    java::ArrayList<Marker*>* getMarkers() const;

    void setStartId(int startId);
    void setCount(int count);
    void setMarkerSizeMm(double markerSizeMm);
    void setOutputPdf(const char* outputPdf);
    void setMarkers(java::ArrayList<Marker*>* markers);

  private:
    static const int OUTPUT_PDF_MAX_LENGTH = 256;

    int startId_;
    int count_;
    double markerSizeMm_;
    char outputPdf_[OUTPUT_PDF_MAX_LENGTH];
    java::ArrayList<Marker*>* markers_;
};

#endif
