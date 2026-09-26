#ifndef __OPEN_GL_1_SCALE_GIZMO_RENDERER__
#define __OPEN_GL_1_SCALE_GIZMO_RENDERER__
class ScaleGizmo; class Camera;
class OpenGL1ScaleGizmoRenderer { public: static void draw(ScaleGizmo* gizmo, Camera* camera); };
#endif
