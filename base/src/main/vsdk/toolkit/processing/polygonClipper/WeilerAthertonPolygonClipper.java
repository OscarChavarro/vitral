//= References:                                                             =
//= [WEATH1977] Kevin Weiler and Peter Atherton. "HIDDEN SURFACE REMOVAL    =
//= USING POLYGON AREA SORTING",                                            =
//= Program of Computer Graphics, Cornell University. Ithaca, New York, 1977=
package vsdk.toolkit.processing.polygonClipper;

import java.util.ArrayList;
import vsdk.toolkit.environment.geometry.Polygon2D;

/**
 * Implements the Weiler-Atherton polygon clipping algorithm for convex and
 * non-convex polygons, including polygons with holes.
 *
 * This implementation also handles degenerate cases in which vertices from the
 * clip polygon and the subject polygon lie on each other's boundary.
 *
 * The code in this class corresponds to the "POLYGON CLIPPING ALGORITHM"
 * section of [WEATH1977].
*/
public class WeilerAthertonPolygonClipper {

    private _Polygon2DWA clipPolyWA;
    private _Polygon2DWA subjectPolyWA;
    // Per-edge traversal state used while building the intersection graph.
    private boolean firstIntersection;
    private boolean previousOut;
    private final boolean coincidentPoints[] = new boolean[4];

    /**
     * Computes the intersection and exterior regions produced by clipping the
     * subject polygon against the clip polygon.
     *
     * @param clipPoly clipping polygon
     * @param subjectPoly polygon to be clipped
     * @param innerPolyOut output polygon that receives the regions inside the
     * clipping polygon
     * @param outerPolyOut output polygon that receives the regions outside the
     * clipping polygon
     */
    public void clipPolygons(Polygon2D clipPoly, Polygon2D subjectPoly, Polygon2D innerPolyOut, Polygon2D outerPolyOut) {
        _DoubleLinkedListNode<_VertexNode2D> dllnVertNodeC;
        _DoubleLinkedListNode<_VertexNode2D> dllnVertNodeS;
        _DoubleLinkedListNode<_VertexNode2D> dllnVertNodePrevC;
        _DoubleLinkedListNode<_VertexNode2D> dllnVertNodePrevS;
        _VertexNode2D nodeC, nodePrevC;
        _VertexNode2D nodeS, nodePrevS;
        _VertexNode2D nodeIntersecS;
        ArrayList<_DoubleLinkedListNode<_VertexNode2D>> intersecVertListOut = new ArrayList<_DoubleLinkedListNode<_VertexNode2D>>();
        ArrayList<_DoubleLinkedListNode<_VertexNode2D>> intersecVertListIn = new ArrayList<_DoubleLinkedListNode<_VertexNode2D>>();
        _DoubleLinkedListNode<_VertexNode2D> iterator;
        boolean emptyInnerPolyOut = true;
        boolean emptyOuterPolyOut = true;
        int i,j;

        if ( innerPolyOut == null ) {
            return;
        }
        if ( outerPolyOut == null ) {
            return;
        }
        clipPolyWA = new _Polygon2DWA(clipPoly, true);
        subjectPolyWA = new _Polygon2DWA(subjectPoly, true);
        // Find all intersections and populate the entry/exit lists.
        for ( i = 0; i < clipPolyWA.loops.size(); ++i ) {
            _Polygon2DContourWA p2DContClip = clipPolyWA.loops.get(i);
            p2DContClip.isClipped = false;
            dllnVertNodeC = p2DContClip.vertices.getHead();
            if ( p2DContClip.vertices.size() > 1 ) {
                do {
                    nodeC = dllnVertNodeC.data;
                    dllnVertNodePrevC = dllnVertNodeC.previous;
                    nodePrevC = dllnVertNodePrevC.data;
                    previousOut = false;
                    for ( j = 0; j < subjectPolyWA.loops.size(); ++j ) {
                        _Polygon2DContourWA p2DContSubj = subjectPolyWA.loops.get(j);
                        dllnVertNodeS = p2DContSubj.vertices.getHead();
                        if ( p2DContSubj.vertices.size() > 1 ) {
                            firstIntersection = true;
                            do {
                                nodeS = dllnVertNodeS.data;
                                dllnVertNodePrevS = dllnVertNodeS.previous;
                                nodePrevS = dllnVertNodePrevS.data;
                                nodeIntersecS = new _VertexNode2D();
                                if ( intersecLineLine2D(nodePrevC, nodeC, nodePrevS, nodeS, nodeIntersecS, coincidentPoints) ) {
                                    makeCut(
                                        p2DContClip, p2DContSubj, dllnVertNodePrevC, dllnVertNodeC, dllnVertNodePrevS, dllnVertNodeS, nodeIntersecS, intersecVertListOut, intersecVertListIn);
                                }
                                dllnVertNodeS = dllnVertNodeS.next;
                            } while ( dllnVertNodeS != p2DContSubj.vertices.getHead() );
                        }
                    }
                    dllnVertNodeC = dllnVertNodeC.next;
                } while ( dllnVertNodeC != p2DContClip.vertices.getHead() );
            }
        }
        // Traverse the generated intersection structure to build the result.
        // Inner polygons:
        for ( i=0; i < intersecVertListOut.size(); ++i ) {
            _DoubleLinkedListNode<_VertexNode2D> dllnNodeS = intersecVertListOut.get(i);
            if ( (dllnNodeS.data.flags & 0x01) == 0 ) {
                if ( emptyInnerPolyOut ) {
                    emptyInnerPolyOut = false;
                } else {
                    innerPolyOut.nextLoop();
                }
                iterator = dllnNodeS;
                do {
                    iterator.data.flags = (byte) (iterator.data.flags | 0x01); //Now this node may not be used in an interior polygon.
                    innerPolyOut.addVertex(iterator.data.x, iterator.data.y, iterator.data.color.r, iterator.data.color.g, iterator.data.color.b);
                    iterator = iterator.next;
                    iterator.data.flags = (byte) (iterator.data.flags | 0x01); //Now this node may not be used in an interior polygon.
                    if ( iterator.data.pairNode != null ) {
                        iterator = iterator.data.pairNode;
                    }
                } while ( iterator != dllnNodeS && iterator.data.pairNode != dllnNodeS );
            }
        }
        // Outer polygons:
        for ( i=0; i < intersecVertListIn.size(); ++i ) {
            _DoubleLinkedListNode<_VertexNode2D> dllnNodeS = intersecVertListIn.get(i);
            if ( (dllnNodeS.data.flags & 0x02) == 0 ) {
                boolean isSubject;

                if ( emptyOuterPolyOut ) {
                    emptyOuterPolyOut = false;
                } else {
                    outerPolyOut.nextLoop();
                }
                iterator = dllnNodeS;
                isSubject = true;
                do {
                    iterator.data.flags = (byte) (iterator.data.flags | 0x02); //Now this node may not be used in an outer polygon.
                    outerPolyOut.addVertex(iterator.data.x, iterator.data.y, iterator.data.color.r, iterator.data.color.g, iterator.data.color.b);
                    if ( isSubject ) {
                        iterator = iterator.next;
                    } else {
                        iterator = iterator.previous;
                    }
                    iterator.data.flags = (byte) (iterator.data.flags | 0x02); //Now this node may not be used in an outer polygon.
                    if ( iterator.data.pairNode != null ) {
                        iterator = iterator.data.pairNode;
                        isSubject = !isSubject;
                    }
                } while ( iterator != dllnNodeS && iterator.data.pairNode != dllnNodeS );
            }
        }

        // Handle loops that never intersected but are fully contained in the other polygon.
        classifyHolesAndContours(getClipPolyWA());
        classifyHolesAndContours(getSubjectPolyWA());

        // Handle non-intersecting clip contours that lie entirely inside the subject polygon.
        for ( i=0; i < clipPolyWA.loops.size(); ++i ) {
            _Polygon2DContourWA p2DContClip = clipPolyWA.loops.get(i);
            if ( !p2DContClip.isClipped ) {
                _DoubleLinkedListNode<_VertexNode2D> dllnNode, head;
                boolean insideAContourNotAHole = false;
                _VertexNode2D vertClip;
                boolean pointInPolygon;

                head = p2DContClip.vertices.getHead();
                if ( head == null ) {
                    continue;
                }
                for ( j=0; j < subjectPolyWA.loops.size(); ++j ) {
                    _Polygon2DContourWA p2DContSubj = subjectPolyWA.loops.get(j);
                    dllnNode = head;
                    pointInPolygon = false;
                    do {
                        vertClip = dllnNode.data;
                        if ( isPointInPolygon2D(vertClip, p2DContSubj.vertices) == 1 ) {
                            pointInPolygon = true;
                            break;
                        }
                        dllnNode = dllnNode.next;
                    } while ( dllnNode != head );
                    if ( pointInPolygon ) {
                        insideAContourNotAHole = !insideAContourNotAHole;
                    }
                }
                if ( insideAContourNotAHole ) {
                    if ( emptyInnerPolyOut ) {
                        emptyInnerPolyOut = false;
                    } else {
                        innerPolyOut.nextLoop();
                    }
                    if ( emptyOuterPolyOut ) {
                        emptyOuterPolyOut = false;
                    } else {
                        outerPolyOut.nextLoop();
                    }
                    dllnNode = head;
                    do {
                        innerPolyOut.addVertex(dllnNode.data.x, dllnNode.data.y, dllnNode.data.color.r, dllnNode.data.color.g, dllnNode.data.color.b);
                        dllnNode = dllnNode.next;
                    } while ( dllnNode != head );
                    dllnNode = head;
                    do {
                        outerPolyOut.addVertex(dllnNode.data.x, dllnNode.data.y, dllnNode.data.color.r, dllnNode.data.color.g, dllnNode.data.color.b);
                        dllnNode = dllnNode.previous;
                    } while ( dllnNode != head );
                }
            }
        }
        // Handle non-intersecting subject contours.
        for ( i=0; i < subjectPolyWA.loops.size(); ++i ) {
            _Polygon2DContourWA p2DContSubj = subjectPolyWA.loops.get(i);
            if ( !p2DContSubj.isClipped ) {
                boolean insideAContourNotAHole = false;
                _VertexNode2D vertSubj;
                _DoubleLinkedListNode<_VertexNode2D> dllnNode, head;
                boolean pointInPolygon;

                head = p2DContSubj.vertices.getHead();
                if ( head == null ) {
                    continue;
                }
                for ( j=0; j < clipPolyWA.loops.size(); ++j ) {
                    _Polygon2DContourWA p2DContClip = clipPolyWA.loops.get(j);
                    dllnNode = head;
                    pointInPolygon = false;

                    do {
                        vertSubj = dllnNode.data;
                        if ( isPointInPolygon2D(vertSubj, p2DContClip.vertices) == 1 ) {
                            pointInPolygon = true;
                            break;
                        }
                        dllnNode = dllnNode.next;
                    } while ( dllnNode != head );
                    if ( pointInPolygon ) {
                        insideAContourNotAHole = !insideAContourNotAHole;
                    }
                }
                head = p2DContSubj.vertices.getHead();

                if ( insideAContourNotAHole ) {
                    if ( emptyInnerPolyOut ) {
                        emptyInnerPolyOut = false;
                    } else {
                        innerPolyOut.nextLoop();
                    }
                } else {
                    if ( emptyOuterPolyOut ) {
                        emptyOuterPolyOut = false;
                    } else {
                        outerPolyOut.nextLoop();
                    }
                }

                dllnNode = head;
                do {
                    if ( insideAContourNotAHole ) {
                        innerPolyOut.addVertex(dllnNode.data.x, dllnNode.data.y, dllnNode.data.color.r, dllnNode.data.color.g, dllnNode.data.color.b);
                    } else {
                        outerPolyOut.addVertex(dllnNode.data.x, dllnNode.data.y, dllnNode.data.color.r, dllnNode.data.color.g, dllnNode.data.color.b);
                    }
                    dllnNode = dllnNode.next;
                } while ( dllnNode != head );
            }
        }

    }

    /**
     * Classifies the current edge-edge contact and inserts cut vertices when
     * the Weiler-Atherton traversal requires them.
     *
     * The method handles both regular intersections and degenerate cases with
     * coincident endpoints or overlapping parallel edges.
     */
    private void makeCut(
        _Polygon2DContourWA p2DContClip, _Polygon2DContourWA p2DContSubj, _DoubleLinkedListNode<_VertexNode2D> dllnVertNodePrevC, _DoubleLinkedListNode<_VertexNode2D> dllnVertNodeC, _DoubleLinkedListNode<_VertexNode2D> dllnVertNodePrevS, _DoubleLinkedListNode<_VertexNode2D> dllnVertNodeS, _VertexNode2D nodeIntersecS, ArrayList<_DoubleLinkedListNode<_VertexNode2D>> intersecVertListOut, ArrayList<_DoubleLinkedListNode<_VertexNode2D>> intersecVertListIn) {
        _VertexNode2D nodeC, nodePrevC;
        _VertexNode2D nodeS, nodePrevS;
        double dotProd;
        boolean firstCutOutOfSubject;

        if ( !coincidentPoints[0] && !coincidentPoints[2] ) {
            firstCutOutOfSubject = false;
            if ( firstIntersection ) {
                firstCutOutOfSubject = (crossProduct2D(
                    dllnVertNodePrevS.data, dllnVertNodeS.data, dllnVertNodePrevC.data, dllnVertNodeC.data) < 0);
            }
            updatePolygonsAndListsWithCuts(
                p2DContClip, p2DContSubj, dllnVertNodePrevC, dllnVertNodeC, dllnVertNodePrevS, dllnVertNodeS, nodeIntersecS, firstCutOutOfSubject, intersecVertListOut, intersecVertListIn, true);
            return;
        }
        // There are one or more coincident points.
        nodePrevC = dllnVertNodePrevC.data;
        nodeC = dllnVertNodeC.data;
        nodePrevS = dllnVertNodePrevS.data;
        nodeS = dllnVertNodeS.data;
        dotProd = dotProductNorm2D(nodePrevC, nodeC, nodePrevS, nodeS);
        if ( dotProd >= 0.9999 && dotProd <= 1.0001 ) {
            /**
             * The vectors below are described by the companion diagrams:
             * for the first endpoints:
             * <img src="doc-files\ParallelLinesFirstPoints.png" alt="Parallel lines, first points">
             * and for the second endpoints:
             * <img src="doc-files\ParallelLinesSecondPoints.png" alt="Parallel lines, second points">
             */
            _VertexNode2D vecParallel = new _VertexNode2D();
            _VertexNode2D negVecParallel = new _VertexNode2D();
            _VertexNode2D vecAwayParallelLines1C = new _VertexNode2D();
            _VertexNode2D vecAwayParallelLines1S = new _VertexNode2D();
            _VertexNode2D vecAwayParallelLines2C = new _VertexNode2D();
            _VertexNode2D vecAwayParallelLines2S = new _VertexNode2D();

            vecParallel.x = nodeC.x - nodePrevC.x;
            vecParallel.y = nodeC.y - nodePrevC.y;
            negVecParallel.x = -vecParallel.x;
            negVecParallel.y = -vecParallel.y;
            if ( coincidentPoints[0] ) {
                vecAwayParallelLines1C.x = dllnVertNodePrevC.previous.data.x - dllnVertNodePrevC.data.x;
                vecAwayParallelLines1C.y = dllnVertNodePrevC.previous.data.y - dllnVertNodePrevC.data.y;
            } else {
                vecAwayParallelLines1C.x = negVecParallel.x;
                vecAwayParallelLines1C.y = negVecParallel.y;
            }
            if ( coincidentPoints[2] ) {
                vecAwayParallelLines1S.x = dllnVertNodePrevS.previous.data.x - dllnVertNodePrevS.data.x;
                vecAwayParallelLines1S.y = dllnVertNodePrevS.previous.data.y - dllnVertNodePrevS.data.y;
            } else {
                vecAwayParallelLines1S.x = negVecParallel.x;
                vecAwayParallelLines1S.y = negVecParallel.y;
            }
            if ( coincidentPoints[1] ) {
                vecAwayParallelLines2C.x = dllnVertNodeC.next.data.x - dllnVertNodeC.data.x;
                vecAwayParallelLines2C.y = dllnVertNodeC.next.data.y - dllnVertNodeC.data.y;
            } else {
                vecAwayParallelLines2C.x = vecParallel.x;
                vecAwayParallelLines2C.y = vecParallel.y;
            }
            if ( coincidentPoints[3] ) {
                vecAwayParallelLines2S.x = dllnVertNodeS.next.data.x - dllnVertNodeS.data.x;
                vecAwayParallelLines2S.y = dllnVertNodeS.next.data.y - dllnVertNodeS.data.y;
            } else {
                vecAwayParallelLines2S.x = vecParallel.x;
                vecAwayParallelLines2S.y = vecParallel.y;
            }
            // First endpoints.
            if ( are3VectorsOrderedCounterclockwise2D(
                vecParallel, vecAwayParallelLines1C, vecAwayParallelLines1S) == 1 ) {
                firstCutOutOfSubject = true;
                updatePolygonsAndListsWithCuts(
                    p2DContClip, p2DContSubj, dllnVertNodePrevC, dllnVertNodeC, dllnVertNodePrevS, dllnVertNodeS, nodeIntersecS, firstCutOutOfSubject, intersecVertListOut, intersecVertListIn, true);
            }
            // Second endpoints.
            if ( are3VectorsOrderedCounterclockwise2D(
                negVecParallel, vecAwayParallelLines2S, vecAwayParallelLines2C) == 1 ) {
                firstCutOutOfSubject = false;
                updatePolygonsAndListsWithCuts(
                    p2DContClip, p2DContSubj, dllnVertNodePrevC, dllnVertNodeC, dllnVertNodePrevS, dllnVertNodeS, nodeIntersecS, firstCutOutOfSubject, intersecVertListOut, intersecVertListIn, false);
            }
        } else if ( Math.abs(dotProd) < 0.9999 ) {
            // Only coincidentPoints[0] and coincidentPoints[2] matter in this branch.
            _VertexNode2D vecAC = new _VertexNode2D();
            _VertexNode2D vecBC = new _VertexNode2D();
            _VertexNode2D vecAS = new _VertexNode2D();
            _VertexNode2D vecBS = new _VertexNode2D();
            boolean thereAreCut;
            byte orderVecAC, orderVecBC;

            vecAC.x = dllnVertNodeC.data.x - dllnVertNodePrevC.data.x;
            vecAC.y = dllnVertNodeC.data.y - dllnVertNodePrevC.data.y;
            if ( coincidentPoints[0] ) {
                vecBC.x = dllnVertNodePrevC.previous.data.x - dllnVertNodePrevC.data.x;
                vecBC.y = dllnVertNodePrevC.previous.data.y - dllnVertNodePrevC.data.y;
            } else {
                vecBC.x = -vecAC.x;
                vecBC.y = -vecAC.y;
            }
            vecAS.x = dllnVertNodeS.data.x - dllnVertNodePrevS.data.x;
            vecAS.y = dllnVertNodeS.data.y - dllnVertNodePrevS.data.y;
            if ( coincidentPoints[2] ) {
                vecBS.x = dllnVertNodePrevS.previous.data.x - dllnVertNodePrevS.data.x;
                vecBS.y = dllnVertNodePrevS.previous.data.y - dllnVertNodePrevS.data.y;
            } else {
                vecBS.x = -vecAS.x;
                vecBS.y = -vecAS.y;
            }
            // Determine whether the coincident endpoint configuration creates a cut.
            orderVecAC = are3VectorsOrderedCounterclockwise2D(vecAS, vecAC, vecBS);
            orderVecBC = are3VectorsOrderedCounterclockwise2D(vecAS, vecBC, vecBS);
            thereAreCut = false;
            firstCutOutOfSubject = false;
            if ( orderVecAC != 0 && orderVecBC != 0 ) { // Parallel lines are handled above.
                if ( (orderVecAC == 1) != (orderVecBC == 1) ) {
                    thereAreCut = true;
                    firstCutOutOfSubject = (orderVecAC == -1); // Clockwise ordering.
                }
            }
            if ( thereAreCut ) {
                updatePolygonsAndListsWithCuts(
                    p2DContClip, p2DContSubj, dllnVertNodePrevC, dllnVertNodeC, dllnVertNodePrevS, dllnVertNodeS, nodeIntersecS, firstCutOutOfSubject, intersecVertListOut, intersecVertListIn, true);
            }
        }
    }

    /**
     * Inserts the cut vertices into both polygon contours, or links existing
     * vertices when the intersection occurs at coincident endpoints.
     *
     * This method also updates the intersection lists used later to traverse
     * interior and exterior output loops.
     *
     * @param firstCutOutOfSubject only meaningful while processing the first
     * intersection on the current subject contour
     * @param operateOnFirstPointsOfLines false only when processing the second
     * endpoint of overlapping parallel edges
     */
    private void updatePolygonsAndListsWithCuts(
        _Polygon2DContourWA p2DContClip, _Polygon2DContourWA p2DContSubj, _DoubleLinkedListNode<_VertexNode2D> dllnVertNodePrevC, _DoubleLinkedListNode<_VertexNode2D> dllnVertNodeC, _DoubleLinkedListNode<_VertexNode2D> dllnVertNodePrevS, _DoubleLinkedListNode<_VertexNode2D> dllnVertNodeS, _VertexNode2D nodeIntersecS, boolean firstCutOutOfSubject, ArrayList<_DoubleLinkedListNode<_VertexNode2D>> intersecVertListOut, ArrayList<_DoubleLinkedListNode<_VertexNode2D>> intersecVertListIn, boolean operateOnFirstPointsOfLines) {
        _VertexNode2D nodeIntersecC;
        _DoubleLinkedListNode<_VertexNode2D> dllnVertNodeCutS;

        dllnVertNodeCutS = null;
        if ( !coincidentPoints[0] && !coincidentPoints[2] ) {
            nodeIntersecC = new _VertexNode2D(nodeIntersecS);
            // Insert the cut point into both polygon contours.
            nodeIntersecS.pairNode = insertOrderedNodeBetweenTwoNodes(
                p2DContClip.vertices, dllnVertNodePrevC, dllnVertNodeC, nodeIntersecC);
            nodeIntersecC.pairNode = p2DContSubj.vertices.insertBefore(nodeIntersecS, dllnVertNodeS);
            dllnVertNodeCutS = nodeIntersecC.pairNode;
        } else {
            _VertexNode2D nodeIntersec;

            if ( operateOnFirstPointsOfLines ) {
                if ( coincidentPoints[0] ) {
                    if ( coincidentPoints[2] ) {
                        dllnVertNodePrevC.data.pairNode = dllnVertNodePrevS;
                        dllnVertNodePrevS.data.pairNode = dllnVertNodePrevC;
                        dllnVertNodeCutS = dllnVertNodePrevS;
                    } else {
                        nodeIntersec = new _VertexNode2D(dllnVertNodePrevC.data);
                        dllnVertNodePrevC.data.pairNode
                            = p2DContSubj.vertices.insertBefore(nodeIntersec, dllnVertNodeS);
                        nodeIntersec.pairNode = dllnVertNodePrevC;
                        dllnVertNodeCutS = dllnVertNodePrevC.data.pairNode;
                    }
                } else if ( coincidentPoints[2] ) {
                    nodeIntersec = new _VertexNode2D(dllnVertNodePrevS.data);
                    dllnVertNodePrevS.data.pairNode = insertOrderedNodeBetweenTwoNodes(
                        p2DContClip.vertices, dllnVertNodePrevC, dllnVertNodeC, nodeIntersec);
                    nodeIntersec.pairNode = dllnVertNodePrevS;
                    dllnVertNodeCutS = dllnVertNodePrevS;
                }
            } else {
                if ( coincidentPoints[1] ) {
                    if ( coincidentPoints[3] ) {
                        dllnVertNodeC.data.pairNode = dllnVertNodeS;
                        dllnVertNodeS.data.pairNode = dllnVertNodeC;
                        dllnVertNodeCutS = dllnVertNodeS;
                    } else {
                        nodeIntersec = new _VertexNode2D(dllnVertNodeC.data);
                        dllnVertNodeC.data.pairNode
                            = p2DContSubj.vertices.insertBefore(nodeIntersec, dllnVertNodeS);
                        nodeIntersec.pairNode = dllnVertNodeC;
                        dllnVertNodeCutS = dllnVertNodeC.data.pairNode;
                    }
                } else if ( coincidentPoints[3] ) {
                    nodeIntersec = new _VertexNode2D(dllnVertNodeS.data);
                    dllnVertNodeS.data.pairNode = insertOrderedNodeBetweenTwoNodes(
                        p2DContClip.vertices, dllnVertNodePrevC, dllnVertNodeC, nodeIntersec);
                    nodeIntersec.pairNode = dllnVertNodeS;
                    dllnVertNodeCutS = dllnVertNodeS;
                }
            }
        }
        // Record subject-polygon intersections as exit or entry events for the later traversal pass.
        if ( firstIntersection ) {
            p2DContSubj.isClipped = true;
            p2DContClip.isClipped = true;
            if ( firstCutOutOfSubject ) {
                intersecVertListOut.add(dllnVertNodeCutS);
                previousOut = true;
            } else {
                intersecVertListIn.add(dllnVertNodeCutS);
                previousOut = false;
            }
            firstIntersection = false;
        } else {
            if ( previousOut ) {
                intersecVertListIn.add(dllnVertNodeCutS);
            } else {
                intersecVertListOut.add(dllnVertNodeCutS);
            }
            previousOut = !previousOut;
        }
    }

    /**
     * Classifies each loop in the polygon as either an outer contour or a hole.
     *
     * The method assumes the polygon loops do not intersect each other.
     *
     * @param polygon polygon whose loops will be classified
     */
    private void classifyHolesAndContours(_Polygon2DWA polygon) {
        int i,j;
        
        for ( i = 0; i < polygon.loops.size(); ++i ) {
            _Polygon2DContourWA p2DContTest = polygon.loops.get(i);
            _DoubleLinkedListNode<_VertexNode2D> dllnNode, head;
            _VertexNode2D vertTest;
            boolean pointInPolygon;

            for ( j = 0; j < polygon.loops.size(); ++j ) {
                _Polygon2DContourWA p2DCont = polygon.loops.get(j);
                if ( p2DCont != p2DContTest ) {
                    head = p2DContTest.vertices.getHead();
                    if ( head == null ) {
                        continue;
                    }
                    dllnNode = head;
                    pointInPolygon = false;
                    do {
                        vertTest = dllnNode.data;
                        if ( isPointInPolygon2D(vertTest, p2DCont.vertices) == 1 ) {
                            pointInPolygon = true;
                            break;
                        }
                        dllnNode = dllnNode.next;
                    } while ( dllnNode != head );
                    if ( pointInPolygon ) {
                        p2DContTest.isHole = !p2DContTest.isHole;
                    }
                }
            }
        }
    }

    /**
     * Tests the cyclic order of three vectors anchored at the origin.
     *
     * @param v1 first vector
     * @param v2 second vector
     * @param v3 third vector
     * @return 1 if the vectors are ordered counterclockwise, -1 if they are
     * ordered clockwise, or 0 if any pair is collinear and points in the same
     * direction
     */
    private byte are3VectorsOrderedCounterclockwise2D(_VertexNode2D v1, _VertexNode2D v2, _VertexNode2D v3) {
        double temp;

        temp = Math.sqrt(v1.x * v1.x + v1.y * v1.y);
        v1.x = v1.x / temp;
        v1.y = v1.y / temp;
        temp = Math.sqrt(v2.x * v2.x + v2.y * v2.y);
        v2.x = v2.x / temp;
        v2.y = v2.y / temp;
        temp = Math.sqrt(v3.x * v3.x + v3.y * v3.y);
        v3.x = v3.x / temp;
        v3.y = v3.y / temp;
        temp = dotProduct2D(v1, v2);
        if ( temp > 0.9999 && temp < 1.0001 ) {
            return 0;
        }
        temp = dotProduct2D(v2, v3);
        if ( temp > 0.9999 && temp < 1.0001 ) {
            return 0;
        }
        temp = dotProduct2D(v1, v3);
        if ( temp > 0.9999 && temp < 1.0001 ) {
            return 0;
        }

        if ( crossProduct2D(v1, v3) < 0 ) {
            if ( crossProduct2D(v2, v3) >= 0 ) {
                return 1;
            } else if ( crossProduct2D(v1, v2) > 0 ) {
                return 1;
            } else {
                return -1;
            }
        } else {
            if ( crossProduct2D(v2, v3) <= 0 ) {
                return -1;
            } else if ( crossProduct2D(v2, v1) > 0 ) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    /* Function isPointInPolygon2D is based on the function developed by
     * W. Randolph Franklin:
     * http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
     * visited on Sep 08, 2014.
     * The boundary test was added in this implementation.
    
     License to Use

     Copyright (c) 1970-2003, Wm. Randolph Franklin

     Permission is hereby granted, free of charge, to any person obtaining a
     copy of this software and associated documentation files (the "Software"),
     to deal in the Software without restriction, including without limitation
     the rights to use, copy, modify, merge, publish, distribute, sublicense,
     and/or sell copies of the Software, and to permit persons to whom the
     Software is furnished to do so, subject to the following conditions:

     Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimers.
     Redistributions in binary form must reproduce the above copyright notice
     in the documentation and/or other materials provided with the distribution.
     The name of W. Randolph Franklin may not be used to endorse or promote
     products derived from this Software without specific prior written permission.
     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
     OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
     FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
     AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
     WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
     IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
     */
    /**
     * Classifies a point as inside, outside, or on the boundary of a polygon loop.
     *
     * @param point point to classify
     * @param polygon polygon loop
     * @return 1 if the point is inside, -1 if it is outside, or 0 if it lies
     * on the boundary
     */
    private byte isPointInPolygon2D(_VertexNode2D point, _CircularDoubleLinkedList<_VertexNode2D> polygon) {
        _DoubleLinkedListNode<_VertexNode2D> dllnNode;
        boolean isInside = false;
        double temp;

        dllnNode = polygon.getHead();
        if ( dllnNode == null ) {
            return -1;
        }
        do {
            temp = dllnNode.data.y - dllnNode.next.data.y;
            if ( Math.abs(temp) < 0.0001 ) { // Horizontal edge.
                if ( Math.abs(point.y - (dllnNode.next.data.y + temp / 2)) < 0.0001 ) {
                    if ( (dllnNode.next.data.x - point.x) * (point.x - dllnNode.data.x) >= 0 ) {
                        return 0;
                    }
                }
            }
            if ( point.y < dllnNode.data.y != point.y < dllnNode.next.data.y ) {
                temp = ((dllnNode.next.data.x - dllnNode.data.x) * (point.y - dllnNode.data.y) / (dllnNode.next.data.y - dllnNode.data.y) + dllnNode.data.x);
                if ( Math.abs(point.x - temp) < 0.0001 ) {
                    return 0;
                }
                if ( point.x < temp ) {
                    isInside = !isInside;
                }
            }
            dllnNode = dllnNode.next;
        } while ( dllnNode != polygon.getHead() );
        if ( isInside ) {
            return 1;
        } else {
            return -1;
        }
    }

    /**
     * Returns the scalar z-component of the 2D cross product between two segments.
     *
     * The segments are interpreted as vectors {@code a1->a2} and {@code b1->b2}.
     */
    private double crossProduct2D(_VertexNode2D a1, _VertexNode2D a2, _VertexNode2D b1, _VertexNode2D b2) {
        double[] v1, v2;

        v1 = new double[2];
        v2 = new double[2];
        v1[0] = a2.x - a1.x;
        v1[1] = a2.y - a1.y;
        v2[0] = b2.x - b1.x;
        v2[1] = b2.y - b1.y;
        return v1[0] * v2[1] - v1[1] * v2[0];
    }

    /**
     * Returns the scalar z-component of the 2D cross product of two vectors.
     *
     * The vectors are assumed to be anchored at the origin.
     */
    private double crossProduct2D(_VertexNode2D v1, _VertexNode2D v2) {
        return v1.x * v2.y - v1.y * v2.x;
    }

    private double dotProduct2D(_VertexNode2D v1, _VertexNode2D v2) {
        return v1.x * v2.x + v1.y * v2.y;
    }

    private double dotProductNorm2D(_VertexNode2D a1, _VertexNode2D a2, _VertexNode2D b1, _VertexNode2D b2) {
        double temp;
        double[] v1, v2;

        v1 = new double[2];
        v2 = new double[2];
        v1[0] = a2.x - a1.x;
        v1[1] = a2.y - a1.y;
        v2[0] = b2.x - b1.x;
        v2[1] = b2.y - b1.y;
        temp = Math.sqrt(v1[0] * v1[0] + v1[1] * v1[1]);
        v1[0] = v1[0] / temp;
        v1[1] = v1[1] / temp;
        temp = Math.sqrt(v2[0] * v2[0] + v2[1] * v2[1]);
        v2[0] = v2[0] / temp;
        v2[1] = v2[1] / temp;
        return v1[0] * v2[0] + v1[1] * v2[1];
    }

    /**
     * Inserts an intersection vertex between two existing contour nodes.
     *
     * Additional intersection vertices may already exist between the input
     * nodes; this method preserves the ordering along the segment.
     */
    private _DoubleLinkedListNode<_VertexNode2D> insertOrderedNodeBetweenTwoNodes(
            _CircularDoubleLinkedList<_VertexNode2D> linkedList, _DoubleLinkedListNode<_VertexNode2D> dllnVertNode, _DoubleLinkedListNode<_VertexNode2D> dllnVertNodeNext, _VertexNode2D nodeIntersec) {
        int sign;
        _DoubleLinkedListNode<_VertexNode2D> dllnNode;

        dllnNode = dllnVertNode;
        if ( Math.abs(dllnVertNode.data.x - dllnVertNodeNext.data.x) > Math.abs(dllnVertNode.data.y - dllnVertNodeNext.data.y) ) {
            sign = (int) ((dllnVertNodeNext.data.x - dllnVertNode.data.x) / Math.abs(dllnVertNodeNext.data.x - dllnVertNode.data.x));
            while ( dllnNode.data.x * sign < nodeIntersec.x * sign && dllnNode != dllnVertNodeNext ) {
                dllnNode = dllnNode.next;
            }
        } else {
            sign = (int) ((dllnVertNodeNext.data.y - dllnVertNode.data.y) / Math.abs(dllnVertNodeNext.data.y - dllnVertNode.data.y));
            while ( dllnNode.data.y * sign < nodeIntersec.y * sign && dllnNode != dllnVertNodeNext ) {
                dllnNode = dllnNode.next;
            }
        }
        return linkedList.insertBefore(nodeIntersec, dllnNode);
    }

    /**
     * Computes the intersection between two 2D line segments.
     *
     * For coincident-endpoint cases, only the {@code inP0}/{@code inP2}
     * configuration is considered a clipping event by this implementation.
     *
     * @param outPIntersec receives the computed intersection point when the
     * intersection is not represented only by coincident endpoints
     * @param outCoincidentPoints four-element array describing endpoint
     * coincidences against the opposite segment
     * @return true if the segments intersect under the rules used by this
     * clipper
     */
    public boolean intersecLineLine2D(
        _VertexNode2D inP0, _VertexNode2D inP1, _VertexNode2D inP2, _VertexNode2D inP3, _VertexNode2D outPIntersec, boolean[] outCoincidentPoints) {
        double temp1, temp2;
        boolean vertical1, vertical2;
        double m1, b1, m2, b2;
        boolean intersec1, intersec2;
        boolean pointToPointCoincidence;
        boolean point0ToPoint2Coincidence;
        boolean point0ToPoint3Coincidence;
        boolean point2ToPoint1Coincidence;

        outCoincidentPoints[0] = false;
        outCoincidentPoints[1] = false;
        outCoincidentPoints[2] = false;
        outCoincidentPoints[3] = false;
        pointToPointCoincidence = false;
        point0ToPoint2Coincidence = false;
        point0ToPoint3Coincidence = false;
        point2ToPoint1Coincidence = false;
        if ( Math.abs(inP0.x - inP2.x) < 0.0001 && Math.abs(inP0.y - inP2.y) < 0.0001 ) {
            outCoincidentPoints[0] = true;
            outCoincidentPoints[2] = true;
            point0ToPoint2Coincidence = true;
            pointToPointCoincidence = true;
        }
        if ( Math.abs(inP0.x - inP3.x) < 0.0001 && Math.abs(inP0.y - inP3.y) < 0.0001 ) {
            outCoincidentPoints[0] = true;
            outCoincidentPoints[3] = true;
            point0ToPoint3Coincidence = true;
            pointToPointCoincidence = true;
        }
        if ( Math.abs(inP1.x - inP2.x) < 0.0001 && Math.abs(inP1.y - inP2.y) < 0.0001 ) {
            outCoincidentPoints[1] = true;
            outCoincidentPoints[2] = true;
            point2ToPoint1Coincidence = true;
            pointToPointCoincidence = true;
        }
        if ( Math.abs(inP1.x - inP3.x) < 0.0001 && Math.abs(inP1.y - inP3.y) < 0.0001 ) {
            outCoincidentPoints[1] = true;
            outCoincidentPoints[3] = true;
            pointToPointCoincidence = true;
        }
        vertical1 = false;
        vertical2 = false;
        temp1 = inP1.x - inP0.x;
        temp2 = inP3.x - inP2.x;
        if ( temp1 > -0.00001 && temp1 < 0.00001 ) {
            vertical1 = true;
        }
        if ( temp2 > -0.00001 && temp2 < 0.00001 ) {
            vertical2 = true;
        }
        if ( vertical1 && vertical2 ) {
            if ( Math.abs(inP0.x - inP2.x) < 0.0001 ) {
                if ( (inP3.y - inP0.y) * (inP0.y - inP2.y) > 0 ) {
                    outCoincidentPoints[0] = true;
                }
                if ( (inP1.y - inP2.y) * (inP2.y - inP0.y) > 0 ) {
                    outCoincidentPoints[2] = true;
                }
                if ( (inP3.y - inP1.y) * (inP1.y - inP2.y) > 0 ) {
                    outCoincidentPoints[1] = true;
                }
                if ( (inP1.y - inP3.y) * (inP3.y - inP0.y) > 0 ) {
                    outCoincidentPoints[3] = true;
                }
                if ( pointToPointCoincidence ) {
                    if ( point0ToPoint2Coincidence ) {
                        return true;
                    } else {
                        return (outCoincidentPoints[0] && !point0ToPoint3Coincidence) || (outCoincidentPoints[2] && !point2ToPoint1Coincidence);
                    }
                } else {
                    return outCoincidentPoints[0] || outCoincidentPoints[2];
                }
            } else {
                return point0ToPoint2Coincidence;
            }
        }
        m1 = 0;
        b1 = 0;
        m2 = 0;
        b2 = 0;
        if ( !vertical1 ) {
            m1 = (inP1.y - inP0.y) / (inP1.x - inP0.x);
            b1 = inP0.y - m1 * inP0.x;
        }
        if ( !vertical2 ) {
            m2 = (inP3.y - inP2.y) / (inP3.x - inP2.x);
            b2 = inP2.y - m2 * inP2.x;
        }
        if ( vertical1 ) {
            if ( (inP1.y - inP3.y) * (inP3.y - inP0.y) > 0 && Math.abs(inP0.x - inP3.x) < 0.0001 ) {
                outCoincidentPoints[3] = true;
            }
            if ( (inP1.y - inP2.y) * (inP2.y - inP0.y) > 0 && Math.abs(inP0.x - inP2.x) < 0.0001 ) {
                outCoincidentPoints[2] = true;
                if ( pointToPointCoincidence ) {
                    return point0ToPoint2Coincidence;
                } else {
                    return true;
                }
            }
            outPIntersec.x = inP0.x;
            outPIntersec.y = m2 * outPIntersec.x + b2;
            if ( (inP3.x - outPIntersec.x) * (outPIntersec.x - inP2.x) > 0 ) {
                if ( Math.abs(inP1.y - outPIntersec.y) < 0.0001 ) {
                    outCoincidentPoints[1] = true;
                }
                if ( Math.abs(inP0.y - outPIntersec.y) < 0.0001 ) {
                    outCoincidentPoints[0] = true;
                    if ( pointToPointCoincidence ) {
                        return point0ToPoint2Coincidence;
                    } else {
                        return true;
                    }
                }
                if ( (inP1.y - outPIntersec.y) * (outPIntersec.y - inP0.y) > 0 ) {
                    if ( pointToPointCoincidence ) {
                        return point0ToPoint2Coincidence;
                    } else {
                        return true;
                    }
                }
            }
            return point0ToPoint2Coincidence;
        }
        if ( vertical2 ) {
            if ( (inP3.y - inP1.y) * (inP1.y - inP2.y) > 0 && Math.abs(inP2.x - inP1.x) < 0.0001 ) {
                outCoincidentPoints[1] = true;
            }
            if ( (inP3.y - inP0.y) * (inP0.y - inP2.y) > 0 && Math.abs(inP2.x - inP0.x) < 0.0001 ) {
                outCoincidentPoints[0] = true;
                if ( pointToPointCoincidence ) {
                    return point0ToPoint2Coincidence;
                } else {
                    return true;
                }
            }
            outPIntersec.x = inP2.x;
            outPIntersec.y = m1 * outPIntersec.x + b1;
            if ( (inP1.x - outPIntersec.x) * (outPIntersec.x - inP0.x) > 0 ) {
                if ( Math.abs(inP3.y - outPIntersec.y) < 0.0001 ) {
                    outCoincidentPoints[3] = true;
                }
                if ( Math.abs(inP2.y - outPIntersec.y) < 0.0001 ) {
                    outCoincidentPoints[2] = true;
                    if ( pointToPointCoincidence ) {
                        return point0ToPoint2Coincidence;
                    } else {
                        return true;
                    }
                }
                if ( (inP3.y - outPIntersec.y) * (outPIntersec.y - inP2.y) > 0 ) {
                    if ( pointToPointCoincidence ) {
                        return point0ToPoint2Coincidence;
                    } else {
                        return true;
                    }
                }
            }
            return point0ToPoint2Coincidence;
        }
        if ( Math.abs(m2 - m1) < 0.00001 ) { // The two lines are parallel.
            if ( Math.abs(b1 - b2) < 0.0001 ) {
                if ( (inP3.x - inP0.x) * (inP0.x - inP2.x) >= 0 ) {
                    outCoincidentPoints[0] = true;
                }
                if ( (inP3.x - inP1.x) * (inP1.x - inP2.x) >= 0 ) {
                    outCoincidentPoints[1] = true;
                }
                if ( (inP1.x - inP2.x) * (inP2.x - inP0.x) >= 0 ) {
                    outCoincidentPoints[2] = true;
                }
                if ( (inP1.x - inP3.x) * (inP3.x - inP0.x) >= 0 ) {
                    outCoincidentPoints[3] = true;
                }
                if ( pointToPointCoincidence ) {
                    if ( point0ToPoint2Coincidence ) {
                        return true;
                    } else {
                        return (outCoincidentPoints[0] && !point0ToPoint3Coincidence) || (outCoincidentPoints[2] && !point2ToPoint1Coincidence);
                    }
                } else {
                    return outCoincidentPoints[0] || outCoincidentPoints[2];
                }
            }
            return point0ToPoint2Coincidence;
        }
        // The two lines are not parallel.
        intersec1 = false;
        intersec2 = false;
        if ( m1 < 1 ) {//For precision purposes.
            outPIntersec.x = (b1 - b2) / (m2 - m1);
            outPIntersec.y = m1 * outPIntersec.x + b1;

            if ( (inP1.x - outPIntersec.x) * (outPIntersec.x - inP0.x) >= 0 ) {
                intersec1 = true;
            }
        } else {
            outPIntersec.y = (b1 * m2 - b2 * m1) / (m2 - m1);
            outPIntersec.x = (outPIntersec.y - b1) / m1;
            if ( (inP1.y - outPIntersec.y) * (outPIntersec.y - inP0.y) >= 0 ) {
                intersec1 = true;
            }
        }
        if ( m2 < 1 ) {//For precision purposes.
            if ( (inP3.x - outPIntersec.x) * (outPIntersec.x - inP2.x) >= 0 ) {
                intersec2 = true;
            }
        } else {
            if ( (inP3.y - outPIntersec.y) * (outPIntersec.y - inP2.y) >= 0 ) {
                intersec2 = true;
            }
        }
        if ( intersec1 && intersec2 ) {
            if ( Math.abs(inP0.x - outPIntersec.x) < 0.0001 && Math.abs(inP0.y - outPIntersec.y) < 0.0001 ) {
                outCoincidentPoints[0] = true;
                if ( pointToPointCoincidence ) {
                    return point0ToPoint2Coincidence;
                } else {
                    return true;
                }
            }
            if ( Math.abs(inP1.x - outPIntersec.x) < 0.0001 && Math.abs(inP1.y - outPIntersec.y) < 0.0001 ) {
                outCoincidentPoints[1] = true;
                return point0ToPoint2Coincidence;
            }
            if ( Math.abs(inP2.x - outPIntersec.x) < 0.0001 && Math.abs(inP2.y - outPIntersec.y) < 0.0001 ) {
                outCoincidentPoints[2] = true;
                if ( pointToPointCoincidence ) {
                    return point0ToPoint2Coincidence;
                } else {
                    return true;
                }
            }
            if ( Math.abs(inP3.x - outPIntersec.x) < 0.0001 && Math.abs(inP3.y - outPIntersec.y) < 0.0001 ) {
                outCoincidentPoints[3] = true;
                return point0ToPoint2Coincidence;
            }
            if ( pointToPointCoincidence ) {
                return point0ToPoint2Coincidence;
            } else {
                return true;
            }
        } else {
            return point0ToPoint2Coincidence;
        }
    }

    /**
     * @return internal clip polygon representation after cut vertices have been inserted
     */
    public _Polygon2DWA getClipPolyWA() {
        return clipPolyWA;
    }

    /**
     * @return internal subject polygon representation after cut vertices have been inserted
     */
    public _Polygon2DWA getSubjectPolyWA() {
        return subjectPolyWA;
    }
}
