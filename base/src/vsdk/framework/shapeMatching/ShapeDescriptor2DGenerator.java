//===========================================================================

package vsdk.framework.shapeMatching;

// Java classes
import java.util.HashMap;
import java.io.InputStream;

// VSDK Classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Complex;
import vsdk.toolkit.media.FourierShapeDescriptor;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.processing.SignalProcessing;
import vsdk.framework.Component;
import vsdk.toolkit.media.ShapeDescriptor;

/**
This class constitutes a Singleton design pattern: code is not re-entrant!
*/
public class ShapeDescriptor2DGenerator extends Component
{
    /**
    This method returns a shape descriptor. Its specific behavior is implemented
    in each of the subclases.
    */
    public ShapeDescriptor calculateShapeDescriptor(
        InputStream sceneSource,
        String sceneSourceUrl,
        HashMap<String, InputStream> subObjects)
    {
        return null;
    }

    public FourierShapeDescriptor calculateCircularHarmonicsShapeDescriptor(Image distanceField, String label)
    {
        FourierShapeDescriptor fourierShapeDescriptor;
        fourierShapeDescriptor = new FourierShapeDescriptor(label);
        double hi, hr;
        int j, k, val;
        ColorRgb c;
        double r, u, v, tetha;
        Complex function[];
        Complex fourierCoefficients[];

        function = new Complex[64];

        for ( j = 0; j < 32; j++ ) {
            // r varies from 0 to 0.5
            r = (((double)j)/(((double)distanceField.getXSize())));
            for ( k = 0; k < 64; k++ ) {
                tetha = 2*Math.PI * ((double)k) / 64.0;
                u = 0.5 + r * Math.cos(tetha);
                v = 0.5 - r * Math.sin(tetha);
                c = distanceField.getColorRgbBiLinear(u, v);
                val = (int)(((c.r + c.g + c.b)/3.0) * 255.0);
                function[k] = new Complex((double)(val) / 255.0, 0.0);
            }
            fourierCoefficients = SignalProcessing.fft(function);
            for ( k = 0; k < 16; k++ ) {
                hr = fourierCoefficients[k].r;
                hi = fourierCoefficients[k].i;
                fourierShapeDescriptor.setFeature(j, k, hr, hi);
            }
        }

        return fourierShapeDescriptor;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
