package buddha.compressor;

import arc.graphics.Pixmap;
import arc.scene.ui.layout.Table;

public abstract class Compressor {
    public final String name;

    public Compressor(String name) {
        this.name = name;
    }

    public abstract Pixmap compress(Pixmap pixmap);

    public abstract boolean compressing();
    public abstract float progress();

    public abstract void build(Table table);
}