#include <gtest/gtest.h>
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/IndexOutOfBoundsException.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "java/util/concurrent/Callable.h"
#include "java/util/concurrent/ExecutorService.h"
#include "java/util/concurrent/Executors.h"
#include "java/util/concurrent/Future.h"
#include "vsdk/toolkit/common/dataStructures/ArrayLinkable.h"
#include "vsdk/toolkit/common/dataStructures/CircularArrayList.h"

/*
Direct coverage for the array-backed circular list that backs every face,
loop, edge, vertex and half-edge collection of PolyhedralBoundedSolid. The
first cases are the ones of the linked list it replaced, unchanged: the
order of elements and the preconditions are the same.

Traceability: this container replaces the intrusive nxt/prv pointers of
[MANT1988] Ch. 10.2-10.3 and the list procedures of Ch. 11.2 (addlist,
dellist). The cases marked "hidden defect" were found by the Fable 5.2
kernel analysis (doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md,
defects L-01 to L-03).
C++ counterpart of Java's `CircularArrayListTest`.
*/
namespace {

class Item : public ArrayLinkable
{
private:
    const void* listOwner;
    int listIndex;

public:
    Item() : listOwner(nullptr), listIndex(-1) {}

    virtual const void* getListOwner() const
    {
        return listOwner;
    }

    virtual int getListIndex() const
    {
        return listIndex;
    }

    virtual void setListSlot(const void* owner, int index)
    {
        listOwner = owner;
        listIndex = index;
    }
};

typedef CircularArrayList<java::String> StringList;

void abc(StringList& list)
{
    list.add("a");
    list.add("b");
    list.add("c");
}

void assertHandles(CircularArrayList<Item*>& list)
{
    for ( int i = 0; i < list.size(); i++ ) {
        Item* item = list.get(i);
        EXPECT_EQ((const void*)&list, item->getListOwner());
        EXPECT_EQ(i, item->getListIndex());
        EXPECT_EQ(i, list.indexOf(item));
        EXPECT_EQ(list.get((i + 1) % list.size()), list.nextOf(item));
    }
}

const int SHARED_ITEMS = 2000;

class SharedListReader : public java::Callable<int>
{
private:
    CircularArrayList<Item*>* list;
    Item** items;
    int stride;

public:
    SharedListReader(CircularArrayList<Item*>* list, Item** items,
        int stride) : list(list), items(items), stride(stride) {}

    virtual int call()
    {
        int mismatches = 0;
        for ( int round = 0; round < 50; round++ ) {
            for ( int i = 0; i < SHARED_ITEMS; i += stride ) {
                if ( list->get(i) != items[i] ) mismatches++;
                if ( list->nextOf(items[i]) !=
                     items[(i + 1) % SHARED_ITEMS] ) mismatches++;
                if ( list->get(SHARED_ITEMS - 1 - i) !=
                     items[SHARED_ITEMS - 1 - i] ) mismatches++;
            }
        }
        return mismatches;
    }
};

}

TEST(CircularArrayListTest, ThreeElementsTraversedOrderAndNeighborsAreCircular) {
    StringList list;
    abc(list);

    EXPECT_EQ(3, list.size());
    EXPECT_TRUE(list.get(0) == "a");
    EXPECT_TRUE(list.get(2) == "c");
    EXPECT_TRUE(list.nextOf("c") == "a");
    EXPECT_TRUE(list.previousOf("a") == "c");
}

TEST(CircularArrayListTest, InsertBeforeHeadMakesNewElementTheHead) {
    StringList list;
    abc(list);

    list.insertBefore("x", "a");

    EXPECT_EQ(4, list.size());
    EXPECT_TRUE(list.get(0) == "x");
    EXPECT_TRUE(list.nextOf("x") == "a");
    EXPECT_TRUE(list.previousOf("x") == "c");
}

TEST(CircularArrayListTest, ReverseMirrorsOrder) {
    StringList list;
    abc(list);

    list.reverse();

    EXPECT_TRUE(list.get(0) == "c");
    EXPECT_TRUE(list.get(1) == "b");
    EXPECT_TRUE(list.get(2) == "a");
}

/*
Defect L-01 (fixed): removeElemAtWindow() leaves `head` pointing at the
removed node when the list becomes empty, so the next add() links the new
element behind a stale node and get(0) resurrects the removed element.
This is exactly the sequence lkev() performs when it kills the last edge
of a loop (unlist both half-edges, then re-add the surviving one).
*/
TEST(CircularArrayListTest, ListEmptiedByRemoveThenAddShowsOnlyNewElement) {
    StringList list;
    list.add("a");
    list.remove(0);

    list.add("b");

    EXPECT_EQ(1, list.size());
    EXPECT_TRUE(list.get(0) == "b");
    EXPECT_TRUE(list.nextOf("b") == "b");
    EXPECT_TRUE(list.previousOf("b") == "b");
}

/*
Defect L-01 (fixed, continued): after the stale-head corruption the ring
holds a phantom node, so neighbor queries disagree with indexed access.
*/
TEST(CircularArrayListTest, ListEmptiedByRemoveThenAddAndInsertBeforeNeighborsMatchIndexOrder) {
    StringList list;
    list.add("a");
    list.remove(0);
    list.add("b");

    list.insertBefore("c", "b");

    EXPECT_EQ(2, list.size());
    EXPECT_TRUE(list.get(0) == "c");
    EXPECT_TRUE(list.get(1) == "b");
    EXPECT_TRUE(list.nextOf("c") == "b");
    EXPECT_TRUE(list.nextOf("b") == "c");
}

/*
Defect L-02 (fixed): remove(index) with an out-of-range index used to
delete whatever element the window was left on by an unrelated earlier
call. It is now a hard error that leaves the list unchanged.
*/
TEST(CircularArrayListTest, RemoveWithInvalidIndexFailsAndRemovesNothing) {
    StringList list;
    abc(list);
    list.locateWindowAtElem("b");

    EXPECT_THROW(list.remove(7), java::IndexOutOfBoundsException);

    EXPECT_EQ(3, list.size());
    EXPECT_TRUE(list.get(1) == "b");
}

/*
Defect L-03 (fixed): insertBefore(newElem, pivot) with a pivot that is
not in the list used to insert at the head silently. It is now a hard
error that leaves the list unchanged. Euler operators rely on this call
(addhe) to place new half-edges, so a wrong pivot corrupts a loop without
any diagnostic.
*/
TEST(CircularArrayListTest, InsertBeforeMissingPivotFailsAndListIsNotModified) {
    StringList list;
    abc(list);

    EXPECT_THROW(list.insertBefore("x", "not-in-list"),
        java::IllegalArgumentException);

    EXPECT_EQ(3, list.size());
    EXPECT_TRUE(list.get(0) == "a");
}

TEST(CircularArrayListTest, LinkableElementsReorderedAndRemovedKeepNeighborsConsistent) {
    CircularArrayList<Item*> list;
    Item a;
    Item b;
    Item c;
    list.add(&a);
    list.add(&b);
    list.add(&c);

    list.reverse();
    EXPECT_EQ(&b, list.nextOf(&c));
    EXPECT_EQ(&a, list.previousOf(&c));

    list.swapElements(&a, &c);
    EXPECT_EQ(&a, list.get(0));
    EXPECT_EQ(&b, list.nextOf(&a));

    list.locateWindowAtElem(&b);
    list.removeElemAtWindow();
    EXPECT_FALSE(list.contains(&b));
    EXPECT_EQ(nullptr, b.getListOwner());
    EXPECT_EQ(&c, list.nextOf(&a));
    EXPECT_EQ(&a, list.nextOf(&c));

    CircularArrayList<Item*> other;
    other.add(&a);
    EXPECT_EQ(&c, list.nextOf(&a)) << "falls back to a scan";
    EXPECT_EQ(&a, other.nextOf(&a));
    other.clear();
    list.clear();
}

/*
Reading does not write: two index loops interleaved over the same list
(the pattern of every "all pairs" pass of the kernel) see every pair,
and several threads reading at once see the same elements.
*/
TEST(CircularArrayListTest, SharedListReadFromSeveralThreadsGivesEveryReaderTheSameOrder) {
    CircularArrayList<Item*> list;
    Item* items[SHARED_ITEMS];
    for ( int i = 0; i < SHARED_ITEMS; i++ ) {
        items[i] = new Item();
        list.add(items[i]);
    }
    java::ExecutorService* pool = java::Executors::newFixedThreadPool(8);
    java::ArrayList<java::Future<int> > results;
    for ( int t = 0; t < 8; t++ ) {
        const int stride = t + 1;
        results.add(pool->submit(new SharedListReader(&list, items, stride)));
    }
    for ( long r = 0; r < results.size(); r++ ) {
        EXPECT_EQ(0, results[r].get());
    }
    pool->shutdownNow();
    delete pool;
    list.clear();
    for ( int i = 0; i < SHARED_ITEMS; i++ ) {
        delete items[i];
    }
}

TEST(CircularArrayListTest, LinkableElementsEveryKindOfEditKeepsHandlesMatchingIndexes) {
    CircularArrayList<Item*> list;
    Item items[40];
    for ( int i = 0; i < 20; i++ ) list.add(&items[i]);
    assertHandles(list);
    for ( int i = 20; i < 30; i++ ) list.insertBefore(&items[i], &items[i - 20]);
    assertHandles(list);
    for ( int i = 30; i < 40; i++ ) list.push(&items[i]);
    assertHandles(list);
    list.remove(0);
    list.remove(list.size() - 1);
    list.remove(7);
    assertHandles(list);
    list.locateWindowAtElem(&items[5]);
    list.removeElemAtWindow();
    EXPECT_EQ(nullptr, items[5].getListOwner());
    assertHandles(list);
    list.swapElements(&items[1], &items[25]);
    list.reverse();
    assertHandles(list);
    list.clear();
    EXPECT_EQ(0, list.size());
    for ( int i = 0; i < 40; i++ ) {
        EXPECT_EQ(nullptr, items[i].getListOwner());
    }
}

TEST(CircularArrayListTest, CursorIteratedBothWaysWrapsAround) {
    StringList list;
    abc(list);

    EXPECT_TRUE(list.next() == "a");
    EXPECT_TRUE(list.next() == "b");
    EXPECT_TRUE(list.next() == "c");
    EXPECT_TRUE(list.next() == "a");
    list.locateWindowAtIndex(0);
    EXPECT_TRUE(list.previous() == "a");
    EXPECT_TRUE(list.previous() == "c");
    EXPECT_TRUE(list.getWindow() == "b");
    list.push("z");
    EXPECT_TRUE(list.get(0) == "z");
    EXPECT_TRUE(list.getWindow() == "a");
}
