//===========================================================================

precision mediump float;

uniform sampler2D sTexture;
varying vec3 vertexColor;
varying float activateTexture;     // input: boolean 1.0 true 0.0 false
varying vec2 uvTextureCoordinate;

void main() {
    if ( activateTexture > 0.0 ) {
        gl_FragColor = vec4(vertexColor, 1.0)*texture2D(sTexture, uvTextureCoordinate);
    }
    else {
        gl_FragColor = vec4(vertexColor, 1.0);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
