//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 27 2008 - Oscar Chavarro: Original base version                   =
//===========================================================================

package vsdk.toolkit.common;

public class ArrayListOfInts extends FundamentalEntity {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20080527L;

    private int increment;
    private int assignedSize;    
  
    public int size; // Currently used elements
    public int array[];
    private int array2[];

    public ArrayListOfInts(int increment) {
        this.increment = increment;
        array = new int[this.increment];
        assignedSize = this.increment;
        size = 0;
    }

    public void append(int val)
    {
        if ( size >= assignedSize ) {
            grow();
        }
        array[size] = val;
        size++;
    }

    private void grow()
    {
        array2 = new int[assignedSize+increment];
        assignedSize += increment;

        int i;
        for ( i = 0; i < size; i++ ) {
            array2[i] = array[i];
        }

        array = array2;
    }

    public void clean()
    {
        size = 0;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
