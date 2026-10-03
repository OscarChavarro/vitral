#include "vsdk/toolkit/render/opengl4/OpenGL4SimpleMaterialRenderer.h"

SimpleMaterial OpenGL4SimpleMaterialRenderer::activeMaterial;

void OpenGL4SimpleMaterialRenderer::activate(const SimpleMaterial* material)
{
    if ( material == nullptr ) {
        activeMaterial = SimpleMaterial();
        return;
    }
    activeMaterial = SimpleMaterial(*material);
}

SimpleMaterial OpenGL4SimpleMaterialRenderer::getActiveMaterial()
{
    return SimpleMaterial(activeMaterial);
}
