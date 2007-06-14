//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - December 8 2006 - Oscar Chavarro: Original base version               =
//===========================================================================

package vsdk.toolkit.common;

/**
DEFINITION: A `PersistenceElement` in VitralSDK is a software element with
algorithms and data structures (i.e. a class) with the specific functionality
of providing persistence operations for a data Entity.

The PersistenceElement abstract class provides an interface for *Persistence
style classes. This serves two purposes:
  - To help in design level organization of persistence classes (this eases the
    study of the class hierarchy)
  - To provide a place to locate possible future operations, common to
    all persistence classes and persistence private utility/supporting
    classes.  In particular, this class contains basic low level
    persistence operations for converting bit streams from and to basic
    numeric data types. Note that this code is NOT portable, as it needs
    explicit programmer configuration for little-endian or big-endian
    hardware platform.
*/

public abstract class PersistenceElement {
    ;
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
