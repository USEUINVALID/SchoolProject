package buddha.compressor;

import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import com.github.bsideup.jabel.Desugar;

// Реализация фрактального сжатия на основе похожего цвета
public class FractalCompressor extends Compressor {
    public static int rangeBlocksSize = 4;
    public static int rangeBlocksPerDomain = 2;

    public FractalCompressor() {
        super("Fractal");
    }

    public Pixmap compress(Pixmap pixmap) {
        var rangeBlocks = new Seq<Block>();

        // Создаем ранговые блоки
        for (int y = 0; y < pixmap.height; y += rangeBlocksSize) {
            for (int x = 0; x < pixmap.width; x += rangeBlocksSize) {
                var colors = new int[rangeBlocksSize * rangeBlocksSize];

                // Заполняем ранговый блок
                for (int cx = 0; cx < rangeBlocksSize; cx++)
                    for (int cy = 0; cy < rangeBlocksSize; cy++)
                        colors[cx + cy * rangeBlocksSize] = pixmap.get(x + cx, y + cy);

                // Создаем ранговый блок
                rangeBlocks.add(new Block(colors));
            }
        }

        var compressed = new Pixmap(pixmap.width, pixmap.height);

        // Вычисляем размер доменного блока и общее количество блоков
        int domainBlocksSize = rangeBlocksSize * rangeBlocksPerDomain;
        start((float) (pixmap.width * pixmap.height) / (domainBlocksSize * domainBlocksSize));

        // Создаем доменные блоки и заменяем их ранговыми
        for (int y = 0; y < pixmap.height; y += domainBlocksSize) {
            for (int x = 0; x < pixmap.width; x += domainBlocksSize) {
                var colors = new int[domainBlocksSize * domainBlocksSize];

                // Заполняем доменный блок
                for (int cx = 0; cx < domainBlocksSize; cx++)
                    for (int cy = 0; cy < domainBlocksSize; cy++)
                        colors[cx + cy * domainBlocksSize] = pixmap.get(x + cx, y + cy);

                // Создаем доменный блок
                var domainBlock = new Block(colors);

                // Находим ранговый блок, максимально близкий к доменному
                var rangeBlock = rangeBlocks.min(block -> getDifference(domainBlock.average, block.average));

                // Заменяем доменный блок ранговым
                for (int cx = 0; cx < rangeBlocksSize; cx++) {
                    for (int cy = 0; cy < rangeBlocksSize; cy++) {
                        int color = rangeBlock.colors[cx + cy * rangeBlocksSize];
                        for (int sx = 0; sx < rangeBlocksPerDomain; sx++)
                            for (int sy = 0; sy < rangeBlocksPerDomain; sy++)
                                compressed.set(x + cx * rangeBlocksPerDomain + sx, y + cy * rangeBlocksPerDomain + sy, color);
                    }
                }

                current++;
            }
        }

        end();
        return compressed;
    }

    @Override
    public void build(Table table) {
        // Добавляем слайдер для размера рангового блока
        table.label(() -> "Размер рангового блока: [yellow]" + rangeBlocksSize).labelAlign(Align.center).left().row();
        table.slider(2, 64, 1, rangeBlocksSize, value -> rangeBlocksSize = (int) value).disabled(slider -> compressing).padTop(24f).width(240f).align(Align.left);

        // Переходим на новый ряд
        table.row();

        // Добавляем слайдер для количества ранговых блоков в доменном
        table.label(() -> "Ранговых блоков в доменном: [yellow]" + rangeBlocksPerDomain).labelAlign(Align.center).padTop(48f).left().row();
        table.slider(2, 16, 1, rangeBlocksPerDomain, value -> rangeBlocksPerDomain = (int) value).disabled(slider -> compressing).padTop(24f).width(240f).align(Align.left);
    }

    // Вычисляет разницу между двумя RGB цветами
    private static float getDifference(int color1, int color2) {
        int red1 = (color1 >> 24) & 0xFF;
        int green1 = (color1 >> 16) & 0xFF;
        int blue1 = (color1 >> 8) & 0xFF;

        int red2 = (color2 >> 24) & 0xFF;
        int green2 = (color2 >> 16) & 0xFF;
        int blue2 = (color2 >> 8) & 0xFF;

        return Mathf.sqrt((red1 - red2) * (red1 - red2) + (green1 - green2) * (green1 - green2) + (blue1 - blue2) * (blue1 - blue2)) / (255f * 2f);
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

    // Хранит в себе массив цветов и средний цвет для них
    @Desugar
    public record Block(int[] colors, int average) {
        public Block(int[] colors) {
            this(colors, getAverageColor(colors));
        }
    }
}