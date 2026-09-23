#ifndef __OPEN_GL_4_ROTATE_GIZMO_RENDERER__
#define __OPEN_GL_4_ROTATE_GIZMO_RENDERER__
class RotateGizmo; class Camera;
class OpenGL4RotateGizmoRenderer { public: static void draw(RotateGizmo* gizmo, Camera* camera); };
#endif
