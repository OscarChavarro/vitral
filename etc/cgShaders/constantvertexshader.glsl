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
attribute vec4 PObject;                   // input: glVertex3d
varying vec4 vertexColor;                 // output

void main() {
    // Compute homogeneous position of vertex for rasterizer
    gl_Position = modelViewProjectionLocal * PObject;
    vertexColor = vec4(1.0, 1.0, 1.0, 1.0);
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
