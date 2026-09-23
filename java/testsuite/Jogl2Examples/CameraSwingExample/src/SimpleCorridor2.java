import com.jogamp.opengl.GL2;

public class SimpleCorridor2
{
    private double width;
    private int widthTiles;
    private double length;
    private int lengthTiles;
    private double height;
    private int heightTiles;
    private double interSpace;

    public SimpleCorridor2()
    {
        width = 6;
        widthTiles = 6;
        length = 20;
        lengthTiles = 20;
        height = 4;
        heightTiles = 4;
        interSpace = 0.05;
    }

    private void drawTilesCenter(GL2 gl)
    {
        double da;
        double x, y;
        double EPSILON = 0.005;
        int i;
        int j;

        da = width / ((double)widthTiles);

        gl.glNormal3d(0, 0, 1);
        gl.glBegin(gl.GL_QUADS);
        for ( x = -width/2, i = 0; i < widthTiles; i++, x += da ) {
            for ( y = -width/2, j = 0; j < widthTiles; j++, y += da ) {
                gl.glVertex3d(x+interSpace/2, y+interSpace/2, -EPSILON);
                gl.glVertex3d(x+da-interSpace/2, y+interSpace/2, -EPSILON);
                gl.glVertex3d(x+da-interSpace/2, y+da-interSpace/2, -EPSILON);
                gl.glVertex3d(x+interSpace/2, y+da-interSpace/2, -EPSILON);
            }
        }
        gl.glEnd();
    }

    private void drawTilesLong(GL2 gl)
    {
        double da;
        double db;
        double x, y;
        double EPSILON = 0.001;
        int i;
        int j;

        da = width / ((double)widthTiles);
        db = length / ((double)lengthTiles);

        gl.glNormal3d(0, 0, 1);
        gl.glBegin(gl.GL_QUADS);
        for ( x = -width/2 - length, i = 0; i < lengthTiles; i++, x += db ) {
            for ( y = -width/2, j = 0; j < widthTiles; j++, y += da ) {
                gl.glVertex3d(x+interSpace/2, y+interSpace/2, -EPSILON);
                gl.glVertex3d(x+da-interSpace/2, y+interSpace/2, -EPSILON);
                gl.glVertex3d(x+da-interSpace/2, y+da-interSpace/2, -EPSILON);
                gl.glVertex3d(x+interSpace/2, y+da-interSpace/2, -EPSILON);
            }
        }
        gl.glEnd();
    }

    private void drawTilesWallA(GL2 gl)
    {
        double y, z, da, dc;
        int i, j;

        da = width / ((double)widthTiles);
        dc = height / ((double)heightTiles);

        gl.glNormal3d(1, 0, 0);
        gl.glBegin(gl.GL_QUADS);
        for ( z = 0, i = 0; i < heightTiles; i++, z += dc ) {
            for ( y = -width/2, j = 0; j < widthTiles; j++, y += da ) {
                gl.glVertex3d(-width/2-length, y+interSpace/2, z+dc-interSpace/2);
                gl.glVertex3d(-width/2-length, y+interSpace/2, z+interSpace/2);
                gl.glVertex3d(-width/2-length, y+da-interSpace/2, z+interSpace/2);
                gl.glVertex3d(-width/2-length, y+da-interSpace/2, z+dc-interSpace/2);
            }
        }
        gl.glEnd();
    }

    private void drawTilesWallB(GL2 gl)
    {
        double y, z, db, dc;
        int i, j;

        db = length / ((double)lengthTiles);
        dc = height / ((double)heightTiles);

        gl.glNormal3d(1, 0, 0);
        gl.glBegin(gl.GL_QUADS);
        for ( z = 0, i = 0; i < heightTiles; i++, z += dc ) {
            for ( y = width/2, j = 0; j < lengthTiles; j++, y += db ) {
                gl.glVertex3d(-width/2, y+interSpace/2, z+dc-interSpace/2);
                gl.glVertex3d(-width/2, y+interSpace/2, z+interSpace/2);
                gl.glVertex3d(-width/2, y+db-interSpace/2, z+interSpace/2);
                gl.glVertex3d(-width/2, y+db-interSpace/2, z+dc-interSpace/2);
            }
        }
        gl.glEnd();
    }

    private void drawTilesWallC(GL2 gl)
    {
        double x, z, db, dc;
        int i, j;

        db = length / ((double)lengthTiles);
        dc = height / ((double)heightTiles);

        gl.glNormal3d(0, -1, 0);
        gl.glBegin(gl.GL_QUADS);
        for ( x = -width/2-length, i = 0; i < lengthTiles; i++, x += db ) {
            for ( z = 0, j = 0; j < heightTiles; j++, z += dc ) {
                gl.glVertex3d(x+interSpace/2, width/2, z+interSpace/2);
                gl.glVertex3d(x+db-interSpace/2, width/2, z+interSpace/2);
                gl.glVertex3d(x+db-interSpace/2, width/2, z+dc-interSpace/2);
                gl.glVertex3d(x+interSpace/2, width/2, z+dc-interSpace/2);
            }
        }
        gl.glEnd();
    }

    public void drawGL(GL2 gl)
    {
        int i;

        // Configure for inside looking
        gl.glEnable(gl.GL_CULL_FACE);
        gl.glCullFace(gl.GL_BACK);
        gl.glDisable(gl.GL_LIGHTING);

        // Build floor
        gl.glColor3d(0.5, 0.5, 0.9);
        drawTilesCenter(gl);
        for ( i = 0; i < 4; i++ ) {
            gl.glPushMatrix();
            gl.glRotated(90*i, 0, 0, 1);
            drawTilesLong(gl);
            gl.glPopMatrix();
        }

        // Build ceiling
        gl.glColor3d(0, 0, 1);
        gl.glPushMatrix();
        gl.glTranslated(0, 0, height);
        gl.glRotated(180, 1, 0, 0);
        drawTilesCenter(gl);
        for ( i = 0; i < 4; i++ ) {
            gl.glPushMatrix();
            gl.glRotated(90*i, 0, 0, 1);
            drawTilesLong(gl);
            gl.glPopMatrix();
        }
        gl.glPopMatrix();

        // Build walls
        for ( i = 0; i < 4; i++ ) {
            switch ( i ) {
              case 0: gl.glColor3d(0.9, 0.5, 0.5); break; // -X
              case 1: gl.glColor3d(0.5, 0.9, 0.5); break; // -Y
              case 2: gl.glColor3d(1, 0, 0); break; // X
              case 3: gl.glColor3d(0, 1, 0); break; // Y
            }
            gl.glPushMatrix();
            gl.glRotated(90*i, 0, 0, 1);
            drawTilesWallA(gl);
            gl.glPopMatrix();
        }

        gl.glColor3d(0.9, 0.5, 0.8);
        for ( i = 0; i < 4; i++ ) {
            gl.glPushMatrix();
            gl.glRotated(90*i, 0, 0, 1);
            drawTilesWallB(gl);
            drawTilesWallC(gl);
            gl.glPopMatrix();
        }

    }
}
