#ifndef __STL_WRITER__
#define __STL_WRITER__

#include "java/io/OutputStream.h"
#include "vsdk/toolkit/io/PersistenceElement.h"

class PolyhedralBoundedSolid;

/**
Exports a `PolyhedralBoundedSolid` to binary STL.

STL carries no unit metadata. Callers targeting slicers that assume
millimetres should use the `scaleFactor` overload to convert from the
model's internal units.

C++ counterpart of Java's `vsdk.toolkit.io.geometry.stl.StlWriter`. As in
Java, the whole file is built in memory first, so nothing is written to
the stream when the export is rejected.
*/
class StlWriter : public PersistenceElement {
public:
    /**
    @throws java::Exception (IllegalArgumentException, IllegalStateException)
    when the solid cannot be exported
    */
    static void exportSolid(PolyhedralBoundedSolid* solid,
                            java::OutputStream* outputStream);

    static void exportSolid(PolyhedralBoundedSolid* solid,
                            java::OutputStream* outputStream,
                            double scaleFactor);

private:
    static const char* const HEADER_LABEL;
    static const int STL_BINARY_HEADER_LENGTH = 80;

    StlWriter() {}
};

#endif
