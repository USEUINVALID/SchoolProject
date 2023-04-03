package buddha.compressor;

import arc.graphics.Pixmap;
import arc.scene.ui.layout.Table;

public abstract class Compressor {
    public final String name;

    public boolean compressing;
    public float current, total;

    public Compressor(String name) {
        this.name = name;
    }

    public abstract Pixmap compress(Pixmap pixmap);

    public void start(float total) {
        this.compressing = true;
        this.current = 0f;
        this.total = total;
    }

    public float progress() {
        return compressing ? current / total * 100f : 0f;
    }

    public void end() {
        this.compressing = false;
        this.current = 0f;
        this.total = 0f;
    }

    public abstract void build(Table table);
}