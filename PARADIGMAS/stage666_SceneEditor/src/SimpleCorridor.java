import javax.media.opengl.GL;

public class SimpleCorridor
{
    private double a;
    private int na;
    private double b;
    private int nb;
    private double c;
    private int nc;
    private double interSpace;

    public SimpleCorridor()
    {
        a = 6;
        na = 6;
        b = 20;
        nb = 20;
        c = 4;
        nc = 4;
        interSpace = 0.05;
    }

    private void drawTilesCenter(GL gl)
    {
        double da;
        double x, y;
        double EPSILON = 0.005;
        int i;
        int j;

        da = a / ((double)na);

        gl.glNormal3d(0, 0, 1);
        gl.glBegin(gl.GL_QUADS);
        for ( x = -a/2, i = 0; i < na; i++, x += da ) {
            for ( y = -a/2, j = 0; j < na; j++, y += da ) {
                gl.glVertex3d(x+interSpace/2, y+interSpace/2, -EPSILON);
                gl.glVertex3d(x+da-interSpace/2, y+interSpace/2, -EPSILON);
                gl.glVertex3d(x+da-interSpace/2, y+da-interSpace/2, -EPSILON);
                gl.glVertex3d(x+interSpace/2, y+da-interSpace/2, -EPSILON);
            }
        }
        gl.glEnd();
    }

    private void drawTilesLong(GL gl)
    {
        double da;
        double db;
        double x, y;
        double EPSILON = 0.001;
        int i;
        int j;

        da = a / ((double)na);
        db = b / ((double)nb);

        gl.glNormal3d(0, 0, 1);
        gl.glBegin(gl.GL_QUADS);
        for ( x = -a/2 - b, i = 0; i < nb; i++, x += db ) {
            for ( y = -a/2, j = 0; j < na; j++, y += da ) {
                gl.glVertex3d(x+interSpace/2, y+interSpace/2, -EPSILON);
                gl.glVertex3d(x+da-interSpace/2, y+interSpace/2, -EPSILON);
                gl.glVertex3d(x+da-interSpace/2, y+da-interSpace/2, -EPSILON);
                gl.glVertex3d(x+interSpace/2, y+da-interSpace/2, -EPSILON);
            }
        }
        gl.glEnd();
    }

    private void drawTilesWallA(GL gl)
    {
        double y, z, da, dc;
        int i, j;

        da = a / ((double)na);
        dc = c / ((double)nc);

        gl.glNormal3d(1, 0, 0);
        gl.glBegin(gl.GL_QUADS);
        for ( z = 0, i = 0; i < nc; i++, z += dc ) {
            for ( y = -a/2, j = 0; j < na; j++, y += da ) {
                gl.glVertex3d(-a/2-b, y+interSpace/2, z+dc-interSpace/2);
                gl.glVertex3d(-a/2-b, y+interSpace/2, z+interSpace/2);
                gl.glVertex3d(-a/2-b, y+da-interSpace/2, z+interSpace/2);
                gl.glVertex3d(-a/2-b, y+da-interSpace/2, z+dc-interSpace/2);
            }
        }
        gl.glEnd();
    }

    private void drawTilesWallB(GL gl)
    {
        double y, z, db, dc;
        int i, j;

        db = b / ((double)nb);
        dc = c / ((double)nc);

        gl.glNormal3d(1, 0, 0);
        gl.glBegin(gl.GL_QUADS);
        for ( z = 0, i = 0; i < nc; i++, z += dc ) {
            for ( y = a/2, j = 0; j < nb; j++, y += db ) {
                gl.glVertex3d(-a/2, y+interSpace/2, z+dc-interSpace/2);
                gl.glVertex3d(-a/2, y+interSpace/2, z+interSpace/2);
                gl.glVertex3d(-a/2, y+db-interSpace/2, z+interSpace/2);
                gl.glVertex3d(-a/2, y+db-interSpace/2, z+dc-interSpace/2);
            }
        }
        gl.glEnd();
    }

    private void drawTilesWallC(GL gl)
    {
        double x, z, db, dc;
        int i, j;

        db = b / ((double)nb);
        dc = c / ((double)nc);

        gl.glNormal3d(0, -1, 0);
        gl.glBegin(gl.GL_QUADS);
        for ( x = -a/2-b, i = 0; i < nb; i++, x += db ) {
            for ( z = 0, j = 0; j < nc; j++, z += dc ) {
                gl.glVertex3d(x+interSpace/2, a/2, z+interSpace/2);
                gl.glVertex3d(x+db-interSpace/2, a/2, z+interSpace/2);
                gl.glVertex3d(x+db-interSpace/2, a/2, z+dc-interSpace/2);
                gl.glVertex3d(x+interSpace/2, a/2, z+dc-interSpace/2);
            }
        }
        gl.glEnd();
    }

    public void drawGL(GL gl)
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
        gl.glTranslated(0, 0, c);
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
