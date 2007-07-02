//===========================================================================

// Java basic classes
import java.util.ArrayList;

/**
Each web user own an associated `ServletSessionInformation`. This class
contains all the needed information to keep an independent web session.
*/
public class ServletSessionInformation
{
    public static long nextSessionId = 1;
    private long sessionId;
    private ArrayList <Result> similarModels;

    public ServletSessionInformation()
    {
        sessionId = nextSessionId;
        nextSessionId++;
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
