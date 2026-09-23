#include <cmath>

#include "java/io/FileInputStream.h"
#include "java/io/FileOutputStream.h"
#include "java/lang/System.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/curve/ParametricCurve.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/Voxelization.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.h"
#include "vsdk/toolkit/environment/geometry/volume/Arrow.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/geometry/volume/Torus.h"
#include "vsdk/toolkit/environment/geometry/volume/VoxelVolume.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleBodyGroup.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/feedback/ProgressMonitorConsole.h"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/io/geometry/EnvironmentPersistence.h"
#include "vsdk/toolkit/io/image/RGBColorPalettePersistence.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBColorPalette.h"
#include "model/ApplicationModel.h"
#include "model/DrawingArea.h"
#include "model/InteractionMode.h"
#include "model/Scene.h"
#include "model/history/SceneHistory.h"
#include "model/selection/SelectionSet.h"
#include "application/GuiEventExecutor.h"

GuiEventExecutor::GuiEventExecutor(ApplicationModel* model,
                                   Presenter* presenter)
    : model(model), presenter(presenter)
{
}

Scene* GuiEventExecutor::scene() const
{
    return model->getScene();
}

java::String GuiEventExecutor::message(const char* id) const
{
    // Without I18N context the identifier itself is shown, as
    // `Widget::getMessage` does for unknown messages
    Widget* context = model->getI18nContext();
    return context != nullptr ? context->getMessage(id) : java::String(id);
}

bool GuiEventExecutor::executeCommand(const java::String& label)
{
    return execute(label) == CommandResult::DONE;
}

GuiEventExecutor::CommandResult GuiEventExecutor::execute(
    const java::String& label)
{
    SceneHistory* history = model->getEditHistory()->getSceneHistory();
    CommandResult result;

    history->begin();
    try {
        result = executeModelCommand(label);
    }
    catch ( ... ) {
        history->end(label, false);
        throw;
    }
    history->end(label, false);
    return result;
}

GuiEventExecutor::CommandResult GuiEventExecutor::executeModelCommand(
    const java::String& label)
{
    Light* light;

    //- CREATE --------------------------------------------------------
    if ( label.equals("IDC_CREATE_SPHERE") ) {
        scene()->addThing(new Sphere(1.0));
    }
    else if ( label.equals("IDC_CREATE_CONE") ) {
        scene()->addThing(new Cone(1, 0, 2));
    }
    else if ( label.equals("IDC_CREATE_CYLINDER") ) {
        scene()->addThing(new Cone(1, 1, 2));
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
    else if ( label.equals("IDC_CREATE_PLANE") ) {
        InfinitePlane* plane;
        plane = new InfinitePlane(Vector3Dd(-0.2, 0, 1), Vector3Dd(0, 0, -1));
        scene()->addThing(plane);
    }
    else if ( label.equals("IDC_CREATE_SPHERE_HARMONIC") ) {
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
    else if ( label.equals("IDC_CREATE_PROJECTED_VIEWS") ) {
        model->getDrawingArea()->setProjectedViewsDebugRequested(true);
    }
    else if ( label.equals("IDC_CREATE_VOLUME") ) {
        //- Select current object, if empty selection take a temp. sphere -
        int selectedThing = scene()->selectedThings->firstSelected();
        Geometry* referenceGeometry;
        Sphere temporarySphere(0.5);
        SimpleBody* thing = nullptr;

        if ( selectedThing < 0 ) {
            referenceGeometry = &temporarySphere;
        }
        else {
            thing = scene()->scene->getSimpleBodies().get(selectedThing);
            referenceGeometry = thing->getGeometry();
        }

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

        //- Append newly created volume to scene, matching reference form -
        SimpleBody* newThing = scene()->addThing(vv);
        Vector3Dd pos = M.extractTranslation();
        if ( thing != nullptr ) {
            pos = pos.add(thing->getPosition());
            newThing->setRotation(thing->getRotation());
            newThing->setScale(thing->getScale());
        }
        newThing->setPosition(pos);
        Vector3Dd size(M.get(0, 0), M.get(1, 1), M.get(2, 2));
        newThing->setScale(size);
    }
    else if ( label.equals("IDC_CREATE_BREP") ) {
        PolyhedralBoundedSolid* brep;

        Box box(0.9, 0.9, 0.9);
        brep = box.exportToPolyhedralBoundedSolid();
        Matrix4x4d R;
        R = R.translation(0.55, 0.55, 0.55);
        PolyhedralBoundedSolidModeler::applyTransformation(brep, R);
        //- Cube modification to holed box ----------------------------
        PolyhedralBoundedSolidEulerOperators::smev(brep, 6, 5, 9, Vector3Dd(0.3, 0.3, 1));
        PolyhedralBoundedSolidEulerOperators::kemr(brep, 6, 6, 5, 9, 9, 5);
        PolyhedralBoundedSolidEulerOperators::smev(brep, 6, 9, 10, Vector3Dd(0.8, 0.3, 1));
        PolyhedralBoundedSolidEulerOperators::smev(brep, 6, 10, 11, Vector3Dd(0.8, 0.8, 1));
        PolyhedralBoundedSolidEulerOperators::smev(brep, 6, 11, 12, Vector3Dd(0.3, 0.8, 1));
        PolyhedralBoundedSolidEulerOperators::mef(brep, 6, 6, 9, 10, 12, 11, 7);

        //- Box extrusion ---------------------------------------------
        PolyhedralBoundedSolidEulerOperators::smev(brep, 7, 9, 13, Vector3Dd(0.3, 0.3, 0.1));
        PolyhedralBoundedSolidEulerOperators::smev(brep, 7, 10, 14, Vector3Dd(0.8, 0.3, 0.1));
        PolyhedralBoundedSolidEulerOperators::mef(brep, 7, 7, 13, 9, 14, 10, 8);
        PolyhedralBoundedSolidEulerOperators::smev(brep, 7, 11, 15, Vector3Dd(0.8, 0.8, 0.1));
        PolyhedralBoundedSolidEulerOperators::mef(brep, 7, 7, 14, 10, 15, 11, 9);
        PolyhedralBoundedSolidEulerOperators::smev(brep, 7, 12, 16, Vector3Dd(0.3, 0.8, 0.1));
        PolyhedralBoundedSolidEulerOperators::mef(brep, 7, 7, 15, 11, 16, 12, 10);
        PolyhedralBoundedSolidEulerOperators::mef(brep, 7, 7, 13, 14, 16, 12, 11);

        //- Hole creation ---------------------------------------------
        PolyhedralBoundedSolidEulerOperators::kfmrh(brep, 2, 11);

        R = R.translation(-0.55, -0.55, -0.55);
        PolyhedralBoundedSolidModeler::applyTransformation(brep, R);
        PolyhedralBoundedSolidValidationEngine::validateIntermediate(brep);

        //brep = createCircle(0.5, 0.5, 0.5, 0.1, 12);

        //
        PolyhedralBoundedSolidValidationEngine::validateIntermediate(brep);
        scene()->addThing(brep);
    }
    else if ( label.equals("IDC_CREATE_PARAMETRICCUBICCURVE") ) {
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
    else if ( label.equals("IDC_CREATE_FUNCTIONALEXPLICITSURFACE") ) {
        SimpleBody* newThing;
        FunctionalExplicitSurface* functionalSurface;
        functionalSurface = new FunctionalExplicitSurface("cos((PI*x)/2)");
        functionalSurface->setBounds(-10, -10, -10, 10, 10, 10);
        functionalSurface->setTesselationHint(100, 100);
        newThing = scene()->addThing(functionalSurface);
        newThing->setMaterial(new SimpleMaterial(
            newThing->getMaterial()->withDoubleSided(true)));
    }
    else if ( label.equals("IDC_CREATE_PARAMETRICBICUBICPATCH") ) {
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
        //-----------------------------------------------------------------
    }
    else if ( label.equals("IDC_CREATE_OMNILIGHT") ) {
        light = model->addNewLight();
        if ( light == nullptr ) {
            Logger::reportMessage("GuiEventExecutor", Logger::WARNING,
                "execute", "No visible viewport where to create the light");
            return CommandResult::FAILED;
        }
    }
    //- RENDERING -----------------------------------------------------
    else if ( label.equals("IDC_RENDERING_OBTAINZBUFFERIMAGE") ) {
        presenter->showStatusMessage(message("IDM_PENDING_ZBUFFER_COLOR_IMAGE"));
        model->getDrawingArea()->setColorCaptureRequested(true);
    }
    else if ( label.equals("IDC_RENDERING_OBTAINZBUFFERDEPTHMAP") ) {
        presenter->showStatusMessage(message("IDM_PENDING_ZBUFFER_DEPTH"));
        model->getDrawingArea()->setDepthCaptureRequested(true);
    }
    else if ( label.equals("IDC_RENDERING_OBTAINCONTOURNS") ) {
        presenter->showStatusMessage(message("IDM_PENDING_CONTOURNS"));
        model->getDrawingArea()->setDepthCaptureRequested(true);
        model->getDrawingArea()->setContoursRequested(true);
    }
    //-----------------------------------------------------------------
    else if ( label.equals("IDC_OTHERS_CYCLE_BACKGROUND") ) {
        scene()->rotateBackground();
    }
    else if ( label.equals("IDC_OTHERS_TOGGLE_TEST_CORRIDOR") ) {
        scene()->showCorridor = !scene()->showCorridor;
    }
    else if ( label.equals("IDC_OTHERS_TOGGLE_GRID") ) {
        model->getDrawingArea()->toggleSelectedViewportGrid();
    }
    else if ( label.equals("IDC_OTHERS_PRINT_SCENE_ON_CONSOLE") ) {
        scene()->print();
    }
    //-----------------------------------------------------------------
    else if ( label.equals("IDC_TOOLS_CAMERA") ) {
        presenter->showStatusMessage(message("IDM_CAMERA_MODE"));
        model->getDrawingArea()->setInteractionMode(InteractionMode::CAMERA);
    }
    else if ( label.equals("IDC_TOOLS_SELECT") ) {
        presenter->showStatusMessage(message("IDM_SELECTION_MODE"));
        model->getDrawingArea()->setInteractionMode(InteractionMode::SELECT);
    }
    else if ( label.equals("IDC_TOOLS_TRANSLATE") ) {
        presenter->showStatusMessage(message("IDM_TRANSLATION_MODE"));
        model->getDrawingArea()->setInteractionMode(InteractionMode::TRANSLATE);
    }
    else if ( label.equals("IDC_TOOLS_ROTATE") ) {
        presenter->showStatusMessage(message("IDM_ROTATION_MODE"));
        model->getDrawingArea()->setInteractionMode(InteractionMode::ROTATE);
    }
    else if ( label.equals("IDC_TOOLS_SCALE") ) {
        presenter->showStatusMessage(message("IDM_SCALE_MODE"));
        model->getDrawingArea()->setInteractionMode(InteractionMode::SCALE);
    }
    else if ( label.equals("IDC_TOOLS_RAY") ) {
        model->setWithVisualDebugRay(!model->isWithVisualDebugRay());
    }
    else if ( label.equals("IDC_NEW_VIEW") ) {
        model->getDrawingArea()->addViewport();
    }
    else if ( label.equals("IDC_DEL_VIEW") ) {
        model->getDrawingArea()->removeLastViewport();
    }
    else {
        return CommandResult::NOT_HANDLED;
    }
    return CommandResult::DONE;
}

bool GuiEventExecutor::importObjects(const java::File& file)
{
    if ( !file.canRead() ) {
        return false;
    }
    SceneHistory* history = model->getEditHistory()->getSceneHistory();

    history->begin();
    try {
        EnvironmentPersistence::importEnvironment(file, scene()->scene);
    }
    catch ( ... ) {
        history->end(java::String("Import of ") + file.getName(), false);
        throw;
    }
    history->end(java::String("Import of ") + file.getName(), false);
    return true;
}

bool GuiEventExecutor::exportObjects(const java::File& file,
                                     ExportFormat format)
{
    java::FileOutputStream fos(file.getPath().c_str());
    if ( !file.canWrite() ) {
        return false;
    }

    switch ( format ) {
      case ExportFormat::OBJ:
        EnvironmentPersistence::exportEnvironmentObj(fos, scene()->scene);
        break;
      case ExportFormat::GTS:
        EnvironmentPersistence::exportEnvironmentGts(fos, scene()->scene);
        break;
      default:
        EnvironmentPersistence::exportEnvironmentVtk(fos, scene()->scene);
        break;
    }

    fos.close();
    return true;
}

bool GuiEventExecutor::loadPalette(const java::File& file)
{
    if ( !file.canRead() ) {
        return false;
    }
    java::FileInputStream source(file.getPath().c_str());
    model->setPalette(RGBColorPalettePersistence::importGimpPalette(source));
    source.close();
    return true;
}

SimpleBody* GuiEventExecutor::addDebugSphere(SimpleBody* voxelBody,
                                             int groupIndex,
                                             const Vector3Dd& cm,
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
