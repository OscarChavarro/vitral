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
uniform mat4 modelViewITLocal;         // input: MVM_IT
uniform vec4 lightPositionGlobal;
attribute vec4 PObject;                // input: glVertex3d
attribute vec3 NObject;
uniform vec3 ambientColor;             // input: material parameters
uniform vec3 diffuseColor;
uniform vec3 specularColor;
varying vec4 vertexColor;              // output: color to pass to pixel shader

void main() {
    vec4 PGlobal = modelViewProjectionLocal * PObject;

    // Ambient term: implementing equation [FOLE1992].16.2
    vec3 ambientTerm;
    ambientTerm = 0.0*ambientColor;

    // Diffuse term: implementing equation [FOLE1992].16.4
    vec3 diffuseTerm;
    vec3 N;
    vec3 L;

    N = normalize(vec3(modelViewITLocal * vec4(NObject, 1.0)));
    L = normalize(lightPositionGlobal.xyz - PGlobal.xyz);
    diffuseTerm = diffuseColor * max(dot(N, L), 0.0);

    // Specular term
    vec3 specularTerm;
    specularTerm = 0.0*specularColor;

    // Output results to pixel shader
    vertexColor = vec4(ambientTerm + diffuseTerm + specularTerm, 1);
    gl_Position = PGlobal;
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
