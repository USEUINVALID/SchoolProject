package buddha.compressor;

import arc.graphics.Pixmap;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import com.github.bsideup.jabel.Desugar;

// Доменные - большие
// Ранговые - маленькие
public class FractalCompressor extends Compressor {
    public static int rangeBlocksSize = 4;
    public static int rangeBlocksPerDomain = 2;

    public FractalCompressor() {
        super("Fractal");
    }

    public Pixmap compress(Pixmap pixmap) {
        var domainBlocks = new Seq<Block>();
        var rangeBlocks = new Seq<Block>();

        int domainBlocksSize = rangeBlocksSize * rangeBlocksPerDomain;

        for (int y = 0; y < pixmap.height; y += domainBlocksSize) {
            for (int x = 0; x < pixmap.width; x += domainBlocksSize) {
                var colors = new int[domainBlocksSize * domainBlocksSize];

                int index = 0;
                for (int yy = y; yy < y + domainBlocksSize; yy++)
                    for (int xx = x; xx < x + domainBlocksSize; xx++)
                        colors[index++] = pixmap.get(xx, yy);

                domainBlocks.add(new Block(colors));
            }
        }

        for (int y = 0; y < pixmap.height; y += rangeBlocksSize) {
            for (int x = 0; x < pixmap.width; x += rangeBlocksSize) {
                var colors = new int[rangeBlocksSize * rangeBlocksSize];

                int index = 0;
                for (int yy = y; yy < y + rangeBlocksSize; yy++)
                    for (int xx = x; xx < x + rangeBlocksSize; xx++)
                        colors[index++] = pixmap.get(xx, yy);

                rangeBlocks.add(new Block(colors));
            }
        }

        start(domainBlocks.size);

        var result = domainBlocks.map(domainBlock -> {
            current++;
            return rangeBlocks.min(rangeBlock -> getDifference(domainBlock.average, rangeBlock.average));
        }).reverse();

        end();

        var compressed = new Pixmap(pixmap.width / rangeBlocksPerDomain, pixmap.height / rangeBlocksPerDomain);
        for (int y = 0; y < compressed.height; y += rangeBlocksSize) {
            for (int x = 0; x < compressed.width; x += rangeBlocksSize) {
                var block = result.pop();

                int index = 0;
                for (int yy = y; yy < y + rangeBlocksSize; yy++)
                    for (int xx = x; xx < x + rangeBlocksSize; xx++)
                        compressed.set(xx, yy, block.colors[index++]);

                current++;
            }
        }

        return compressed;
    }

    @Override
    public void build(Table table) {
        table.label(() -> "Размер рангового блока: [yellow]" + rangeBlocksSize).labelAlign(Align.center).left().row();
        table.slider(2, 64, 1, rangeBlocksSize, value -> rangeBlocksSize = (int) value).disabled(slider -> compressing).padTop(24f).width(240f).align(Align.left);

        table.row();

        table.label(() -> "Ранговых блоков в доменном: [yellow]" + rangeBlocksPerDomain).labelAlign(Align.center).padTop(48f).left().row();
        table.slider(2, 16, 1, rangeBlocksPerDomain, value -> rangeBlocksPerDomain = (int) value).disabled(slider -> compressing).padTop(24f).width(240f).align(Align.left);
    }

    private static float getDifference(int[] color1, int[] color2) {
        return Math.abs(color1[0] - color2[0])
                + Math.abs(color1[1] - color2[1])
                + Math.abs(color1[2] - color2[2])
                + Math.abs(color1[3] - color2[3]);
    }

    private static int[] getAverageColor(int[] colors) {
        int red = 0, green = 0, blue = 0, alpha = 0;
        for (int color : colors) {
            red += (color >> 24) & 0xFF;
            green += (color >> 16) & 0xFF;
            blue += (color >> 8) & 0xFF;
            alpha += color & 0xFF;
        }

        return new int[] {red / colors.length, green / colors.length, blue / colors.length, alpha / colors.length};
    }

    @Desugar
    public record Block(int[] colors, int[] average) {
        public Block(int[] colors) {
            this(colors, getAverageColor(colors));
        }
    }
}