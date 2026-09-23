#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LineRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MinMaxRenderer.h"
namespace {
void edge(java::ArrayList<float>& p, java::ArrayList<float>& c,
          float ax,float ay,float az,float bx,float by,float bz) {
    p.add(ax);p.add(ay);p.add(az);p.add(bx);p.add(by);p.add(bz);
    for(int i=0;i<2;i++){c.add(1);c.add(1);c.add(0);}
}
}
void OpenGL4MinMaxRenderer::draw(Geometry* g, Camera* c, const Matrix4x4d& m) { if(g) draw(g->getMinMax(),c,m); }
void OpenGL4MinMaxRenderer::draw(const double* q, Camera* camera, const Matrix4x4d& m) {
    if(!q||!camera)return; float x0=q[0],y0=q[1],z0=q[2],x1=q[3],y1=q[4],z1=q[5];
    java::ArrayList<float> p,c;
    edge(p,c,x0,y0,z0,x1,y0,z0);edge(p,c,x0,y0,z1,x1,y0,z1);edge(p,c,x0,y1,z0,x1,y1,z0);edge(p,c,x0,y1,z1,x1,y1,z1);
    edge(p,c,x0,y0,z0,x0,y1,z0);edge(p,c,x1,y0,z0,x1,y1,z0);edge(p,c,x0,y0,z1,x0,y1,z1);edge(p,c,x1,y0,z1,x1,y1,z1);
    edge(p,c,x0,y0,z0,x0,y0,z1);edge(p,c,x1,y0,z0,x1,y0,z1);edge(p,c,x0,y1,z0,x0,y1,z1);edge(p,c,x1,y1,z0,x1,y1,z1);
    OpenGL4LineRenderer::drawLines(camera->calculateProjectionMatrix().multiply(m),p,c,1);
}
void OpenGL4MinMaxRenderer::dispose(){OpenGL4LineRenderer::release();}
