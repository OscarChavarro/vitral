import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Vector;

public class SketchCanvas extends Canvas
{
    private int id;
    private Sketch parent;
    private History history = new History();
    private static final int NR_HELP_STRINGS = 6;
    private String[] help_strings = new String[6];
    public static final int INACTIVE = 0;
    public static final int ACTIVE = 1;
    public static final int SENDING = 2;
    public static final int RECEIVING = 3;
    public static final int DISABLED = 4;
    private int status = 1;
    private Color background_colour = new Color(224, 224, 192);
    private Color draw_colour = new Color(0, 0, 0);
    private Color outline_colour = new Color(125, 125, 125);
    private Color text_colour = new Color(205, 133, 63);
    private Color gray_colour = new Color(100, 100, 128);
    private Color help_colour = new Color(0, 0, 0);
    private Font text_font = new Font("Helvetica", 0, 14);
    private Font help_font = new Font("Helvetica", 0, 10);
    private Image offscreen;
    private Image single_width_image;
    private int image_width;
    private int image_height;
    private Graphics off_g;
    private Graphics single_g;
    private Graphics this_g;
    private Vector<Integer> points_x;
    private Vector<Integer> points_y;
    private Vector<Integer> start_indices;
    private Vector<Integer> operation;
    private Vector<Integer> prev_points_x;
    private Vector<Integer> prev_points_y;
    private Vector<Integer> prev_start_indices;
    private Vector<Integer> prev_operation;
    private static final int DRAW = 0;
    private static final int ERASE = 1;
    private static final int OVAL_SIZE = 20;

    public SketchCanvas(int id, Sketch sketch) {
        this.setBackground(Color.black);
        this.id = id;
        parent = sketch;
        points_x = new Vector<Integer>(100, 100);
        points_y = new Vector<Integer>(100, 100);
        operation = new Vector<Integer>(50, 25);
        start_indices = new Vector<Integer>(50, 25);
        prev_points_x = new Vector<Integer>(100, 100);
        prev_points_y = new Vector<Integer>(100, 100);
        prev_operation = new Vector<Integer>(50, 25);
        prev_start_indices = new Vector<Integer>(50, 25);
        int i;
        for ( i = 0; i < 6; i++ ) {
            help_strings[i] = null;
        }
    }
    
    public void clear(Graphics graphics) {
        graphics.setColor(background_colour);
        graphics.fillRect(0, 0, image_width, image_height);
    }
    
    public void clear_points() {
        history.add_command("cl", parent.get_timer());
        int i = points_x.size();
        if (i > 1) {
            prev_points_x.removeAllElements();
            prev_points_y.removeAllElements();
            prev_start_indices.removeAllElements();
            prev_operation.removeAllElements();
            for (int i_1_ = 0; i_1_ < i; i_1_++) {
                prev_points_x.addElement(points_x.elementAt(i_1_));
                prev_points_y.addElement(points_y.elementAt(i_1_));
            }
            int i_2_ = start_indices.size();
            for (int i_3_ = 0; i_3_ < i_2_; i_3_++) {
                prev_start_indices.addElement(start_indices.elementAt(i_3_));
                prev_operation.addElement(operation.elementAt(i_3_));
            }
        }
        points_x.removeAllElements();
        points_y.removeAllElements();
        start_indices.removeAllElements();
        operation.removeAllElements();
        paint();
    }
    
    private void draw_outline(Graphics graphics) {
        graphics.setColor(outline_colour);
        for (int i = 1; i <= 2; i++) {
            int i_4_ = i - 1;
            graphics.drawLine(i_4_, i_4_, image_width - i, i_4_);
            graphics.drawLine(image_width - i, i_4_, image_width - i,
                              image_height - i);
            graphics.drawLine(image_width - i, image_height - i, i_4_,
                              image_height - i);
            graphics.drawLine(i_4_, image_height - i, i_4_, i_4_);
        }
    }
    
    public void draw_sketch(Graphics graphics, boolean bool) {
        int i = start_indices.size();
        if ( i != 0 ) {
            int i_5_ = points_x.size();
            graphics.setColor(draw_colour);
            for (int i_6_ = 0; i_6_ < i; i_6_++) {
                int i_7_ = ((Integer) operation.elementAt(i_6_)).intValue();
                if (i_7_ == 0)
                    graphics.setColor(draw_colour);
                else
                    graphics.setColor(background_colour);
                int i_8_
                    = ((Integer) start_indices.elementAt(i_6_)).intValue();
                int i_9_ = i_5_;
                if (i_6_ < i - 1)
                    i_9_ = ((Integer) start_indices.elementAt(i_6_ + 1))
                               .intValue();
                int i_10_ = ((Integer) points_x.elementAt(i_8_)).intValue();
                int i_11_ = ((Integer) points_y.elementAt(i_8_)).intValue();
                for (int i_12_ = i_8_ + 1; i_12_ < i_9_; i_12_++) {
                    int i_13_
                        = ((Integer) points_x.elementAt(i_12_)).intValue();
                    int i_14_
                        = ((Integer) points_y.elementAt(i_12_)).intValue();
                    if (i_7_ == 0) {
                        graphics.drawLine(i_10_, i_11_, i_13_, i_14_);
                        if (bool) {
                            int i_15_ = i_13_ - i_10_;
                            int i_16_ = i_14_ - i_11_;
                            double d;
                            if (i_15_ != 0) {
                                double d_17_ = (double) (i_16_ / i_15_);
                                d = (180.0 * Math.atan(d_17_)
                                     / 3.141592653589793);
                            } else
                                d = 90.0;
                            if (d > 45.0) {
                                graphics.drawLine(i_10_ - 1, i_11_, i_13_ - 1,
                                                  i_14_);
                                graphics.drawLine(i_10_ + 1, i_11_, i_13_ + 1,
                                                  i_14_);
                            } else {
                                graphics.drawLine(i_10_, i_11_ - 1, i_13_,
                                                  i_14_ - 1);
                                graphics.drawLine(i_10_, i_11_ + 1, i_13_,
                                                  i_14_ + 1);
                            }
                        }
                    } else
                        graphics.fillOval(i_13_ - 10, i_14_ - 10, 20, 20);
                    i_10_ = i_13_;
                    i_11_ = i_14_;
                }
            }
        }
    }
    
    public String get_history_text() {
        return history.get_text();
    }
    
    public int get_id() {
        return id;
    }
    
    public Image get_image() {
        return single_width_image;
    }
    
    public int get_status() {
        return status;
    }
    
    public boolean is_empty() {
        int i = points_x.size();
        return i == 0;
    }
    
    public Dimension getMinimumSize() {
        return new Dimension(150, 150);
    }
    
    public boolean mouseDown(Event event, int i, int i_18_) {
        if (status == 4)
            return true;
        parent.notify_canvas_active(id);
        int i_19_ = 0;
        if ((event.modifiers & 0x8) != 0) {
            undo_segment();
            return true;
        }
        if ((event.modifiers & 0x4) != 0)
            i_19_ = 1;
        operation.addElement(new Integer(i_19_));
        if (i_19_ == 0)
            history.add_command("md", parent.get_timer());
        else
            history.add_command("er", parent.get_timer());
        history.add_coords(i, i_18_);
        int i_20_ = points_x.size();
        start_indices.addElement(new Integer(i_20_));
        points_x.addElement(new Integer(i));
        points_y.addElement(new Integer(i_18_));
        paint();
        return true;
    }
    
    public boolean mouseDrag(Event event, int i, int i_21_) {
        if (status == 4)
            return true;
        points_x.addElement(new Integer(i));
        points_y.addElement(new Integer(i_21_));
        history.add_coords(i, i_21_);
        paint();
        return true;
    }
    
    public boolean mouseUp(Event event, int i, int i_22_) {
        if (status == 4)
            return true;
        history.add_command("mu", parent.get_timer());
        return true;
    }
    
    public void paint() {
        Dimension dimension = this.getSize();
        if (offscreen == null || dimension.width != image_width
            || dimension.height != image_height) {
            if (dimension.width < 1 || dimension.height < 1)
                return;
            System.out.println("SketchCanvas(" + id + ") size: "
                               + dimension.width + ", " + dimension.height);
            offscreen = this.createImage(dimension.width, dimension.height);
            single_width_image
                = this.createImage(dimension.width, dimension.height);
            off_g = offscreen.getGraphics();
            single_g = single_width_image.getGraphics();
            image_width = dimension.width;
            image_height = dimension.height;
        }
        clear(off_g);
        clear(single_g);
        if (status != 4) {
            draw_sketch(off_g, true);
            draw_sketch(single_g, false);
        }
        Graphics graphics = this.getGraphics();
        if (status == 2 || status == 3) {
            clear(graphics);
            draw_outline(graphics);
            graphics.setFont(((SketchCanvas) this).text_font);
            if (points_x.size() > 0) {
                graphics.setColor(text_colour);
                graphics.drawString("[Sending image]", 20, image_height / 2);
            } else {
                graphics.setColor(gray_colour);
                graphics.drawString("[Skipping empty image]", 20,
                                    image_height / 2);
            }
        } else {
            if (status == 1)
                draw_outline(off_g);
            if (points_x.size() == 0 || status == 4) {
                off_g.setFont(((SketchCanvas) this).help_font);
                off_g.setColor(help_colour);
                for (int i = 0; i < 6; i++) {
                    if (help_strings[i] != null)
                        off_g.drawString(help_strings[i], 6, 30 + 20 * i);
                }
            }
            graphics.drawImage(offscreen, 0, 0, this);
        }
    }
    
    public void paint(Graphics graphics) {
        paint();
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(150, 150);
    }
    
    public void set_active(boolean bool) {
        if (bool)
            status = 1;
        else
            status = 0;
    }
    
    public void set_help_string(int i, String string) {
        help_strings[i] = string;
    }
    
    public void setStatus(int i) {
        status = i;
        paint();
    }
    
    public void undo_segment() {
        history.add_command("ud", parent.get_timer());
        int i = start_indices.size();
        if (i == 0) {
            int i_23_ = prev_points_x.size();
            if (i_23_ > 1) {
                Vector<Integer> vector = points_x;
                points_x = prev_points_x;
                prev_points_x = vector;
                vector = points_y;
                points_y = prev_points_y;
                prev_points_y = vector;
                vector = start_indices;
                start_indices = prev_start_indices;
                prev_start_indices = vector;
                vector = operation;
                operation = prev_operation;
                prev_operation = vector;
                paint();
            }
        } else {
            int i_24_ = ((Integer) start_indices.elementAt(i - 1)).intValue();
            int i_25_ = points_x.size();
            int i_26_ = i_25_ - i_24_;
            for (int i_27_ = i_25_ - 1; i_27_ >= i_24_; i_27_--) {
                points_x.removeElementAt(i_27_);
                points_y.removeElementAt(i_27_);
            }
            start_indices.removeElementAt(i - 1);
            operation.removeElementAt(i - 1);
            if (i_26_ == 1)
                undo_segment();
            paint();
        }
    }
}

