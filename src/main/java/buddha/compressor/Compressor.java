package buddha.compressor;

import arc.graphics.Pixmap;
import arc.scene.ui.layout.Table;

public abstract class Compressor {

    public abstract Pixmap compress(Pixmap pixmap);

    public abstract boolean compressing();
    public abstract float progress();

    public abstract Table build(Table table);
}