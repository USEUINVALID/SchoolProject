package buddha.compressor;

import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import com.github.bsideup.jabel.Desugar;

import java.util.concurrent.atomic.AtomicInteger;

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

        var counter = new AtomicInteger();
        var result = domainBlocks.map(domainBlock -> {
            Log.info("Processing block @ / @", counter.incrementAndGet(), domainBlocks.size);
            return rangeBlocks.min(rangeBlock -> getDifference(domainBlock.average, rangeBlock.average));
        }).reverse();

        var compressed = new Pixmap(pixmap.width / rangeBlocksPerDomain, pixmap.height / rangeBlocksPerDomain);
        for (int y = 0; y < compressed.height; y += rangeBlocksSize) {
            for (int x = 0; x < compressed.width; x += rangeBlocksSize) {
                var block = result.pop();

                int index = 0;
                for (int yy = y; yy < y + rangeBlocksSize; yy++)
                    for (int xx = x; xx < x + rangeBlocksSize; xx++)
                        compressed.set(xx, yy, block.colors[index++]);
            }
        }

        return compressed;
    }

    @Override
    public boolean compressing() {
        return true;
    }

    @Override
    public float progress() {
        return Mathf.random(100f);
    }

    @Override
    public void build(Table table) {

    }

//    private static float getDifference(int[] domainBlock, int[] rangeBlock) {
//        float difference = 0f;
//        for (int i = 0; i < domainBlock.length; i++)
//            difference += getDifference(domainBlock[i], rangeBlock[i / rangeBlocksPerDomain / rangeBlocksPerDomain]);
//
//        return difference;
//    }

    private static float getDifference(int color1, int color2) {
        int r1 = (color1 >> 24) & 0xFF;
        int g1 = (color1 >> 16) & 0xFF;
        int b1 = (color1 >> 8) & 0xFF;

        int r2 = (color2 >> 24) & 0xFF;
        int g2 = (color2 >> 16) & 0xFF;
        int b2 = (color2 >> 8) & 0xFF;

        return Mathf.sqrt((r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2)) / (255f * 2f);
    }

    private static int getAverageColor(int[] colors) {
        int r = 0, g = 0, b = 0, a = 0;
        for (int color : colors) {
            r += (color >> 24) & 0xFF;
            g += (color >> 16) & 0xFF;
            b += (color >> 8) & 0xFF;
            a += color & 0xFF;
        }

        return ((r / colors.length) << 24) | ((g / colors.length) << 16) | ((b / colors.length) << 8) | (a / colors.length);
    }

    @Desugar
    public record Block(int[] colors, int average) {
        public Block(int[] colors) {
            this(colors, getAverageColor(colors));
        }
    }
}