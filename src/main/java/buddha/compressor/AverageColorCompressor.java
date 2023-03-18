package buddha.compressor;

import arc.graphics.Pixmap;

public class AverageColorCompressor {

    public static int blockSize = 16;

    public static Pixmap compress(Pixmap pixmap) {
        var compressed = new Pixmap(pixmap.width, pixmap.height);

        for (int y = 0; y < pixmap.height; y += blockSize) {
            for (int x = 0; x < pixmap.width; x += blockSize) {
                var block = new int[blockSize * blockSize];

                int index = 0;
                for (int yy = y; yy < y + blockSize; yy++)
                    for (int xx = x; xx < x + blockSize; xx++)
                        block[index++] = pixmap.get(xx, yy);

                int averageColor = getAverageColor(block);
                for (int yy = y; yy < y + blockSize; yy++)
                    for (int xx = x; xx < x + blockSize; xx++)
                        compressed.set(xx, yy, averageColor);
            }
        }

        return compressed;
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