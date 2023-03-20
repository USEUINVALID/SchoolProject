package buddha.compressor;

import arc.graphics.Pixmap;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.util.Align;

public class AverageColorCompressor extends Compressor {

    public int blockSize = 16;

    public boolean compressing;
    public float total;
    public float current;

    public Pixmap compress(Pixmap pixmap) {
        var compressed = new Pixmap(pixmap.width, pixmap.height);

        compressing = true;
        total = (float) (pixmap.width * pixmap.height) / (blockSize * blockSize);
        current = 0f;

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

        compressing = false;
        total = 0f;
        current = 0f;

        return compressed;
    }

    @Override
    public boolean compressing() {
        return compressing;
    }

    @Override
    public float progress() {
        return compressing ? current / total * 100f : 0f;
    }

    @Override
    public Table build(Table table) {
        table.bottom();

        table.label(() -> "Размер блока: [yellow]" + blockSize).labelAlign(Align.center).touchable(Touchable.disabled).labelAlign(Align.left).left().row();
        table.slider(1, 128, 1, blockSize, value -> blockSize = (int) value).padTop(24f).width(240f).align(Align.left);

        table.row();

        table.label(() -> "Уровень сжатия: [yellow]ничего не делает").labelAlign(Align.center).touchable(Touchable.disabled).padTop(32f).labelAlign(Align.left).left().row();
        table.slider(2, 16, 1, 0, value -> {
        }).padTop(24f).width(240f).align(Align.left);

        return table;
    }

    private static int getAverageColor(int[] block) {
        int r = 0, g = 0, b = 0, a = 0;
        for (int color : block) {
            r += (color >> 24) & 0xFF;
            g += (color >> 16) & 0xFF;
            b += (color >> 8) & 0xFF;
            a += color & 0xFF;
        }

        return ((r / block.length) << 24) | ((g / block.length) << 16) | ((b / block.length) << 8) | (a / block.length);
    }
}