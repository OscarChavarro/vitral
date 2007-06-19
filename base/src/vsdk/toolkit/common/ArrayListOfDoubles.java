//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 5 2006 - Oscar Chavarro: Original base version               =
//===========================================================================

package vsdk.toolkit.common;

public class ArrayListOfDoubles extends FundamentalEntity {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20061105L;

    private int increment;
    private int assignedSize;    
  
    public int size; // Currently used elements
    public double array[];
    private double array2[];

    public ArrayListOfDoubles(int increment) {
        this.increment = increment;
        array = new double[this.increment];
        assignedSize = this.increment;
        size = 0;
    }

    public void append(double val)
    {
        if ( size >= assignedSize ) {
            grow();
        }
        array[size] = val;
        size++;
    }

    private void grow()
    {
        array2 = new double[assignedSize+increment];
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
