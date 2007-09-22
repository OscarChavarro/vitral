//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 21 2006 - Oscar Chavarro: Original base version                =
//===========================================================================

package scivis;

// Java classes
import java.util.ArrayList;

// VitralSDK classes
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.media.Image;


/**
A `Study` is a data/information container for a project in scientific
visualization scheme, where 3D models are to be obtained from a set
of image slices.

The Study contains five (5) main aspects:
  - A set of images (the "original" images)
  - A set of segmented images (each one corresponding to original images)
  - A set of internationalized labels for segment identifier in segmented
    images
  - An organization for images based in time takes and slices
  - Unit / calibration values for current input devices used in image
    adquisition

As usual VSDK design-based applications, this is a model only class
which is complemented by persistence classes (io package) and rendering
classes (render packages).

The amount of storage needed and/or used for a current Study can exceed
the RAM or hard disk storage capacity for a given computer system. For
dealing with this, a Study need/uses the RAM needed only for current
editing/visualized slice in a time take. For other slices a virtual
memory scheme is implemented over a distributed computing infrastructure.
*/
public class Study
{
    /**
    This attribute determines the current XYZ dimension ratio for voxels
    in this study. Note that from this value and `unitsInMetersExponent`, 
    current volume measures can be derived, given a voxel count for each
    dimension.
    */
    private Vector3D voxelSizeFactor;

    /**
    The `unitsInMetersExponent` determines the interpretation of units
    for all the volume, combined with the `voxelSizeFactor` attribute.
    This attribute is in integer exponents of 10, and is mapped to
    meters.  For example, if this attribute's value is 0, this means
    that current units are in 10^0*m (that is meters). If it's value is 
    3, current units are in 10^3*m (that is kilometers). If it's value
    is -3, current units are in 10^-3m (that is milimeters) and so on.
    */
    private int unitsInMeters10Exponent;

    /**
    This attribute hold the maximum number of slices found in any of the
    time takes inside this study.  Could be never less than 1.
    */
    private int maxSlicesPerTimeTake;

    /**
    A `Study` must have at least one `TimeTake`, and can have as many
    `TimeTake`s as current storage permits.
    */
    private ArrayList <TimeTake>timeTakesArray;

    /**
    This manage the loading / unloading of the memory chaching schema.
    */
    private CacheManager cacheManager;

    /**
    This constructor creates a new Study, with one TimeTake and one
    slice per TimeTake. The default units assigned are in meters, with
    a 1-1-1 voxel factor size and 256 voxels in X-Y dimensions.
    */
    public Study()
    {
        init(0);
    }

    public void init(int timeTakes)
    {
        //-----------------------------------------------------------------
        System.out.println("Study.init");
        //-----------------------------------------------------------------
        int i;

        for ( i = 0; i < 10; i++ ) {
            System.gc();
        }

        cacheManager = new CacheManager(Runtime.getRuntime().freeMemory());
        setVoxelSizeFactor(new Vector3D(1.0, 1.0, 1.0));
        setUnitsInMeters10Exponent(1);
        timeTakesArray = new ArrayList<TimeTake>();

        maxSlicesPerTimeTake = 1;

        TimeTake timeTake;

        for ( i = 0; i < timeTakes; i++ ) {
            timeTake = new TimeTake(this);
            timeTakesArray.add(timeTake);
        }
    }

    public void addCachedChunk(CachedInformation c)
    {
        cacheManager.addChunk(c);
    }

    public void setVoxelSizeFactor(Vector3D newFactor)
    {
        voxelSizeFactor = new Vector3D(newFactor);
    }

    public Vector3D getVoxelSizeFactor()
    {
        return new Vector3D(voxelSizeFactor);
    }

    public int getUnitsInMeters10Exponent()
    {
        return unitsInMeters10Exponent;
    }

    public void setUnitsInMeters10Exponent(int new10Exponent)
    {
        unitsInMeters10Exponent = new10Exponent;
    }

    public TimeTake getTimeTake(int pos)
    {
        if ( pos < 0 || pos >= timeTakesArray.size() ) {
            return null;
        }
        return timeTakesArray.get(pos);
    }

    public int getNumTimeTakes()
    {
        return timeTakesArray.size();
    }

    public int getNumSlicesAtTimeTake(int i)
    {
        if ( i < 0 || i >= timeTakesArray.size() ) {
            return 0;
        }

        return timeTakesArray.get(i).getNumSlices();
    }

    public Image getSliceImageAt(int timeTake, int slice)
    {
        if ( timeTake < 0 || timeTake >= timeTakesArray.size() ) {
            return null;
        }

        TimeTake tt = timeTakesArray.get(timeTake);

        Image img = tt.getSliceImageAt(slice);

        cacheManager.execute();

        return img;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
