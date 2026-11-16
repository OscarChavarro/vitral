// C++ port of Java's CircularArrayListTest (common/dataStructures).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/IndexOutOfBoundsException.h"
#include "java/lang/Runnable.h"
#include "java/lang/String.h"
#include "java/lang/Thread.h"
#include "vsdk/toolkit/common/dataStructures/ArrayLinkable.h"
#include "vsdk/toolkit/common/dataStructures/CircularArrayList.h"

/**
Java's `Item implements ArrayLinkable`: the C++ base class already holds the
handle fields.
*/
class CircularArrayListTestItem : public ArrayLinkable
{
};

typedef CircularArrayListTestItem Item;

/**
C++ port note: the list can not be copied, so the Java `abc()` factory fills
a list given by the caller.
*/
static void
abc(CircularArrayList<String>& list)
{
    list.add("a");
    list.add("b");
    list.add("c");
}

TEST(CircularArrayList, given_threeElements_when_traversed_then_orderAndNeighborsAreCircular)
{
    CircularArrayList<String> list;
    abc(list);

    EXPECT_EQ(3, list.size());
    EXPECT_TRUE(list.get(0) == "a");
    EXPECT_TRUE(list.get(2) == "c");
    EXPECT_TRUE(list.nextOf("c") == "a");
    EXPECT_TRUE(list.previousOf("a") == "c");
}

TEST(CircularArrayList, given_threeElements_when_insertBeforeHead_then_newElementBecomesHead)
{
    CircularArrayList<String> list;
    abc(list);

    list.insertBefore("x", "a");

    EXPECT_EQ(4, list.size());
    EXPECT_TRUE(list.get(0) == "x");
    EXPECT_TRUE(list.nextOf("x") == "a");
    EXPECT_TRUE(list.previousOf("x") == "c");
}

TEST(CircularArrayList, given_threeElements_when_reverse_then_orderIsMirrored)
{
    CircularArrayList<String> list;
    abc(list);

    list.reverse();

    EXPECT_TRUE(list.get(0) == "c");
    EXPECT_TRUE(list.get(1) == "b");
    EXPECT_TRUE(list.get(2) == "a");
}

/**
Defect L-01 (fixed): removeElemAtWindow() leaves `head` pointing at the
removed node when the list becomes empty, so the next add() links the new
element behind a stale node and get(0) resurrects the removed element.
This is exactly the sequence lkev() performs when it kills the last edge
of a loop (unlist both half-edges, then re-add the surviving one).
*/
TEST(CircularArrayList, given_listEmptiedByRemove_when_add_then_onlyTheNewElementIsVisible)
{
    CircularArrayList<String> list;
    list.add("a");
    list.remove(0);

    list.add("b");

    EXPECT_EQ(1, list.size());
    EXPECT_TRUE(list.get(0) == "b");
    EXPECT_TRUE(list.nextOf("b") == "b");
    EXPECT_TRUE(list.previousOf("b") == "b");
}

/**
Defect L-01 (fixed, continued): after the stale-head corruption the ring
holds a phantom node, so neighbor queries disagree with indexed access.
*/
TEST(CircularArrayList, given_listEmptiedByRemove_when_addAndInsertBefore_then_neighborsMatchIndexOrder)
{
    CircularArrayList<String> list;
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

/**
Defect L-02 (fixed): remove(index) with an out-of-range index used to
delete whatever element the window was left on by an unrelated earlier
call. It is now a hard error that leaves the list unchanged.
*/
TEST(CircularArrayList, given_windowLeftOnAnElement_when_removeWithInvalidIndex_then_errorAndNothingIsRemoved)
{
    CircularArrayList<String> list;
    abc(list);
    list.locateWindowAtElem("b");

    EXPECT_THROW(list.remove(7), IndexOutOfBoundsException);

    EXPECT_EQ(3, list.size());
    EXPECT_TRUE(list.get(1) == "b");
}

/**
Defect L-03 (fixed): insertBefore(newElem, pivot) with a pivot that is
not in the list used to insert at the head silently. It is now a hard
error that leaves the list unchanged. Euler operators rely on this call
(addhe) to place new half-edges, so a wrong pivot corrupts a loop without
any diagnostic.
*/
TEST(CircularArrayList, given_missingPivot_when_insertBefore_then_errorAndListIsNotModified)
{
    CircularArrayList<String> list;
    abc(list);

    EXPECT_THROW(list.insertBefore("x", "not-in-list"),
        IllegalArgumentException);

    EXPECT_EQ(3, list.size());
    EXPECT_TRUE(list.get(0) == "a");
}

TEST(CircularArrayList, given_linkableElements_when_reorderedAndRemoved_then_neighborsStayConsistent)
{
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
    EXPECT_TRUE(b.getListOwner() == NULL);
    EXPECT_EQ(&c, list.nextOf(&a));
    EXPECT_EQ(&a, list.nextOf(&c));

    CircularArrayList<Item*> other;
    other.add(&a);
    // falls back to a scan
    EXPECT_EQ(&c, list.nextOf(&a));
    EXPECT_EQ(&a, other.nextOf(&a));
}

/**
One reader of the shared-list test: Java's lambda submitted to the pool.
*/
class CircularArrayListTestReader : public Runnable
{
public:
    CircularArrayList<Item*>* list;
    Item* items;
    long count;
    long stride;
    long mismatches;

    virtual void run()
    {
        long round;
        long i;

        mismatches = 0;
        for ( round = 0; round < 50; round++ ) {
            for ( i = 0; i < count; i += stride ) {
                if ( list->get(i) != &items[i] ) mismatches++;
                if ( list->nextOf(&items[i]) !=
                     &items[(i + 1) % count] ) mismatches++;
                if ( list->get(count - 1 - i) !=
                     &items[count - 1 - i] ) mismatches++;
            }
        }
    }
};

/**
Reading does not write: two index loops interleaved over the same list
(the pattern of every "all pairs" pass of the kernel) see every pair,
and several threads reading at once see the same elements.

C++ port note: the Java pool of 8 threads is 8 `Thread` objects that are
joined. Without POSIX threads (VITRAL_WITHOUT_PTHREADS) no thread starts, so
the readers run one after another on the calling thread.
*/
TEST(CircularArrayList, given_sharedList_when_readFromSeveralThreads_then_everyReaderSeesTheSameOrder)
{
    enum { ITEMS = 2000, READERS = 8 };
    CircularArrayList<Item*> list;
    Item* items = new Item[ITEMS];
    CircularArrayListTestReader readers[READERS];
    Thread* threads[READERS];
    long i;
    int t;

    for ( i = 0; i < ITEMS; i++ ) {
        list.add(&items[i]);
    }
    for ( t = 0; t < READERS; t++ ) {
        readers[t].list = &list;
        readers[t].items = items;
        readers[t].count = ITEMS;
        readers[t].stride = t + 1;
        readers[t].mismatches = -1;
        threads[t] = new Thread(&readers[t]);
#ifdef VITRAL_WITHOUT_PTHREADS
        readers[t].run();
#else
        threads[t]->start();
#endif
    }
    for ( t = 0; t < READERS; t++ ) {
        threads[t]->join();
        delete threads[t];
        EXPECT_EQ(0, readers[t].mismatches);
    }
    list.clear();
    delete[] items;
}

static void
assertHandles(CircularArrayList<Item*>& list)
{
    long i;

    for ( i = 0; i < list.size(); i++ ) {
        Item* item = list.get(i);
        EXPECT_TRUE(item->getListOwner() == (const void*)&list);
        EXPECT_EQ(i, item->getListIndex());
        EXPECT_EQ(i, list.indexOf(item));
        EXPECT_EQ(list.get((i + 1) % list.size()), list.nextOf(item));
    }
}

TEST(CircularArrayList, given_linkableElements_when_everyKindOfEdit_then_handlesMatchIndexes)
{
    CircularArrayList<Item*> list;
    Item items[40];
    int i;

    for ( i = 0; i < 20; i++ ) list.add(&items[i]);
    assertHandles(list);
    for ( i = 20; i < 30; i++ ) list.insertBefore(&items[i], &items[i - 20]);
    assertHandles(list);
    for ( i = 30; i < 40; i++ ) list.push(&items[i]);
    assertHandles(list);
    list.remove(0);
    list.remove(list.size() - 1);
    list.remove(7);
    assertHandles(list);
    list.locateWindowAtElem(&items[5]);
    list.removeElemAtWindow();
    EXPECT_TRUE(items[5].getListOwner() == NULL);
    assertHandles(list);
    list.swapElements(&items[1], &items[25]);
    list.reverse();
    assertHandles(list);
    list.clear();
    EXPECT_EQ(0, list.size());
    for ( i = 0; i < 40; i++ ) {
        EXPECT_TRUE(items[i].getListOwner() == NULL);
    }
}

TEST(CircularArrayList, given_cursor_when_iteratedBothWays_then_itWrapsAround)
{
    CircularArrayList<String> list;
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

VITRAL_TEST_MAIN()
