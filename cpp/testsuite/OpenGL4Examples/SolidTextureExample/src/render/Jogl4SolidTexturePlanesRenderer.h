#ifndef __JOGL4_SOLID_TEXTURE_PLANES_RENDERER_CPP__
#define __JOGL4_SOLID_TEXTURE_PLANES_RENDERER_CPP__

#include <vector>

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
    void buildPlaneFrame(int planeCount, std::vector<float>& positions,
                         std::vector<float>& normals, std::vector<float>& uvs);
    void uploadFrame(const std::vector<float>& positions,
                     const std::vector<float>& normals,
                     const std::vector<float>& uvs);
    void configureClippingPlane(unsigned int programId,
                                InfinitePlane* clippingPlane);

public:
    Jogl4SolidTexturePlanesRenderer();
    void draw(java::ArrayList<Image*>& images, Camera* camera,
              InfinitePlane* clippingPlane);
    void dispose();
};

#endif
