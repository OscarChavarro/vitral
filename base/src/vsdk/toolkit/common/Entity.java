//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 2 2006 - Oscar Chavarro: Original base version                    =
//===========================================================================

package vsdk.toolkit.common;

import java.io.Serializable;

/**
This class is a base superclass for all classes in the VSDK model (as of
Vitral applications are based upon Model-View-Controller or MVC design
pattern). Note that this class supports two functionalities:
  - As implementing the Serializable interface, this permits to serialize 
    all of the 
*/

public class Entity extends ModelElement implements Serializable
{
    /// Constants used for operations of type getSizeInBytes
    public static final int BYTE_SIZE_IN_BYTES = 1;
    public static final int INT_SIZE_IN_BYTES = 4;
    public static final int LONG_SIZE_IN_BYTES = 8;
    public static final int FLOAT_SIZE_IN_BYTES = 4;
    public static final int DOUBLE_SIZE_IN_BYTES = 8;
    public static final int VECTOR3D_SIZE_IN_BYTES = 24;
    public static final int COLORRGB_SIZE_IN_BYTES = 24;
    public static final int POINTER_SIZE_IN_BYTES = 8;
    
    /**
    This is a value used for the standard java serialization mechanism to
    keep track of software versions.  To avoid warning at compilation time
    and to ease to keep compatibility tracking of software structure changes
    in retrieving old saved data, it is suggested that all Entity's in VSDK
    define this value.  The proposed number to asign is the concatenation of
    8 digits YYYYMMDD, for year, month and day respectively.
    */
    //public static final long serialVersionUID = 20060502L;

    /**
    Each Entity object in the VSDK model should be responsible for calculating
    the size in bytes that occupies in RAM, including its own attributes and
    the aggregated objects (note that the associated objects only must count
    the size of the references).  If the class doen't overload this method,
    a 0 size is assumed.

    This is important for applications implementing memory chaching schema.
    @return the number of bytes current object ocupies in RAM when loaded
    */
    public int getSizeInBytes()
    {
        return 0;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
