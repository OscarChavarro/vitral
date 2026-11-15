package vsdk.toolkit.common.dataStructures;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
Direct coverage for the circular list that backs every face, loop, edge and
vertex collection of PolyhedralBoundedSolid.

<p>Traceability: this container replaces the intrusive nxt/prv pointers of
[MANT1988] Ch. 10.2-10.3 and the list procedures of Ch. 11.2 (addlist,
dellist). The cases marked "hidden defect" were found by the Fable 5.2
kernel analysis (doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md,
defects L-01 to L-03); they stay disabled until the production fix lands.</p>
 */
class CircularDoubleLinkedListTest
{
    @Test
    void given_threeElements_when_traversed_then_orderAndNeighborsAreCircular()
    {
        CircularDoubleLinkedList<String> list = abc();

        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(0)).isEqualTo("a");
        assertThat(list.get(2)).isEqualTo("c");
        assertThat(list.nextOf("c")).isEqualTo("a");
        assertThat(list.previousOf("a")).isEqualTo("c");
    }

    @Test
    void given_threeElements_when_insertBeforeHead_then_newElementBecomesHead()
    {
        CircularDoubleLinkedList<String> list = abc();

        list.insertBefore("x", "a");

        assertThat(list.size()).isEqualTo(4);
        assertThat(list.get(0)).isEqualTo("x");
        assertThat(list.nextOf("x")).isEqualTo("a");
        assertThat(list.previousOf("x")).isEqualTo("c");
    }

    @Test
    void given_threeElements_when_reverse_then_orderIsMirrored()
    {
        CircularDoubleLinkedList<String> list = abc();

        list.reverse();

        assertThat(list.get(0)).isEqualTo("c");
        assertThat(list.get(1)).isEqualTo("b");
        assertThat(list.get(2)).isEqualTo("a");
    }

    /**
    Hidden defect L-01: removeElemAtWindow() leaves `head` pointing at the
    removed node when the list becomes empty, so the next add() links the new
    element behind a stale node and get(0) resurrects the removed element.
    This is exactly the sequence lkev() performs when it kills the last edge
    of a loop (unlist both half-edges, then re-add the surviving one).
    */
    @Disabled("Hidden defect L-01: stale head after removing the last element; see Fable5.2Analysis.md")
    @Test
    void given_listEmptiedByRemove_when_add_then_onlyTheNewElementIsVisible()
    {
        CircularDoubleLinkedList<String> list =
            new CircularDoubleLinkedList<String>();
        list.add("a");
        list.remove(0);

        list.add("b");

        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0)).isEqualTo("b");
        assertThat(list.nextOf("b")).isEqualTo("b");
        assertThat(list.previousOf("b")).isEqualTo("b");
    }

    /**
    Hidden defect L-01 (continued): after the stale-head corruption the ring
    holds a phantom node, so neighbor queries disagree with indexed access.
    */
    @Disabled("Hidden defect L-01: stale head after removing the last element; see Fable5.2Analysis.md")
    @Test
    void given_listEmptiedByRemove_when_addAndInsertBefore_then_neighborsMatchIndexOrder()
    {
        CircularDoubleLinkedList<String> list =
            new CircularDoubleLinkedList<String>();
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
    Hidden defect L-02: remove(index) with an out-of-range index does not
    reposition the window, so it silently deletes whatever element the window
    was left on by an unrelated earlier call.
    */
    @Disabled("Hidden defect L-02: remove(invalid index) deletes the stale window element; see Fable5.2Analysis.md")
    @Test
    void given_windowLeftOnAnElement_when_removeWithInvalidIndex_then_nothingIsRemoved()
    {
        CircularDoubleLinkedList<String> list = abc();
        list.locateWindowAtElem("b");

        list.remove(7);

        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(1)).isEqualTo("b");
    }

    /**
    Hidden defect L-03: insertBefore(newElem, pivot) with a pivot that is not
    in the list silently inserts at the head instead of reporting the broken
    precondition. Euler operators rely on this call (addhe) to place new
    half-edges, so a wrong pivot corrupts a loop without any diagnostic.
    */
    @Disabled("Hidden defect L-03: insertBefore with a missing pivot inserts at head; see Fable5.2Analysis.md")
    @Test
    void given_missingPivot_when_insertBefore_then_listIsNotModified()
    {
        CircularDoubleLinkedList<String> list = abc();

        list.insertBefore("x", "not-in-list");

        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(0)).isEqualTo("a");
    }

    private static CircularDoubleLinkedList<String> abc()
    {
        CircularDoubleLinkedList<String> list =
            new CircularDoubleLinkedList<String>();
        list.add("a");
        list.add("b");
        list.add("c");
        return list;
    }
}
