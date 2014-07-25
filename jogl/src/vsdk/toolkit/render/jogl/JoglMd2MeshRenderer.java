/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package vsdk.toolkit.render.jogl;

// Java basic classes
import javax.media.opengl.GL2;

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
        int i, n, numFrameNext;
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
        n = 0;
        for(int[] vertIndexStrip : md2Mesh.glCmdVertIndexStrip) {
            glCmdTexCoords = md2Mesh.glCmdTexCoordsStrip.get(n++);
            gl.glBegin(GL2.GL_TRIANGLE_STRIP);
                for(i=0;i<vertIndexStrip.length;++i) {
                    normal = normals[frameNormalInds[vertIndexStrip[i]]];
                    normalN = normals[frameNormalIndsNext[vertIndexStrip[i]]];
                    gl.glNormal3d(normal[0] + t*(normalN[0]-normal[0])
                                 ,normal[1] + t*(normalN[1]-normal[1])
                                 ,normal[2] + t*(normalN[2]-normal[2]));
                    //Apparently the image has the origin in the top left, not on the bottom left as in OpenGL.
                    gl.glTexCoord2d(glCmdTexCoords[i*2], 1-glCmdTexCoords[i*2+1]);
                    vertInd = vertIndexStrip[i]*3;
                    gl.glVertex3d(frameVerts[vertInd] + t*(frameVertsNext[vertInd]-frameVerts[vertInd])
                                 ,frameVerts[vertInd+1] + t*(frameVertsNext[vertInd+1]-frameVerts[vertInd+1])
                                 ,frameVerts[vertInd+2] + t*(frameVertsNext[vertInd+2]-frameVerts[vertInd+2]));
                }
            gl.glEnd();
        }
        n = 0;
        for(int[] vertIndexFan : md2Mesh.glCmdVertIndexFan) {
            glCmdTexCoords = md2Mesh.glCmdTexCoordsFan.get(n++);
            gl.glBegin(GL2.GL_TRIANGLE_FAN);
                for(i=0;i<vertIndexFan.length;++i) {
                    normal = normals[frameNormalInds[vertIndexFan[i]]];
                    normalN = normals[frameNormalIndsNext[vertIndexFan[i]]];
                    gl.glNormal3d(normal[0] + t*(normalN[0]-normal[0])
                                 ,normal[1] + t*(normalN[1]-normal[1])
                                 ,normal[2] + t*(normalN[2]-normal[2]));
                    gl.glTexCoord2d(glCmdTexCoords[i*2], 1-glCmdTexCoords[i*2+1]);
                    vertInd = vertIndexFan[i]*3;
                    gl.glVertex3d(frameVerts[vertInd] + t*(frameVertsNext[vertInd]-frameVerts[vertInd])
                                 ,frameVerts[vertInd+1] + t*(frameVertsNext[vertInd+1]-frameVerts[vertInd+1])
                                 ,frameVerts[vertInd+2] + t*(frameVertsNext[vertInd+2]-frameVerts[vertInd+2]));
                }
            gl.glEnd();
        }
    }
}
