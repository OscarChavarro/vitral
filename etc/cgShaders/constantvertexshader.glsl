//===========================================================================
//= This vertex shader follows the parameters indicated in the VitralSDK    =
//= shader programming guidelines, as documented in the file                =
//= ./doc/shaderStandards.txt inside the main VitralSDK distribution        =
//= directory.  Please refer to that file to get a clear understanding of   =
//= variable names and common programming patterns used.                    =
//= Note that any variable name change could make the shader incompatible   =
//= with main VitralSDK shader framework!                                   =
//===========================================================================

uniform mat4 modelViewProjectionLocal;    // input: PM * MVM
uniform int withTexture;                  // input: boolean 1 true 0 false
attribute vec4 PObject;                   // input: glVertex3d
attribute vec4 emissionColor;
attribute vec2 uvVertexTextureCoordinate; // input: glTexCoord2d
varying vec4 vertexColor;                 // output
varying float activateTexture;            // output
varying vec2 uvTextureCoordinate;         // output

void main() {
    // Compute homogeneous position of vertex for rasterizer
    gl_Position = modelViewProjectionLocal * PObject;

    // Transform positions and normals from model-space to view-space
    vertexColor = emissionColor;
    if ( withTexture == 1 ) {
        activateTexture = 1.0;
        uvTextureCoordinate.x = uvVertexTextureCoordinate.x;
        uvTextureCoordinate.y = 1.0 - uvVertexTextureCoordinate.y;
      }
      else {
        activateTexture = 0.0;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
