package buddha.compressor;

import arc.graphics.Pixmap;
import arc.scene.ui.layout.Table;
import arc.util.Align;

public class AverageColorCompressor extends Compressor {
    public int blockSize = 16;

    public AverageColorCompressor() {
        super("Average");
    }

    public Pixmap compress(Pixmap pixmap) {
        var compressed = new Pixmap(pixmap.width, pixmap.height);
        start((float) (pixmap.width * pixmap.height) / (blockSize * blockSize));

        for (int x = 0; x < pixmap.width; x += blockSize) {
            for (int y = 0; y < pixmap.height; y += blockSize) {
                var block = new int[blockSize * blockSize];

                int index = 0;
                for (int xx = x; xx < x + blockSize; xx++)
                    for (int yy = y; yy < y + blockSize; yy++)
                        block[index++] = pixmap.get(xx, yy);

                int averageColor = getAverageColor(block);
                for (int xx = x; xx < x + blockSize; xx++)
                    for (int yy = y; yy < y + blockSize; yy++)
                        compressed.set(xx, yy, averageColor);

                current++;
            }
        }

        end();
        return compressed;
    }

    @Override
    public void build(Table table) {
        table.label(() -> "Размер блока: [yellow]" + blockSize).labelAlign(Align.center).left().row();
        table.slider(2, 64, 1, blockSize, value -> blockSize = (int) value).padTop(24f).width(240f).align(Align.left);
    }

    private static int getAverageColor(int[] colors) {
        int red = 0, green = 0, blue = 0, alpha = 0;
        for (int color : colors) {
            red += (color >> 24) & 0xFF;
            green += (color >> 16) & 0xFF;
            blue += (color >> 8) & 0xFF;
            alpha += color & 0xFF;
        }

        return ((red / colors.length) << 24) | ((green / colors.length) << 16) | ((blue / colors.length) << 8) | (alpha / colors.length);
    }
}