#ifndef __OPEN_GL_4_SCALE_GIZMO_RENDERER__
#define __OPEN_GL_4_SCALE_GIZMO_RENDERER__
class ScaleGizmo; class Camera;
class OpenGL4ScaleGizmoRenderer { public: static void draw(ScaleGizmo* gizmo, Camera* camera); };
#endif
