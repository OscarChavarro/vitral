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
uniform int numberOfLights;
attribute vec4 PObject;                // input: glVertex3d
attribute vec3 NObject;                // input: glNormal3d
uniform vec3 ambientColor;             // input: material parameters
uniform vec3 diffuseColor;
uniform vec3 specularColor;
uniform float phongExponent;
varying vec3 vertexColor;              // output: color to pass to pixel shader

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

        N = normalize(vec3(modelViewITLocal * vec4(NObject, 1.0)));
        L = normalize(lightPositionsGlobal[i] - PGlobal.xyz);
        diffuseTerm += diffuseColor * max(dot(N, L), 0.0);

        // Specular term: implementing the last term on equation
        // [FOLE1992].16.14
        vec3 V; // Direction to the viewpoint from current vertex
        vec3 R; // Direction of specular reflection (maximum highlights)

        V = normalize(cameraPositionGlobal - PGlobal.xyz);
        //R = 2.0 * N * dot(N, L) - L; // Equation [FOLE1992].16.16
        R = reflect(-L, N); // Using OpenGLSL implementation of the equation
        specularTerm += specularColor * pow(max(dot(R, V), 0.0), phongExponent);
    }

    // Output results to pixel shader
    vertexColor = ambientTerm + diffuseTerm + specularTerm;
    gl_Position = modelViewProjectionLocal * PObject;
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
