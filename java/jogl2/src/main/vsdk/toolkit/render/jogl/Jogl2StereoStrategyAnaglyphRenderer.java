package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;

/**
Jogl2StereoStrategyAnaglyphRenderer supports two methods for color blending
of anaglyph stereo layers: basic colormask and general blending color
controlled. The later method requires GL_ARG_imaging extension.
*/
public class Jogl2StereoStrategyAnaglyphRenderer extends Jogl2StereoStrategyRenderer
{
    private boolean colorBlendMethod;
    private boolean colormasklr;
    private boolean colormasklg;
    private boolean colormasklb;
    private boolean colormaskrr;
    private boolean colormaskrg;
    private boolean colormaskrb;
    double leftRed;
    double leftGreen;
    double leftBlue;
    double rightRed;
    double rightGreen;
    double rightBlue;

    public Jogl2StereoStrategyAnaglyphRenderer()
    {
        super();
        colorBlendMethod = true;
        colormasklr = true;
        colormasklg = false;
        colormasklb = false;
        colormaskrr = false;
        colormaskrg = false;
        colormaskrb = true;
        leftRed = 1.0;
        leftGreen = 0.0;
        leftBlue = 0.0;
        rightRed = 0.0;
        rightGreen = 0.0;
        rightBlue = 1.0;
    }

    /**
    If `blendColor` is true, the color blending method is activated,
    otherwise, the color masking method is activated.

    Left channel anaglyph image will be combined with color <leftRed, leftGreen, leftBlue>,
    while right channel anaglyph image will be combined with color <rightRed, rightGreen, rightBlue>.

    Note that given combined colors will be exact on the blend color method,
    but approximated when using color mask method. On color mask method,
    any color value below 0.5 will be clamped to "false" value, and
    any color value above 0.5 will be clamped to "true" value.
    */
    public void
    setBlendingMethod(boolean blendColorMethod,
                      double leftRed, double leftGreen, double leftBlue,
                      double rightRed, double rightGreen, double rightBlue)
    {
        colorBlendMethod = blendColorMethod;

        this.leftRed = leftRed;
        this.leftGreen = leftGreen;
        this.leftBlue = leftBlue;
        this.rightRed = rightRed;
        this.rightGreen = rightGreen;
        this.rightBlue = rightBlue;

        colormasklr = false;
        colormasklg = false;
        colormasklb = false;
        colormaskrr = false;
        colormaskrg = false;
        colormaskrb = false;

        if ( leftRed > 0.5 ) {
            colormasklr = true;
        }
        if ( leftGreen > 0.5 ) {
            colormasklg = true;
        }
        if ( leftBlue > 0.5 ) {
            colormasklb = true;
        }
        if ( rightRed > 0.5 ) {
            colormaskrr = true;
        }
        if ( rightGreen > 0.5 ) {
            colormaskrg = true;
        }
        if ( rightBlue > 0.5 ) {
            colormaskrb = true;
        }

    }

    @Override
    public boolean configureDefaultLeftChannel(GL2 gl)
    {
        if ( swapChannels  ) {
            if ( colorBlendMethod ) {
                gl.glBlendFunc(GL2ES2.GL_ONE_MINUS_CONSTANT_COLOR, GL2.GL_CONSTANT_COLOR);
                gl.glBlendColor((float)(1-rightRed), (float)(1-rightGreen), (float)(1-rightBlue), 1);
            }
            else {
                gl.glColorMask(colormaskrr, colormaskrg, colormaskrb, true);
            }
        }
        else {
            if ( colorBlendMethod ) {
                gl.glBlendFunc(GL2ES2.GL_ONE_MINUS_CONSTANT_COLOR, GL2.GL_CONSTANT_COLOR);
                gl.glBlendColor((float)(1-leftRed), (float)(1-leftGreen), (float)(1-leftBlue), 1);
            }
            else {
                gl.glColorMask(colormasklr, colormasklg, colormasklb, true);
            }
        }
        return true;
    }

    @Override
    public boolean configureDefaultRightChannel(GL2 gl)
    {
        if ( swapChannels  ) {
            if ( colorBlendMethod ) {
                gl.glBlendFunc(GL2ES2.GL_ONE_MINUS_CONSTANT_COLOR, GL2.GL_CONSTANT_COLOR);
                gl.glBlendColor((float)(1-leftRed), (float)(1-leftGreen), (float)(1-leftBlue), 1);
            }
            else {
                gl.glColorMask(colormasklr, colormasklg, colormasklb, true);
            }
        }
        else {
            if ( colorBlendMethod ) {
                gl.glBlendFunc(GL2ES2.GL_ONE_MINUS_CONSTANT_COLOR, GL2.GL_CONSTANT_COLOR);
                gl.glBlendColor((float)(1-rightRed), (float)(1-rightGreen), (float)(1-rightBlue), 1);
            }
            else {
                gl.glColorMask(colormaskrr, colormaskrg, colormaskrb, true);
            }
        }
        return true;
    }

    @Override
    public void activateStereoMode(GL2 gl)
    {
        if ( colorBlendMethod ) {
            gl.glEnable(GL.GL_BLEND);
            gl.glEnable(GL2ES2.GL_BLEND_COLOR);
        }
        else {
            gl.glEnable(GL.GL_COLOR_WRITEMASK);
        }
    }

    @Override
    public void deactivateStereoMode(GL2 gl)
    {
        if ( colorBlendMethod ) {
            gl.glDisable(GL.GL_BLEND);
            gl.glDisable(GL2ES2.GL_BLEND_COLOR);
        }
        else {
            gl.glColorMask(true, true, true, true);
            gl.glDisable(GL.GL_COLOR_WRITEMASK);
        }
    }

}
