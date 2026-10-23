#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "model/selection/SelectionSet.h"

void SelectionSet::sync()
{
    if ( elements.size() == selection.size() ) {
        return;
    }
    while ( elements.size() > selection.size() ) {
        selection.add(FALSE);
    }
    while ( elements.size() < selection.size() ) {
        selection.remove((long)(selection.size()-1));
    }
}

String SelectionSet::toString() const
{
    String msg = "Selection set: [";
    int i;

    for ( i = 0; i < selection.size(); i++ ) {
        msg = msg + (selection.get(i) ? "*" : "-");
    }
    msg = msg + "]";
    return msg;
}

BOOLEAN SelectionSet::isSelected(int i) const
{
    if ( i < 0 || i >= selection.size() || !selection.get(i) ) {
        return FALSE;
    }
    return TRUE;
}

int SelectionSet::firstSelected() const
{
    int index = -1;
    int i;

    for ( i = 0; i < selection.size(); i++ ) {
        if ( selection.get(i) ) {
            index = i;
            break;
        }
    }
    return index;
}

void SelectionSet::selectAll()
{
    int i;

    for ( i = 0; i < selection.size(); i++ ) {
        selection.set(i, TRUE);
    }
}

void SelectionSet::unselectAll()
{
    int i;

    for ( i = 0; i < selection.size(); i++ ) {
        selection.set(i, FALSE);
    }
}

void SelectionSet::select(int i)
{
    sync();
    if ( i < 0 || i >= selection.size() ) return;
    selection.set(i, TRUE);
}

void SelectionSet::unselect(int i)
{
    sync();
    if ( i < 0 || i >= selection.size() ) return;
    selection.set(i, FALSE);
}

void SelectionSet::change(int i)
{
    sync();
    if ( i < 0 || i >= selection.size() ) return;
    if ( isSelected(i) ) {
        selection.set(i, FALSE);
    }
    else {
        selection.set(i, TRUE);
    }
}

void SelectionSet::selectPrevious()
{
    sync();
    if ( selection.size() < 1 ) {
        return;
    }
    int f;
    f = firstSelected();
    if ( f < 0 ) {
        select((int)selection.size()-1);
        return;
    }
    unselect(f);
    f--;
    if ( f < 0 ) {
        return;
    }
    select(f);
}

void SelectionSet::selectNext()
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

ArrayList<Entity*> SelectionSet::removeSelected()
{
    ArrayList<Entity*> removed;
    sync();
    for ( long i = selection.size() - 1; i >= 0; i-- ) {
        if ( selection.get(i) ) {
            removed.add(elements.remove(i));
            selection.remove(i);
        }
    }
    return removed;
}

void SelectionSet::insertElement(int index, Entity* element, BOOLEAN selected)
{
    sync();
    if ( elements.insert(index, element) ) {
        selection.add((long)index, selected);
    }
}

Entity* SelectionSet::removeElement(int index)
{
    sync();
    selection.remove((long)index);
    return elements.remove(index);
}

int SelectionSet::numberOfSelections()
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

int SelectionSet::size() const
{
    return (int)selection.size();
}

const EntityListView& SelectionSet::getElements() const
{
    return elements;
}
