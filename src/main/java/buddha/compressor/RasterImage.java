package buddha.compressor;

import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.util.Tmp;

public class RasterImage {
    public int width, height;
    public int[] argb;

    public RasterImage(int width, int height) {
        this.width = width;
        this.height = height;
        this.argb = new int[width * height];
    }

    public RasterImage(Fi file) {
        var pixmap = new Pixmap(file);

        this.width = pixmap.width;
        this.height = pixmap.height;

        this.argb = new int[width * height];
        pixmap.each((x, y) -> set(x, y, Tmp.c1.set(pixmap.get(x, y)).argb8888()));
    }

    public int get(int x, int y) {
        return argb[x + y * width];
    }

    public void set(int x, int y, int argb) {
        this.argb[x + y * width] = argb;
    }

    public Pixmap toPixmap() {
        var pixmap = new Pixmap(width, height);
        pixmap.each((x, y) -> pixmap.set(x, y, Tmp.c1.argb8888(get(x, y)).rgba8888()));

        return pixmap;
    }
}