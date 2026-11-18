#ifndef __OPEN_GL_1_VIEWPORT_WINDOW__
#define __OPEN_GL_1_VIEWPORT_WINDOW__
#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "java/util/ArrayList.h"
class Camera;class CameraControlGizmo;class InputGizmo;class OpenGL1InputGizmoRenderer;class OpenGL1InputGizmoHost;class OpenGL1LabelImageProvider;class RendererConfiguration;class RGBAImageUncompressed;class RotateGizmo;class TranslateGizmo;class Viewport;class ViewportSet;
class OpenGL1ViewportWindow { ViewportSet*set;Viewport*viewport;OpenGL1LabelImageProvider*labels;RGBAImageUncompressed*titleImage;String titleText;ColorRgb titleColor;int titleFontSize;OpenGL1InputGizmoHost*inputGizmoHost;OpenGL1InputGizmoRenderer*inputGizmoRenderer;
// Labels of the faces of the camera control gizmo, by text
ArrayList<String> cameraControlGizmoLabelTexts;ArrayList<RGBAImageUncompressed*> cameraControlGizmoLabelImages;int cameraControlGizmoLabelFontSize;
void discardCameraControlGizmoLabels();
public: enum { RENDER_MODE_ZBUFFER=1,RENDER_MODE_RAYTRACING=2 };OpenGL1ViewportWindow(ViewportSet*,Viewport*,OpenGL1LabelImageProvider*);~OpenGL1ViewportWindow();Viewport*getViewport()const;int getRenderMode()const;int getViewportStartX()const;int getViewportStartY()const;int getViewportSizeX()const;int getViewportSizeY()const;BOOLEAN isSelected()const;BOOLEAN isActive()const;Camera*getCamera()const;RendererConfiguration*getRendererConfiguration()const;void drawReferenceBase();void toggleGrid();void drawGrid();void drawTitle();void drawLabel(RGBAImageUncompressed*,int,int);void drawLabelsForTranslateGizmo(TranslateGizmo*);void drawLabelForRotateGizmoArc(RotateGizmo*);void drawInputGizmo(InputGizmo*);
/**
Draws a camera control gizmo (ViewCube) as a HUD over this viewport, with
the labels of its faces. The gizmo must be already placed in this
viewport (see `CameraControlGizmo::placeInViewport`).
@param gizmo the gizmo
*/
void drawCameraControlGizmo(CameraControlGizmo*gizmo);
/**
Draws a label of a face of the camera control gizmo, centered at (x, y) of
the HUD (y grows downwards), with its image cached by text; used by the
label painter of `drawCameraControlGizmo`.
*/
void drawCameraControlGizmoLabel(CameraControlGizmo*gizmo,const String&label,double x,double y,int fontSize);
void disposeGlResources();void invalidateGlResources();};
#endif
