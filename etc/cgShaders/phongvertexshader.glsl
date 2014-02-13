//===========================================================================
//= This vertex shader follows the parameters indicated in the VitralSDK    =
//= shader programming guidelines, as documented in the file                =
//= ./doc/shaderStandards.txt inside the main VitralSDK distribution        =
//= directory.  Please refer to that file to get a clear understanding of   =
//= variable names and common programming patterns used.                    =
//= Note that any variable name change could make the shader incompatible   =
//= with main VitralSDK shader framework!                                   =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =
//===========================================================================

uniform mat4 modelViewProjectionLocal; // input: PM * MVM
uniform mat4 modelViewLocal;           // input: MVM
uniform mat4 modelViewITLocal;         // input: MVM_IT
uniform vec3 cameraPositionGlobal;
uniform int numberOfLights;
uniform vec3 lightPositionsGlobal[8];  // input: maximum light supported: 8
uniform vec3 lightColorsGlobal[8];     // input: maximum light supported: 8
uniform int withTexture;               // input: boolean 1 true 0 false
attribute vec4 PObject;                // input: glVertex3d
attribute vec3 NObject;                // input: glNormal3d
attribute vec2 uvVertexTextureCoordinate; // input: glTexCoord2d
uniform vec3 diffuseColor;
uniform vec3 specularColor;
uniform float phongExponent;

varying vec4 PGlobal; // Vertex postion
varying vec3 N; // Normal for current vertex on global coordinates
varying vec3 V; // Direction to the viewpoint from current vertex
varying vec2 uvTextureCoordinate;      // output

void main() {
    // Current position is computed on a per-vertex basis, and passed
    // to pixel shader as an interpolated value
    PGlobal = modelViewLocal * PObject;

    // Current normal is computed on a per-vertex basis, and passed
    // to pixel shader as an interpolated value
    N = normalize(vec3(modelViewITLocal * vec4(NObject, 1.0)));

    // Current vector from surface to camera is computed
    // on a per-vertex basis, and passed to pixel shader
    // as an interpolated value
    V = normalize(cameraPositionGlobal - PGlobal.xyz);

    // Pass texture coordinates
    if ( withTexture == 1 ) {
        uvTextureCoordinate.x = uvVertexTextureCoordinate.x;
        uvTextureCoordinate.y = uvVertexTextureCoordinate.y;
    }

    // Pass 2D projected vertex position
    gl_Position = modelViewProjectionLocal * PObject;
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
