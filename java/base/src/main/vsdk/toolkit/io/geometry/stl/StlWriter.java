package vsdk.toolkit.io.geometry.stl;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

/**
Exports a `PolyhedralBoundedSolid` to binary STL.

STL carries no unit metadata. Callers targeting slicers that assume
millimetres should use the `scaleFactor` overload to convert from the
model's internal units.
*/
public class StlWriter extends PersistenceElement
{
    private static final String HEADER_LABEL = "VitralSolid";
    private static final int STL_BINARY_HEADER_LENGTH = 80;

    private StlWriter()
    {
    }

    public static void exportSolid(PolyhedralBoundedSolid solid,
                                   OutputStream outputStream)
        throws Exception
    {
        exportSolid(solid, outputStream, 1.0);
    }

    public static void exportSolid(PolyhedralBoundedSolid solid,
                                   OutputStream outputStream,
                                   double scaleFactor)
        throws Exception
    {
        if ( solid == null ) {
            throw new IllegalArgumentException("solid is null");
        }
        if ( outputStream == null ) {
            throw new IllegalArgumentException("outputStream is null");
        }
        if ( !Double.isFinite(scaleFactor) || scaleFactor <= 0.0 ) {
            throw new IllegalArgumentException(
                "scaleFactor must be finite and > 0");
        }

        _StlSolidValidator.validate(solid);
        List<_StlFacetEmitter.Facet> facets =
            _StlFaceTriangulator.triangulateSolid(solid);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream(
            STL_BINARY_HEADER_LENGTH + 4 + facets.size() * 50);
        _StlFacetEmitter.writeBinaryStl(buffer, HEADER_LABEL, facets, scaleFactor);
        buffer.writeTo(outputStream);
    }
}
