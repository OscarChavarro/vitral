#include <math.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <limits.h>

#include "java/io/File.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/lang/Double.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.h"
#include "vsdk/toolkit/environment/geometry/volume/QuadricSurface.h"
#include "vsdk/toolkit/environment/geometry/volume/PolynomialSurface.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/HeightFieldSolid.h"
#include "vsdk/toolkit/environment/geometry/surface/SubdividedBezierPatch.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/background/SimpleBackground.h"
#include "vsdk/toolkit/environment/background/CubemapBackground.h"
#include "vsdk/toolkit/environment/geometry/surface/QuadMesh.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/geometry/volume/Torus.h"
#include "vsdk/toolkit/environment/geometry/volume/Prism.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector2Dd.h"
#ifndef TURBO_C_8086_REAL_MODE_ENABLED
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.h"
#endif
#include "vsdk/toolkit/io/geometry/EnvironmentPersistence.h"
#include "vsdk/toolkit/io/geometry/ReaderMitQuadGroup.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/media/IndexedColorImageUncompressed.h"
#include "vsdk/toolkit/media/NormalMap.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "java/io/File.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/light/AmbientLight.h"
#include "vsdk/toolkit/environment/light/DirectionalLight.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/environment/scene/csg/CsgBody.h"
#include "vsdk/toolkit/io/geometry/ReaderAltimetry.h"
#include "vsdk/toolkit/io/geometry/ReaderMitScene.h"
#include "vsdk/toolkit/io/geometry/ReaderMitTriangleGroup.h"
ReaderMitScene::ReaderMitScene()
    : yaw(0), pitch(0), roll(0), currentTexture(NULL), currentNormalMap(NULL)
{
}

void ReaderMitScene::resetState(const char* fileName)
{
    const char* slash = strrchr(fileName, '/');
    // Turbo C++ 3.0 cannot destroy String temporaries of a conditional expression
    sceneDirectory = String("");
    if ( slash != NULL ) {
        sceneDirectory = String(fileName).substr(0, (size_t)(slash - fileName) + 1);
    }
    yaw = 0;
    pitch = 0;
    roll = 0;
    currentTexture = NULL;
    currentNormalMap = NULL;
    texturePaths.clear();
    textures.clear();
    normalMapKeys.clear();
    normalMaps.clear();
}

BOOLEAN ReaderMitScene::hasRotation() const
{
    return yaw != 0 || pitch != 0 || roll != 0;
}

String ReaderMitScene::resolveResource(const String& path) const
{
    if (path.isEmpty() || path[0] == '/' || sceneDirectory.isEmpty()) return path;
    const String fromScene = sceneDirectory + path;
    FILE* probe = fopen(fromScene.toCString(), "rb");
    if (probe != NULL) {
        fclose(probe);
        return fromScene;
    }
    return path;
}

Image* ReaderMitScene::loadTexture(const String& path, SimpleScene* scene)
{
    for (long int i = 0; i < texturePaths.size(); ++i)
        if (texturePaths[i] == path) return textures[i];
    RGBImageUncompressed* texture = ImagePersistence::importRGB(File(resolveResource(path)));
    // Unreadable files come back as empty images.
    if (texture != NULL && (texture->getXSize() <= 0 || texture->getYSize() <= 0)) {
        delete texture;
        texture = NULL;
    }
    if (texture == NULL) return NULL;
    scene->addImage(texture);
    texturePaths.add(path);
    textures.add(texture);
    return texture;
}

NormalMap* ReaderMitScene::loadNormalMap(const String& path, const Vector3Dd& scale,
    SimpleScene* scene)
{
    char suffix[128];
    snprintf(suffix, sizeof(suffix), "|%.17g|%.17g|%.17g", scale.x(), scale.y(), scale.z());
    const String key = path + suffix;
    for (long int i = 0; i < normalMapKeys.size(); ++i)
        if (normalMapKeys[i] == key) return normalMaps[i];
    IndexedColorImageUncompressed* bumpMap =
        ImagePersistence::importIndexedColor(File(resolveResource(path)));
    if (bumpMap != NULL && (bumpMap->getXSize() <= 0 || bumpMap->getYSize() <= 0)) {
        delete bumpMap;
        bumpMap = NULL;
    }
    if (bumpMap == NULL) return NULL;
    NormalMap* normalMap = new NormalMap();
    normalMap->importBumpMap(bumpMap, scale);
    delete bumpMap;
    scene->addNormalMap(normalMap);
    normalMapKeys.add(key);
    normalMaps.add(normalMap);
    return normalMap;
}

void ReaderMitScene::applyRotation(SimpleBody* body) const
{
    if (!hasRotation()) return;
    const Matrix4x4d rotation = Matrix4x4d().eulerAnglesRotation(yaw, pitch, roll);
    body->setRotation(rotation);
    body->setRotationInverse(rotation.inverse());
}

void ReaderMitScene::dressBody(SimpleBody* body) const
{
    applyRotation(body);
    if (currentTexture != NULL) body->setTextureReference(currentTexture);
    if (currentNormalMap != NULL) body->setNormalMapReference(currentNormalMap);
}

BOOLEAN ReaderMitScene::importObj(const String& path, const Vector3Dd& translation,
    double uniformScale, const SimpleMaterial* fallbackMaterial, SimpleScene* scene) const
{
    const String resolved = resolveResource(path);
    FILE* probe = fopen(resolved.toCString(), "rb");
    if (probe == NULL) return FALSE;
    fclose(probe);
    SimpleScene imported;
    EnvironmentPersistence::importEnvironment(File(resolved), &imported);
    ArrayList<SimpleBody*>& bodies = imported.getSimpleBodies();
    if (bodies.isEmpty()) return FALSE;
    const Matrix4x4d sceneRotation = Matrix4x4d().eulerAnglesRotation(yaw, pitch, roll);
    for (long int i = 0; i < bodies.size(); ++i) {
        SimpleBody* body = bodies[i];
        if (body->getMaterial() == NULL) body->setMaterial(new SimpleMaterial(*fallbackMaterial));
        const Matrix4x4d rotation = sceneRotation.multiply(body->getRotation());
        const Vector3Dd scale = body->getScale().multiply(uniformScale);
        const Vector3Dd position =
            sceneRotation.multiply(body->getPosition().multiply(uniformScale)).add(translation);
        body->setRotation(rotation);
        body->setRotationInverse(rotation.inverse());
        body->setScale(scale);
        body->setPosition(position);
        scene->addBody(body);
    }
    // The bodies now belong to the target scene.
    bodies.clear();
    return TRUE;
}

static String trimLine(const String& in)
{
    size_t a = 0;
    while ( a < (size_t)in.size() && (in[a] == ' ' || in[a] == '\t' || in[a] == '\r' || in[a] == '\n') ) a++;
    size_t b = in.size();
    while ( b > a && (in[b-1] == ' ' || in[b-1] == '\t' || in[b-1] == '\r' || in[b-1] == '\n') ) b--;
    return in.substr(a, b - a);
}

/**
C++ port note: the try block around ReaderAltimetry::importHeightField of
importEnvironment of the cpp version, as a function of its own: gcc 2.7.2
crashes compiling it inside that long function.
*/
static HeightFieldSolid*
readHeightField(const String& path, BOOLEAN& readFailed)
{
    HeightFieldSolid* heightField = NULL;
    readFailed = FALSE;
    try {
        heightField = ReaderAltimetry::importHeightField(path.toCString());
    }
#ifndef VSDK_WITHOUT_EXCEPTIONS
    catch (const VSDKFatalException&) {
        readFailed = TRUE;
    }
#endif
    return heightField;
}

/**
C++ port note: the state that the lambdas of importEnvironment of the cpp
version capture by reference, and those lambdas as its members.
*/
class MitSceneImportContext {
public:
    FILE*& file;
    SimpleMaterial*& currentMaterial;
    CsgBody*& csgBody;
    ArrayList<long long>& csgNodes;
    PolynomialSurface*& polynomial;
    BlobSolid*& blob;
    BOOLEAN& hasPlacement;
    Matrix4x4d& placement;
    SimpleScene* outScene;
    ReaderMitScene& reader;
    const char* fileName;
    ReaderMitTriangleGroup& triangleGroup;
    BOOLEAN& inTriangles;
    BOOLEAN& inQuads;
    BOOLEAN& inPatch;
    BezierPatchSubdivision::Value& patchSubdivision;
    int& patchUSteps;
    int& patchVSteps;
    double& patchFlatness;
    int& patchPointCount;
    Vector3Dd (*patchPoints)[4];

    MitSceneImportContext(FILE*& fileIn, SimpleMaterial*& currentMaterialIn,
        CsgBody*& csgBodyIn, ArrayList<long long>& csgNodesIn,
        PolynomialSurface*& polynomialIn, BlobSolid*& blobIn,
        BOOLEAN& hasPlacementIn, Matrix4x4d& placementIn, SimpleScene* outSceneIn,
        ReaderMitScene& readerIn, const char* fileNameIn,
        ReaderMitTriangleGroup& triangleGroupIn, BOOLEAN& inTrianglesIn,
        BOOLEAN& inQuadsIn,
        BOOLEAN& inPatchIn, BezierPatchSubdivision::Value& patchSubdivisionIn,
        int& patchUStepsIn, int& patchVStepsIn, double& patchFlatnessIn,
        int& patchPointCountIn, Vector3Dd (*patchPointsIn)[4]) :
        file(fileIn), currentMaterial(currentMaterialIn), csgBody(csgBodyIn),
        csgNodes(csgNodesIn), polynomial(polynomialIn), blob(blobIn),
        hasPlacement(hasPlacementIn), placement(placementIn), outScene(outSceneIn),
        reader(readerIn), fileName(fileNameIn), triangleGroup(triangleGroupIn),
        inTriangles(inTrianglesIn), inQuads(inQuadsIn), inPatch(inPatchIn),
        patchSubdivision(patchSubdivisionIn), patchUSteps(patchUStepsIn),
        patchVSteps(patchVStepsIn), patchFlatness(patchFlatnessIn),
        patchPointCount(patchPointCountIn), patchPoints(patchPointsIn)
    {
    }

    void fail(const char* message)
    {
        fclose(file);
        delete currentMaterial;
        delete csgBody;
        delete polynomial;
        delete blob;
        throw VSDKFatalException(message);
    }

    BOOLEAN place(SimpleBody* body)
    {
        if (!hasPlacement) return TRUE;
        hasPlacement = FALSE;
        const Matrix4x4d forward = placement.multiply(body->getAffinePlacement());
        return body->setAffinePlacement(forward, forward.inverse()) == AffinePlacementStatus::Valid;
    }

    // Every primitive takes the current rotation, texture and normal map
    // (the caller dresses it with ReaderMitScene::dressBody first).
    void attach(SimpleBody* body)
    {
        if (!place(body)) { delete body; fail("ReaderMitScene: transform is not an invertible affine map"); }
        if (csgBody == NULL) { outScene->addBody(body); return; }
        if (!csgBody->addLeaf((long int)csgNodes[csgNodes.size()-1], body))
            fail("ReaderMitScene: invalid csg operand");
    }

    /**
    C++ port note: gcc 2.7.2 does not dereference the pointer of a reference
    member for a copy constructor argument; through a local it does.
    */
    SimpleMaterial* copyMaterial()
    {
        SimpleMaterial* material = currentMaterial;
        return new SimpleMaterial(*material);
    }

    /**
    C++ port note: the commands of the analytic, CSG, patch, height field
    and triangle blocks of importEnvironment of the cpp version, moved out
    of that function as gcc 2.7.2 crashes compiling it whole.
    @return true when the command is one of these
    */
    // C++ port note: transform, poly, blob and patch blocks
    // C++ port note: the options compare with strcmp, as gcc 2.7.2 crashes on
    // String temporaries in the operands of && and ||
    BOOLEAN shapeCommand(const String& cmd, const String& javaLine)
    {
        if ( cmd == "transform" ) {
            double m[12];
            if (sscanf(javaLine.toCString(), "%*s %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf",
                    &m[0], &m[1], &m[2], &m[3], &m[4], &m[5], &m[6], &m[7], &m[8], &m[9],
                    &m[10], &m[11]) != 12) fail("ReaderMitScene: transform requires twelve numbers");
            for (int i = 0; i < 12; ++i)
                if (!Double::isFinite(m[i])) fail("ReaderMitScene: transform requires finite numbers");
            if (hasPlacement) fail("ReaderMitScene: transform must be followed by a primitive");
            const double rows[4][4] = {{m[0], m[1], m[2], m[3]}, {m[4], m[5], m[6], m[7]},
                {m[8], m[9], m[10], m[11]}, {0, 0, 0, 1}};
            placement = Matrix4x4d(rows);
            hasPlacement = TRUE;
        }
        else if ( cmd == "poly" ) {
            int order = 0;
            char option[256] = {0};
            const int fields = sscanf(javaLine.toCString(), "%*s %d %255s", &order, option);
            if (fields < 1 || !PolynomialSurface::isValidOrder(order) ||
                (fields == 2 && strcmp(option, "sturm") != 0))
                fail("ReaderMitScene: poly requires an order from 2 to 7 and optionally sturm");
            polynomial = new PolynomialSurface(order, fields == 2);
        }
        else if ( cmd == "term" && polynomial != NULL ) {
            double coefficient;
            int x, y, z;
            if (sscanf(javaLine.toCString(), "%*s %lf %d %d %d", &coefficient, &x, &y, &z) != 4 ||
                !Double::isFinite(coefficient))
                fail("ReaderMitScene: term requires a coefficient and three powers");
            const int index = PolynomialSurface::getTermIndex(polynomial->getOrder(), x, y, z);
            if (index < 0) fail("ReaderMitScene: term powers exceed the polynomial order");
            polynomial->getCoefficients()[index] += coefficient;
        }
        else if ( cmd == "end" && polynomial != NULL ) {
            SimpleBody* body = new SimpleBody();
            body->setGeometry(polynomial);
            polynomial = NULL;
            body->setMaterial(copyMaterial());
            reader.dressBody(body); attach(body);
        }
        else if ( cmd == "blob" ) {
            double threshold;
            char option[256] = {0};
            const int fields = sscanf(javaLine.toCString(), "%*s %lf %255s", &threshold, option);
            if (fields < 1 || !Double::isFinite(threshold) ||
                (fields == 2 && strcmp(option, "sturm") != 0))
                fail("ReaderMitScene: blob requires a threshold and optionally sturm");
            blob = new BlobSolid(threshold, fields == 2);
        }
        else if ( cmd == "component" && blob != NULL ) {
            double strength, radius, x, y, z;
            if (sscanf(javaLine.toCString(), "%*s %lf %lf %lf %lf %lf", &strength, &radius, &x, &y, &z) != 5)
                fail("ReaderMitScene: component requires strength, radius and center");
            const BlobComponent component(Vector3Dd(x, y, z), radius, strength);
            if (component.isDegenerate()) fail("ReaderMitScene: degenerate blob component");
            blob->addComponent(component);
        }
        else if ( cmd == "end" && blob != NULL ) {
            if (blob->getComponentCount() == 0) fail("ReaderMitScene: blob requires a component");
            SimpleBody* body = new SimpleBody();
            body->setGeometry(blob);
            blob = NULL;
            body->setMaterial(copyMaterial());
            reader.dressBody(body); attach(body);
        }
        else if ( cmd == "patch" ) {
            if (csgBody) fail("ReaderMitScene: patches are open surfaces and cannot be csg operands");
            char method[256] = {0};
            char option[256] = {0};
            int uSteps = 0, vSteps = 0;
            double flatnessValue = 0.1;
            const int fields = sscanf(javaLine.toCString(), "%*s %255s %d %d %255s %lf", method, &uSteps,
                &vSteps, option, &flatnessValue);
            const String name(method);
            if (name == "uniform") patchSubdivision = BezierPatchSubdivision::UniformGrid;
            else if (name == "grid") patchSubdivision = BezierPatchSubdivision::PrecomputedGrid;
            else if (name == "adaptive") patchSubdivision = BezierPatchSubdivision::Adaptive;
            else if (name == "tree") patchSubdivision = BezierPatchSubdivision::AdaptiveTree;
            else if (name == "smooth") patchSubdivision = BezierPatchSubdivision::SmoothGrid;
            else fail("ReaderMitScene: patch requires uniform, grid, adaptive, tree or smooth");
            if (!(fields == 3 || (fields == 5 && strcmp(option, "flatness") == 0 &&
                    Double::isFinite(flatnessValue) && flatnessValue >= 0)) ||
                uSteps < 1 || vSteps < 1 || uSteps > 64 || vSteps > 64)
                fail("ReaderMitScene: patch requires steps from 1 to 64 and optionally flatness <value>");
            inPatch = TRUE;
            patchUSteps = uSteps;
            patchVSteps = vSteps;
            patchFlatness = flatnessValue;
            patchPointCount = 0;
        }
        else if ( cmd == "control" && inPatch ) {
            double x, y, z;
            if (sscanf(javaLine.toCString(), "%*s %lf %lf %lf", &x, &y, &z) != 3 ||
                !Double::isFinite(x) || !Double::isFinite(y) || !Double::isFinite(z))
                fail("ReaderMitScene: control requires three finite coordinates");
            if (patchPointCount >= 16) fail("ReaderMitScene: patch takes 16 control points");
            patchPoints[patchPointCount / 4][patchPointCount % 4] = Vector3Dd(x, y, z);
            patchPointCount++;
        }
        else if ( cmd == "end" && inPatch ) {
            if (patchPointCount != 16) fail("ReaderMitScene: patch takes 16 control points");
            inPatch = FALSE;
            SimpleBody* body = new SimpleBody();
            body->setGeometry(new SubdividedBezierPatch(patchSubdivision, patchUSteps, patchVSteps,
                patchFlatness, patchPoints));
            body->setMaterial(copyMaterial());
            reader.dressBody(body); attach(body);
        }
        else {
            return FALSE;
        }
        return TRUE;
    }

    // C++ port note: heightfield and triangles blocks
    BOOLEAN meshCommand(const String& cmd, const String& javaLine)
    {
        if ( cmd == "heightfield" ) {
            char path[512] = {0};
            char option[256] = {0};
            double water = 0;
            const int fields = sscanf(javaLine.toCString(), "%*s %511s %255s %lf", path, option, &water);
            if (fields != 1 && !(fields == 3 && strcmp(option, "water") == 0 && Double::isFinite(water)))
                fail("ReaderMitScene: heightfield requires a file and optionally water <level>");
            // Relative files are resolved from the scene file's directory.
            String resolved(path);
            const char* slash = strrchr(fileName, '/');
            if (path[0] != '/' && slash != NULL)
                resolved = String(fileName).substr(0, (size_t)(slash - fileName) + 1) + resolved;
            BOOLEAN readFailed = FALSE;
            HeightFieldSolid* heightField = readHeightField(resolved, readFailed);
            if (readFailed)
                fail("ReaderMitScene: could not read the heightfield altimetry file");

            if (fields == 3) {
                Vector3Dd minimum = heightField->getMinimum().withY(water);
                Vector3Dd maximum = heightField->getMaximum();
                if (!(water < maximum.y())) {
                    delete heightField;
                    fail("ReaderMitScene: heightfield water level must lie below its highest point");
                }
                heightField->setBounds(minimum, maximum);
                heightField->updateBlocks();
            }
            SimpleBody* body = new SimpleBody();
            body->setGeometry(heightField);
            body->setMaterial(copyMaterial());
            reader.dressBody(body); attach(body);
        }
        else if ( cmd == "triangles" ) {
            if (csgBody) fail("ReaderMitScene: triangles cannot be csg operands");
            if (hasPlacement) fail("ReaderMitScene: triangles cannot be transformed");
            if (inTriangles || inQuads) fail("ReaderMitScene: nested mesh block");
            triangleGroup.reset();
            inTriangles = TRUE;
        }
        else if ( cmd == "v" && inTriangles ) {
            double x, y, z;
            if (sscanf(javaLine.toCString(), "%*s %lf %lf %lf", &x, &y, &z) != 3 ||
                !triangleGroup.addVertex(Vector3Dd(x,y,z))) {
                fclose(file);
                delete currentMaterial;
                throw VSDKFatalException("ReaderMitScene: invalid triangle vertex");
            }
        }
        else if ( cmd == "f" && inTriangles ) {
            if (!triangleGroup.addFace(javaLine.toCString() + 1)) {
                fclose(file);
                delete currentMaterial;
                throw VSDKFatalException("ReaderMitScene: invalid triangle face");
            }
        }
        else if ( cmd == "end" && inTriangles ) {
            SimpleBody* mesh = triangleGroup.flush(outScene, currentMaterial);
            if (mesh != NULL) reader.applyRotation(mesh);
            triangleGroup.reset();
            inTriangles = FALSE;
        }
        else {
            return FALSE;
        }
        return TRUE;
    }

    // C++ port note: csg block and box, halfspace and quadric
    BOOLEAN solidCommand(const String& cmd, const String& javaLine)
    {
        if ( cmd == "csg" ) {
            char operationStr[256] = {0};
            char evaluatorStr[256] = {0};
            const int fields = sscanf(javaLine.toCString(), "%*s %255s %255s", operationStr, evaluatorStr);
            String operationName(operationStr);
            String evaluatorName(evaluatorStr);
            CsgOperation::Value operation = CsgOperation::UNION;
            if ( operationName == "union" ) operation = CsgOperation::UNION;
            else if ( operationName == "intersection" ) operation = CsgOperation::INTERSECTION;
            else if ( operationName == "difference" ) operation = CsgOperation::DIFFERENCE;
            else fail("ReaderMitScene: csg requires union, intersection or difference");
            if ( csgBody == NULL ) {
                CsgBodyEvaluator::Value evaluator = CsgBodyEvaluator::ROTH;
                if ( fields == 2 && evaluatorName == "pov" ) evaluator = CsgBodyEvaluator::CROSSING_MEMBERSHIP;
                else if ( fields == 2 && evaluatorName != "roth" ) fail("ReaderMitScene: csg evaluator must be roth or pov");
                csgBody = new CsgBody(operation, evaluator);
                csgBody->setMaterial(copyMaterial());
                if (!place(csgBody)) fail("ReaderMitScene: transform is not an invertible affine map");
                csgNodes.add(0);
            }
            else {
                if ( fields == 2 ) fail("ReaderMitScene: only the outermost csg selects an evaluator");
                if ( hasPlacement ) fail("ReaderMitScene: nested csg blocks cannot be transformed");
                const long int node = csgBody->addNode((long int)csgNodes[csgNodes.size()-1], operation);
                if ( node < 0 ) fail("ReaderMitScene: invalid csg node");
                csgNodes.add(node);
            }
        }
        else if ( cmd == "end" && csgBody ) {
            csgNodes.remove(csgNodes.size()-1);
            if ( csgNodes.size() == 0 ) {
                outScene->addBody(csgBody);
                csgBody = NULL;
            }
        }
        else if ( cmd == "box" ) {
            double xmin, ymin, zmin, xmax, ymax, zmax;
            if (sscanf(javaLine.toCString(), "%*s %lf %lf %lf %lf %lf %lf",
                    &xmin, &ymin, &zmin, &xmax, &ymax, &zmax) != 6 ||
                !Double::isFinite(xmin) || !Double::isFinite(ymin) || !Double::isFinite(zmin) ||
                !Double::isFinite(xmax) || !Double::isFinite(ymax) || !Double::isFinite(zmax) ||
                xmin >= xmax || ymin >= ymax || zmin >= zmax) {
                delete currentMaterial; fclose(file);
                throw VSDKFatalException("ReaderMitScene: box requires ordered minimum and maximum corners");
            }
            SimpleBody* b = new SimpleBody();
            b->setGeometry(new Box(Vector3Dd(xmin,ymin,zmin), Vector3Dd(xmax,ymax,zmax)));
            b->setMaterial(copyMaterial());
            reader.dressBody(b); attach(b);
        }
        else if ( cmd == "halfspace" ) {
            double a, b, c, d;
            if (sscanf(javaLine.toCString(), "%*s %lf %lf %lf %lf", &a, &b, &c, &d) != 4 ||
                !Double::isFinite(a) || !Double::isFinite(b) ||
                !Double::isFinite(c) || !Double::isFinite(d) ||
                (a == 0 && b == 0 && c == 0)) {
                delete currentMaterial; fclose(file);
                throw VSDKFatalException("ReaderMitScene: halfspace requires a nonzero normal and constant");
            }
            SimpleBody* body = new SimpleBody();
            body->setGeometry(new PlanarHalfSpace(a,b,c,d));
            body->setMaterial(copyMaterial());
            reader.dressBody(body); attach(body);
        }
        else if ( cmd == "quadric" ) {
            double xx, yy, zz, xy, xz, yz, x, y, z, c;
            if (sscanf(javaLine.toCString(), "%*s %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf",
                    &xx, &yy, &zz, &xy, &xz, &yz, &x, &y, &z, &c) != 10) {
                delete currentMaterial;
                fclose(file);
                throw VSDKFatalException("ReaderMitScene: quadric requires ten coefficients");
            }
            SimpleBody* b = new SimpleBody();
            b->setGeometry(new QuadricSurface(Vector3Dd(xx,yy,zz), Vector3Dd(xy,xz,yz),
                Vector3Dd(x,y,z), c));
            b->setMaterial(copyMaterial());
            reader.dressBody(b); attach(b);
        }
        else {
            return FALSE;
        }
        return TRUE;
    }

    BOOLEAN analyticCommand(const String& cmd, const String& javaLine)
    {
        // C++ port note: split in three as gcc 2.7.2 also crashes compiling
        // it whole; the branches keep the cpp order
        return shapeCommand(cmd, javaLine) || meshCommand(cmd, javaLine) ||
            solidCommand(cmd, javaLine);
    }
};

// Strips the quotes around a file name.
static String
unquote(const char* text)
{
    String value(text);
    if (value.size() >= 2 && value[0] == '"' && value[value.size()-1] == '"')
        value = value.substr(1, value.size() - 2);
    return value;
}

void ReaderMitScene::importEnvironment(const char* fileName, SimpleScene* outScene)
{
    if ( outScene == 0 ) {
        Logger::reportMessage("ReaderMitScene", Logger::ERROR, "importEnvironment", "outScene is null");
        throw VSDKFatalException("ReaderMitScene::importEnvironment: outScene is null");
    }

    const String platformName = File::toPlatformName(fileName);
    FILE* file = fopen(platformName.toCString(), "r");
    if ( !file ) {
        Logger::reportMessage("ReaderMitScene", Logger::ERROR, "importEnvironment", "could not open file");
        throw VSDKFatalException("ReaderMitScene::importEnvironment: could not open file");
    }

    outScene->clearOwnedElements();
    resetState(fileName);

    Camera* camera =
        new Camera();
    SimpleBackground* background = new SimpleBackground();
    outScene->addCamera(camera);
    outScene->addBackground(background);
    outScene->setActiveCameraIndex(0);
    outScene->setActiveBackgroundIndex(0);

    Vector3Dd eye = Vector3Dd(0, 0, 10);
    Vector3Dd up = Vector3Dd(0, 1, 0);
    Vector3Dd lookat = camera->getFocusedPosition();
    BOOLEAN hasEye = FALSE;
    BOOLEAN hasUp = FALSE;
    BOOLEAN hasLookat = FALSE;
    BOOLEAN hasViewport = FALSE;
    int viewportXSize = 320;
    int viewportYSize = 240;
    double importedHorizontalFov = 30.0;

    SimpleMaterial* currentMaterial = new SimpleMaterial();
    ReaderMitTriangleGroup triangleGroup;
    BOOLEAN inTriangles = FALSE;
    ReaderMitQuadGroup quadGroup;
    BOOLEAN inQuads = FALSE;
    // Open CSG body and its node stack; primitives inside become its leaves.
    CsgBody* csgBody = NULL;
    ArrayList<long long> csgNodes;
    // Open polynomial or blob block.
    PolynomialSurface* polynomial = NULL;
    BlobSolid* blob = NULL;
    // Open patch block: its subdivision settings and control points so far.
    BOOLEAN inPatch = FALSE;
    BezierPatchSubdivision::Value patchSubdivision = BezierPatchSubdivision::UniformGrid;
    int patchUSteps = 0;
    int patchVSteps = 0;
    double patchFlatness = 0;
    int patchPointCount = 0;
    Vector3Dd patchPoints[4][4];
    // Pending placement of the next primitive or outermost csg block.
    BOOLEAN hasPlacement = FALSE;
    Matrix4x4d placement;
    // C++ port note: the fail, place and attach lambdas of the cpp version
    // are members of this context, that refers to the state they capture
    MitSceneImportContext context(file, currentMaterial, csgBody, csgNodes,
        polynomial, blob, hasPlacement, placement, outScene, *this, fileName,
        triangleGroup, inTriangles, inQuads, inPatch, patchSubdivision, patchUSteps,
        patchVSteps, patchFlatness, patchPointCount, patchPoints);

    char lineBuf[1024];
    while ( fgets(lineBuf, sizeof(lineBuf), file) ) {
        // Remove comment
        char* sharp = strchr(lineBuf, '#');
        if ( sharp ) {
            *sharp = '\0';
        }
        String javaLine(lineBuf);
        javaLine = trimLine(javaLine);
        if ( javaLine.empty() ) {
            continue;
        }

        char cmdStr[256] = {0};
        sscanf(javaLine.toCString(), "%255s", cmdStr);
        String cmd(cmdStr);

        if ( polynomial != NULL && cmd != "term" && cmd != "end" )
            context.fail("ReaderMitScene: poly blocks only hold term lines");
        if ( blob != NULL && cmd != "component" && cmd != "end" )
            context.fail("ReaderMitScene: blob blocks only hold component lines");
        if ( inPatch && cmd != "control" && cmd != "end" )
            context.fail("ReaderMitScene: patch blocks only hold control lines");
        if ( inTriangles && cmd != "v" && cmd != "f" && cmd != "surface" && cmd != "end" )
            context.fail("ReaderMitScene: triangles blocks only hold v, f and surface lines");
        if ( inQuads && cmd != "v" && cmd != "q" && cmd != "surface" && cmd != "end" )
            context.fail("ReaderMitScene: quads blocks only hold v, q and surface lines");
        if ( context.analyticCommand(cmd, javaLine) ) {
            // C++ port note: see MitSceneImportContext::analyticCommand
        }
        else if ( cmd == "viewport" ) {
            int w, h;
            if ( sscanf(javaLine.toCString(), "%*s %d %d", &w, &h) == 2 ) {
                viewportXSize = w;
                viewportYSize = h;
                hasViewport = TRUE;
            }
        }
        else if ( cmd == "eye" ) {
            double x, y, z;
            if ( sscanf(javaLine.toCString(), "%*s %lf %lf %lf", &x, &y, &z) == 3 ) {
                eye = Vector3Dd(x, y, z);
                hasEye = TRUE;
            }
        }
        else if ( cmd == "up" ) {
            double x, y, z;
            if ( sscanf(javaLine.toCString(), "%*s %lf %lf %lf", &x, &y, &z) == 3 ) {
                up = Vector3Dd(x, y, z);
                hasUp = TRUE;
            }
        }
        else if ( cmd == "lookat" ) {
            double x, y, z;
            if ( sscanf(javaLine.toCString(), "%*s %lf %lf %lf", &x, &y, &z) == 3 ) {
                lookat = Vector3Dd(x, y, z);
                hasLookat = TRUE;
            }
        }
        else if ( cmd == "fov" ) {
            double fov;
            if ( sscanf(javaLine.toCString(), "%*s %lf", &fov) == 1 ) {
                importedHorizontalFov = fov;
            }
        }
        else if ( cmd == "background" ) {
            double r, g, b;
            if ( sscanf(javaLine.toCString(), "%*s %lf %lf %lf", &r, &g, &b) == 3 ) {
                background->setColor(r, g, b);
                outScene->setActiveBackgroundIndex(0);
            }
        }
        else if ( cmd == "surface" ) {
            if (inTriangles) {
                SimpleBody* mesh = triangleGroup.flush(outScene, currentMaterial);
                if (mesh != NULL) applyRotation(mesh);
            }
            if (inQuads) {
                SimpleBody* mesh = quadGroup.flush(outScene, currentMaterial);
                if (mesh != NULL) applyRotation(mesh);
            }
            double r, g, b;
            double ka, kd, ks;
            double ns;
            double kr, kt, index;
            if ( sscanf(javaLine.toCString(), "%*s %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf",
                        &r, &g, &b, &ka, &kd, &ks, &ns, &kr, &kt, &index) == 10 ) {
                delete currentMaterial;
                currentMaterial = new SimpleMaterial(
                    "surface",
                    ColorRgb(r * ka, g * ka, b * ka),
                    ColorRgb(r * kd, g * kd, b * kd),
                    ColorRgb(ks, ks, ks),
                    ColorRgb(0.0, 0.0, 0.0),
                    ColorRgb(0.0, 0.0, 0.0),
                    TRUE,
                    kr,
                    kt,
                    index,
                    1.0,
                    ns);
            }
        }
        else if ( cmd == "sphere" ) {
            double x, y, z, r;
            if ( sscanf(javaLine.toCString(), "%*s %lf %lf %lf %lf", &x, &y, &z, &r) == 4 ) {
                SimpleBody* b = new SimpleBody();
                b->setGeometry(new Sphere(r));
                b->setPosition(Vector3Dd(x, y, z));
                b->setMaterial(new SimpleMaterial(*currentMaterial));
                dressBody(b); context.attach(b);
            }
            else context.fail("ReaderMitScene: sphere requires a center and a radius");
        }
        else if ( cmd == "light" ) {
            double r, g, b;
            char typeStr[256] = {0};
            if ( sscanf(javaLine.toCString(), "%*s %lf %lf %lf %255s", &r, &g, &b, typeStr) == 4 ) {
                String type(typeStr);
                if ( type == "ambient" ) {
                    outScene->addLight(new AmbientLight(ColorRgb(r,g,b)));
                }
                else if ( type == "point" ) {
                    double x, y, z;
                    if ( sscanf(javaLine.toCString(), "%*s %*f %*f %*f %*s %lf %lf %lf", &x, &y, &z) == 3 ) {
                        outScene->addLight(new PointLight(Vector3Dd(x,y,z), ColorRgb(r,g,b)));
                    }
                }
                else if ( type == "directional" ) {
                    double x, y, z;
                    if ( sscanf(javaLine.toCString(), "%*s %*f %*f %*f %*s %lf %lf %lf", &x, &y, &z) == 3 ) {
                        outScene->addLight(new DirectionalLight(Vector3Dd(x,y,z), ColorRgb(r,g,b)));
                    }
                    else context.fail("ReaderMitScene: directional light requires a direction");
                }
                else context.fail("ReaderMitScene: light type must be ambient, point or directional");
            }
            else context.fail("ReaderMitScene: light requires a color and a type");
        }
        else if ( cmd == "cube" ) {
            double x, y, z, size;
            if (sscanf(javaLine.toCString(), "%*s %lf %lf %lf %lf", &x, &y, &z, &size) != 4 ||
                !Double::isFinite(size) || size <= 0)
                context.fail("ReaderMitScene: cube requires a center and a positive size");
            SimpleBody* body = new SimpleBody();
            body->setGeometry(new Box(size, size, size));
            body->setPosition(Vector3Dd(x, y, z));
            body->setMaterial(new SimpleMaterial(*currentMaterial));
            dressBody(body); context.attach(body);
        }
        else if ( cmd == "cylinder" ) {
            double x, y, z, bottomRadius, topRadius, height;
            if (sscanf(javaLine.toCString(), "%*s %lf %lf %lf %lf %lf %lf",
                    &x, &y, &z, &bottomRadius, &topRadius, &height) != 6 ||
                !(bottomRadius >= 0) || !(topRadius >= 0) || !(height > 0) ||
                (bottomRadius == 0 && topRadius == 0))
                context.fail("ReaderMitScene: cylinder requires a base center, two radii and a positive height");
            if (csgBody) context.fail("ReaderMitScene: cylinder cannot be a csg operand");
            SimpleBody* body = new SimpleBody();
            body->setGeometry(new Cone(bottomRadius, topRadius, height));
            body->setPosition(Vector3Dd(x, y, z));
            body->setMaterial(new SimpleMaterial(*currentMaterial));
            dressBody(body); context.attach(body);
        }
        else if ( cmd == "torus" ) {
            double x, y, z, majorRadius, minorRadius;
            if (sscanf(javaLine.toCString(), "%*s %lf %lf %lf %lf %lf",
                    &x, &y, &z, &majorRadius, &minorRadius) != 5 ||
                !(majorRadius > 0) || !(minorRadius > 0))
                context.fail("ReaderMitScene: torus requires a center and two positive radii");
            if (csgBody) context.fail("ReaderMitScene: torus cannot be a csg operand");
            SimpleBody* body = new SimpleBody();
            body->setGeometry(new Torus(majorRadius, minorRadius));
            body->setPosition(Vector3Dd(x, y, z));
            body->setMaterial(new SimpleMaterial(*currentMaterial));
            dressBody(body); context.attach(body);
        }
        else if ( cmd == "prism" ) {
            // prism x y z height n x1 y1 ... xn yn: a simple polygon of n
            // vertices on the plane z = 0 of the body, placed at (x, y, z),
            // swept height units along +Z. It is a parametric solid, so it
            // can be a csg operand.
            // C++ port note: the vertex count is also limited to INT_MAX, as
            // an int has 16 bits on Turbo C++ 3.0
            const char* cursor = javaLine.toCString();
            while ( *cursor != '\0' && *cursor != ' ' && *cursor != '\t' ) cursor++;
            double values[5];
            BOOLEAN ok = TRUE;
            int i;
            for ( i = 0; i < 5 && ok; i++ ) {
                char* next = NULL;
                values[i] = strtod(cursor, &next);
                ok = next != cursor && Double::isFinite(values[i]);
                cursor = next;
            }
            const int n = ok && values[4] >= 3 && values[4] <= 100000 &&
                values[4] <= (double)INT_MAX ? (int)values[4] : 0;
            ok = ok && n >= 3 && values[4] == (double)n && values[3] > 0;
            ArrayList<Vector2Dd> polygon;
            for ( i = 0; i < 2 * n && ok; i += 2 ) {
                char* next = NULL;
                const double px = strtod(cursor, &next);
                ok = next != cursor && Double::isFinite(px);
                cursor = next;
                if ( !ok ) break;
                const double py = strtod(cursor, &next);
                ok = next != cursor && Double::isFinite(py);
                cursor = next;
                polygon.add(Vector2Dd(px, py));
            }
            while ( ok && (*cursor == ' ' || *cursor == '\t') ) cursor++;
            ok = ok && *cursor == '\0';
            if ( !ok ) context.fail("ReaderMitScene: prism requires a base position, a positive height, a vertex count of at least 3 and that many x y pairs");
            Prism* prism = new Prism(polygon, values[3]);
            if ( !prism->hasValidParameters() ) {
                delete prism;
                context.fail("ReaderMitScene: prism base must be a simple polygon with non-zero area");
            }
            SimpleBody* body = new SimpleBody();
            body->setGeometry(prism);
            body->setPosition(Vector3Dd(values[0], values[1], values[2]));
            body->setMaterial(new SimpleMaterial(*currentMaterial));
            dressBody(body); context.attach(body);
        }
#ifndef TURBO_C_8086_REAL_MODE_ENABLED
        else if ( cmd == "polybox" || cmd == "appel" ) {
            if (csgBody) context.fail("ReaderMitScene: polyhedral solids cannot be csg operands");
            double x, y, z;
            double scale = 1.0;
            PolyhedralBoundedSolid* solid = NULL;
            if ( cmd == "polybox" ) {
                double sx, sy, sz;
                if (sscanf(javaLine.toCString(), "%*s %lf %lf %lf %lf %lf %lf",
                        &x, &y, &z, &sx, &sy, &sz) != 6 || !(sx > 0) || !(sy > 0) || !(sz > 0))
                    context.fail("ReaderMitScene: polybox requires a center and three positive sizes");
                Box box(sx, sy, sz);
                solid = box.exportToPolyhedralBoundedSolid();
            }
            else {
                int id;
                const int fields = sscanf(javaLine.toCString(), "%*s %d %lf %lf %lf %lf",
                    &id, &x, &y, &z, &scale);
                if (fields < 4 || (fields == 5 && !(scale > 0)) || id < 1 || id > 3)
                    context.fail("ReaderMitScene: appel requires an object 1 to 3, a center and optionally a positive scale");
                if (id == 1) solid = SimpleTestGeometryLibrary::createTestObjectAPPE1967_1();
                else if (id == 2) solid = SimpleTestGeometryLibrary::createTestObjectAPPE1967_2();
                else solid = SimpleTestGeometryLibrary::createTestObjectAPPE1967_3();
                // The APPEL objects are modelled in [0,1]^3; center them on the origin.
                PolyhedralBoundedSolidModeler::applyTransformation(solid,
                    Matrix4x4d().translation(-0.5, -0.5, -0.5));
            }
            if (solid == NULL) context.fail("ReaderMitScene: could not build the polyhedral solid");
            SimpleBody* body = new SimpleBody();
            body->setGeometry(solid);
            body->setPosition(Vector3Dd(x, y, z));
            body->setScale(Vector3Dd(scale, scale, scale));
            body->setMaterial(new SimpleMaterial(*currentMaterial));
            dressBody(body); context.attach(body);
        }
#endif
        else if ( cmd == "obj" ) {
            char path[512] = {0};
            double x, y, z;
            double scale = 1.0;
            const int fields = sscanf(javaLine.toCString(), "%*s %511s %lf %lf %lf %lf",
                path, &x, &y, &z, &scale);
            if (fields < 4 || (fields == 5 && !(scale > 0)))
                context.fail("ReaderMitScene: obj requires a file, a position and optionally a positive scale");
            if (csgBody || hasPlacement) context.fail("ReaderMitScene: obj models cannot be csg operands or transformed");
            if (!importObj(unquote(path), Vector3Dd(x, y, z), scale, currentMaterial, outScene))
                context.fail("ReaderMitScene: could not read the obj file");
        }
        else if ( cmd == "quads" ) {
            if (csgBody) context.fail("ReaderMitScene: quads cannot be csg operands");
            if (hasPlacement) context.fail("ReaderMitScene: quads cannot be transformed");
            if (inQuads || inTriangles) context.fail("ReaderMitScene: nested mesh block");
            quadGroup.reset();
            inQuads = TRUE;
        }
        else if ( cmd == "v" && inQuads ) {
            double x, y, z;
            if (sscanf(javaLine.toCString(), "%*s %lf %lf %lf", &x, &y, &z) != 3 ||
                !quadGroup.addVertex(Vector3Dd(x, y, z)))
                context.fail("ReaderMitScene: invalid quad vertex");
        }
        else if ( cmd == "q" && inQuads ) {
            if (!quadGroup.addQuad(javaLine.toCString() + 1)) context.fail("ReaderMitScene: invalid quad face");
        }
        else if ( cmd == "end" && inQuads ) {
            SimpleBody* mesh = quadGroup.flush(outScene, currentMaterial);
            if (mesh != NULL) applyRotation(mesh);
            quadGroup.reset();
            inQuads = FALSE;
        }
        else if ( cmd == "rotation" ) {
            double a, b, c;
            if (sscanf(javaLine.toCString(), "%*s %lf %lf %lf", &a, &b, &c) != 3 ||
                !Double::isFinite(a) || !Double::isFinite(b) || !Double::isFinite(c))
                context.fail("ReaderMitScene: rotation requires three finite angles");
            yaw = a;
            pitch = b;
            roll = c;
        }
        else if ( cmd == "texture" ) {
            char path[512] = {0};
            if (sscanf(javaLine.toCString(), "%*s %511s", path) != 1)
                context.fail("ReaderMitScene: texture requires a file");
            currentTexture = loadTexture(unquote(path), outScene);
            if (currentTexture == NULL) context.fail("ReaderMitScene: could not read the texture file");
        }
        else if ( cmd == "notexture" ) {
            currentTexture = NULL;
        }
        else if ( cmd == "bumpmap" ) {
            char path[512] = {0};
            double sx = 1, sy = 1, sz = 0.2;
            const int fields = sscanf(javaLine.toCString(), "%*s %511s %lf %lf %lf", path, &sx, &sy, &sz);
            if (fields != 1 && fields != 4)
                context.fail("ReaderMitScene: bumpmap requires a file and optionally three scale factors");
            currentNormalMap = loadNormalMap(unquote(path), Vector3Dd(sx, sy, sz), outScene);
            if (currentNormalMap == NULL) context.fail("ReaderMitScene: could not read the bump map file");
        }
        else if ( cmd == "nobumpmap" ) {
            currentNormalMap = NULL;
        }
        else if ( cmd == "backgroundcubemap" ) {
            char directoryStr[512] = {0};
            String directory("../../cubemaps/dorise1");
            if (sscanf(javaLine.toCString(), "%*s %511s", directoryStr) == 1) directory = unquote(directoryStr);
            RGBAImageUncompressed* faces[6];
            int i;
            for (i = 0; i < 6; ++i) {
                char name[32];
                snprintf(name, sizeof(name), "/entorno%d.jpg", i);
                faces[i] = ImagePersistence::importRGBA(File(resolveResource(directory + name)));
                if (faces[i] != NULL && (faces[i]->getXSize() <= 0 || faces[i]->getYSize() <= 0)) {
                    delete faces[i];
                    faces[i] = NULL;
                }
                if (faces[i] == NULL) {
                    for (int j = 0; j < i; ++j) delete faces[j];
                    context.fail("ReaderMitScene: could not read the cubemap faces");
                }
            }
            for (i = 0; i < 6; ++i) outScene->addImage(faces[i]);
            outScene->addBackground(new CubemapBackground(camera,
                faces[0], faces[1], faces[2], faces[3], faces[4], faces[5]));
            outScene->setActiveBackgroundIndex((int)outScene->getBackgrounds().size() - 1);
        }
#ifdef TURBO_C_8086_REAL_MODE_ENABLED
        // C++ port note: the BREP/model commands are ignored in the DOS
        // reader, that builds no PolyhedralBoundedSolid, to keep the
        // executable within the DOS limits
        else if ( cmd == "brep" || cmd == "model" || cmd == "object" ||
                  cmd == "polybox" || cmd == "appel" ) {
            continue;
        }
#endif
        else context.fail("ReaderMitScene: unsupported command");
    }

    if (polynomial) context.fail("ReaderMitScene: unterminated poly block");
    if (blob) context.fail("ReaderMitScene: unterminated blob block");
    if (inPatch) context.fail("ReaderMitScene: unterminated patch block");
    if (hasPlacement) context.fail("ReaderMitScene: transform must be followed by a primitive");
    if (csgBody) context.fail("ReaderMitScene: unterminated csg block");
    fclose(file);
    if (inTriangles || inQuads) {
        delete currentMaterial;
        throw VSDKFatalException("ReaderMitScene: unterminated mesh block");
    }

    if ( hasEye ) {
        camera->setPosition(eye);
    }
    camera->setUpDirect(up);
    if ( hasLookat ) {
        camera->setFocusedPositionMaintainingOrthogonality(lookat);
    }

    if ( viewportXSize > 0 && viewportYSize > 0 ) {
        double aspect = ((double)viewportXSize) / ((double)viewportYSize);
        double horizontalHalfAngle = importedHorizontalFov * M_PI / 360.0;
        double verticalHalfAngle = atan(tan(horizontalHalfAngle) / aspect);
        double verticalFov = (verticalHalfAngle * 360.0) / M_PI;
        camera->setFov(verticalFov);
    }
    if ( hasViewport ) {
        camera->updateViewportResize(viewportXSize, viewportYSize);
    }
    else {
        camera->updateVectors();
    }

    delete currentMaterial;
}
