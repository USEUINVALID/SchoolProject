package buddha.compressor;

import arc.graphics.Pixmap;
import arc.scene.ui.layout.Table;
import arc.util.Align;

// Реализация сжатия на основе похожего цвета
public class AverageColorCompressor extends Compressor {
    public int blockSize = 8;

    public AverageColorCompressor() {
        super("Average");
    }

    public Pixmap compress(Pixmap pixmap) {
        var compressed = new Pixmap(pixmap.width, pixmap.height);

        // Вычисляем общее количество блоков
        start((float) (pixmap.width * pixmap.height) / (blockSize * blockSize));

        // Создаем блоки и заменяем их средним цветом
        for (int x = 0; x < pixmap.width; x += blockSize) {
            for (int y = 0; y < pixmap.height; y += blockSize) {
                var colors = new int[blockSize * blockSize];

                // Заполняем блок
                for (int cx = 0; cx < blockSize; cx++)
                    for (int cy = 0; cy < blockSize; cy++)
                        colors[cx + cy * blockSize] = pixmap.get(x + cx, y + cy);

                // Вычисляем средний цвет
                int averageColor = getAverageColor(colors);

                // Заменяем блок средним цветом
                for (int cx = x; cx < x + blockSize; cx++)
                    for (int cy = y; cy < y + blockSize; cy++)
                        compressed.set(cx, cy, averageColor);

                current++;
            }
        }

        end();
        return compressed;
    }

    @Override
    public void build(Table table) {
        // Создаем слайдер для размера блока
        table.label(() -> "Размер блока: [yellow]" + blockSize).labelAlign(Align.center).left().row();
        table.slider(2, 64, 1, blockSize, value -> blockSize = (int) value).padTop(24f).width(240f).align(Align.left);
    }

    // Вычисляет средний цвет для массива цветов
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