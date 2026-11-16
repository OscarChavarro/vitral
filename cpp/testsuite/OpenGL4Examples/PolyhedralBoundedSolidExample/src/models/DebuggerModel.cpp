#include "java/util/ArrayList.txx"
#include "models/DebuggerModel.h"

#include "java/lang/Math.h"
#include "java/lang/System.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/CsgKurlanderBowlFixture.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/gui/CameraControllerOrbiter.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"

DebuggerModel::DebuggerModel()
    : solidModelName(SolidModelNames::STEP_IMPORT),
      csgSample(CsgSampleNames::KURLANDER_BOWL_SINGLE_MOTIF),
      kurlanderBowlSingleMotifIndex(38),
      subdivisionCircumference(16),
      subdivisionHeight(8),
      camera(),
      material(defaultMaterial()),
      light1(Vector3Dd(3, -3, 2), ColorRgb(1, 1, 1)),
      light2(Vector3Dd(-2, 5, -2), ColorRgb(0.9, 0.5, 0.5)),
      solid(nullptr),
      csgPreviewOperandA(nullptr),
      csgPreviewOperandB(nullptr),
      faceIndex(-2),
      edgeIndex(-2),
      debugVertices(false),
      quality(),
      qualityController(nullptr),
      cameraController(nullptr),
      canvas(nullptr),
      csgOperation(CsgOperationNames::DIFFERENCE_A_MINUS_B),
      debugEdges(false),
      appelDisplayMode(AppelDisplayMode::OFF),
      showCoordinateSystem(false),
      debugCsg(false),
      errorState(false),
      errorMessage(""),
      mainFrame(nullptr),
      hasWindowedBounds(false),
      fullScreenMode(false),
      hudEnabled(true),
      solidAnimationEnabled(false),
      solidRotationZDegrees(0.0)
{
    camera.setPosition(Vector3Dd(2, -1, 2));
    Matrix4x4d rotationMatrix;
    rotationMatrix = rotationMatrix.eulerAnglesRotation(
        java::Math::toRadians(135), java::Math::toRadians(-35), 0);
    camera.setRotation(rotationMatrix);

    quality.changeWires();
    quality.setShadingType(RendererConfiguration::SHADING_TYPE_PHONG);
    qualityController = new RendererConfigurationController(&quality);
    cameraController = new CameraControllerOrbiter(&camera);

    light1.setId(0);
    light2.setId(1);
}

DebuggerModel::~DebuggerModel()
{
    delete cameraController;
    delete qualityController;
    delete solid;
    delete csgPreviewOperandA;
    delete csgPreviewOperandB;
}

SimpleMaterial DebuggerModel::defaultMaterial()
{
    SimpleMaterial m;

    m = m.withAmbient(ColorRgb(0.2, 0.2, 0.2));
    m = m.withDiffuse(ColorRgb(0.5, 0.5, 0.9));
    m = m.withSpecular(ColorRgb(1, 1, 1));
    m = m.withDoubleSided(false);
    m = m.withPhongExponent(100);
    return m;
}

void DebuggerModel::clearErrorState()
{
    errorState = false;
    errorMessage = "";
}

void DebuggerModel::setErrorState(const java::String& message)
{
    errorState = true;
    errorMessage = message;
    java::System::err.println(
        (java::String("[PolyhedralBoundedSolidExample] ") + errorMessage).c_str());
}

void DebuggerModel::clampSubdivisions()
{
    if ( subdivisionCircumference < MIN_SUBDIVISION_CIRCUMFERENCE ) {
        subdivisionCircumference = MIN_SUBDIVISION_CIRCUMFERENCE;
    }
    if ( subdivisionHeight < MIN_SUBDIVISION_HEIGHT ) {
        subdivisionHeight = MIN_SUBDIVISION_HEIGHT;
    }
}

int DebuggerModel::getFaceCount() const
{
    if ( solid == nullptr ) {
        return 0;
    }
    return (int)solid->getPolygonsList().size();
}

void DebuggerModel::clampFaceIndex()
{
    if ( faceIndex < -2 ) {
        faceIndex = -2;
        return;
    }

    int totalFaces = getFaceCount();
    int maxFaceIndex = totalFaces - 1;
    if ( faceIndex > maxFaceIndex ) {
        faceIndex = maxFaceIndex;
    }
}

SolidModelNames DebuggerModel::getSolidModelName() const
{
    return solidModelName;
}

void DebuggerModel::setSolidModelName(SolidModelNames solidModelName)
{
    this->solidModelName = solidModelName;
}

int DebuggerModel::getSubdivisionCircumference() const
{
    return subdivisionCircumference;
}

void DebuggerModel::setSubdivisionCircumference(int subdivisionCircumference)
{
    this->subdivisionCircumference = subdivisionCircumference;
}

int DebuggerModel::getSubdivisionHeight() const
{
    return subdivisionHeight;
}

void DebuggerModel::setSubdivisionHeight(int subdivisionHeight)
{
    this->subdivisionHeight = subdivisionHeight;
}

Camera* DebuggerModel::getCamera()
{
    return &camera;
}

const SimpleMaterial* DebuggerModel::getMaterial() const
{
    return &material;
}

Light* DebuggerModel::getLight1()
{
    return &light1;
}

Light* DebuggerModel::getLight2()
{
    return &light2;
}

PolyhedralBoundedSolid* DebuggerModel::getSolid() const
{
    return solid;
}

void DebuggerModel::setSolid(PolyhedralBoundedSolid* solid)
{
    if ( this->solid != solid ) {
        delete this->solid;
    }
    this->solid = solid;
}

PolyhedralBoundedSolid* DebuggerModel::getCsgPreviewOperandA() const
{
    return csgPreviewOperandA;
}

void DebuggerModel::setCsgPreviewOperandA(PolyhedralBoundedSolid* csgPreviewOperandA)
{
    if ( this->csgPreviewOperandA != csgPreviewOperandA ) {
        delete this->csgPreviewOperandA;
    }
    this->csgPreviewOperandA = csgPreviewOperandA;
}

PolyhedralBoundedSolid* DebuggerModel::getCsgPreviewOperandB() const
{
    return csgPreviewOperandB;
}

void DebuggerModel::setCsgPreviewOperandB(PolyhedralBoundedSolid* csgPreviewOperandB)
{
    if ( this->csgPreviewOperandB != csgPreviewOperandB ) {
        delete this->csgPreviewOperandB;
    }
    this->csgPreviewOperandB = csgPreviewOperandB;
}

int DebuggerModel::getFaceIndex() const
{
    return faceIndex;
}

void DebuggerModel::setFaceIndex(int faceIndex)
{
    this->faceIndex = faceIndex;
}

int DebuggerModel::getEdgeIndex() const
{
    return edgeIndex;
}

void DebuggerModel::setEdgeIndex(int edgeIndex)
{
    this->edgeIndex = edgeIndex;
}

AppelDisplayMode DebuggerModel::getAppelDisplayMode() const
{
    return appelDisplayMode;
}

void DebuggerModel::setAppelDisplayMode(AppelDisplayMode appelDisplayMode)
{
    this->appelDisplayMode = appelDisplayMode;
}

void DebuggerModel::cycleAppelDisplayMode()
{
    appelDisplayMode = AppelDisplayModeUtil::nextCircular(appelDisplayMode);
}

bool DebuggerModel::notDebugVertices() const
{
    return !debugVertices;
}

void DebuggerModel::setDebugVertices(bool debugVertices)
{
    this->debugVertices = debugVertices;
}

RendererConfiguration* DebuggerModel::getQuality()
{
    return &quality;
}

RendererConfigurationController* DebuggerModel::getQualityController()
{
    return qualityController;
}

CameraController* DebuggerModel::getCameraController()
{
    return cameraController;
}

GLFWwindow* DebuggerModel::getCanvas() const
{
    return canvas;
}

void DebuggerModel::setCanvas(GLFWwindow* canvas)
{
    this->canvas = canvas;
}

CsgOperationNames DebuggerModel::getCsgOperation() const
{
    return csgOperation;
}

void DebuggerModel::setCsgOperation(CsgOperationNames csgOperation)
{
    this->csgOperation = csgOperation;
}

CsgSampleNames DebuggerModel::getCsgSample() const
{
    return csgSample;
}

void DebuggerModel::setCsgSample(CsgSampleNames csgSample)
{
    this->csgSample = csgSample;
    this->csgOperation =
        CsgSampleNamesUtil::getPreferredOperation(csgSample, csgOperation);
}

int DebuggerModel::getKurlanderBowlSingleMotifIndex() const
{
    return kurlanderBowlSingleMotifIndex;
}

void DebuggerModel::setKurlanderBowlSingleMotifIndex(int motifIndex)
{
    kurlanderBowlSingleMotifIndex =
        CsgKurlanderBowlFixture::normalizeSingleMotifIndex(motifIndex);
}

java::String DebuggerModel::getKurlanderBowlSingleMotifLabel() const
{
    return CsgKurlanderBowlFixture::describeSingleMotif(
        kurlanderBowlSingleMotifIndex);
}

bool DebuggerModel::usesKurlanderBowlSingleMotifControls() const
{
    if ( csgSample != CsgSampleNames::KURLANDER_BOWL_SINGLE_MOTIF ) {
        return false;
    }
    return solidModelName == SolidModelNames::CSG_DIRECT ||
        solidModelName == SolidModelNames::CSG_OPERAND1_PARTIAL ||
        solidModelName == SolidModelNames::CSG_OPERAND2_PARTIAL;
}

bool DebuggerModel::isDebugEdges() const
{
    return debugEdges;
}

void DebuggerModel::setDebugEdges(bool debugEdges)
{
    this->debugEdges = debugEdges;
}

bool DebuggerModel::isShowCoordinateSystem() const
{
    return showCoordinateSystem;
}

void DebuggerModel::setShowCoordinateSystem(bool showCoordinateSystem)
{
    this->showCoordinateSystem = showCoordinateSystem;
}

bool DebuggerModel::isDebugCsg() const
{
    return debugCsg;
}

void DebuggerModel::setDebugCsg(bool debugCsg)
{
    this->debugCsg = debugCsg;
}

bool DebuggerModel::isErrorState() const
{
    return errorState;
}

const java::String& DebuggerModel::getErrorMessage() const
{
    return errorMessage;
}

GLFWwindow* DebuggerModel::getMainFrame() const
{
    return mainFrame;
}

void DebuggerModel::setMainFrame(GLFWwindow* mainFrame)
{
    this->mainFrame = mainFrame;
}

const DebuggerModel::WindowBounds* DebuggerModel::getWindowedBounds() const
{
    return hasWindowedBounds ? &windowedBounds : nullptr;
}

void DebuggerModel::setWindowedBounds(const WindowBounds& windowedBounds)
{
    this->windowedBounds = windowedBounds;
    hasWindowedBounds = true;
}

bool DebuggerModel::isFullScreenMode() const
{
    return fullScreenMode;
}

void DebuggerModel::setFullScreenMode(bool fullScreenMode)
{
    this->fullScreenMode = fullScreenMode;
}

bool DebuggerModel::isHudEnabled() const
{
    return hudEnabled;
}

void DebuggerModel::setHudEnabled(bool hudEnabled)
{
    this->hudEnabled = hudEnabled;
}

bool DebuggerModel::isSolidAnimationEnabled() const
{
    return solidAnimationEnabled;
}

void DebuggerModel::setSolidAnimationEnabled(bool solidAnimationEnabled)
{
    this->solidAnimationEnabled = solidAnimationEnabled;
}

void DebuggerModel::toggleSolidAnimationEnabled()
{
    solidAnimationEnabled = !solidAnimationEnabled;
}

double DebuggerModel::getSolidRotationZDegrees() const
{
    return solidRotationZDegrees;
}

void DebuggerModel::rotateSolidAroundZDegrees(double deltaDegrees)
{
    solidRotationZDegrees += deltaDegrees;
}

Matrix4x4d DebuggerModel::getSolidModelMatrix() const
{
    return Matrix4x4d().axisRotation(
        java::Math::toRadians(solidRotationZDegrees), 0, 0, 1);
}
