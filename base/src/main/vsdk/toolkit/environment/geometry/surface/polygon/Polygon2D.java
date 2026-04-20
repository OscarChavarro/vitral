package vsdk.toolkit.environment.geometry.surface.polygon;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vsdk.toolkit.common.Vertex2D;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.dataStructures.BinaryTreeNode;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.geometry.surface.Surface;
import vsdk.toolkit.processing.polygonClipper.PolygonProcessor;

public class Polygon2D extends Surface
{
    @Serial
    private static final long serialVersionUID = 20260419L;

    public List<_Polygon2DContour> loops;
    private _Polygon2DContour currentLoop;
    // headNode is the head node of the n-ary tree codified in a binary tree
    // in left child-right sibling way.
    private BinaryTreeNode<_Polygon2DContour> headNode;
    
    public Polygon2D()
    {
        loops = new ArrayList<_Polygon2DContour>();
        nextLoop();
    }

    public void addVertex(double x, double y, double r, double g, double b)
    {
        currentLoop.addVertex(x, y, r, g, b);
    }

    public void addVertex(double x, double y)
    {
        currentLoop.addVertex(x, y);
    }

    public void pushVertex(double x, double y)
    {
        currentLoop.pushVertex(x, y);
    }

    public final void nextLoop()
    {
        currentLoop = new _Polygon2DContour();
        loops.add(currentLoop);
    }

    public final void eraseLastLoop()
    {
        int i;
        _Polygon2DContour loop;
        
        if(loops.size() > 1) {
            // Remove also the holes of that loop. LRR.
            for(i=0; i<loops.size(); ++i) {
                loop = loops.get(i);
                if(loop.getExteriorContour() == loops.get(loops.size()-1)) {
                    loops.remove(i);
                    --i;
                }
            }
            if(loops.size() > 1) {
                loops.remove(loops.size()-1);
                currentLoop = loops.get(loops.size()-1);
            }
            // Rebuild the n-ary tree codified in a binary tree.
            PolygonProcessor.classifyContourHoles(this);
        }
    }
    
    public void invert()
    {
        for(_Polygon2DContour p2DContour : loops) {
            Collections.reverse(p2DContour.vertices);
        }
    }
    
    /**
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    @Override
    public double[] getMinMax()
    {
        double minMax[];
        _Polygon2DContour l;
        Vertex2D v;
        int i;
        int j;
        minMax = new double[6];

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for ( i = 0; i < loops.size(); i++ ) {
            for ( j = 0; j < loops.get(i).vertices.size() - 1; j++ ) {
                v = loops.get(i).vertices.get(j);
                if ( v.x > maxX ) {
                    maxX = v.x;
                }
                if ( v.x < minX ) {
                    minX = v.x;
                }
                if ( v.y > maxY ) {
                    maxY = v.y;
                }
                if ( v.y < minY ) {
                    minY = v.y;
                }
            }
        }

        minMax[0] = minX;
        minMax[1] = minY;
        minMax[2] = 0;
        minMax[3] = maxX;
        minMax[4] = maxY;
        minMax[5] = 0;

        return minMax;
    }

    public Ray doIntersection(Ray inOut_ray)
    {
        return null;
    }

    @Override
    public boolean doIntersection(Ray inRay, RayHit outHit)
    {
        return false;
    }
    public void
    doExtraInformation(Ray inRay, double intT, 
                       RayHit outData)
    {

    }
    
    /**
     * @return the headNode
     */
    public BinaryTreeNode<_Polygon2DContour> getHeadNode() {
        return headNode;
    }

    /**
     * @param headNode the headNode to set
     */
    public void setHeadNode(BinaryTreeNode<_Polygon2DContour> headNode) {
        this.headNode = headNode;
    }
}
