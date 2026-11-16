#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/io/geometry/stl/_StlFacetEmitter.h"

#include <cstring>

#include "java/lang/Double.h"
#include "java/lang/Float.h"
#include "java/lang/IllegalStateException.h"
#include "vsdk/toolkit/io/PersistenceElement.h"

void _StlFacetEmitter::writeBinaryStl(java::OutputStream& outputStream,
                                      const java::String* name,
                                      const java::ArrayList<Facet>& facets,
                                      double scaleFactor)
{
    unsigned char header[80];
    std::memset(header, 0, sizeof(header));
    // Java: `getBytes(StandardCharsets.US_ASCII)`, which turns each
    // non-ASCII character into '?'
    java::String label = buildHeader(name);
    int labelLength = label.length();
    int copyLength = labelLength < 80 ? labelLength : 80;
    int i;
    for ( i = 0; i < copyLength; i++ ) {
        unsigned char c = (unsigned char)label.charAt(i);
        header[i] = c < 0x80 ? c : (unsigned char)'?';
    }
    PersistenceElement::writeBytes(outputStream, header, 80);
    PersistenceElement::writeLongLE(outputStream, (long)facets.size());

    for ( i = 0; i < (int)facets.size(); i++ ) {
        const Facet& facet = facets[i];
        writeVector(outputStream, facet.normal, 1.0, "normal", i);
        writeVector(outputStream, facet.vertex0, scaleFactor, "vertex a", i);
        writeVector(outputStream, facet.vertex1, scaleFactor, "vertex b", i);
        writeVector(outputStream, facet.vertex2, scaleFactor, "vertex c", i);
        PersistenceElement::writeSignedShortLE(outputStream, 0);
    }
}

void _StlFacetEmitter::writeVector(java::OutputStream& outputStream,
                                   const Vector3Dd& vector,
                                   double scaleFactor,
                                   const java::String& fieldName,
                                   int facetIndex)
{
    writeFloat(outputStream, vector.x() * scaleFactor, fieldName, facetIndex);
    writeFloat(outputStream, vector.y() * scaleFactor, fieldName, facetIndex);
    writeFloat(outputStream, vector.z() * scaleFactor, fieldName, facetIndex);
}

void _StlFacetEmitter::writeFloat(java::OutputStream& outputStream,
                                  double value,
                                  const java::String& fieldName,
                                  int facetIndex)
{
    if ( !java::Double::isFinite(value) ||
         value < -java::Float::MAX_VALUE || value > java::Float::MAX_VALUE ) {
        throw java::IllegalStateException(
            java::String("STL export rejected: facet ") +
            java::String::valueOf(facetIndex) + " " + fieldName +
            " cannot be represented as 32-bit float (" +
            java::Double::toString(value) + ")");
    }
    PersistenceElement::writeFloatLE(outputStream, (float)value);
}

java::String _StlFacetEmitter::buildHeader(const java::String* name)
{
    if ( name == nullptr || name->trim().isEmpty() ) {
        return "Vitral STL binary";
    }
    return java::String("Vitral STL binary: ") + name->trim();
}
