#ifndef __XML_MANAGER__
#define __XML_MANAGER__

#include "java/lang/String.h"

class Geometry;

/**
Imports and exports the geometries with XML persistence (parametric curves
and bicubic patches).

C++ port note: the Java version works over `Object`s; here over
`Geometry`s, the common base of the supported entities.
*/
class XmlManager {
public:
    /**
    @param object geometry to export; only `ParametricCurve` and
    `ParametricBiCubicPatch` are written
    @param outputFilename XML file to write
    @param dtdFilename DTD referenced by the document
    */
    static void exportXml(Geometry* object, const java::String& outputFilename,
                          const java::String& dtdFilename);

    /**
    @param inputFilename XML file to read
    @return a new geometry owned by the caller, or null if the file could not
    be read or has no supported geometry
    */
    static Geometry* importXml(const java::String& inputFilename);
};

#endif
