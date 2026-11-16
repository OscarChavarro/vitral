#ifndef __POLYHEDRAL_EXAMPLE_DEBUGGER_MODEL__
#define __POLYHEDRAL_EXAMPLE_DEBUGGER_MODEL__

#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "models/AppelDisplayMode.h"
#include "models/CsgOperationNames.h"
#include "models/CsgSampleNames.h"
#include "models/SolidModelNames.h"

struct GLFWwindow;
class CameraController;
class PolyhedralBoundedSolid;
class RendererConfigurationController;

/**
C++ port of Java's `models.DebuggerModel`.

C++ port notes:
- The Java `GLCanvas` and `JFrame` are both the GLFW window.
- The Java `Rectangle` of the windowed bounds is `WindowBounds`.
- The model owns the solid and the CSG preview operands: setting a new one
  deletes the previous one.
*/
class DebuggerModel {
public:
    /// Java: `java.awt.Rectangle`
    class WindowBounds {
    public:
        int x;
        int y;
        int width;
        int height;

        WindowBounds() : x(0), y(0), width(0), height(0) {}
        WindowBounds(int x, int y, int width, int height)
            : x(x), y(y), width(width), height(height)
        {
        }
    };

    DebuggerModel();
    ~DebuggerModel();

    void clearErrorState();
    void setErrorState(const java::String& message);
    void clampSubdivisions();
    int getFaceCount() const;
    void clampFaceIndex();

    SolidModelNames getSolidModelName() const;
    void setSolidModelName(SolidModelNames solidModelName);
    int getSubdivisionCircumference() const;
    void setSubdivisionCircumference(int subdivisionCircumference);
    int getSubdivisionHeight() const;
    void setSubdivisionHeight(int subdivisionHeight);
    Camera* getCamera();
    const SimpleMaterial* getMaterial() const;
    Light* getLight1();
    Light* getLight2();
    PolyhedralBoundedSolid* getSolid() const;
    void setSolid(PolyhedralBoundedSolid* solid);
    PolyhedralBoundedSolid* getCsgPreviewOperandA() const;
    void setCsgPreviewOperandA(PolyhedralBoundedSolid* csgPreviewOperandA);
    PolyhedralBoundedSolid* getCsgPreviewOperandB() const;
    void setCsgPreviewOperandB(PolyhedralBoundedSolid* csgPreviewOperandB);
    int getFaceIndex() const;
    void setFaceIndex(int faceIndex);
    int getEdgeIndex() const;
    void setEdgeIndex(int edgeIndex);
    AppelDisplayMode getAppelDisplayMode() const;
    void setAppelDisplayMode(AppelDisplayMode appelDisplayMode);
    void cycleAppelDisplayMode();
    bool notDebugVertices() const;
    void setDebugVertices(bool debugVertices);
    RendererConfiguration* getQuality();
    RendererConfigurationController* getQualityController();
    CameraController* getCameraController();
    GLFWwindow* getCanvas() const;
    void setCanvas(GLFWwindow* canvas);
    CsgOperationNames getCsgOperation() const;
    void setCsgOperation(CsgOperationNames csgOperation);
    CsgSampleNames getCsgSample() const;
    void setCsgSample(CsgSampleNames csgSample);
    int getKurlanderBowlSingleMotifIndex() const;
    void setKurlanderBowlSingleMotifIndex(int motifIndex);
    java::String getKurlanderBowlSingleMotifLabel() const;
    bool usesKurlanderBowlSingleMotifControls() const;
    bool isDebugEdges() const;
    void setDebugEdges(bool debugEdges);
    bool isShowCoordinateSystem() const;
    void setShowCoordinateSystem(bool showCoordinateSystem);
    bool isDebugCsg() const;
    void setDebugCsg(bool debugCsg);
    bool isErrorState() const;
    const java::String& getErrorMessage() const;
    GLFWwindow* getMainFrame() const;
    void setMainFrame(GLFWwindow* mainFrame);
    /// @return null when not set
    const WindowBounds* getWindowedBounds() const;
    void setWindowedBounds(const WindowBounds& windowedBounds);
    bool isFullScreenMode() const;
    void setFullScreenMode(bool fullScreenMode);
    bool isHudEnabled() const;
    void setHudEnabled(bool hudEnabled);
    bool isSolidAnimationEnabled() const;
    void setSolidAnimationEnabled(bool solidAnimationEnabled);
    void toggleSolidAnimationEnabled();
    double getSolidRotationZDegrees() const;
    void rotateSolidAroundZDegrees(double deltaDegrees);
    Matrix4x4d getSolidModelMatrix() const;

private:
    static const int MIN_SUBDIVISION_CIRCUMFERENCE = 3;
    static const int MIN_SUBDIVISION_HEIGHT = 1;

    SolidModelNames solidModelName;
    CsgSampleNames csgSample;
    int kurlanderBowlSingleMotifIndex;
    int subdivisionCircumference;
    int subdivisionHeight;
    Camera camera;
    SimpleMaterial material;
    PointLight light1;
    PointLight light2;
    PolyhedralBoundedSolid* solid;
    PolyhedralBoundedSolid* csgPreviewOperandA;
    PolyhedralBoundedSolid* csgPreviewOperandB;
    int faceIndex;
    int edgeIndex;
    bool debugVertices;
    RendererConfiguration quality;
    RendererConfigurationController* qualityController;
    CameraController* cameraController;
    GLFWwindow* canvas;
    CsgOperationNames csgOperation;
    bool debugEdges;
    AppelDisplayMode appelDisplayMode;
    bool showCoordinateSystem;
    bool debugCsg;
    bool errorState;
    java::String errorMessage;
    GLFWwindow* mainFrame;
    bool hasWindowedBounds;
    WindowBounds windowedBounds;
    bool fullScreenMode;
    bool hudEnabled;
    bool solidAnimationEnabled;
    double solidRotationZDegrees;

    static SimpleMaterial defaultMaterial();

    DebuggerModel(const DebuggerModel&);
    DebuggerModel& operator=(const DebuggerModel&);
};

#endif
