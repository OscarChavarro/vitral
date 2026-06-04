package vsdk.toolkit.io.geometry.stl;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.io.PersistenceElement;

final class _StlFacetEmitter
{
    static final class Facet {
        final Vector3Dd normal;
        final Vector3Dd a;
        final Vector3Dd b;
        final Vector3Dd c;

        Facet(Vector3Dd normal, Vector3Dd a, Vector3Dd b, Vector3Dd c)
        {
            this.normal = normal;
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    private _StlFacetEmitter()
    {
    }

    static void writeBinaryStl(OutputStream outputStream,
                               String name,
                               List<Facet> facets,
                               double scaleFactor)
        throws Exception
    {
        byte[] header = new byte[80];
        byte[] labelBytes = buildHeader(name).getBytes(StandardCharsets.US_ASCII);
        int copyLength = Math.min(header.length, labelBytes.length);
        System.arraycopy(labelBytes, 0, header, 0, copyLength);
        PersistenceElement.writeBytes(outputStream, header);
        PersistenceElement.writeLongLE(outputStream, facets.size());

        int i;
        for ( i = 0; i < facets.size(); i++ ) {
            Facet facet = facets.get(i);
            writeVector(outputStream, facet.normal, 1.0, "normal", i);
            writeVector(outputStream, facet.a, scaleFactor, "vertex a", i);
            writeVector(outputStream, facet.b, scaleFactor, "vertex b", i);
            writeVector(outputStream, facet.c, scaleFactor, "vertex c", i);
            PersistenceElement.writeSignedShortLE(outputStream, 0);
        }
    }

    private static void writeVector(OutputStream outputStream,
                                    Vector3Dd vector,
                                    double scaleFactor,
                                    String fieldName,
                                    int facetIndex)
        throws Exception
    {
        writeFloat(outputStream, vector.x() * scaleFactor, fieldName, facetIndex);
        writeFloat(outputStream, vector.y() * scaleFactor, fieldName, facetIndex);
        writeFloat(outputStream, vector.z() * scaleFactor, fieldName, facetIndex);
    }

    private static void writeFloat(OutputStream outputStream,
                                   double value,
                                   String fieldName,
                                   int facetIndex)
        throws Exception
    {
        if ( !Double.isFinite(value) ||
             value < -Float.MAX_VALUE || value > Float.MAX_VALUE ) {
            throw new IllegalStateException(
                "STL export rejected: facet " + facetIndex + " " + fieldName
                + " cannot be represented as 32-bit float (" + value + ")");
        }
        PersistenceElement.writeFloatLE(outputStream, (float)value);
    }

    private static String buildHeader(String name)
    {
        if ( name == null || name.isBlank() ) {
            return "Vitral STL binary";
        }
        return "Vitral STL binary: " + name.trim();
    }
}
