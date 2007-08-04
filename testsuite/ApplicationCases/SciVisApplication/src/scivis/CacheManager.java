//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - September 3 2006 - Oscar Chavarro: Original base version              =
//===========================================================================

package scivis;

import java.util.ArrayList;

public class CacheManager
{
    private ArrayList<CachedInformation> cachedElementsArray;
    private long limitInBytes;

    public CacheManager(long limitInBytes)
    {
        this.limitInBytes = limitInBytes;
        cachedElementsArray = new ArrayList<CachedInformation>();
    }

    public void addChunk(CachedInformation c)
    {
        cachedElementsArray.add(c);
    }

    public void execute()
    {
        int i;
        int totalSizeInBytes = 0;

        for ( i = 0; i < cachedElementsArray.size(); i++ ) {
            totalSizeInBytes += cachedElementsArray.get(i).getSizeInBytes();
        }
        System.out.println("Cache Manager:");
        System.out.println("  - " + 
            cachedElementsArray.size() + " cached elements");
        System.out.println("  - " + 
            totalSizeInBytes + " total bytes used");
        System.out.println("  - " + 
            limitInBytes + " limit in bytes");
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
