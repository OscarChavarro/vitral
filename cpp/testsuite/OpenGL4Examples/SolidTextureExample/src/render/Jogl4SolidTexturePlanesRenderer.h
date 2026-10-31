#ifndef __JOGL4_SOLID_TEXTURE_PLANES_RENDERER_CPP__
#define __JOGL4_SOLID_TEXTURE_PLANES_RENDERER_CPP__


#include "java/util/ArrayList.h"

class Camera;
class Image;
class InfinitePlane;

class Jogl4SolidTexturePlanesRenderer {
private:
    unsigned int vaoId;
    unsigned int positionVboId;
    unsigned int normalVboId;
    unsigned int uvVboId;

    void ensureBuffers();
    void buildPlaneFrame(int planeCount, java::ArrayList<float>& positions,
                         java::ArrayList<float>& normals, java::ArrayList<float>& uvs);
    void uploadFrame(const java::ArrayList<float>& positions,
                     const java::ArrayList<float>& normals,
                     const java::ArrayList<float>& uvs);
    void configureClippingPlane(unsigned int programId,
                                InfinitePlane* clippingPlane);

public:
    Jogl4SolidTexturePlanesRenderer();
    void draw(java::ArrayList<Image*>& images, Camera* camera,
              InfinitePlane* clippingPlane);
    void dispose();
};

#endif
