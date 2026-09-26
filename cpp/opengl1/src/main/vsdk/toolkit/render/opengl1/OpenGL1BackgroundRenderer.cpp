#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/background/Background.h"
#include "vsdk/toolkit/environment/background/SimpleBackground.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ColoredPrimitiveRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1BackgroundRenderer.h"
#include "java/util/ArrayList.txx"

void OpenGL1BackgroundRenderer::draw(Background* background)
{
    SimpleBackground* simple = dynamic_cast<SimpleBackground*>(background);
    if ( simple == 0 ) return;
    ColorRgb color = simple->colorInDireccion(Vector3Dd(1, 0, 0));
    const float coordinates[] = {-1,-1,0, 1,-1,0, -1,1,0, 1,1,0};
    java::ArrayList<float> positions, colors;
    for ( int i = 0; i < 12; ++i ) positions.add(coordinates[i]);
    for ( int i = 0; i < 4; ++i ) {
        colors.add((float)color.r()); colors.add((float)color.g());
        colors.add((float)color.b()); colors.add(1.0f);
    }
    glDisable(GL_DEPTH_TEST); glDisable(GL_BLEND); glDisable(GL_CULL_FACE);
    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    OpenGL1ColoredPrimitiveRenderer::draw(
        Matrix4x4d::identityMatrix(), GL_TRIANGLE_STRIP, positions, colors);
    glEnable(GL_DEPTH_TEST);
}
