#include <GL/glew.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/gui/gizmo/RotateGizmo.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ColoredPrimitiveRenderer.h"
#include "vsdk/toolkit/render/opengl4/gizmo/OpenGL4RotateGizmoRenderer.h"
namespace{void strip(const java::ArrayList<Vector3Dd>&v,const ColorRgb&x,const Matrix4x4d&m){java::ArrayList<float>p,c;for(long i=0;i<v.size();i++){p.add(v[i].x());p.add(v[i].y());p.add(v[i].z());c.add(x.r());c.add(x.g());c.add(x.b());c.add(1);}OpenGL4ColoredPrimitiveRenderer::draw(m,GL_TRIANGLE_STRIP,p,c);}}
void OpenGL4RotateGizmoRenderer::draw(RotateGizmo*g,Camera*c){if(!g||!c)return;g->setCamera(c);g->updateGeometryState();Matrix4x4d m=c->calculateProjectionMatrix();for(int r=0;r<RotateGizmo::RING_COUNT;r++){java::ArrayList<java::ArrayList<Vector3Dd> >a=g->buildRingStrips(r);for(long i=0;i<a.size();i++)strip(a[i],g->getRingColor(r),m);}strip(g->buildCameraRingStrip(),g->getCameraRingColor(),m);if(g->isArcVisible())strip(g->buildArcFan(),g->getArcColor(),m);}
