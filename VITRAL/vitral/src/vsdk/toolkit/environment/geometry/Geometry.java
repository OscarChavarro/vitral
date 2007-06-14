//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Ray;

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
    public abstract void doExtraInformation(Ray inRay, double intT, 
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
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
