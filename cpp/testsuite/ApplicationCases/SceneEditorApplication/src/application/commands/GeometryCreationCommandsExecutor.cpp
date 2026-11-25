#include <cmath>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/curve/ParametricCurve.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/Voxelization.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.h"
#include "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.h"
#include "vsdk/toolkit/environment/geometry/volume/Arrow.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobComponent.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/geometry/volume/HeightFieldSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.h"
#include "vsdk/toolkit/environment/geometry/volume/PolynomialSurface.h"
#include "vsdk/toolkit/environment/geometry/volume/Prism.h"
#include "vsdk/toolkit/environment/geometry/volume/QuadricSurface.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/geometry/volume/Torus.h"
#include "vsdk/toolkit/environment/geometry/volume/Tube.h"
#include "vsdk/toolkit/environment/geometry/volume/VoxelVolume.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleBodyGroup.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/feedback/ProgressMonitorConsole.h"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "model/ApplicationModel.h"
#include "model/Scene.h"
#include "model/selection/SelectionSet.h"
#include "application/commands/GeometryCreationCommandsExecutor.h"
#include "application/commands/Presenter.h"

GeometryCreationCommandsExecutor::GeometryCreationCommandsExecutor(
    ApplicationModel* model, Presenter* presenter)
    : model(model), presenter(presenter)
{
}

Scene* GeometryCreationCommandsExecutor::scene() const
{
    return model->getScene();
}

java::String GeometryCreationCommandsExecutor::message(const char* id) const
{
    // Without I18N context the identifier itself is shown, as
    // `Widget::getMessage` does for unknown messages
    Widget* context = model->getI18nContext();
    return context != nullptr ? context->getMessage(id) : java::String(id);
}

CommandResult GeometryCreationCommandsExecutor::execute(
    const java::String& label)
{
    if ( label.equals("IDC_CREATE_SPHERE") ) {
        scene()->addThing(new Sphere(1.0));
    }
    else if ( label.equals("IDC_CREATE_CONE") ) {
        scene()->addThing(new Cone(1, 0, 2));
    }
    else if ( label.equals("IDC_CREATE_CYLINDER") ) {
        scene()->addThing(new Cone(1, 1, 2));
    }
    else if ( label.equals("IDC_CREATE_TUBE") ) {
        scene()->addThing(new Tube(0.6, 1, 2));
    }
    else if ( label.equals("IDC_CREATE_WASHER") ) {
        // A tube of height 0: a circular lamina with a hole
        SimpleBody* body = scene()->addThing(new Tube(0.6, 1, 0));
        body->setMaterial(new SimpleMaterial(
            body->getMaterial()->withDoubleSided(true)));
    }
    else if ( label.equals("IDC_CREATE_CUBE") ) {
        scene()->addThing(new Box(1, 1, 1));
    }
    else if ( label.equals("IDC_CREATE_BOX") ) {
        scene()->addThing(new Box(1, 3, 2));
    }
    else if ( label.equals("IDC_CREATE_ARROW") ) {
        scene()->addThing(new Arrow(0.7, 0.3, 0.05, 0.1));
    }
    else if ( label.equals("IDC_CREATE_TORUS") ) {
        scene()->addThing(new Torus(2, 1));
    }
    else if ( label.equals("IDC_CREATE_PRISM") ) {
        // A hexagonal prism; its number of sides, circumradius and height
        // are edited with the generic editor
        scene()->addThing(new Prism(6, 1, 2));
    }
    else if ( label.equals("IDC_CREATE_BLOB") ) {
        createBlob();
    }
    else if ( label.equals("IDC_CREATE_QUADRIC") ) {
        createQuadric();
    }
    else if ( label.equals("IDC_CREATE_POLYNOMIAL") ) {
        createPolynomial();
    }
    else if ( label.equals("IDC_CREATE_HEIGHTFIELD") ) {
        createHeightField();
    }
    else if ( label.equals("IDC_CREATE_HALFSPACE") ) {
        scene()->addThing(new PlanarHalfSpace(0, 0, 1, 0));
    }
    else if ( label.equals("IDC_CREATE_PLANE") ) {
        createPlane();
    }
    else if ( label.equals("IDC_CREATE_SPHERE_HARMONIC") ) {
        createSphericalHarmonicDebugSpheres();
    }
    else if ( label.equals("IDC_CREATE_VOLUME") ) {
        if ( !createVolume() ) {
            return CommandResult::FAILED;
        }
    }
    else if ( label.equals("IDC_CREATE_BREP") ) {
        createBrep();
    }
    else if ( label.equals("IDC_CREATE_PARAMETRICCUBICCURVE") ) {
        createParametricCubicCurve();
    }
    else if ( label.equals("IDC_CREATE_FUNCTIONALEXPLICITSURFACE") ) {
        createFunctionalExplicitSurface();
    }
    else if ( label.equals("IDC_CREATE_PARAMETRICBICUBICPATCH") ) {
        createParametricBiCubicPatch();
    }
    else {
        return CommandResult::NOT_HANDLED;
    }
    return CommandResult::DONE;
}

/**
A blob shaped as a triangular molecule with a hole: three components at
the vertices of an equilateral triangle (at distance 1 from its center,
on the plane z = 0). Each component reaches 1.25 units, so neighbors
overlap; the threshold 0.46 is above the density at the center of the
triangle (0.39) and below the one half way between two components
(0.54), so the three of them merge in a ring around an empty center
(of radius 0.25, wide enough to be kept by the default polygonization).
*/
void GeometryCreationCommandsExecutor::createBlob()
{
    BlobSolid* blob = new BlobSolid(0.46, false);
    int i;

    for ( i = 0; i < 3; i++ ) {
        double angle = M_PI / 2 + 2 * M_PI * i / 3;
        blob->addComponent(BlobComponent(
            Vector3Dd(std::cos(angle), std::sin(angle), 0), 1.25, 1.0));
    }
    scene()->addThing(blob);
}

/**
An ellipsoid of semiaxes 1.5, 1 and 0.7: x^2/1.5^2 + y^2 + z^2/0.7^2 - 1
<= 0.
*/
void GeometryCreationCommandsExecutor::createQuadric()
{
    scene()->addThing(new QuadricSurface(
        Vector3Dd(1.0 / (1.5 * 1.5), 1.0, 1.0 / (0.7 * 0.7)),
        Vector3Dd(0, 0, 0), Vector3Dd(0, 0, 0), -1.0));
}

/**
The quartic "tangle cube" x^4 - 5x^2 + y^4 - 5y^2 + z^4 - 5z^2 + 11.8 < 0,
a solid of genus 5 that fits in a cube of side 4.6, scaled to 0.4.
*/
void GeometryCreationCommandsExecutor::createPolynomial()
{
    int order = 4;
    PolynomialSurface* polynomial = new PolynomialSurface(order, false);
    double* c = polynomial->getCoefficients();

    c[PolynomialSurface::getTermIndex(order, 4, 0, 0)] = 1;
    c[PolynomialSurface::getTermIndex(order, 0, 4, 0)] = 1;
    c[PolynomialSurface::getTermIndex(order, 0, 0, 4)] = 1;
    c[PolynomialSurface::getTermIndex(order, 2, 0, 0)] = -5;
    c[PolynomialSurface::getTermIndex(order, 0, 2, 0)] = -5;
    c[PolynomialSurface::getTermIndex(order, 0, 0, 2)] = -5;
    c[PolynomialSurface::getTermIndex(order, 0, 0, 0)] = 11.8;
    SimpleBody* body = scene()->addThing(polynomial);
    body->setScale(Vector3Dd(0.4, 0.4, 0.4));
}

/**
A terrain of 17 x 17 heights (waves between 0.7 and 2.3) over a box of
height 3. Height fields grow along their Y axis: the body is rotated so
they grow along Z, scaled to 0.25 and centered at the origin.
*/
void GeometryCreationCommandsExecutor::createHeightField()
{
    int n = 17;
    HeightFieldSolid* field = new HeightFieldSolid(n, n,
        Vector3Dd(0, 0, 0), Vector3Dd(n - 1, 3, n - 1));
    int x;
    int z;

    for ( z = 0; z < n; z++ ) {
        for ( x = 0; x < n; x++ ) {
            field->setHeight(x, z,
                1.5 + 0.8 * std::sin(x / 2.5) * std::cos(z / 3.0));
        }
    }
    SimpleBody* body = scene()->addThing(field);
    Matrix4x4d rotation;
    rotation = rotation.axisRotation(M_PI / 2, 1, 0, 0);
    double scale = 0.25;
    body->setRotation(rotation);
    body->setScale(Vector3Dd(scale, scale, scale));
    // Rotated, the center of the grid (8, y, 8) goes to (8, -8, y)
    body->setPosition(Vector3Dd(-scale * (n - 1) / 2.0,
        scale * (n - 1) / 2.0, 0));
}

void GeometryCreationCommandsExecutor::createPlane()
{
    InfinitePlane* plane;
    plane = new InfinitePlane(Vector3Dd(-0.2, 0, 1), Vector3Dd(0, 0, -1));
    scene()->addThing(plane);
}

void GeometryCreationCommandsExecutor::createSphericalHarmonicDebugSpheres()
{
    SimpleBody* voxelBody = nullptr;
    int selectedThing = scene()->selectedThings->firstSelected();

    Geometry* referenceGeometry = nullptr;

    if ( selectedThing >= 0 ) {
        voxelBody = scene()->scene->getSimpleBodies().get(selectedThing);
        referenceGeometry = voxelBody->getGeometry();
    }

    VoxelVolume* vv = dynamic_cast<VoxelVolume*>(referenceGeometry);
    if ( vv == nullptr ) {
        presenter->showStatusMessage("ERROR: A VoxelVolume must be selected for spherical harmonic debugging sphere to be created");
    }
    else {
        //- Calculate the VoxelVolume's center of mass ---------------
        Vector3Dd cm = vv->doCenterOfMass();

        //- Calculate average distance from nonzero voxels to cm -----
        // This accounts for scale normalization as in [FUNK2003].4.1.
        int numberOfNonZeroVoxels = 0;
        Vector3Dd p; // Position of voxel
        double averageDistance = 0;
        int x;
        int y;
        int z;

        for ( x = 0; x < vv->getXSize(); x++ ) {
            for ( y = 0; y < vv->getYSize(); y++ ) {
                for ( z = 0; z < vv->getZSize(); z++ ) {
                    if ( vv->getVoxel(x, y, z) != 0 ) {
                        p = vv->getVoxelPosition(x, y, z);
                        averageDistance += cm.subtract(p).length();
                        numberOfNonZeroVoxels++;
                    }
                }
            }
        }
        averageDistance /= (double)numberOfNonZeroVoxels;

        //- Create spheres -------------------------------------------
        SimpleBody* body;
        SimpleBodyGroup* group = new SimpleBodyGroup();
        int i;
        for ( i = 0; i < 32; i++ ) {
            body = addDebugSphere(voxelBody, i, cm, averageDistance);
            group->getBodies().add(body);
        }
        scene()->debugThingGroups.add(group);
        // Subspheres account for translation & scale
        group->setRotation(voxelBody->getRotation());
    }
}

bool GeometryCreationCommandsExecutor::createVolume()
{
    //- The volume is built from the selected object -------------------
    int selectedThing = scene()->selectedThings->firstSelected();

    if ( selectedThing < 0 ) {
        presenter->showStatusMessage(message("IDM_CREATE_VOLUME_NO_SELECTION"));
        return false;
    }
    SimpleBody* thing = scene()->scene->getSimpleBodies().get(selectedThing);
    Geometry* referenceGeometry = thing->getGeometry();

    //- Calculate transform matrix ------------------------------------
    double* minmax = referenceGeometry->getMinMax();
    Matrix4x4d M; // Transform from voxelspace to geometry minmax space

    M = VoxelVolume::getTransformFromVoxelFrameToMinMax(minmax);
    delete[] minmax;

    //- Auxiliary variables -------------------------------------------
    int nx = 64;
    int ny = 64;
    int nz = 64;

    //- Primitive rasterization ---------------------------------------
    VoxelVolume* vv = new VoxelVolume();
    vv->init(nx, ny, nz);

    ProgressMonitorConsole reporter;
    Voxelization::doVoxelization(*referenceGeometry, *vv, M, &reporter);

    if ( !hasFilledVoxels(*vv) ) {
        presenter->showStatusMessage(message("IDM_CREATE_VOLUME_EMPTY"));
        delete vv;
        return false;
    }

    //- Append newly created volume to scene, matching reference form -
    // The body of the volume composes the transformation of the
    // selected body with M (a translation to the center of the minmax
    // box and a uniform scale): its rotation is the one of the selected
    // body, its scale the product of both scales, and the center is
    // moved by the rotation and scale of the selected body
    Vector3Dd center = M.extractTranslation();
    Vector3Dd thingScale = thing->getScale();
    double voxelSpaceScale = M.get(0, 0);
    Vector3Dd scaledCenter(center.x() * thingScale.x(),
        center.y() * thingScale.y(), center.z() * thingScale.z());

    SimpleBody* newThing = scene()->addThing(vv);
    newThing->setRotation(thing->getRotation());
    newThing->setScale(thingScale.multiply(voxelSpaceScale));
    newThing->setPosition(thing->getPosition().add(
        thing->getRotation().multiply(scaledCenter)));
    return true;
}

bool GeometryCreationCommandsExecutor::hasFilledVoxels(const VoxelVolume& vv)
{
    int x;
    int y;
    int z;

    for ( z = 0; z < vv.getZSize(); z++ ) {
        for ( y = 0; y < vv.getYSize(); y++ ) {
            for ( x = 0; x < vv.getXSize(); x++ ) {
                if ( vv.isFilled(x, y, z) ) {
                    return true;
                }
            }
        }
    }
    return false;
}

void GeometryCreationCommandsExecutor::createBrep()
{
    scene()->addThing(
        SimpleTestGeometryLibrary::createTestObjectMANT1986_1());
}

void GeometryCreationCommandsExecutor::createParametricCubicCurve()
{
    ParametricCurve* curve;

    // Case 1: curve hard-coded in source
    java::ArrayList<Vector3Dd> pointParameters;

    curve = new ParametricCurve();
    // Note that an HERMITE curve uses tangent vectors, BEZIER curves
    // uses control points (tangent vectors are control point minus
    // knot position)
    pointParameters.add(Vector3Dd(0, 0, 0)); // Position 0
    pointParameters.add(Vector3Dd(0, 0, 0)); // Not used
    pointParameters.add(Vector3Dd(0, 1, 0)); // Salient tangent end
    curve->addPoint(pointParameters, ParametricCurve::BEZIER);

    pointParameters.clear();
    pointParameters.add(Vector3Dd(1, 1, 0)); // Position 1
    pointParameters.add(Vector3Dd(0, 1, 0)); // Entry tangent end
    pointParameters.add(Vector3Dd(2, 1, 0)); // Salient tangent end
    curve->addPoint(pointParameters, ParametricCurve::BEZIER);

    pointParameters.clear();
    pointParameters.add(Vector3Dd(2, 0, 1)); // Position 2
    pointParameters.add(Vector3Dd(2, 0, 0)); // Entry tangent end
    pointParameters.add(Vector3Dd(0, 0, 0)); // Not used
    curve->addPoint(pointParameters, ParametricCurve::BEZIER);

    scene()->addThing(curve);
}

void GeometryCreationCommandsExecutor::createFunctionalExplicitSurface()
{
    SimpleBody* newThing;
    FunctionalExplicitSurface* functionalSurface;
    functionalSurface = new FunctionalExplicitSurface("cos((PI*x)/2)");
    functionalSurface->setBounds(-10, -10, -10, 10, 10, 10);
    functionalSurface->setTesselationHint(100, 100);
    newThing = scene()->addThing(functionalSurface);
    newThing->setMaterial(new SimpleMaterial(
        newThing->getMaterial()->withDoubleSided(true)));
}

void GeometryCreationCommandsExecutor::createParametricBiCubicPatch()
{
    //- Create a Ferguson patch ---------------------------------------
    ParametricCurve* contourHermiteLine;
    java::ArrayList<Vector3Dd> pointParameters;

    // C++ port note: the contour curve is referenced by the patch for
    // its whole life, and not deleted
    contourHermiteLine = new ParametricCurve();
    // Note that an HERMITE curve uses tangent vectors, BEZIER curves
    // uses control points (tangent vectors are control point minus
    // knot position)
    pointParameters.add(Vector3Dd(0, 0, 0));  // Position 0
    pointParameters.add(Vector3Dd(0, -1, 0)); // Entry tangent
    pointParameters.add(Vector3Dd(1, 0, 0));  // Salient tangent
    contourHermiteLine->addPoint(pointParameters, ParametricCurve::HERMITE);

    pointParameters.clear();
    pointParameters.add(Vector3Dd(1, 0, 0));  // Position 1
    pointParameters.add(Vector3Dd(1, 0, 0));  // Entry tangent
    pointParameters.add(Vector3Dd(0, 1, 0));  // Salient tangent
    contourHermiteLine->addPoint(pointParameters, ParametricCurve::HERMITE);

    pointParameters.clear();
    pointParameters.add(Vector3Dd(1, 1, 0.4));  // Position 2
    pointParameters.add(Vector3Dd(0, 1, 0));  // Entry tangent
    pointParameters.add(Vector3Dd(-1, 0, 0));  // Salient tangent
    contourHermiteLine->addPoint(pointParameters, ParametricCurve::HERMITE);

    pointParameters.clear();
    pointParameters.add(Vector3Dd(0, 1, 0));  // Position 3
    pointParameters.add(Vector3Dd(-1, 0, 0));  // Entry tangent
    pointParameters.add(Vector3Dd(0, -1, 0));  // Salient tangent
    contourHermiteLine->addPoint(pointParameters, ParametricCurve::HERMITE);

    java::ArrayList<Vector3Dd> firstPoint =
        contourHermiteLine->getPointVector(0);
    contourHermiteLine->addPoint(firstPoint, ParametricCurve::HERMITE);

    ParametricBiCubicPatch* patch;
    patch = new ParametricBiCubicPatch();
    patch->buildFergusonPatch(contourHermiteLine);
    patch->setApproximationSteps(20);

    SimpleBody* newThing;
    newThing = scene()->addThing(patch);
    newThing->setMaterial(new SimpleMaterial(
        newThing->getMaterial()->withDoubleSided(true)));
}

SimpleBody* GeometryCreationCommandsExecutor::addDebugSphere(
    SimpleBody* voxelBody, int groupIndex, const Vector3Dd& cm,
    double averageDistance)
{
    double r = (((double)groupIndex) / 31.0) * (2 * averageDistance);
    VoxelVolume* vv = dynamic_cast<VoxelVolume*>(voxelBody->getGeometry());
    Sphere* sphere;
    RGBAImageUncompressed* texture;
    SimpleBody* body;
    double tetha;
    double phi;
    int s;
    int t;
    int voxelValue;
    Vector3Dd p;
    Vector3Dd pos;
    Vector3Dd scale;
    Vector3Dd cm2;
    Matrix4x4d S;

    sphere = new Sphere(r);
    body = new SimpleBody();
    body->setGeometry(sphere);
    body->setMaterial(new SimpleMaterial(
        Scene::defaultMaterial().withDoubleSided(true)));
    scale = voxelBody->getScale();
    S = S.scale(scale.x(), scale.y(), scale.z());
    scale = scale.multiply(r);
    body->setScale(scale);
    cm2 = S.multiply(cm);
    pos = voxelBody->getPosition().add(cm2);
    body->setPosition(pos);
    body->setRotation(Matrix4x4d());
    body->setRotationInverse(Matrix4x4d());
    body->setName(java::String("Debug sphere for harmonics ") +
                  java::String::valueOf(groupIndex));

    texture = new RGBAImageUncompressed();
    texture->init(64, 64);

    //- Build sphere's texture map from voxel grid --------------------
    for ( s = 0; s < texture->getXSize(); s++ ) {
        for ( t = 0; t < texture->getYSize(); t++ ) {
            tetha =
         ((double)s) / ((double)texture->getXSize()) * M_PI * 2;
            phi =
         ((double)t) / ((double)texture->getYSize()) * M_PI;
            p = Vector3Dd::fromSpherical(r, tetha, phi);
            p = cm.add(p);
            voxelValue = vv->getVoxelAtPosition(p.x(), p.y(), p.z());
            if ( voxelValue < 128 ) {
                texture->putPixel(s, t, (char)0, (char)0, (char)0, (char)0);
            }
            else {
                texture->putPixel(s, t, (char)0, (char)0, (char)0, (char)255);
            }
        }
    }

    //-----------------------------------------------------------------
    body->setTexture(texture);
    return body;
}
