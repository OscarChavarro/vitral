//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =

#include "java/lang/System.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Quaterniond.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/element/Triangle.h"
#include "vsdk/toolkit/environment/geometry/element/Vertex.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleBodyGroup.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/media/IndexedColorImageUncompressed.h"
#include "vsdk/toolkit/media/NormalMap.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/ZBuffer.h"
#include "vsdk/toolkit/processing/ImageProcessing.h"
#include "model/ApplicationModel.h"
#include "model/DrawingArea.h"
#include "model/ProjectedViewsDebugPlan.h"
#include "model/Scene.h"
#include "model/selection/SelectionSet.h"
#include "render/DrawingAreaHost.h"
#include "render/ProjectedViewRenderer.h"
#include "render/ProjectedViewsDebugger.h"

namespace {
const bool DO_DISTANCE_FIELD = false;
const int DISTANCE_FIELD_SIDE = 320;
}

ProjectedViewsDebugger::ProjectedViewsDebugger(ApplicationModel* model,
                                               DrawingAreaHost* host)
    : scene(model->getScene()), drawingArea(model->getDrawingArea()),
      host(host)
{
    quality = new RendererConfiguration();
    quality->setWires(false);
    quality->setSurfaces(true);
    isTransparent = true;

    if ( DO_DISTANCE_FIELD ) {
        viewSize = DISTANCE_FIELD_SIDE;
    }
    else {
        viewSize = 640;
    }
}

ProjectedViewsDebugger::~ProjectedViewsDebugger()
{
    delete quality;
}

void ProjectedViewsDebugger::debugIfNeeded(ProjectedViewRenderer* renderer)
{
    //-----------------------------------------------------------------
    if ( !drawingArea->isProjectedViewsDebugRequested() ) {
        return;
    }
    drawingArea->setProjectedViewsDebugRequested(false);

    //-----------------------------------------------------------------
    int selectedThing = scene->selectedThings->firstSelected();
    SimpleBody* referenceBody = nullptr;
    int i;

    if ( selectedThing >= 0 ) {
        referenceBody = scene->scene->getSimpleBodies().get(selectedThing);
    }

    if ( referenceBody == nullptr ) {
        host->showStatusMessage("ERROR: An object must be selected for projected views debugging to be created");
    }
    else {
        SimpleBodyGroup* group;
        // References the selected bodies of the scene
        SimpleBodyGroup bodySet;

        for ( i = 0; i < scene->selectedThings->size(); i++ ) {
            if ( scene->selectedThings->isSelected(i) ) {
                referenceBody = scene->scene->getSimpleBodies().get(i);
                bodySet.getBodies().add(referenceBody);
            }
        }

        group = addDebugProjectedView(renderer, &bodySet);

        if ( group != nullptr ) {
            scene->debugThingGroups.add(group);
        }
        else {
            host->showStatusMessage("ERROR: cannot create Pbuffer, you need recent 3D hardware acceleration for this function");
        }
    }
}

Image* ProjectedViewsDebugger::createProjectedView(
    ProjectedViewRenderer* renderer, SimpleBodyGroup* referenceBodies,
    int cam)
{
    //- Will render a normalized body inside the unit cube ------------
    double* minmax;
    SimpleBodyGroup bodySet;
    int i;

    Vector3Dd p;
    {
        //-----------------------------------------------------------------
        minmax = referenceBodies->getMinMax();
        Vector3Dd min(minmax[0], minmax[1], minmax[2]);
        Vector3Dd max(minmax[3], minmax[4], minmax[5]);
        delete[] minmax;
        Vector3Dd s(max.x() - min.x(), max.y() - min.y(), max.z() - min.z());

        double maxsize = s.x();
        if ( s.y() > maxsize ) maxsize = s.y();
        if ( s.z() > maxsize ) maxsize = s.z();
        // The 95% scale factor is to allow a full render of the object to
        // fit inside the rendered view
        s = Vector3Dd((2/maxsize) * 0.95, (2/maxsize) * 0.95,
                      (2/maxsize) * 0.95);

        p = max.add(min);
        p = p.multiply(-1/maxsize);

        bodySet.setPosition(p);
        bodySet.setScale(s);
        //-----------------------------------------------------------------
        SimpleBody* referenceBody;
        SimpleBody* framedBody;

        for ( i = 0; i < referenceBodies->getBodies().size(); i++ ) {
            referenceBody = referenceBodies->getBodies().get(i);
            framedBody = new SimpleBody();
            framedBody->setGeometryReference(referenceBody->getGeometry());
            framedBody->setPosition(referenceBody->getPosition());
            framedBody->setRotation(referenceBody->getRotation());
            framedBody->setRotationInverse(referenceBody->getRotationInverse());
            framedBody->setMaterial(new SimpleMaterial(Scene::defaultMaterial()));
            bodySet.getBodies().add(framedBody);
        }
        //-----------------------------------------------------------------
        Matrix4x4d Mset = bodySet.getTransformationMatrix();
        Matrix4x4d R;
        Matrix4x4d Ri;
        Matrix4x4d Mbody;
        Matrix4x4d S;
        Matrix4x4d M;
        SimpleBody* copiedBody;
        Quaterniond q;

        for ( i = 0; i < referenceBodies->getBodies().size(); i++ ) {
            referenceBody = referenceBodies->getBodies().get(i);
            if ( cam == 1 ) {
                copiedBody = scene->addThing(referenceBody->getGeometry());
                // The geometry is shared with the reference body
                copiedBody->setGeometryReference(referenceBody->getGeometry());
                Mbody = referenceBody->getTransformationMatrix();
                M = Mset.multiply(Mbody);
                p = M.extractTranslation();
                M = M.withVal(0, 3, 0.0);
                M = M.withVal(1, 3, 0.0);
                M = M.withVal(2, 3, 0.0);
                q = M.exportToQuaternion().normalized();
                R = Matrix4x4d();
                R = R.importFromQuaternion(q);
                Ri = R.inverse();
                S = Ri.multiply(M);
                s = Vector3Dd(M.get(0, 0), M.get(1, 1), M.get(2, 2));

                copiedBody->setPosition(p);
                copiedBody->setScale(s);
                copiedBody->setRotation(R);
            }
        }

        //-----------------------------------------------------------------
    }

    //- Render will proceed in a PBuffer ------------------------------
    IndexedColorImageUncompressed* distanceFieldIndexed;

    Camera* camera = ProjectedViewsDebugPlan::createCamera(cam);
    ZBuffer* depth = renderer->renderDepth(&bodySet, camera, quality,
                                           viewSize, viewSize);
    delete camera;
    for ( i = 0; i < bodySet.getBodies().size(); i++ ) {
        delete bodySet.getBodies().get(i);
    }
    bodySet.getBodies().clear();
    if ( depth == nullptr ) {
        return nullptr;
    }
    Image* projectedView = createContourImage(depth);
    delete depth;

    //-----------------------------------------------------------------
    Image* finalImage;
    if ( !DO_DISTANCE_FIELD ) {
        finalImage = projectedView;
    }
    else {
        java::System::out.print((java::String("Processing maps for view ") +
            java::String::valueOf(cam) + "... ").c_str());
        distanceFieldIndexed = new IndexedColorImageUncompressed();
        distanceFieldIndexed->init(DISTANCE_FIELD_SIDE, DISTANCE_FIELD_SIDE);
        ImageProcessing::processDistanceFieldWithArray(projectedView,
            distanceFieldIndexed, 1);
        ImageProcessing::gammaCorrection(distanceFieldIndexed, 2.0);

        RGBAImageUncompressed* distanceFieldRgba;
        distanceFieldRgba = distanceFieldIndexed->exportToRgbaImage();
        int x;
        int y;

        for ( x = 0; x < distanceFieldRgba->getXSize(); x++ ) {
            for ( y = 0; y < distanceFieldRgba->getYSize(); y++ ) {
                if ( distanceFieldIndexed->getPixel(x, y) < 1 ) {
                    distanceFieldRgba->putPixel(x, y,
                        (char)255, (char)0, (char)0, (char)128);
                }
            }
        }
        delete distanceFieldIndexed;
        delete projectedView;
        finalImage = distanceFieldRgba;
        java::System::out.println("Ok!");
    }

    //- Obtain Pbuffer's rendered image -------------------------------
    return finalImage;
}

Image* ProjectedViewsDebugger::createContourImage(const ZBuffer* depth) const
{
    IndexedColorImageUncompressed* zbuffer;
    NormalMap nm;
    zbuffer = depth->exportIndexedColorImage();

    //- Erase internal details: keep just the depth frontier border ---
    int x;
    int y;
    int val;
    for ( x = 0; x < zbuffer->getXSize(); x++ ) {
        for ( y = 0; y < zbuffer->getYSize(); y++ ) {
            val = (unsigned char)zbuffer->getPixel(x, y);
            if ( val < 255 ) {
                zbuffer->putPixel(x, y, (char)0);
            }
            else {
                zbuffer->putPixel(x, y, (char)255);
            }
        }
    }

    //- Get contourns from depth buffer's gradient --------------------
    nm.importBumpMap(zbuffer, Vector3Dd(1, 1, 0.1));
    delete zbuffer;

    //- Calculate borders (contourns) ---------------------------------
    if ( isTransparent ) {
        return nm.exportToRgbaImageGradient();
    }
    return nm.exportToRgbImageGradient();
}

SimpleBodyGroup* ProjectedViewsDebugger::addDebugProjectedView(
    ProjectedViewRenderer* renderer, SimpleBodyGroup* referenceBodies)
{
    SimpleBody* boxBody;
    Image* texture;
    SimpleBodyGroup* group;
    int i;
    TriangleMesh* mesh;
    Vector3Dd n;

    group = new SimpleBodyGroup();
    for ( i = 1; i <= ProjectedViewsDebugPlan::VIEW_COUNT; i++ ) {
        ProjectedViewsDebugPlan::ViewPlacement placement;
        ProjectedViewsDebugPlan::getPlacement(i, &placement);
        Matrix4x4d R = placement.getRotation();

        //-----------------------------------------------------------------
        texture = createProjectedView(renderer, referenceBodies, i);
        if ( texture == nullptr ) {
            long j;
            for ( j = 0; j < group->getBodies().size(); j++ ) {
                delete group->getBodies().get(j);
            }
            delete group;
            return nullptr;
        }

        //-----------------------------------------------------------------
        n = Vector3Dd(0, 0, 1);
        java::ArrayList<Vertex> vertexArray;
        vertexArray.add(Vertex(Vector3Dd(-1, -1, 0), n, 0.0, 0.0));
        vertexArray.add(Vertex(Vector3Dd(1, -1, 0), n, 1.0, 0.0));
        vertexArray.add(Vertex(Vector3Dd(1, 1, 0), n, 1.0, 1.0));
        vertexArray.add(Vertex(Vector3Dd(-1, 1, 0), n, 0.0, 1.0));
        java::ArrayList<Triangle> triangleArray;
        triangleArray.add(Triangle(0, 1, 2));
        triangleArray.add(Triangle(2, 3, 0));
        java::ArrayList<Image*> textureArray;
        textureArray.add(texture);
        java::ArrayList< java::ArrayList<int> > textureRanges;
        java::ArrayList<int> textureRange;
        textureRange.add(2);
        textureRange.add(1);
        textureRanges.add(textureRange);
        SimpleMaterial material = Scene::defaultMaterial();
        material = material.withDoubleSided(true);
        material = material.withAmbient(ColorRgb(1, 1, 1));
        material = material.withDiffuse(ColorRgb(1, 1, 1));
        material = material.withSpecular(ColorRgb(1, 1, 1));
        java::ArrayList<SimpleMaterial*> materialArray;
        materialArray.add(new SimpleMaterial(material));
        java::ArrayList< java::ArrayList<int> > materialRanges;
        java::ArrayList<int> materialRange;
        materialRange.add(2);
        materialRange.add(0);
        materialRanges.add(materialRange);

        mesh = new TriangleMesh();
        mesh->setVertexes(vertexArray, true, false, false, true);
        mesh->setTriangles(triangleArray);
        // The texture is owned by the body, the materials by the mesh
        mesh->setTextures(textureArray);
        mesh->setTextureRanges(textureRanges);
        mesh->setMaterials(materialArray);
        mesh->setOwnsMaterials(true);
        mesh->setMaterialRanges(materialRanges);

        //-----------------------------------------------------------------
        boxBody = new SimpleBody();
        boxBody->setGeometry(mesh);
        boxBody->setPosition(placement.getPosition());
        boxBody->setScale(placement.getScale());
        boxBody->setRotation(R);
        boxBody->setRotationInverse(R.inverse());
        boxBody->setMaterial(new SimpleMaterial(material));
        boxBody->setName("Proyected view box");
        boxBody->setTexture(texture);
        //-----------------------------------------------------------------
        group->getBodies().add(boxBody);
    }
    return group;
}
