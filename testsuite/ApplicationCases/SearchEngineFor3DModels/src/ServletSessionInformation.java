//===========================================================================

// Java basic classes
import java.util.ArrayList;

// VSDK classes
import vsdk.toolkit.media.Image;

/**
Each web user own an associated `ServletSessionInformation`. This class
contains all the needed information to keep an independent web session.
*/
public class ServletSessionInformation
{
    public static long nextSessionId = 1;
    private long sessionId;
    private ArrayList <Result> similarModels;
    private Image sourceImages[];
    private Image outlines[];
    private Image distanceFields[];

    public ServletSessionInformation()
    {
        sessionId = nextSessionId;
        nextSessionId++;
        sourceImages = new Image[3];
        outlines = new Image[3];
        distanceFields = new Image[3];
        int i;
        for ( i = 0; i < 3; i++ ) {
            sourceImages[i] = null;
            outlines[i] = null;
            distanceFields[i] = null;
        }
    }

    public void setSourceImage(Image i, int pos)
    {
        if ( pos < 0 || pos > 2 ) {
            return;
        }
        sourceImages[pos] = i;
    }

    public void setOutline(Image i, int pos)
    {
        if ( pos < 0 || pos > 2 ) {
            return;
        }
        outlines[pos] = i;
    }

    public void setDistanceField(Image i, int pos)
    {
        if ( pos < 0 || pos > 2 ) {
            return;
        }
        distanceFields[pos] = i;
    }

    public Image getSourceImage(int pos)
    {
        if ( pos < 0 || pos > 2 ) {
            return null;
        }
        return sourceImages[pos];
    }

    public Image getOutline(int pos)
    {
        if ( pos < 0 || pos > 2 ) {
            return null;
        }
        return outlines[pos];
    }

    public Image getDistanceField(int pos)
    {
        if ( pos < 0 || pos > 2 ) {
            return null;
        }
        return distanceFields[pos];
    }

    public long getId()
    {
        return sessionId;
    }

    public void setSimilarModels(ArrayList <Result> list)
    {
        similarModels = list;
    }

    public ArrayList <Result> getSimilarModels()
    {
        return similarModels;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
