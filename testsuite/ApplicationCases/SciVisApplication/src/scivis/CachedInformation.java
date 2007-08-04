//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - September 3 2006 - Oscar Chavarro: Original base version              =
//===========================================================================

package scivis;

/**
This interfaces is to be used in cached information classes. Its main
purpose is to keep track
*/
public abstract class CachedInformation {
    //public ? lastTimeAccessed
/*
    public ? timeFromLastAccess()
    {

    }
*/

    public abstract int getSizeInBytes();

    public abstract boolean load();

    public abstract void unload();

    public abstract boolean isLoaded();
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
