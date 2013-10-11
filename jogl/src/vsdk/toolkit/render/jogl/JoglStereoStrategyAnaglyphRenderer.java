//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;

/**
JoglStereoStrategyAnaglyphRenderer supports two methods for color blending
of anaglyph stereo layers: basic colormask and general blending color
controlled. The later method requires GL_ARG_imaging extension.
*/
public class JoglStereoStrategyAnaglyphRenderer extends JoglStereoStrategyRenderer
{
    private boolean colorBlendMethod;
    private boolean colormasklr;
    private boolean colormasklg;
    private boolean colormasklb;
    private boolean colormaskrr;
    private boolean colormaskrg;
    private boolean colormaskrb;
    double lr;
    double lg;
    double lb;
    double rr;
    double rg;
    double rb;

    public JoglStereoStrategyAnaglyphRenderer()
    {
        super();
        colorBlendMethod = true;
        colormasklr = true;
        colormasklg = false;
        colormasklb = false;
        colormaskrr = false;
        colormaskrg = false;
        colormaskrb = true;
        lr = 1.0;
        lg = 0.0;
        lb = 0.0;
        rr = 0.0;
        rg = 0.0;
        rb = 1.0;
    }

    /**
    If `blendColor` is true, the color blending method is activated,
    otherwise, the color masking method is activated.

    Left channel anaglyph image will be combined with color <lr, lg, lb>,
    while right channel anaglyph image will be combined with color <rr, rg, rb>.

    Note that given combined colors will be exact on the blend color method,
    but approximated when using color mask method. On color mask method,
    any color value below 0.5 will be clamped to "false" value, and
    any color value above 0.5 will be clamped to "true" value.
    */
    public void
    setBlendingMethod(boolean blendColorMethod,
                      double lr, double lg, double lb,
                      double rr, double rg, double rb)
    {
        colorBlendMethod = blendColorMethod;

        this.lr = lr;
        this.lg = lg;
        this.lb = lb;
        this.rr = rr;
        this.rg = rg;
        this.rb = rb;

        colormasklr = false;
        colormasklg = false;
        colormasklb = false;
        colormaskrr = false;
        colormaskrg = false;
        colormaskrb = false;

        if ( lr > 0.5 ) {
            colormasklr = true;
        }
        if ( lg > 0.5 ) {
            colormasklg = true;
        }
        if ( lb > 0.5 ) {
            colormasklb = true;
        }
        if ( rr > 0.5 ) {
            colormaskrr = true;
        }
        if ( rg > 0.5 ) {
            colormaskrg = true;
        }
        if ( rb > 0.5 ) {
            colormaskrb = true;
        }

    }

    @Override
    public boolean configureDefaultLeftChannel(GL2 gl)
    {
        if ( swapChannels  ) {
            if ( colorBlendMethod ) {
                gl.glBlendFunc(GL2ES2.GL_ONE_MINUS_CONSTANT_COLOR, GL2.GL_CONSTANT_COLOR);
                gl.glBlendColor((float)(1-rr), (float)(1-rg), (float)(1-rb), 1);
            }
            else {
                gl.glColorMask(colormaskrr, colormaskrg, colormaskrb, true);
            }
        }
        else {
            if ( colorBlendMethod ) {
                gl.glBlendFunc(GL2ES2.GL_ONE_MINUS_CONSTANT_COLOR, GL2.GL_CONSTANT_COLOR);
                gl.glBlendColor((float)(1-lr), (float)(1-lg), (float)(1-lb), 1);
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
                gl.glBlendColor((float)(1-lr), (float)(1-lg), (float)(1-lb), 1);
            }
            else {
                gl.glColorMask(colormasklr, colormasklg, colormasklb, true);
            }
        }
        else {
            if ( colorBlendMethod ) {
                gl.glBlendFunc(GL2ES2.GL_ONE_MINUS_CONSTANT_COLOR, GL2.GL_CONSTANT_COLOR);
                gl.glBlendColor((float)(1-rr), (float)(1-rg), (float)(1-rb), 1);
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

//===========================================================================
//= EOF                                                                     =
//===========================================================================
