package buddha.compressor;

import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.struct.Seq;

// Доменные - большие
// Ранговые - маленькие
public class FractalCompressor {

    public static int rangeBlocksSize = 4;
    public static int rangeBlocksPerDomain = 2;

    public static Pixmap compress(Pixmap pixmap) {
        var domainBlocks = new Seq<int[]>();
        var rangeBlocks = new Seq<int[]>();

        int domainBlocksSize = rangeBlocksSize * rangeBlocksPerDomain;

        for (int y = 0; y < pixmap.height; y += domainBlocksSize) {
            for (int x = 0; x < pixmap.width; x += domainBlocksSize) {
                var block = new int[domainBlocksSize * domainBlocksSize];

                int index = 0;
                for (int yy = y; yy < y + domainBlocksSize; yy++)
                    for (int xx = x; xx < x + domainBlocksSize; xx++)
                        block[index++] = pixmap.get(xx, yy);

                domainBlocks.add(block);
            }
        }

        for (int y = 0; y < pixmap.height; y += rangeBlocksSize) {
            for (int x = 0; x < pixmap.width; x += rangeBlocksSize) {
                var block = new int[rangeBlocksSize * rangeBlocksSize];

                int index = 0;
                for (int yy = y; yy < y + rangeBlocksSize; yy++)
                    for (int xx = x; xx < x + rangeBlocksSize; xx++)
                        block[index++] = pixmap.get(xx, yy);

                rangeBlocks.add(block);
            }
        }

        var result = domainBlocks.map(domainBlock -> rangeBlocks.min(rangeBlock -> getDifference(domainBlock, rangeBlock))).reverse();
        var compressed = new Pixmap(pixmap.width / rangeBlocksPerDomain, pixmap.height / rangeBlocksPerDomain);

        for (int y = 0; y < compressed.height; y += rangeBlocksSize) {
            for (int x = 0; x < compressed.width; x += rangeBlocksSize) {
                var block = result.pop();

                int index = 0;
                for (int yy = y; yy < y + rangeBlocksSize; yy++)
                    for (int xx = x; xx < x + rangeBlocksSize; xx++)
                        compressed.set(xx, yy, block[index++]);
            }
        }

        return compressed;
    }

    private static float getDifference(int[] domainBlock, int[] rangeBlock) {
        float difference = 0f;
        for (int i = 0; i < domainBlock.length; i++)
            difference += getDifference(domainBlock[i], rangeBlock[i / rangeBlocksPerDomain / rangeBlocksPerDomain]);

        return difference;
    }

    private static float getDifference(int color1, int color2) {
        int r1 = (color1 >> 24) & 0xFF;
        int g1 = (color1 >> 16) & 0xFF;
        int b1 = (color1 >> 8) & 0xFF;

        int r2 = (color2 >> 24) & 0xFF;
        int g2 = (color2 >> 16) & 0xFF;
        int b2 = (color2 >> 8) & 0xFF;

        return Mathf.sqrt((r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2)) / (255f * 2f);
    }
}