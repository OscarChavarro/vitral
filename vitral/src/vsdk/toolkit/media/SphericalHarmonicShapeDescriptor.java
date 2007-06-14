//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 18 2007 - Oscar Chavarro: Original base version                   =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. "A Search Engine for 3D      =
//=     Models", ACM Transactions on Graphics, Vol 22. No1. January 2003.   =
//=     Pp. 83-105                                                          =
//===========================================================================

package vsdk.toolkit.media;

/**
Stores the feature vector for a 32 concentric spheres
around a volume as described in [FUNK2003]
*/
public class SphericalHarmonicShapeDescriptor extends ShapeDescriptor
{
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20061220L;

    private double featureVector[];
    private static final int numberOfSpheres = 32;
    private static final int numberOfHarmonics = 16;

    public SphericalHarmonicShapeDescriptor()
    {
        featureVector = new double[numberOfSpheres*numberOfHarmonics];
    }

    /**
    Set the Fourier transform (spherical harmonic) for sphere `sphere`, harmonic `harmonic` to complex value <r, i>
    */
    public void setFeature(int sphere, int harmonic, double r, double i)
    {
        if ( sphere < 0 || sphere >= numberOfSpheres || harmonic < 0 || harmonic >= numberOfHarmonics ) {
            return;
        }
        double harmonicAmplitude = Math.sqrt(r*r + i*i);
        featureVector[sphere*numberOfHarmonics+harmonic] = harmonicAmplitude;
    }

    public String toString()
    {
        String msg;
        msg = "SphericalHarmonics amplitudes for " + numberOfSpheres + " spheres and " + numberOfHarmonics + " harmonics:\n";
        int i;
        for ( i = 0; i < numberOfSpheres*numberOfHarmonics; i++ ) {
            msg += "  - " + featureVector[i] + "\n";
        }
        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
