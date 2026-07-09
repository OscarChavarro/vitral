#ifndef __MARKER_GROUP__
#define __MARKER_GROUP__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "model/Marker.hpp"

class MarkerGroup {
public:
    java::String label;
    ColorRgb color;
    double physicalSideLength = 0.035;
    java::ArrayList<Marker> markers;

    bool containsMarkerId(int id) const {
        Marker unused;
        return findMarkerById(id, &unused);
    }

    bool findMarkerById(int id, Marker* outMarker) const {
        if (outMarker == nullptr) return false;
        for (long i = 0; i < markers.size(); ++i) {
            Marker m = markers.get(i);
            if (m.id == id) {
                *outMarker = m;
                return true;
            }
        }
        return false;
    }
};

#endif
