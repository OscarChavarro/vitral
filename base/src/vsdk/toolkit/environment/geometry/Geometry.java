//===========================================================================
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [APPE1967] Appel, Arthur. "The notion of quantitative invisivility and  =
//=          the machine rendering of solids". Proceedings, ACM National    =
//=          meeting 1967.                                                  =
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.gui.ProgressMonitor;

/**
Every geometric entity (prefer not to call it "Object", for not confusing
it with the Object Oriented Programming concept or the base Java superclass)
need to describe its form, its geometric transformation, material and other
properties. The sub-classes of Geometry are responsible of representing
the form only, supossing that this form is located in its own origin, 
without rotation or scaling.<P>

This abstract class defines the required basic operations that must be
supplied by all geometries in order to be managed uniformly by every
geometric algorithm, including but not limited to rendering operations.<P>
*/
public abstract class Geometry extends Entity {

    public static final int INSIDE = 1;
    public static final int LIMIT = 0;
    public static final int OUTSIDE = -1;

    /**
    Takes in a Ray and checks wheter or not that Ray intersects the current 
    Geometry.  The coordinates of the Ray are interpreted as global 
    coordinates, and the current Geometry is supposed to be located in 
    coordinates around the origin, without  rotation or scaling.<P>

    USES/APPLICATIONS:
    This method is one of the most fundamental and important of the whole
    VSDK toolkit. All Surface or Solid Geometry must implement it in order
    to support a great variety of applications, which include but are not
    limited to:
    <UL>
        <LI> Rendering scenes using Raytracing or Raycasting like methods.
        Currently, the class Raytracer makes use of this operation for 
        visualizing scenes.
        <LI> Selecting objects interactively from a mouse click specified
        by a user, in cooperation with the Camera.generateRay method.
        <LI> Implementing simple forms of collision detection in the case
        of particle / Geometry interaction, and posibly as an aid in other
        more complexes cases of collision detection. 
        <LI> Collision detection of a Ray generated from the position of an 
        object and pointing in the direction of a gravity field, to measure
        altitude over a terrain.
        <LI> Partial implementation of the more complex operation of
        doRayClassification which is defined only in the Solid subclass,
        and could be useful for volumetric rendering and constructive
        solid geometry (CSG).
        <LI> Ray path following in an scene. Beside the Raycasting case for
        visualization, this could be useful in the modelling of interactions
        between sound or electromagnetic waves and its environment. This
        could be used to model problem like radar/sonar or auralization and
        sound spacialization.
        <LI>As part of skinning operations, where a "skin" Geometry is
        modified to fit an underlying base "skeleton" or "muscle" Geometry, 
        this operation could be used to calculate the nearest base point.
    </UL><P>

    ILLUSTRATION: As a path following example take into account the red Sphere
    and the gray Box shown in the figure. From the point indicated by the green
    Sphere emanates a Ray in the direction shown by the cyan Arrow. The 
    Geometry.doIntersection method returns true the first time is called,
    and the Ray is modified to have the distance between the Ray origin and
    the red Sphere surface in its Ray.t attribute. From this operation,
    the Geometry.doExtraInformation method could be used to recall the
    normal Vector3D of the red Sphere, which is shown in yellow. Applying a
    reflection rule (equal incident and reflection angles respective to
    the normal) a second ray can be generated and the process repeated.<P>
        <IMG SRC="../images/Geometry.doIntersection_1.jpg"></IMG><P>


    @param inOut_ray It is used both ways: as an input value and as an output
    returned value. As input parameter, it must specify the ray origin and
    ray direction in the two internal Vector3D fields, it's scalar
    distance value Ray.t is not relevant.  As output value, the origin and 
    direction of the Ray are not altered, but the scalar distance value Ray.t 
    could change if the input Ray intersects current geometry.
    @return If the specified input Ray intersects current Geometry, true
    value is returned, otherwise false is returned.
    */
    public abstract boolean doIntersection(Ray inOut_ray);

    /**
    This method returns the number of front facing surface elements (with
    respect to `origin`) between the `origin` point and the `p` point. The
    internal working of this method is usually related with the
    `doIntersection` method.

    This operation is used in queries as the specified on [APPE1967]. Note,
    that as stated on [APPE1967]: "...A bounded surface hides a point when the
    line of sight to that point pierces the surface within the surface
    boundaries, and the piercing point is closer to the observer than the
    point being tested for visibility...". Acording to that definition:
    @param origin contains the "observer" position.
    @param p is the "point being tested for visibility".

    @return The number of surfaces within current Geometry hiding giving point.
    Default (empty) implementation here reports any point as non-obscured,
    that is "phamtom" or "transparent" geometry with respect to visibility.
    This method should be overloaded for each Geometry's subclass.
    */
    public int computeQuantitativeInvisibility(Vector3D origin, Vector3D p)
    {
        return 0;
    }

    /**
    This operation is complementary to doIntersection method. It is used to
    return aditional information after a positive intersection test and
    obtain a GeometryIntersectionInformation structure.

    Usually, the information needed to filled the fields of the 
    GeometryIntersectionInformation data structure are computed by the
    algorithms of the doIntersection method, so each class is responsible
    of remembering the last results. This situation leads to some level of
    dificulty in multithreaded scenarios, making this operation non-reentrant
    nor thread-safe. To solve this situation, an application level
    syncronization should be provided, which warranties an atomic
    critical section behavior between this two methods.  As that should be
    to difficult and impractical to implement, the use of multithreading
    for quering this toolbox data is discouraged, in favor of a
    multiprocess distributed approach.

    Prerequisite: this method should be called only after a call to
    doIntersection method in the same object that returned a true value.
    */
    public abstract void
    doExtraInformation(Ray inRay, double intT, 
                                      GeometryIntersectionInformation outData);

    /**
    This operation returns a simple bounding volume specification in the
    form of a "min-max box", which is a paralelogram aligned with the
    axes, when the geometry is centered in the origin, without scaling or
    rotation.

    @return A 6 position vector of doubles, with the following 
    positions interpretation:
    [0]: minimum value in x coordinate
    [1]: minimum value in y coordinate
    [2]: minimum value in z coordinate
    [3]: maximum value in x coordinate
    [4]: maximum value in y coordinate
    [5]: maximum value in z coordinate
    */
    public abstract double[] getMinMax();

    /**
    @todo This method should be abstract, forcing all subclasses to define it.
    The design of this method could change in future.
    */
    public TriangleMeshGroup exportToTriangleMeshGroup()
    {
        return null;
    }

    /**
    The design of this method could change in future.
    */
    public PolyhedralBoundedSolid exportToPolyhedralBoundedSolid()
    {
        return null;
    }

    /**
    Given a point `p`, this method classifies if point lies `INSIDE` current
    geometry, `OUTSIDE` current geometry or `LIMIT` if it is near the geometry
    at some specified `distanceTolerance`.

    Note that the `INSIDE` and `OUTSIDE` cases are only feasible in `Solid`s,
    while `LIMIT` cases can be defined in 0D, 1D and 2D geometries.
    */
    public int doContainmentTest(Vector3D p, double distanceTolerance)
    {
        return OUTSIDE;
    }

    /**
    Check the related interface contract for method
    Geometry.doIntersection.

    SPECIFIC IMPLEMENTATION: this method solve the intersection problem
    for one triangle in two stages:
    <UL>
    <LI>Calculates the plane containing the Triangle and checks if the Ray
    intersects with that plane.
    <LI>If the Ray intersects the plane, a check is done to determine
    if the intersection is inside the Triangle.
    </UL>
    That logic y repeated for all the Triangles in the TriangleMesh, and
    the shortest length intersected Triangle is reported.<P>

    Precondition:
\f[
    \mathbf{Q} := inOut\_Ray.direction.length() = 1 \;
\f]

    NOTES:
    <UL>
    <LI>The plane normal is determined for each triangle as the cross product
    of two Triangle edge Vector3D's, algorithm step (1).
    <LI>The canonic equation for a plane with normal n is
\f[
        nx*x + ny*y +nz*z + d = 0
\f]
    <LI>The parametric equation for the ray inOut_Ray (call it r) is
\f[
        \vec p = \vec{r.o} + t * \vec{r.d}
\f]
    <LI>Combining those two equations and solving for parameter t, algorithm
    step (2), gives
\f[
        t = \frac{ -(nx*ox+ny*oy+nz*oz+d) }{ nx*dx+ny*dy+nz*dz }
\f]
    and observing that the appearing vector components can be expressed as
    dot product, this equation can be rewritten in the condensed vectorial
    form
\f[
        t = \frac{ -(\vec n \cdot \vec{r.o} +d) }
                               { \vec n \cdot \vec{r.d} }
\f]
    <LI>Scalar value d in that equation can be solve replacing the coordinates
    of any of the Triangle points into the plane equation.
    <LI>To check if an intersected point lies inside the triangle, a left/right
    test is done with each one of the three directed edge vectors. If all three
    tests pass, then the point is inside the triangle.
    <LI>If the normal and the direction of the ray are in the same direction
    (more than 90 degrees of vector angle) then the normal is inverted to manage
     meshes with reversed triangles.
    </UL>
    */
    public static boolean
    doIntersectionWithTriangle(Ray inOut_Ray,
                               Vector3D v0, Vector3D v1, Vector3D v2,
                               Vector3D outPoint, Vector3D outNormal) {
        Vector3D p;           // Point of intersection between ray and plane
        Vector3D u, v, n;     // Edge vectors and normal
        double t, a, b, d;    // Coefficients for solving equation (2)
        double s1, s2, s3;    // Side test for each of triangle border

        // The vectors u & v are two triangle edges, and define the 
        // normal (1)
        u = v1.substract(v0);
        v = v2.substract(v1);
        n = v.crossProduct(u);
        n.normalize();

        // This is the result of replacing point v0 on plane equation, 
        // solving for d
        d = -n.dotProduct(v0);

        // Calculate numerator and denominator for equation (2)
        a = n.dotProduct(inOut_Ray.origin) + d;
        b = n.dotProduct(inOut_Ray.direction);

        // The denominator is big when the ray is not parallel to the plane
        if ( Math.abs(b) > VSDK.EPSILON ) {
            // Solution for equation (2), only if non-zero denominator
            t = (-a) / b;

            if ( t < 0.0 ) return false;

            // Calculate the intersection point between ray and plane
            p = inOut_Ray.origin.add(inOut_Ray.direction.multiply(t));

            // Check if the point is inside the triangle
            s1 = u.crossProduct(p.substract(v0)).dotProduct(n);
            s2 = (v2.substract(v1)).crossProduct(p.substract(v1)).dotProduct(n);
            s3 = (v0.substract(v2)).crossProduct(p.substract(v2)).dotProduct(n);

            if ( (s1 >= 0 && s2 >= 0 && s3 >= 0) || 
                 (s1 <= 0 && s2 <= 0 && s3 <= 0) ) {
                inOut_Ray.t = t;
                outNormal.clone(n);
                outPoint.clone(p);
                return true;
            }
        }
        return false;
    }

    /**
    This method implements a general voxelization strategy based on 
    containment test. Note that in the case of multiple fragment geometries
    (i.e. meshes and polylines) this method is usually inefficient, and
    for that cases voxelization should be explicit, and overload this method.
    Current method is usually well behaved for basic solid models.
    Note that `reporter` can be null.
    */
    public void doVoxelization(VoxelVolume vv, Matrix4x4 M, ProgressMonitor reporter)
    {
        int nx = vv.getXSize();
        int ny = vv.getYSize();
        int nz = vv.getZSize();
        int nmax;
        double minmax[] = getMinMax();
        double greaterScale, sx, sy, sz;

        sx = minmax[3]-minmax[0];
        sy = minmax[4]-minmax[1];
        sz = minmax[5]-minmax[2];
        greaterScale = sx;
        if ( sy > greaterScale ) {
            greaterScale = sy;
        }
        if ( sz > greaterScale ) {
            greaterScale = sz;
        }

        nmax = nx;
        if ( ny > nmax ) nmax = ny;
        if ( nz > nmax ) nmax = nz;
        int containmentStatus;
        int x, y, z;
        Vector3D p = new Vector3D();
        Vector3D transformedP;

        if ( reporter != null ) {
            reporter.begin();
        }
        for ( x = 0; x < nx; x++ ) {
            for ( y = 0; y < ny; y++ ) {
                if ( reporter != null ) {
                    reporter.update(0, nx*ny, x*ny);
                }
                for ( z = 0; z < nz; z++ ) {
                    p = vv.getVoxelPosition(x, y, z);
                    transformedP = M.multiply(p);
                    containmentStatus = doContainmentTest(
                            transformedP, (1/((double)nmax) * greaterScale));
                    if ( containmentStatus == INSIDE ||
                         containmentStatus == LIMIT ) {
                        vv.putVoxel(x, y, z, (byte)-1);
                    }
                }
            }
        }
        if ( reporter != null ) {
            reporter.end();
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
