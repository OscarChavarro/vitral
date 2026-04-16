package scivis;

import java.util.ArrayList;

import vsdk.toolkit.media.IndexedColorImage;

/**
A `SegmentedImageGroup` is the segmented image set equivalent to the normal
image set contained in a `TimeTake`. A `SegmentedImageGroup` is closely
related to its owning `TimeTake`, and for each image in the later, should
be another (possibly null) in the former. One TimeTake can have multiple
`SegmentedImageGroup`s, as each is limited to only 256 segment labels.
*/
public class SegmentedImageGroup
{
    private ArrayList<IndexedColorImage> segmentedImagesArray;
    private ArrayList <StartPointPosition> startingPositionsArray;

    public SegmentedImageGroup()
    {
        segmentedImagesArray = new ArrayList <IndexedColorImage>();
        startingPositionsArray = new ArrayList<StartPointPosition>();
    }

    public void setSegmentedImageAt(IndexedColorImage img, int relativeSlice,
                                    int xpos, int ypos)
    {
        while ( segmentedImagesArray.size() <= relativeSlice ) {
            segmentedImagesArray.add(null);
        }

        segmentedImagesArray.set(relativeSlice, img);
        StartPointPosition p = new StartPointPosition(xpos, ypos);
        startingPositionsArray.set(relativeSlice, p);
    }
}
