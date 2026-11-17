package vsdk.toolkit.environment.geometry.surface;
import java.io.Serial;

// Java basic classes

// VitralSDK classes
import vsdk.toolkit.environment.geometry.element.Vertex;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayHit;

public class TriangleStripMesh extends Surface {

//= Class attributes ========================================================

    @Serial private static final long serialVersionUID = 20060807L;

    // Basic mesh data model
    private String name = "default";
    private Vertex[] vertexes;
    private int[][] strips;

//= Basic class management methods ==========================================

    public TriangleStripMesh() {
    }

    /** Needed for supplying the Geometry.getMinMax operation */
    private double[] calculateMinMaxPositions() {
        double[] minMax = new double[6];

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        int i;

        for ( i = 0; i < vertexes.length; i++ ) {
            double x = vertexes[i].getPosition().x();
            double y = vertexes[i].getPosition().y();
            double z = vertexes[i].getPosition().z();

            if ( x < minX ) minX = x;
            if ( y < minY ) minY = y;
            if ( z < minZ ) minZ = z;
            if ( x > maxX ) maxX = x;
            if ( y > maxY ) maxY = y;
            if ( z > maxZ ) maxZ = z;
        }
        minMax[0] = minX;
        minMax[1] = minY;
        minMax[2] = minZ;
        minMax[3] = maxX;
        minMax[4] = maxY;
        minMax[5] = maxZ;
        return minMax;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.getMinMax.
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    @Override
    public double[] getMinMax() {
        return calculateMinMaxPositions();
    }

    public Vertex[] getVertexes() {
        return this.vertexes;
    }

    public Vertex getVertexAt(int index) {
        return this.vertexes[index];
    }

    public void setVertexes(Vertex[] vertexes) {
        this.vertexes = vertexes;
    }

    public void setStrips(int indexes[][])
    {
        this.strips = indexes;
    }

    public int[][] getStrips()
    {
        return strips;
    }

    /**
    The triangles of the strips, as a triangle mesh sharing the vertex
    positions: in each strip, every vertex after the first two closes a
    triangle with the two before it, alternating their order so all the
    triangles keep the orientation of the first one. Triangles with a
    repeated vertex (used to join strips) are skipped.
    @return a new mesh with positions and triangles only, or null if there
    are no vertexes or strips
    */
    public TriangleMesh exportToTriangleMesh()
    {
        if ( vertexes == null || strips == null ) {
            return null;
        }
        TriangleMesh mesh = new TriangleMesh();
        double[] positions;
        int[] indexes;
        int count = 0;
        int i;
        int j;

        for ( i = 0; i < strips.length; i++ ) {
            if ( strips[i] != null && strips[i].length > 2 ) {
                count += strips[i].length - 2;
            }
        }
        mesh.initVertexPositionsArray(vertexes.length);
        positions = mesh.getVertexPositions();
        for ( i = 0; i < vertexes.length; i++ ) {
            positions[3*i] = vertexes[i].getPosition().x();
            positions[3*i+1] = vertexes[i].getPosition().y();
            positions[3*i+2] = vertexes[i].getPosition().z();
        }
        int[] all = new int[3*count];
        count = 0;
        for ( i = 0; i < strips.length; i++ ) {
            int[] strip = strips[i];
            for ( j = 2; strip != null && j < strip.length; j++ ) {
                int p0 = (j % 2 == 0) ? strip[j-2] : strip[j-1];
                int p1 = (j % 2 == 0) ? strip[j-1] : strip[j-2];
                int p2 = strip[j];
                if ( p0 == p1 || p1 == p2 || p0 == p2 ) {
                    continue;
                }
                all[3*count] = p0;
                all[3*count+1] = p1;
                all[3*count+2] = p2;
                count++;
            }
        }
        mesh.initTriangleArrays(count);
        indexes = mesh.getTriangleIndexes();
        System.arraycopy(all, 0, indexes, 0, 3*count);
        return mesh;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersectionFirstHit.

    \todo  Method not implemented!
    @param inOut_Ray
    @return true if given ray intersects current TriangleStripMesh
    */
    public Ray
    doIntersectionFirstHit(Ray inOut_Ray) {
        return null;
    }

    @Override
    public boolean doIntersectionFirstHit(Ray inRay, RayHit outHit)
    {
        return false;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.

    \todo  Method not implemented!
    @param inT
    */
    public void
    doExtraInformation(Ray inRay, double inT,
                                   RayHit outData) {

    }
}
