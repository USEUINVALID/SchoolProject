package buddha.encoder;

import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.graphics.Pixmaps;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;

public class Compressor {
    public static int domainBlocksSize = 8;
    public static int rangeBlocksPerDomain = 2;

    public static void main(String[] args) {
        test(args[0]);
    }

    public static void test(String path) {
        var source = new Fi(path);
        var pixmap = compress(scale(new Pixmap(source)));

        var result = source.sibling(source.nameWithoutExtension() + "-compressed.png");
        PixmapIO.writePng(result, pixmap);
    }

    public static Pixmap compress(Pixmap pixmap) {
        var domainBlocks = new Seq<Pixmap>(pixmap.width * pixmap.height / domainBlocksSize / domainBlocksSize);
        var rangeBlocks = new Seq<Pixmap>(pixmap.width * pixmap.height * rangeBlocksPerDomain * rangeBlocksPerDomain / domainBlocksSize / domainBlocksSize);

        Time.mark();
        int rangeBlocksSize = domainBlocksSize / rangeBlocksPerDomain;

        for (int x = 0; x < pixmap.width; x += domainBlocksSize) {
            for (int y = 0; y < pixmap.height; y += domainBlocksSize) {
                var domainBlock = new Pixmap(domainBlocksSize, domainBlocksSize);
                domainBlocks.add(domainBlock);

                int fx = x, fy = y;
                domainBlock.each((dx, dy) -> domainBlock.set(dx, dy, pixmap.get(fx + dx, fy + dy)));
            }
        }

        for (int x = 0; x < pixmap.width; x += rangeBlocksSize) {
            for (int y = 0; y < pixmap.height; y += rangeBlocksSize) {
                var rangeBlock = new Pixmap(rangeBlocksSize, rangeBlocksSize);
                rangeBlocks.add(rangeBlock);

                int fx = x, fy = y;
                rangeBlock.each((dx, dy) -> rangeBlock.set(dx, dy, pixmap.get(fx + dx, fy + dy)));
            }
        }

        Log.info("Done in " + Time.elapsed() + "ms");
        return pixmap;
    }

    public static Pixmap scale(Pixmap pixmap) {
        return Pixmaps.scale(pixmap, scale(pixmap.width, domainBlocksSize), scale(pixmap.height, domainBlocksSize));
    }

    public static float scale(int number, int div) {
        return (float) (((number / div) + 1) * div) / number;
    }
}