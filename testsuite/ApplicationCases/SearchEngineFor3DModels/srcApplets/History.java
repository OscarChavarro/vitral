public class History
{
    private StringBuffer history;
    
    public History() {
        clear();
    }
    
    public void add_command(String string, MyTimer mytimer) {
        this.history.append("\n" + string + " " + mytimer.elapsed() + " ");
    }
    
    public void add_coords(int i, int i_0_) {
        this.history
            .append(Integer.toString(i) + " " + Integer.toString(i_0_) + " ");
    }
    
    public void clear() {
        this.history = new StringBuffer("");
    }
    
    public String get_text() {
        return this.history.toString();
    }
}
