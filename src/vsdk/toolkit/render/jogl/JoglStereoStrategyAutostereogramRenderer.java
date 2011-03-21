//===========================================================================

package vsdk.toolkit.render.jogl;

import java.util.Random;

import javax.media.opengl.GL2;
import javax.media.opengl.GLCapabilities;

import vsdk.toolkit.media.ZBuffer;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.AutoStereogramGenerator;
import vsdk.toolkit.render.jogl.JoglImageRenderer;

public class JoglStereoStrategyAutostereogramRenderer extends JoglStereoStrategyRenderer
{
    /// Use for positioning tile pattern on SIRD
    private Random randomNumberGenerator;

    /// Resulting image of the autostereogram generation to be displayed
    private RGBImage stereogramTilePattern;

    /// Resulting image of the autostereogram generation to be displayed
    private RGBImage stereogramResult;

    /// A parameter for AutoStereogramGenerator, in inches
    private double observationDistanceSIRD;

    /// A parameter for AutoStereogramGenerator, in inches
    private double eyeSeparationDistanceSIRD;

    /// A parameter for AutoStereogramGenerator, in inches
    private double maxDistanceSIRD;

    /// A parameter for AutoStereogramGenerator, in inches
    private double minDistanceSIRD;

    /// A parameter for AutoStereogramGenerator, in points per inch
    private int horizontalPPISIRD;

    /// A parameter for AutoStereogramGenerator, in points per inch
    private int verticalPPISIRD;

    public JoglStereoStrategyAutostereogramRenderer(RGBImage stereogramTilePattern)
    {
        super();
        this.stereogramTilePattern = stereogramTilePattern;

        randomNumberGenerator = null;
        stereogramTilePattern = null;
        stereogramResult = null;

        // Set some common default values for SIRD parameters
        observationDistanceSIRD = 14.0;
        eyeSeparationDistanceSIRD = 2.3228;
        maxDistanceSIRD = 12;
        minDistanceSIRD = 6;
        horizontalPPISIRD = 112;
        verticalPPISIRD = 112;
    }

    public JoglStereoStrategyAutostereogramRenderer(
        RGBImage stereogramTilePattern,
        double observationDistanceSIRD,
        double eyeSeparationDistanceSIRD,
        double maxDistanceSIRD,
        double minDistanceSIRD,
        int horizontalPPISIRD,
        int verticalPPISIRD
    )
    {
        super();
        this.stereogramTilePattern = stereogramTilePattern;

        randomNumberGenerator = null;
        stereogramTilePattern = null;
        stereogramResult = null;

        // Set some common default values for SIRD parameters
        this.observationDistanceSIRD = observationDistanceSIRD;
        this.eyeSeparationDistanceSIRD = eyeSeparationDistanceSIRD;
        this.maxDistanceSIRD = maxDistanceSIRD;
        this.minDistanceSIRD = minDistanceSIRD;
        this.horizontalPPISIRD = horizontalPPISIRD;
        this.verticalPPISIRD = verticalPPISIRD;
    }

    private double
    getObservationDistanceSIRD()
    {
        return observationDistanceSIRD;
    }

    public void
    setObservationDistanceSIRD(double observationDistanceSIRD)
    {
        this.observationDistanceSIRD = observationDistanceSIRD;
    }

    public double
    getEyeSeparationDistanceSIRD()
    {
        return eyeSeparationDistanceSIRD;
    }

    public void
    setEyeSeparationDistanceSIRD(double eyeSeparationDistanceSIRD)
    {
        this.eyeSeparationDistanceSIRD = eyeSeparationDistanceSIRD;
    }

    public double
    getMaxDistanceSIRD()
    {
        return maxDistanceSIRD;
    }

    public void
    setMaxDistanceSIRD(double maxDistanceSIRD)
    {
        this.maxDistanceSIRD = maxDistanceSIRD;
    }

    public double
    getMinDistanceSIRD()
    {
        return minDistanceSIRD;
    }

    public void
    setMinDistanceSIRD(double minDistanceSIRD)
    {
        this.minDistanceSIRD = minDistanceSIRD;
    }

    public int
    getHorizontalPPISIRD()
    {
        return horizontalPPISIRD;
    }

    public void
    setHorizontalPPISIRD(int horizontalPPISIRD)
    {
        this.horizontalPPISIRD = horizontalPPISIRD;
    }

    public int
    getVerticalPPISIRD()
    {
        return verticalPPISIRD;
    }

    public void
    setVerticalPPISIRD(int verticalPPISIRD)
    {
        this.verticalPPISIRD = verticalPPISIRD;
    }

    public boolean configureDefaultLeftChannel(GL2 gl)
    {
        return false;
    }

    public boolean configureDefaultRightChannel(GL2 gl)
    {
        return true;
    }

    public void activateStereoMode(GL2 gl)
    {
        ;
    }

    public void deactivateStereoMode(GL2 gl)
    {
        ZBuffer zbuffer;

        zbuffer = JoglZBufferRenderer.importJOGLZBuffer(gl);

        if ( randomNumberGenerator == null ) {
            randomNumberGenerator = new Random();
        }

        if ( stereogramResult == null ) {
            stereogramResult = new RGBImage();
        }

        if ( zbuffer.getXSize() != stereogramResult.getXSize() ||
             zbuffer.getYSize() != stereogramResult.getYSize() ) {
            stereogramResult.initNoFill(zbuffer.getXSize(), zbuffer.getYSize());
        }

        if ( stereogramTilePattern == null ) {
            stereogramTilePattern = new RGBImage();
            stereogramTilePattern.initNoFill(64, 64);
            stereogramTilePattern.createTestPattern();
        }

        stereogramResult.createTestPattern();

        AutoStereogramGenerator.generate(
            stereogramResult, stereogramTilePattern, zbuffer,
            observationDistanceSIRD, eyeSeparationDistanceSIRD,
            maxDistanceSIRD, minDistanceSIRD,
            horizontalPPISIRD, verticalPPISIRD, 
            randomNumberGenerator.nextInt(),
            randomNumberGenerator.nextInt());

        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        gl.glClear(gl.GL_DEPTH_BUFFER_BIT);

        JoglImageRenderer.draw(gl, stereogramResult);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
