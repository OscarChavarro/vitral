//===========================================================================

precision mediump float;

uniform sampler2D sTexture;
varying vec2 uvTextureCoordinate;

void main() {
    gl_FragColor = texture2D(sTexture, uvTextureCoordinate);
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
