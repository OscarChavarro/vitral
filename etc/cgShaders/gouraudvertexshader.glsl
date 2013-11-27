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
uniform vec3 lightPositionsGlobal[8];  // input: maximum light supported: 8
uniform vec3 lightColorsGlobal[8];  // input: maximum light supported: 8
uniform int numberOfLights;
uniform int withTexture;               // input: boolean 1 true 0 false
attribute vec4 PObject;                // input: glVertex3d
attribute vec3 NObject;                // input: glNormal3d
attribute vec2 uvVertexTextureCoordinate; // input: glTexCoord2d
uniform vec3 ambientColor;             // input: material parameters
uniform vec3 diffuseColor;
uniform vec3 specularColor;
uniform float phongExponent;
//flat varying vec4 vertexColor;              // output: color to pass to pixel shader
varying vec4 vertexColor;              // output: color to pass to pixel shader
varying float activateTexture;         // output
varying vec2 uvTextureCoordinate;      // output

void main() {
    vec4 PGlobal = modelViewLocal * PObject;

    // Ambient term: implementing equation [FOLE1992].16.2
    vec3 ambientTerm;
    ambientTerm = ambientColor;

    int i;
    vec3 diffuseTerm = vec3(0, 0, 0);
    vec3 specularTerm = vec3(0, 0, 0);

    for ( i = 0; i < numberOfLights; i++ ) {
        // Diffuse term: implementing equation [FOLE1992].16.4
        vec3 N; // Normal for current vertex on global coordinates
        vec3 L; // Direction to the light source
        vec3 diffuseFactor;

        N = normalize(vec3(modelViewITLocal * vec4(NObject, 1.0)));
        L = normalize(lightPositionsGlobal[i] - PGlobal.xyz);
        diffuseFactor = lightColorsGlobal[i] * diffuseColor;
        diffuseTerm += diffuseFactor * max(dot(N, L), 0.0);

        // Specular term: implementing the last term on equation
        // [FOLE1992].16.14
        vec3 V; // Direction to the viewpoint from current vertex
        vec3 R; // Direction of specular reflection (maximum highlights)
        vec3 specularFactor;

        V = normalize(cameraPositionGlobal - PGlobal.xyz);
        //R = 2.0 * N * dot(N, L) - L; // Equation [FOLE1992].16.16
        R = reflect(-L, N); // Using OpenGLSL implementation of the equation
        specularFactor = lightColorsGlobal[i] * specularColor;
        specularTerm += specularFactor * pow(max(dot(R, V), 0.0), phongExponent);
    }

    // Output results to pixel shader
    gl_Position = modelViewProjectionLocal * PObject;
    vertexColor = vec4(ambientTerm + diffuseTerm + specularTerm, 1.0);
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
