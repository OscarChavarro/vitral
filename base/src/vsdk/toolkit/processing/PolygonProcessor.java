package vsdk.toolkit.processing;

import java.util.Stack;
import vsdk.toolkit.common.Vertex2D;
import vsdk.toolkit.environment.geometry.Polygon2D;
import vsdk.toolkit.environment.geometry._Polygon2DContour;

//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - July 14 2014 - Leonardo Rebolledo: Original base version              =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [RAMER1972] Urs Ramer, "An iterative procedure for the polygonal        =
//= approximation of plane curves", Computer Graphics and Image Processing, =
//= 1(3), 244–256 (1972).                                                   =
//= [DOPEU1973] David Douglas & Thomas Peucker, "Algorithms for the         =
//= reduction of the number of points required to represent a digitized     =
//= line or its caricature", The Canadian Cartographer 10(2), 112–122 (1973)=
//===========================================================================



/**
 * This class implements the Ramer–Douglas–Peucker algorithm in a non recursive
 * way, to simplify polygons.
 */
public class PolygonProcessor extends ProcessingElement {
    /**
     * Given a Polygon2D object, return a simplified version of that set of polygons
     * using the Ramer–Douglas–Peucker algorithm.
     * @param pol2DIn array of polygons.
     * @param epsilon Umbral distance, used to leave or discard points.
     * @param copy the points of the contours are copied or referenced?
     * @return array of simplified polygons.
     */
    public static Polygon2D polygon2DSimplify(Polygon2D pol2DIn, float epsilon, boolean copy){
        Polygon2D pol2DSimp = new Polygon2D();
        int i;
        
        pol2DSimp.loops.clear();
//        for(_Polygon2DContour p2DContour : pol2DIn.loops) {
        for(i=0; i < pol2DIn.loops.size(); ++i) {
            _Polygon2DContour p2DContour = pol2DIn.loops.get(i);
            pol2DSimp.loops.add(polygon2DContourSimplify(p2DContour,epsilon,true));
        }
        return pol2DSimp;
    }
    
    /**
     * Given a polygon2DContour object, return a simplified version of that polygon
     * using the Ramer–Douglas–Peucker algorithm in non recursive fashion.
     * @param p2DContour   Single polygon.
     * @param epsilon Umbral distance, used to leave or discard points.
     * @param copy the points of the contour are copied or referenced?
     */
    private static _Polygon2DContour  polygon2DContourSimplify(_Polygon2DContour p2DContour, float epsilon, boolean copy){
        _Polygon2DContour p2DContourSimp = new _Polygon2DContour();
        Vertex2D point;
        /** In missingNodes we put two vertex at a time */
        Stack<Integer> indsStkMissingNodes = new Stack<Integer>();
        int ind0,ind1,indFar;
        //float epsilon = 0.01f;///
        float[] dist = new float[1];
        //LinkedList<Vertex2D> P2DContourSimp = new LinkedList();
        /** The default initialization in java of indContourSimp is used
          * (filled with zeros)*/
        int[] ContourSimpFlags;
        int numVertex,i;
        
        
        numVertex = p2DContour.vertices.size();
        if(numVertex < 3) { //One line or one point.
            if(copy)
                for(i=0;i<numVertex;++i) {
                        point = p2DContour.vertices.get(i);
                        point = new Vertex2D(point.x, point.y, point.color.r, point.color.g, point.color.b);
                        p2DContourSimp.vertices.add(point);
                }
            else
                for(i=0;i<numVertex;++i) {
                        point = p2DContour.vertices.get(i);
                        p2DContourSimp.vertices.add(point);
                }
            return p2DContourSimp;
        }
        ContourSimpFlags = new int[numVertex];
        indsStkMissingNodes.push(numVertex-1); //The last one.
        indsStkMissingNodes.push(0); //The fist one.
        while(!indsStkMissingNodes.isEmpty()) {
            ind0 = indsStkMissingNodes.pop();
            ind1 = indsStkMissingNodes.pop();
            ContourSimpFlags[ind0] = 1;
            ContourSimpFlags[ind1] = 1;
            if((ind1-ind0)>1) {
                indFar = getFarthestNodeToLine(p2DContour,ind0,ind1,dist);
                if(dist[0]>epsilon) {
                    indsStkMissingNodes.push(indFar);
                    indsStkMissingNodes.push(ind0);
                    indsStkMissingNodes.push(ind1);
                    indsStkMissingNodes.push(indFar);
                }
            }
        }
        if(copy)
            for(i=0;i<numVertex;++i) {
                if(ContourSimpFlags[i]==1) {
                    point = p2DContour.vertices.get(i);
                    point = new Vertex2D(point.x, point.y, point.color.r, point.color.g, point.color.b);
                    p2DContourSimp.vertices.add(point);
                }
            }
        else
            for(i=0;i<numVertex;++i) {
                if(ContourSimpFlags[i]==1) {
                    point = p2DContour.vertices.get(i);
                    p2DContourSimp.vertices.add(point);
                }
            }
        return p2DContourSimp;
    }
    /**
     * Return the index of the farthest point to the line formed by the point at index ind0
     * and the point at index ind1 in the p2DContour; the distance is also returned in outDist[0].
     * @param p2DContour   Polygon.
     * @param ind0 Index 0 of the line.
     * @param ind1 Index 1 of the line.
     * @param outDist Array of one element: distance to the farthest point.
     */
    private static int getFarthestNodeToLine(_Polygon2DContour p2DContour, int ind0, int ind1, float[] outDist) {
        int i;
        float xInd0,yInd0,nx,ny,m,temp;
        float[] v = new float[2];
        float dist, maxDist;
        int indFar;
        
        //Find the unitary normal vector to the line.
        xInd0 = (float)p2DContour.vertices.get(ind0).x;
        yInd0 = (float)p2DContour.vertices.get(ind0).y;
        temp = (float)(p2DContour.vertices.get(ind1).y - yInd0);
        if(temp<0.00001 && temp>-0.00001) {
            nx=0;
            ny=1;
        }
        else {
            //m = Slope of the normal to the line.
            m =  -(float)((p2DContour.vertices.get(ind1).x - xInd0)
                         /temp);
            temp=(float)Math.sqrt(1+m*m);
            nx=1/temp;
            ny=m/temp;
        }
        //Find the farthest point and its distance.
        maxDist=0;
        indFar=ind0+1;
        for(i=ind0+1; i<ind1; ++i) {
            v[0] = (float)(p2DContour.vertices.get(i).x - xInd0);
            v[1] = (float)(p2DContour.vertices.get(i).y - yInd0);
            //Dot product.
            dist = Math.abs(v[0]*nx + v[1]*ny);
            if(dist>maxDist) {
                maxDist=dist;
                indFar=i;
            }
        }
        outDist[0]=maxDist;
        return indFar;
    }    
}
