package vsdk.toolkit.render.jogl;

// Java basic classes
import com.jogamp.opengl.GL2;

// VitralSDK classes
import vsdk.toolkit.environment.geometry.Md2Mesh;

/**
 *
 * @author 
 */
public class JoglMd2MeshRenderer extends JoglRenderer {
    //static int numFrame;
    //static float t=0; // Fraction of a frame.
    
    public static void initGL(GL2 gl, Md2Mesh md2Mesh){
//        int[] tex = new int[1];
//        
//        gl.glGenTextures(1, tex,1);
//        gl.glBindTexture(gl.GL_TEXTURE_2D, tex[0]);
        
//        JoglImageRenderer.activate(gl, md2Mesh.skins[0]);
    }
    
    public static void draw(GL2 gl, Md2Mesh md2Mesh){
        int numFrame;
        int i,j, numFrameNext;
        float[] frameVerts,frameVertsNext;
        float[] glCmdTexCoords;
        short[] frameNormalInds,frameNormalIndsNext;
        int vertInd;
        float[] normal;
        float[] normalN;
        float[][] normals;
        float frameTimeSeg;
        float elapsedTimeSeg;
        boolean animLoop;
        float t;
        short[] animStartEnd = new short[2];
        
//        try {
//            Thread.sleep(1000/24);
//        } catch ( InterruptedException ex ) {
//            Logger.getLogger(JoglMd2MeshRenderer.class.getName()).log(Level.SEVERE, null, ex);
//        }
        
        normals = Md2Mesh.anorms;
        md2Mesh.returnStartEndAnim(md2Mesh.getCurrentAnimationInd(), animStartEnd);
        frameTimeSeg = md2Mesh.getFrameTimeSeg();
        elapsedTimeSeg = md2Mesh.getElapsedTimeSeg();
        t = elapsedTimeSeg/frameTimeSeg;
//t = t+frameTimeSeg;
        numFrame = ((int)t % (animStartEnd[1]-animStartEnd[0]+1)) + animStartEnd[0];
        t = t-(int)t;
        // We assume, for this animation, that the last frame connects with the first,
        // note that this is not always the case(ej. death animation).
        animLoop = true;
        if(numFrame == animStartEnd[1])
            if(animLoop)
                numFrameNext = animStartEnd[0];
            else
                numFrameNext = numFrame;
        else
            numFrameNext = numFrame + 1;
        frameVerts          = md2Mesh.frameVertices.get(numFrame);
        frameNormalInds     = md2Mesh.frameNormalIndices.get(numFrame);
        frameVertsNext      = md2Mesh.frameVertices.get(numFrameNext);
        frameNormalIndsNext = md2Mesh.frameNormalIndices.get(numFrameNext);
        JoglImageRenderer.activate(gl, md2Mesh.skins[0]);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        if(!md2Mesh.glCmdVertIndexStrip.isEmpty() || !md2Mesh.glCmdVertIndexFan.isEmpty()) {
//        if(false) {
    //        for(int[] vertIndexStrip : md2Mesh.glCmdVertIndexStrip) {
            for(i=0; i < md2Mesh.glCmdVertIndexStrip.size(); ++i) {
                int[] vertIndexStrip = md2Mesh.glCmdVertIndexStrip.get(i);
                glCmdTexCoords = md2Mesh.glCmdTexCoordsStrip.get(i);
                gl.glBegin(GL2.GL_TRIANGLE_STRIP);
                    for(j=0;j<vertIndexStrip.length;++j) {
                        normal = normals[frameNormalInds[vertIndexStrip[j]]];
                        normalN = normals[frameNormalIndsNext[vertIndexStrip[j]]];
                        gl.glNormal3d(normal[0] + t*(normalN[0]-normal[0])
                                     ,normal[1] + t*(normalN[1]-normal[1])
                                     ,normal[2] + t*(normalN[2]-normal[2]));
                        //Apparently the image has the origin in the top left, not on the bottom left as in OpenGL.
                        gl.glTexCoord2d(glCmdTexCoords[j*2], 1-glCmdTexCoords[j*2+1]);
                        vertInd = vertIndexStrip[j]*3;//In frameVerts, a one-dimensional array, are the 3 vertex coords.
                        gl.glVertex3d(frameVerts[vertInd] + t*(frameVertsNext[vertInd]-frameVerts[vertInd])
                                     ,frameVerts[vertInd+1] + t*(frameVertsNext[vertInd+1]-frameVerts[vertInd+1])
                                     ,frameVerts[vertInd+2] + t*(frameVertsNext[vertInd+2]-frameVerts[vertInd+2]));
                    }
                gl.glEnd();
            }
    //        for(int[] vertIndexFan : md2Mesh.glCmdVertIndexFan) {
            for(i=0; i < md2Mesh.glCmdVertIndexFan.size(); ++i) {
                int[] vertIndexFan = md2Mesh.glCmdVertIndexFan.get(i);
                glCmdTexCoords = md2Mesh.glCmdTexCoordsFan.get(i);
                gl.glBegin(GL2.GL_TRIANGLE_FAN);
                    for(j=0;j<vertIndexFan.length;++j) {
                        normal = normals[frameNormalInds[vertIndexFan[j]]];
                        normalN = normals[frameNormalIndsNext[vertIndexFan[j]]];
                        gl.glNormal3d(normal[0] + t*(normalN[0]-normal[0])
                                     ,normal[1] + t*(normalN[1]-normal[1])
                                     ,normal[2] + t*(normalN[2]-normal[2]));
                        gl.glTexCoord2d(glCmdTexCoords[j*2], 1-glCmdTexCoords[j*2+1]);
                        vertInd = vertIndexFan[j]*3;
                        gl.glVertex3d(frameVerts[vertInd] + t*(frameVertsNext[vertInd]-frameVerts[vertInd])
                                     ,frameVerts[vertInd+1] + t*(frameVertsNext[vertInd+1]-frameVerts[vertInd+1])
                                     ,frameVerts[vertInd+2] + t*(frameVertsNext[vertInd+2]-frameVerts[vertInd+2]));
                    }
                gl.glEnd();
            }
        } else {
            // Each triangle has three vertex indices and three tex. coord. indices.
            gl.glBegin(GL2.GL_TRIANGLES);
                for(i=0;i<md2Mesh.numTriangles;++i){
                    for(j=0;j<3;++j){
                        normal = normals[frameNormalInds[md2Mesh.triangles[i*2][j]]];
                        normalN = normals[frameNormalIndsNext[md2Mesh.triangles[i*2][j]]];
                        gl.glNormal3d(normal[0] + t*(normalN[0]-normal[0])
                                     ,normal[1] + t*(normalN[1]-normal[1])
                                     ,normal[2] + t*(normalN[2]-normal[2]));
                        gl.glTexCoord2d(md2Mesh.texCoords[md2Mesh.triangles[i*2+1][j]*2]
                                      , 1 - md2Mesh.texCoords[md2Mesh.triangles[i*2+1][j]*2 + 1]);
                        vertInd = md2Mesh.triangles[i*2][j]*3;//In frameVerts, a one-dimensional array, are the 3 vertex coords.
                        gl.glVertex3d(frameVerts[vertInd] + t*(frameVertsNext[vertInd]-frameVerts[vertInd])
                                     ,frameVerts[vertInd+1] + t*(frameVertsNext[vertInd+1]-frameVerts[vertInd+1])
                                     ,frameVerts[vertInd+2] + t*(frameVertsNext[vertInd+2]-frameVerts[vertInd+2]));
                    }
                }
            gl.glEnd();
        }
    }
}