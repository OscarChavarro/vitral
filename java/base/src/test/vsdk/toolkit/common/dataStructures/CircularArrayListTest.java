package vsdk.toolkit.common.dataStructures;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
Direct coverage for the array-backed circular list that backs every face,
loop, edge, vertex and half-edge collection of PolyhedralBoundedSolid. The
first cases are the ones of the linked list it replaced, unchanged: the
order of elements and the preconditions are the same.

<p>Traceability: this container replaces the intrusive nxt/prv pointers of
[MANT1988] Ch. 10.2-10.3 and the list procedures of Ch. 11.2 (addlist,
dellist). The cases marked "hidden defect" were found by the Fable 5.2
kernel analysis (doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md,
defects L-01 to L-03).</p>
 */
class CircularArrayListTest
{
    @Test
    void given_threeElements_when_traversed_then_orderAndNeighborsAreCircular()
    {
        CircularArrayList<String> list = abc();

        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(0)).isEqualTo("a");
        assertThat(list.get(2)).isEqualTo("c");
        assertThat(list.nextOf("c")).isEqualTo("a");
        assertThat(list.previousOf("a")).isEqualTo("c");
    }

    @Test
    void given_threeElements_when_insertBeforeHead_then_newElementBecomesHead()
    {
        CircularArrayList<String> list = abc();

        list.insertBefore("x", "a");

        assertThat(list.size()).isEqualTo(4);
        assertThat(list.get(0)).isEqualTo("x");
        assertThat(list.nextOf("x")).isEqualTo("a");
        assertThat(list.previousOf("x")).isEqualTo("c");
    }

    @Test
    void given_threeElements_when_reverse_then_orderIsMirrored()
    {
        CircularArrayList<String> list = abc();

        list.reverse();

        assertThat(list.get(0)).isEqualTo("c");
        assertThat(list.get(1)).isEqualTo("b");
        assertThat(list.get(2)).isEqualTo("a");
    }

    /**
    Defect L-01 (fixed): removeElemAtWindow() leaves `head` pointing at the
    removed node when the list becomes empty, so the next add() links the new
    element behind a stale node and get(0) resurrects the removed element.
    This is exactly the sequence lkev() performs when it kills the last edge
    of a loop (unlist both half-edges, then re-add the surviving one).
    */
    @Test
    void given_listEmptiedByRemove_when_add_then_onlyTheNewElementIsVisible()
    {
        CircularArrayList<String> list =
            new CircularArrayList<String>();
        list.add("a");
        list.remove(0);

        list.add("b");

        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0)).isEqualTo("b");
        assertThat(list.nextOf("b")).isEqualTo("b");
        assertThat(list.previousOf("b")).isEqualTo("b");
    }

    /**
    Defect L-01 (fixed, continued): after the stale-head corruption the ring
    holds a phantom node, so neighbor queries disagree with indexed access.
    */
    @Test
    void given_listEmptiedByRemove_when_addAndInsertBefore_then_neighborsMatchIndexOrder()
    {
        CircularArrayList<String> list =
            new CircularArrayList<String>();
        list.add("a");
        list.remove(0);
        list.add("b");

        list.insertBefore("c", "b");

        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0)).isEqualTo("c");
        assertThat(list.get(1)).isEqualTo("b");
        assertThat(list.nextOf("c")).isEqualTo("b");
        assertThat(list.nextOf("b")).isEqualTo("c");
    }

    /**
    Defect L-02 (fixed): remove(index) with an out-of-range index used to
    delete whatever element the window was left on by an unrelated earlier
    call. It is now a hard error that leaves the list unchanged.
    */
    @Test
    void given_windowLeftOnAnElement_when_removeWithInvalidIndex_then_errorAndNothingIsRemoved()
    {
        CircularArrayList<String> list = abc();
        list.locateWindowAtElem("b");

        assertThatThrownBy(() -> list.remove(7))
            .isInstanceOf(IndexOutOfBoundsException.class);

        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(1)).isEqualTo("b");
    }

    /**
    Defect L-03 (fixed): insertBefore(newElem, pivot) with a pivot that is
    not in the list used to insert at the head silently. It is now a hard
    error that leaves the list unchanged. Euler operators rely on this call (addhe) to place new
    half-edges, so a wrong pivot corrupts a loop without any diagnostic.
    */
    @Test
    void given_missingPivot_when_insertBefore_then_errorAndListIsNotModified()
    {
        CircularArrayList<String> list = abc();

        assertThatThrownBy(() -> list.insertBefore("x", "not-in-list"))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(0)).isEqualTo("a");
    }

    @Test
    void given_linkableElements_when_reorderedAndRemoved_then_neighborsStayConsistent()
    {
        CircularArrayList<Item> list = new CircularArrayList<>();
        Item a = new Item();
        Item b = new Item();
        Item c = new Item();
        list.add(a);
        list.add(b);
        list.add(c);

        list.reverse();
        assertThat(list.nextOf(c)).isSameAs(b);
        assertThat(list.previousOf(c)).isSameAs(a);

        list.swapElements(a, c);
        assertThat(list.get(0)).isSameAs(a);
        assertThat(list.nextOf(a)).isSameAs(b);

        list.locateWindowAtElem(b);
        list.removeElemAtWindow();
        assertThat(list.contains(b)).isFalse();
        assertThat(b.getListOwner()).isNull();
        assertThat(list.nextOf(a)).isSameAs(c);
        assertThat(list.nextOf(c)).isSameAs(a);

        CircularArrayList<Item> other = new CircularArrayList<>();
        other.add(a);
        assertThat(list.nextOf(a)).as("falls back to a scan").isSameAs(c);
        assertThat(other.nextOf(a)).isSameAs(a);
    }

    /**
    Reading does not write: two index loops interleaved over the same list
    (the pattern of every "all pairs" pass of the kernel) see every pair,
    and several threads reading at once see the same elements.
    */
    @Test
    void given_sharedList_when_readFromSeveralThreads_then_everyReaderSeesTheSameOrder()
        throws Exception
    {
        CircularArrayList<Item> list = new CircularArrayList<>();
        Item[] items = new Item[2000];
        for ( int i = 0; i < items.length; i++ ) {
            items[i] = new Item();
            list.add(items[i]);
        }
        java.util.concurrent.ExecutorService pool =
            java.util.concurrent.Executors.newFixedThreadPool(8);
        java.util.List<java.util.concurrent.Future<Integer>> results =
            new java.util.ArrayList<>();
        for ( int t = 0; t < 8; t++ ) {
            final int stride = t + 1;
            results.add(pool.submit(() -> {
                int mismatches = 0;
                for ( int round = 0; round < 50; round++ ) {
                    for ( int i = 0; i < items.length; i += stride ) {
                        if ( list.get(i) != items[i] ) mismatches++;
                        if ( list.nextOf(items[i]) !=
                             items[(i + 1) % items.length] ) mismatches++;
                        if ( list.get(items.length - 1 - i) !=
                             items[items.length - 1 - i] ) mismatches++;
                    }
                }
                return mismatches;
            }));
        }
        pool.shutdown();
        for ( java.util.concurrent.Future<Integer> r : results ) {
            assertThat(r.get()).isZero();
        }
    }

    @Test
    void given_linkableElements_when_everyKindOfEdit_then_handlesMatchIndexes()
    {
        CircularArrayList<Item> list = new CircularArrayList<>();
        Item[] items = new Item[40];
        for ( int i = 0; i < items.length; i++ ) {
            items[i] = new Item();
        }
        for ( int i = 0; i < 20; i++ ) list.add(items[i]);
        assertHandles(list);
        for ( int i = 20; i < 30; i++ ) list.insertBefore(items[i], items[i - 20]);
        assertHandles(list);
        for ( int i = 30; i < 40; i++ ) list.push(items[i]);
        assertHandles(list);
        list.remove(0);
        list.remove(list.size() - 1);
        list.remove(7);
        assertHandles(list);
        list.locateWindowAtElem(items[5]);
        list.removeElemAtWindow();
        assertThat(items[5].getListOwner()).isNull();
        assertHandles(list);
        list.swapElements(items[1], items[25]);
        list.reverse();
        assertHandles(list);
        list.clear();
        assertThat(list.size()).isZero();
        for ( Item item : items ) {
            assertThat(item.getListOwner()).isNull();
        }
    }

    @Test
    void given_cursor_when_iteratedBothWays_then_itWrapsAround()
    {
        CircularArrayList<String> list = abc();

        assertThat(list.next()).isEqualTo("a");
        assertThat(list.next()).isEqualTo("b");
        assertThat(list.next()).isEqualTo("c");
        assertThat(list.next()).isEqualTo("a");
        list.locateWindowAtIndex(0);
        assertThat(list.previous()).isEqualTo("a");
        assertThat(list.previous()).isEqualTo("c");
        assertThat(list.getWindow()).isEqualTo("b");
        list.push("z");
        assertThat(list.get(0)).isEqualTo("z");
        assertThat(list.getWindow()).isEqualTo("a");
    }

    private static void assertHandles(CircularArrayList<Item> list)
    {
        for ( int i = 0; i < list.size(); i++ ) {
            Item item = list.get(i);
            assertThat(item.getListOwner()).isSameAs(list);
            assertThat(item.getListIndex()).isEqualTo(i);
            assertThat(list.indexOf(item)).isEqualTo(i);
            assertThat(list.nextOf(item)).isSameAs(list.get((i + 1) % list.size()));
        }
    }

    private static final class Item implements ArrayLinkable
    {
        private Object listOwner;
        private int listIndex = -1;

        @Override
        public Object getListOwner()
        {
            return listOwner;
        }

        @Override
        public int getListIndex()
        {
            return listIndex;
        }

        @Override
        public void setListSlot(Object owner, int index)
        {
            listOwner = owner;
            listIndex = index;
        }
    }

    private static CircularArrayList<String> abc()
    {
        CircularArrayList<String> list =
            new CircularArrayList<String>();
        list.add("a");
        list.add("b");
        list.add("c");
        return list;
    }
}
