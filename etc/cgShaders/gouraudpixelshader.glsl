//===========================================================================

precision mediump float;

uniform highp int withTexture;               // input: boolean 1 true 0 false

uniform sampler2D sTexture;

//flat varying vec4 vertexColor;
varying vec4 vertexColor;
varying vec2 uvTextureCoordinate;

void main() {
    if ( withTexture > 0 ) {
        gl_FragColor = texture2D(sTexture, uvTextureCoordinate) * vertexColor;    }
    else {
        gl_FragColor = vertexColor;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
