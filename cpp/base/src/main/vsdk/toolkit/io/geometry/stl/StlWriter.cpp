#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/io/geometry/stl/StlWriter.h"

#include "java/lang/Double.h"
#include "java/lang/IllegalArgumentException.h"
#include "vsdk/toolkit/io/geometry/stl/_StlFaceTriangulator.h"
#include "vsdk/toolkit/io/geometry/stl/_StlFacetEmitter.h"
#include "vsdk/toolkit/io/geometry/stl/_StlSolidValidator.h"

const char* const StlWriter::HEADER_LABEL = "VitralSolid";

namespace {

/**
C++ port of the `java.io.ByteArrayOutputStream` used by Java's
`StlWriter`: the file is assembled in memory and copied to the target
stream only when it is complete (`writeTo`).
*/
class _StlByteArrayOutputStream : public java::OutputStream {
public:
    java::ArrayList<unsigned char> bytes;

    explicit _StlByteArrayOutputStream(long int initialSize)
    {
        bytes.reserve(initialSize);
    }

    virtual void write(int value) override
    {
        bytes.add((unsigned char)(value & 0xFF));
    }

    virtual void write(const unsigned char* buffer, int offset,
                       int length) override
    {
        int i;
        for ( i = 0; i < length; i++ ) {
            bytes.add(buffer[offset + i]);
        }
    }

    virtual void flush() override {}
    virtual void close() override {}

    void writeTo(java::OutputStream* out)
    {
        if ( bytes.size() > 0 ) {
            out->write(bytes.data(), 0, (int)bytes.size());
        }
    }
};

}

void StlWriter::exportSolid(PolyhedralBoundedSolid* solid,
                            java::OutputStream* outputStream)
{
    exportSolid(solid, outputStream, 1.0);
}

void StlWriter::exportSolid(PolyhedralBoundedSolid* solid,
                            java::OutputStream* outputStream,
                            double scaleFactor)
{
    if ( solid == nullptr ) {
        throw java::IllegalArgumentException("solid is null");
    }
    if ( outputStream == nullptr ) {
        throw java::IllegalArgumentException("outputStream is null");
    }
    if ( !java::Double::isFinite(scaleFactor) || scaleFactor <= 0.0 ) {
        throw java::IllegalArgumentException(
            "scaleFactor must be finite and > 0");
    }

    _StlSolidValidator::validate(solid);
    java::ArrayList<_StlFacetEmitter::Facet> facets =
        _StlFaceTriangulator::triangulateSolid(solid);

    _StlByteArrayOutputStream buffer(
        STL_BINARY_HEADER_LENGTH + 4 + facets.size() * 50);
    java::String headerLabel(HEADER_LABEL);
    _StlFacetEmitter::writeBinaryStl(buffer, &headerLabel, facets, scaleFactor);
    buffer.writeTo(outputStream);
}
