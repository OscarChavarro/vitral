//===========================================================================

precision mediump float;

uniform sampler2D sTexture;
varying vec4 vertexColor;
varying float activateTexture;
varying vec2 uvTextureCoordinate;

void main() {
    if ( activateTexture > 0.0 ) {
        gl_FragColor = texture2D(sTexture, uvTextureCoordinate) * vertexColor;
    }
    else {
        gl_FragColor = vertexColor;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
