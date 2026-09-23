#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LineRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SelectionCornersRenderer.h"
namespace { void segment(java::ArrayList<float>&p,java::ArrayList<float>&c,float ax,float ay,float az,float bx,float by,float bz){p.add(ax);p.add(ay);p.add(az);p.add(bx);p.add(by);p.add(bz);for(int i=0;i<2;i++){c.add(1);c.add(1);c.add(0);}} }
void OpenGL4SelectionCornersRenderer::draw(Geometry*g,Camera*c,const Matrix4x4d&m){if(g)draw(g->getMinMax(),c,m);}
void OpenGL4SelectionCornersRenderer::draw(const double*q,Camera*camera,const Matrix4x4d&m){
 if(!q||!camera)return; float d[3]={(float)((q[3]-q[0])*.18),(float)((q[4]-q[1])*.18),(float)((q[5]-q[2])*.18)};java::ArrayList<float>p,c;
 for(int ix=0;ix<2;ix++)for(int iy=0;iy<2;iy++)for(int iz=0;iz<2;iz++){float v[3]={(float)q[ix?3:0],(float)q[iy?4:1],(float)q[iz?5:2]};for(int a=0;a<3;a++){float w[3]={v[0],v[1],v[2]};w[a]+=((a==0?ix:(a==1?iy:iz))?-d[a]:d[a]);segment(p,c,v[0],v[1],v[2],w[0],w[1],w[2]);}}
 OpenGL4LineRenderer::drawLines(camera->calculateProjectionMatrix().multiply(m),p,c,2);
}
