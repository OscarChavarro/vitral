//===========================================================================

precision mediump float;

uniform sampler2D sTexture;
//flat varying vec4 vertexColor;
varying vec4 vertexColor;
varying float activateTexture;     // input: boolean 1.0 true 0.0 false
varying vec2 uvTextureCoordinate;
varying float nol;                   // input: cames from an int
varying vec3 Ka;                   // input

void main() {
    float i;
    vec4 diffuseComponent;

    if ( activateTexture > 0.0 ) {
        diffuseComponent = 
            texture2D(sTexture, uvTextureCoordinate) * (vertexColor + vec4(Ka, 1));
    }
    else {
        diffuseComponent = vertexColor + vec4(Ka, 1);
    }

    for ( i = 0.0; i < nol+0.1; i += 1.0 ) {
        gl_FragColor = diffuseComponent;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
