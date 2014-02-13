//===========================================================================

precision mediump float;

uniform sampler2D sTexture;              // input from OpenGL pipeline

uniform highp int withTexture;           // input from CPU: boolean 1 true 0 false
uniform highp int numberOfLights;        // input from CPU
uniform highp vec3 lightColorsGlobal[8]; // input from CPU: maximum light supported: 8
uniform highp vec3 lightPositionsGlobal[8]; // input from CPU
uniform highp vec3 ambientColor;         // input from CPU: material parameter
uniform highp vec3 diffuseColor;         // input from CPU: material parameter
uniform highp vec3 specularColor;        // input from CPU: material parameter
uniform highp float phongExponent;       // input from CPU: material parameter

varying vec4 PGlobal;              // input from vertex shader
varying vec3 N;                    // input from vertex shader
varying vec3 V;                    // input from vertex shader
varying vec2 uvTextureCoordinate;  // input from vertex shader

void main() {
    //---------------------------------------------------------------------
    // Ambient term: implementing equation [FOLE1992].16.2
    vec3 ambientTerm;

    ambientTerm = ambientColor;

    //---------------------------------------------------------------------
    int i;
    vec3 diffuseFactor;
    vec3 diffuseTerm = vec3(0.0, 0.0, 0.0);
    vec3 specularTerm = vec3(0.0, 0.0, 0.0);
    vec3 L;

    for ( i = 0; i < numberOfLights; i++ ) {
        L = normalize(lightPositionsGlobal[i] - PGlobal.xyz);

        // Diffuse term: implementing equation [FOLE1992].16.4
        diffuseFactor = lightColorsGlobal[i] * diffuseColor;
        diffuseTerm += diffuseFactor * max(dot(N, L), 0.0);

        // Specular term: implementing the last term on equation
        // [FOLE1992].16.14
        vec3 R; // Direction of specular reflection (maximum highlights)
        vec3 specularFactor;

        //R = 2.0 * N * dot(N, L) - L; // Equation [FOLE1992].16.16
        R = reflect(-L, N); // Using OpenGLSL implementation of the equation
        specularFactor = lightColorsGlobal[i] * specularColor;
        specularTerm += specularFactor * pow(max(dot(R, V), 0.0), phongExponent);
    }

    //---------------------------------------------------------------------
    if ( withTexture > 0 ) {
        gl_FragColor = vec4(ambientTerm + diffuseTerm + specularTerm, 1.0) * 
            texture2D(sTexture, uvTextureCoordinate);
    }
    else {
        gl_FragColor = vec4(ambientTerm + diffuseTerm + specularTerm, 1.0);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
