//===========================================================================

package application.framework;

import java.util.ArrayList;

public class SelectionSet
{
    // This is an association to an external ArrayList (generic, not template)
    private ArrayList elements;
    private ArrayList<Boolean> selection;

    public SelectionSet(ArrayList externalList)
    {
        elements = externalList;
        selection = new ArrayList<Boolean>();
        sync();
    }

    /**
    Checks the size of the element list. If it is different to current
    selection list, selection list is updated.
    */
    public void sync()
    {
        if ( elements.size() == selection.size() ) {
            return;
        }
        while ( elements.size() > selection.size() ) {
            selection.add(new Boolean(false));
        }
        while ( elements.size() < selection.size() ) {
            selection.remove(selection.size()-1);
        }
    }

    public String toString()
    {
        String msg = "Selection set: [";
        int i;

        for ( i = 0; i < selection.size(); i++ ) {
            msg = msg + (selection.get(i).booleanValue()?"*":"-");
        }
        msg = msg + "]";
        return msg;
    }

    public boolean isSelected(int i) {
        if ( i < 0 || i >= selection.size() || 
             !selection.get(i).booleanValue() 
           ) {
            return false;
        }
        return true;
    }

    public int firstSelected()
    {
        int index = -1;
        int i;

        for ( i = 0; i < selection.size(); i++ ) {
            if ( selection.get(i).booleanValue() ) {
                index = i;
                break;
            }
        }
        return index;
    }

    public void selectAll()
    {
        int i;

        for ( i = 0; i < selection.size(); i++ ) {
            selection.set(i, new Boolean(true));
        }
    }

    public void unselectAll()
    {
        int i;

        for ( i = 0; i < selection.size(); i++ ) {
            selection.set(i, new Boolean(false));
        }
    }

    public void select(int i)
    {
        sync();
        if ( i < 0 || i >= selection.size() ) return;
        selection.set(i, new Boolean(true));
    }

    public void unselect(int i)
    {
        sync();
        if ( i < 0 || i >= selection.size() ) return;
        selection.set(i, new Boolean(false));
    }

    public void change(int i)
    {
        sync();
        if ( i < 0 || i >= selection.size() ) return;
        if ( isSelected(i) ) {
            selection.set(i, new Boolean(false));
          }
          else {
            selection.set(i, new Boolean(true));
        }
    }

    public void selectPrevious()
    {
        sync();
        if ( selection.size() < 1 ) {
            return;
        }
        int f;
        f = firstSelected();
        if ( f < 0 ) {
            select(selection.size()-1);
            return;
        }
        unselect(f);
        f--;
        if ( f < 0 ) {
            return;
        }
        select(f);
    }

    public void selectNext()
    {
        sync();
        if ( selection.size() < 1 ) {
            return;
        }
        int f;
        f = firstSelected();
        if ( f < 0 ) {
            select(0);
            return;
        }
        unselect(f);
        f++;
        if ( f >= selection.size() ) {
            return;
        }
        select(f);
    }

    public int numberOfSelections()
    {
        int i;
        int acum = 0;
        sync();
        for ( i = 0; i < selection.size(); i++ ) {
            if ( isSelected(i) ) {
                acum++;
            }
        }
        return acum;
    }

    public int size()
    {
        return selection.size();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
