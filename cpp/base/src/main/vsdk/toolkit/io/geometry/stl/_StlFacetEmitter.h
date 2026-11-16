#ifndef __STL_FACET_EMITTER__
#define __STL_FACET_EMITTER__

#include "java/io/OutputStream.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

/**
Writes the facets of a binary STL file. Internal collaborator of
`StlWriter`.
*/
class _StlFacetEmitter {
public:
    class Facet {
    public:
        Vector3Dd normal;
        Vector3Dd vertex0;
        Vector3Dd vertex1;
        Vector3Dd vertex2;

        Facet() {}

        Facet(const Vector3Dd& normal, const Vector3Dd& a, const Vector3Dd& b,
              const Vector3Dd& c)
            : normal(normal), vertex0(a), vertex1(b), vertex2(c)
        {
        }
    };

    /**
    @param name label of the header; null for none
    */
    static void writeBinaryStl(java::OutputStream& outputStream,
                               const java::String* name,
                               const java::ArrayList<Facet>& facets,
                               double scaleFactor);

private:
    _StlFacetEmitter() {}

    static void writeVector(java::OutputStream& outputStream,
                            const Vector3Dd& vector,
                            double scaleFactor,
                            const java::String& fieldName,
                            int facetIndex);
    static void writeFloat(java::OutputStream& outputStream,
                           double value,
                           const java::String& fieldName,
                           int facetIndex);
    static java::String buildHeader(const java::String* name);
};

#endif
