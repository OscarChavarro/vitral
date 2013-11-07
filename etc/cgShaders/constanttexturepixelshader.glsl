//===========================================================================

precision mediump float;

uniform sampler2D sTexture;
varying vec4 vertexColor;
varying vec2 uvTextureCoordinate;

void main() {
    gl_FragColor = texture2D(sTexture, uvTextureCoordinate) * vertexColor;
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
