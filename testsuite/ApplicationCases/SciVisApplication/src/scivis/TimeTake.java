//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 21 2006 - Oscar Chavarro: Original base version                =
//===========================================================================

package scivis;

import java.util.ArrayList;

import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.IndexedColorImage;

/**
A `TimeTake` is a set of slices for a current study in a given moment of time.

*/
public class TimeTake
{
    /**
    Current TimeTake needs to know basic configuration items from the
    Study that contains it, so a reference is kept all the time.
    */
    private Study context;

    /**
    Time management inside a study takes is represented as an incremental
    advancement, stored inside each take in seconds.
    */
    private double deltaTimeFromPreviousTimeTakeInSeconds;

    /**
    A TimeTake can have as many slices in it as indicated by it's Study
    maximum, and as little as none. Ususally, a TimeTake will have at
    least one slice, and 0 slices will be a "build in progress" temporary
    state.

    Given a current number of slices in this TimeTake, the slices sequence
    can be positioned in the Z direction, as the first slice will correspond
    to `startingSliceIndex` slice, as compared to global slice enumeration
    scheme for current Study. Note that startingSliceIndex+slicesInThisTimeTake
    must be less than Study's maximum of slices, so inserting more slices
    or repositioning startingSliceIndex for a TimeTake will update maximum
    slice count in its Study context.

    This attribute should be never less than 0.
    */
    private int startingSliceIndex;

    /**
    For each slice in the TimeTake there is an associated image. Note that
    for a yet unspecified image, its corresponding reference can be null 
    inside this list.
    */
    private ArrayList <CachedImage> imageSlicesArray;

    /**
    For each slice in the TimeTake there is an associated starting position.
    */
    private ArrayList <StartPointPosition> startingPositionsArray;

    private ArrayList <SegmentedImageGroup> segmentedImageGroupsArray;

    public TimeTake(Study parent)
    {
        context = parent;
        deltaTimeFromPreviousTimeTakeInSeconds = 0.0;
        startingSliceIndex = 0;
        imageSlicesArray = new ArrayList<CachedImage>();
        startingPositionsArray = new ArrayList<StartPointPosition>();
        segmentedImageGroupsArray = new ArrayList<SegmentedImageGroup>();
    }

    public int getStartingSliceIndex()
    {
        return startingSliceIndex;
    }

    /**
    TODO: implement this method taking into account the Study maximum
    slice count configuration
    */
    public void setStartingSliceIndex(int newIndex)
    {
        startingSliceIndex = newIndex;
    }

    /**
    Given image is placed inside `imageSlicesArray`, with its corresponding
    (x, y) starting voxel position in the `startingPositionArray`. The
    relativeSlice given index is relative to `startingSliceIndex`.
    */
    public void setSliceImageAt(String imageSource, int relativeSlice, 
                                int xpos, int ypos)
    {
        while ( imageSlicesArray.size() <= relativeSlice ) {
            imageSlicesArray.add(null);
        }
        while ( startingPositionsArray.size() <= relativeSlice ) {
            startingPositionsArray.add(null);
        }

        CachedImage img = new CachedImage(imageSource);

        imageSlicesArray.set(relativeSlice, img);
        StartPointPosition p = new StartPointPosition(xpos, ypos);
        startingPositionsArray.set(relativeSlice, p);

        context.addCachedChunk(img);
    }

    public int getNumSlices()
    {
        return imageSlicesArray.size();
    }

    public Image getSliceImageAt(int slice)
    {
        int ns = getNumSlices();

        if ( slice < 0 || slice >= ns ) {
	    return null;
	}
        return imageSlicesArray.get(slice).getImage();
    }

    public void setSegmentedImageAt(IndexedColorImage img, int group, 
                                int relativeSlice, int xpos, int ypos)
    {
        while ( segmentedImageGroupsArray.size() <= group ) {
            segmentedImageGroupsArray.add(null);
        }
        segmentedImageGroupsArray.get(group).setSegmentedImageAt(img, relativeSlice, xpos, ypos);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
