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
  
    private int mysize; // Currently used elements
    private int array[];
    private int array2[];

    public ArrayListOfInts(int increment) {
        this.increment = increment;
        array = new int[this.increment];
        assignedSize = this.increment;
        mysize = 0;
    }

    public int size()
    {
        return mysize;
    }

    public int get(int i)
    {
        return array[i];
    }

    public void set(int i, int val)
    {
        array[i] = val;
    }

    public void add(int val)
    {
        if ( mysize >= assignedSize ) {
            grow();
        }
        array[mysize] = val;
        mysize++;
    }

    private void grow()
    {
        array2 = new int[assignedSize+increment];
        assignedSize += increment;

        int i;
        for ( i = 0; i < mysize; i++ ) {
            array2[i] = array[i];
        }

        array = array2;
    }

    public void clean()
    {
        mysize = 0;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
