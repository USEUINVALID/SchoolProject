package buddha.compressor;

import java.io.DataInputStream;
import java.util.Arrays;

public class FractalCompression {
    public static int blockSize = 8;
    public static int widthKernel = 2;

    public static float[][] imageInfo;
    public static float[][] imageInfoRGB;

    public static float averageError;

    /**
     * Checks if a picture is a grey or colored image.
     */
    public static boolean isGreyScale(RasterImage input) {
        for (int y = 0; y < input.height; y++) {
            for (int x = 0; x < input.width; x++) {
                int r = (input.get(x, y) >> 16) & 0xff;
                int g = (input.get(x, y) >> 8) & 0xff;
                int b = input.get(x, y) & 0xff;

                if (r != g || g != b) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Encodes input image depending on if its grayscale or colored.
     */
    public static RasterImage encode(RasterImage input) throws Exception {
        if (isGreyScale(input))
            return encodeGrayScale(input);
        else
            return encodeRGB(input);
    }

    /**
     * Gets an array of integers and returns the average value.
     */
    private static int getAverage(int[] values) {
        int sum = 0;
        for (int value : values)
            sum += value;

        return sum / values.length;
    }

    /**
     * Generates a Kernel of a given size to scan for best matching domain blocks.
     */
    public static int[] generateKernel(int domainPerWidth, int domainPerHeight, int index) {
        // calculates start position of kernel
        int dx = Math.max(index % domainPerWidth - widthKernel / 2, 0);
        int dy = Math.max(index / domainPerWidth - widthKernel / 2, 0);

        if (dx + widthKernel >= domainPerWidth) dx = domainPerWidth - widthKernel;
        if (dy + widthKernel >= domainPerHeight) dy = domainPerHeight - widthKernel;

        return new int[]{dy, dx};
    }

    /**
     * Applies fractal image compression to a given grayscale RasterImage.
     */
    public static RasterImage encodeGrayScale(RasterImage input) throws Exception {
        // calculate range blocks per dimension
        int rangePerWidth = input.width / blockSize;
        int rangePerHeight = input.height / blockSize;

        // calculate domain blocks per dimension
        int domainPerWidth = rangePerWidth * 2 - 3;
        int domainPerHeight = rangePerHeight * 2 - 3;

        // generate codebook to read domain blocks from
        var codebook = createCodebuch(input);
        var dst = new RasterImage(input.width, input.height);

        int j = 0;
        imageInfo = new float[rangePerWidth * rangePerHeight][3];// for decoder later write to file
        for (int y = 0; y < dst.height; y += blockSize) {
            for (int x = 0; x < dst.width; x += blockSize) {

                int i = getDomainBlockIndex(x, y, rangePerWidth, rangePerHeight, domainPerWidth);

                int dy;
                int dx;

                int[] dXdY = generateKernel(domainPerWidth, domainPerHeight, i);

                dy = dXdY[0];
                dx = dXdY[1];

                // write codebook entries into kernel array
                DomainBlock[] domainKernel = new DomainBlock[widthKernel * widthKernel];

                int[] indices = new int[widthKernel * widthKernel];
                int n = 0;
                for (int ky = 0; ky < widthKernel; ky++) {
                    for (int kx = 0; kx < widthKernel; kx++) {
                        int index = dx + kx + (dy + ky) * domainPerWidth;
                        domainKernel[n] = codebook[index];
                        indices[n] = index;
                        n++;
                    }
                }

                // apply algorithm based on minimum error to find best fit domain block
                int[] rangeBlock = getRangeblock(x, y, input);
                int rangeM = getAverage(rangeBlock);

                imageInfo[j] = getBestDomainBlock(domainKernel, rangeBlock, indices, rangeM);
                j++;
            }
        }

        return getBestGeneratedCollage(input);
    }

    /**
     * Applies fractal image compression to a given color RasterImage.
     */
    public static RasterImage encodeRGB(RasterImage input) throws Exception {
        // calculate rangeblock per dimension
        int rangebloeckePerWidth = input.width / blockSize;
        int rangebloeckePerHeight = input.height / blockSize;

        // calculate domainblock per dimension
        int domainbloeckePerWidth = rangebloeckePerWidth * 2 - 3;
        int domainbloeckePerHeight = rangebloeckePerHeight * 2 - 3;

        // generate codebook to read domain blocks from
        DomainBlock[] codebuch = createCodebuchRGB(input);
        RasterImage dst = new RasterImage(input.width, input.height);

        int j = 0;
        imageInfoRGB = new float[rangebloeckePerWidth * rangebloeckePerHeight][5];// for decoder later write to file
        for (int y = 0; y < dst.height; y += blockSize) {
            for (int x = 0; x < dst.width; x += blockSize) {

                int i = getDomainBlockIndex(x, y, rangebloeckePerWidth, rangebloeckePerHeight, domainbloeckePerWidth);

                int dy;
                int dx;

                int[] dXdY = generateKernel(domainbloeckePerWidth, domainbloeckePerHeight, i);

                dy = dXdY[0];
                dx = dXdY[1];

                int[] indices = new int[widthKernel * widthKernel];
                // write codebuch entries into kernel array
                DomainBlock[] domainKernel = new DomainBlock[widthKernel * widthKernel];
                int n = 0;
                for (int ky = 0; ky < widthKernel; ky++) {
                    for (int kx = 0; kx < widthKernel; kx++) {
                        int index = dx + kx + (dy + ky) * domainbloeckePerWidth;
                        domainKernel[n] = codebuch[index];
                        indices[n] = index;
                        n++;
                    }
                }
                // apply algorithm based on minimum error to find best fit domain block
                imageInfoRGB[j] = getBestDomainBlockRGB(domainKernel, getRangeblockRGB(x, y, input), indices);
                j++;
            }
        }

        return getBestGeneratedCollageRGB(input);
    }

    /**
     * Gets the collage of the encoded picture.
     */
    public static RasterImage getBestGeneratedCollage(RasterImage originalImage) {
        float[][] tmp = imageInfo;
        RasterImage collage = new RasterImage(originalImage.width, originalImage.height);
        calculateIndices(tmp, originalImage.width, originalImage.height, blockSize, widthKernel);

        DomainBlock[] codebuch = createCodebuch(originalImage); // get codebook
        int i = 0;

        // iterate image per rangeblock
        for (int y = 0; y < originalImage.height; y += blockSize) {
            for (int x = 0; x < originalImage.width; x += blockSize) {
                // iterate rangeblock
                for (int ry = 0; ry < blockSize && y + ry < originalImage.height; ry++) {
                    for (int rx = 0; rx < blockSize && x + rx < originalImage.width; rx++) {

                        // get current value of best fit domainblock pixel
                        int domain = codebuch[(int) tmp[i][0]].argb[rx + ry * blockSize];
                        int value = (int) (tmp[i][1] * domain + tmp[i][2]);

                        value = applyThreshold(value);

                        collage.argb[x + rx + (y + ry) * originalImage.width] = 0xff000000 | (value << 16)
                                | (value << 8) | value;
                    }
                }
                i++;
            }
        }

        return collage;
    }

    /**
     * Gets the collage of the encoded piture for colored pictures.
     */
    public static RasterImage getBestGeneratedCollageRGB(RasterImage originalImage) {
        float[][] tmp = imageInfoRGB;
        RasterImage collage = new RasterImage(originalImage.width, originalImage.height);
        calculateIndices(tmp, collage.width, collage.height, blockSize, widthKernel);

        DomainBlock[] codebuch = createCodebuchRGB(originalImage); // get codebook
        int i = 0;

        // iterate image per rangeblock
        for (int y = 0; y < collage.height; y += blockSize) {
            for (int x = 0; x < collage.width; x += blockSize) {
                // iterate rangeblock
                for (int ry = 0; ry < blockSize && y + ry < collage.height; ry++) {
                    for (int rx = 0; rx < blockSize && x + rx < collage.width; rx++) {

                        // get current value of best fit domainblock pixel
                        int domain = codebuch[(int) tmp[i][0]].argb[rx + ry * blockSize];
                        int domainR = (domain >> 16) & 0xff;
                        int domainG = (domain >> 8) & 0xff;
                        int domainB = domain & 0xff;

                        int valueR = (int) (tmp[i][1] * domainR + tmp[i][2]);
                        int valueG = (int) (tmp[i][1] * domainG + tmp[i][3]);
                        int valueB = (int) (tmp[i][1] * domainB + tmp[i][4]);

                        // apply thresshold
                        valueR = applyThreshold(valueR);
                        valueG = applyThreshold(valueG);
                        valueB = applyThreshold(valueB);

                        collage.argb[x + rx + (y + ry) * collage.width] = 0xff000000 | (valueR << 16) | (valueG << 8)
                                | valueB;
                    }
                }
                i++;
            }
        }
        return collage;

    }

    /**
     * Decodes a grey image from an input stream.
     */
    public static RasterImage decodeGreyScale(DataInputStream inputStream) throws Exception {
        int width = inputStream.readInt();
        int height = inputStream.readInt();

        RasterImage image = FractalCompression.generateGrayImage(width, height);

        int inputedBlockgroesse = inputStream.readInt();
        int widthKernel = inputStream.readInt();

        int rangebloeckePerWidth = width / inputedBlockgroesse;
        int rangebloeckePerHeight = height / inputedBlockgroesse;

        float[][] imgData = new float[rangebloeckePerWidth * rangebloeckePerHeight][3];

        while (inputStream.available() > 0) {
            for (int rows = 0; rows < imgData.length; rows++) {
                imgData[rows][0] = (float) inputStream.readInt();
                imgData[rows][1] = (float) inputStream.readInt() / 100f;
                imgData[rows][2] = (float) inputStream.readInt();
            }
        }

        calculateIndices(imgData, width, height, inputedBlockgroesse, widthKernel);

        // make iterations for image reconstruction
        for (int counter = 0; counter < 50; counter++) {
            DomainBlock[] codebuch = createCodebuch(image); // get codebook
            int i = 0;

            // iterate image per rangeblock
            for (int y = 0; y < image.height; y += inputedBlockgroesse) {
                for (int x = 0; x < image.width; x += inputedBlockgroesse) {
                    // iterate rangeblock
                    for (int ry = 0; ry < inputedBlockgroesse && y + ry < image.height; ry++) {
                        for (int rx = 0; rx < inputedBlockgroesse && x + rx < image.width; rx++) {
                            int range = (image.argb[x + rx + (y + ry) * image.width] >> 16) & 0xff; // get current value
                            // of rangeblock
                            // get current value of best fit domainblock pixel
                            int domain = codebuch[(int) imgData[i][0]].argb[rx + ry * inputedBlockgroesse];

                            int value = (int) (imgData[i][1] * domain + imgData[i][2]);

                            // apply thresshold
                            if (value < 0)
                                value = 0;
                            else if (value > 255)
                                value = 255;

                            image.argb[x + rx + (y + ry) * image.width] = 0xff000000 | (value << 16) | (value << 8)
                                    | value;

                            averageError += (range - value) * (range - value); // calculate error
                        }
                    }
                    i++;
                }
            }
            averageError = averageError / (float) (width * height);
            if (averageError < 1)
                break; // stop iterations when error drops below 1
            if (counter != 49)
                averageError = 0;
        }

        return image;
    }

    /**
     * Decodes colored picture from an inputstream.
     */
    public static RasterImage decodeRGB(DataInputStream inputStream) throws Exception {
        int width = inputStream.readInt();
        int height = inputStream.readInt();

        RasterImage image = FractalCompression.generateGrayImage(width, height);

        int inputedBlockgroesse = inputStream.readInt();
        int widthKernel = inputStream.readInt();

        int rangebloeckePerWidth = width / inputedBlockgroesse;
        int rangebloeckePerHeight = height / inputedBlockgroesse;

        float[][] imgData = new float[rangebloeckePerWidth * rangebloeckePerHeight][5];

        while (inputStream.available() > 0) {
            for (int rows = 0; rows < imgData.length; rows++) {
                imgData[rows][0] = (float) inputStream.readInt();
                imgData[rows][1] = (float) inputStream.readInt() / 1000000f;
                imgData[rows][2] = (float) inputStream.readInt() / 100000f;
                imgData[rows][3] = (float) inputStream.readInt() / 100000f;
                imgData[rows][4] = (float) inputStream.readInt();

            }
        }

        calculateIndices(imgData, width, height, inputedBlockgroesse, widthKernel);

        // make iterations for image reconstruction
        for (int counter = 0; counter < 50; counter++) {
            DomainBlock[] codebuch = createCodebuchRGB(image); // get codebook
            int i = 0;

            // iterate image per rangeblock
            for (int y = 0; y < image.height; y += inputedBlockgroesse) {
                for (int x = 0; x < image.width; x += inputedBlockgroesse) {
                    // iterate rangeblock
                    for (int ry = 0; ry < inputedBlockgroesse && y + ry < image.height; ry++) {
                        for (int rx = 0; rx < inputedBlockgroesse && x + rx < image.width; rx++) {
                            int rangeR = (image.argb[x + rx + (y + ry) * image.width] >> 16) & 0xff; // get current
                            // value of
                            // rangeblock
                            int rangeG = (image.argb[x + rx + (y + ry) * image.width] >> 8) & 0xff; // get current value
                            // of rangeblock
                            int rangeB = image.argb[x + rx + (y + ry) * image.width] & 0xff; // get current value of
                            // rangeblock

                            // get current value of best fit domainblock pixel
                            int domain = codebuch[(int) imgData[i][0]].argb[rx + ry * inputedBlockgroesse];
                            int domainR = (domain >> 16) & 0xff;
                            int domainG = (domain >> 8) & 0xff;
                            int domainB = domain & 0xff;

                            int valueR = (int) (imgData[i][1] * domainR + imgData[i][2]);
                            int valueG = (int) (imgData[i][1] * domainG + imgData[i][3]);
                            int valueB = (int) (imgData[i][1] * domainB + imgData[i][4]);

                            valueR = applyThreshold(valueR);
                            valueG = applyThreshold(valueG);
                            valueB = applyThreshold(valueB);

                            image.argb[x + rx + (y + ry) * image.width] = 0xff000000 | (valueR << 16) | (valueG << 8)
                                    | valueB;

                            averageError += (rangeR - valueR) * (rangeR - valueR) + (rangeG - valueG) * (rangeG - valueG)
                                    + (rangeB - valueB) * (rangeB - valueB); // calculate error
                        }
                    }
                    i++;
                }
            }
            averageError = averageError / (float) (width * height);
            if (averageError < 1)
                break; // stop iterations when error drops below 1
            if (counter != 49)
                averageError = 0;
        } // pixel

        return image;
    }

    /**
     * Calculates the index of the domain block on top of the range block.
     */
    private static int getDomainBlockIndex(int x, int y, int rangebloeckePerWidth, int rangebloeckePerHeight,
                                           int domainbloeckePerWidth) {
        int xr = x / blockSize;
        int yr = y / blockSize;
        int i = 0;

        if (yr == 0)
            yr = 1;
        if (xr == 0)
            xr = 1;
        if (yr == rangebloeckePerHeight - 1)
            yr = yr - 1;
        if (xr == rangebloeckePerWidth - 1)
            xr = xr - 1;

        if (xr > 1) {
            if (yr == 0)
                i = xr;
            else
                i = (xr * 2) - 2 + (yr + yr - 1) * domainbloeckePerWidth;
        } else if (xr == 1) {
            if (yr == 0)
                i = xr;
            else
                i = xr + (yr + yr - 1) * domainbloeckePerWidth;
        }
        return i;
    }

    public static RasterImage decode(DataInputStream inputStream) throws Exception {
        int isGreyScale = inputStream.readInt();
        if (isGreyScale == 0)
            return decodeGreyScale(inputStream);
        else
            return decodeRGB(inputStream);
    }

    /**
     * Gets positions x, y and returns the range block starting from these coordinates.
     */
    private static int[] getRangeblockRGB(int x, int y, RasterImage image) {
        int[] rangeblock = new int[blockSize * blockSize];
        int i = 0;

        // iterates range block and extracts grey values
        for (int ry = 0; ry < blockSize && y + ry < image.height; ry++) {
            for (int rx = 0; rx < blockSize && x + rx < image.width; rx++) {
                int value = image.argb[(x + rx) + (y + ry) * image.width];
                rangeblock[i] = value;
                i++;
            }
        }
        return rangeblock;
    }

    /**
     * Gets positions x, y and returns the range block starting from these coordinates.
     */
    private static int[] getRangeblock(int x, int y, RasterImage image) {
        int[] rangeblock = new int[blockSize * blockSize];
        int i = 0;

        // iterates range block and extracts grey values
        for (int ry = 0; ry < blockSize && y + ry < image.height; ry++) {
            for (int rx = 0; rx < blockSize && x + rx < image.width; rx++) {
                int value = image.argb[(x + rx) + (y + ry) * image.width];
                value = (value >> 16) & 0xff;
                rangeblock[i] = value;
                i++;
            }
        }
        return rangeblock;
    }

    /**
     * Finds the best matching domainblock for the given rangeblock out of a given array of domainblocks for a grey picture.
     */
    private static float[] getBestDomainBlock(DomainBlock[] domainblocks, int[] rangeblock, int[] indices,
                                              int mittelWertRangeblock) {
        float smallestError = 10000000;
        float[] bestBlock = {0, 0, 0, 0, 0, 0};

        // iterate domain blocks
        for (int i = 0; i < domainblocks.length; i++) {
            // get Aopt and Bopt for currently visited domainblock

            float[] ab = getErrorVarianceCovariance(indices[i], rangeblock, mittelWertRangeblock, domainblocks[i].argb,
                    domainblocks[i]);
            float error = ab[0];

            // check if current error smaller than previous errors
            if (error < smallestError) {
                smallestError = error;
                bestBlock = new float[]{i, ab[0], ab[1], ab[2], ab[3], ab[4]};
            }
        }

        float a = bestBlock[2] / bestBlock[3];

        if (a < -1)
            a = -1;
        else if (a > 1)
            a = 1;

        float b = bestBlock[4] - a * bestBlock[5];
        return new float[]{bestBlock[0], a, b};
    }

    /**
     * Calculates error, variance and kovariance.
     */
    private static float[] getErrorVarianceCovariance(int codeBuchIndex, int[] range, int rangeMittelwert, int[] domain,
                                                      DomainBlock domainblock) {
        float domainM = domainblock.average;

        float kovarianz = 0;
        float varianzRange = 0;

        float varianzSquare = domainblock.variance;

        // iterate domain block
        for (int i = 0; i < range.length; i++) {
            // subtract average value from current value
            float greyR = range[i] - rangeMittelwert;
            float greyD = domain[i] - domainM;

            kovarianz += greyR * greyD;
            varianzRange += greyR;
        }

        float r;
        float error;

        if (varianzRange == 0 || Math.sqrt(varianzSquare) == 0)
            r = 0;
        else
            r = (float) (kovarianz / (varianzRange * Math.sqrt(varianzSquare)));

        r = r * r;
        error = (varianzRange * varianzRange) * (1 - r);

        return new float[]{error, kovarianz, varianzSquare, rangeMittelwert, domainM};
    }

    /**
     * Finds the best matching domainblock for the given rangeblock out of a given array of domainblocks for a colored picture.
     */
    private static float[] getBestDomainBlockRGB(DomainBlock[] domainblocks, int[] rangeblock, int[] indices) {
        float smallestError = 10000000;
        float[] bestBlock = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        // iterate domain blocks
        for (int i = 0; i < domainblocks.length; i++) {

            // get Aopt and Bopt for currently visited domainblock
            float[] ab = getErrorVarianceCovarianceRGB(domainblocks[i].argb, rangeblock, indices[i], domainblocks[i]);

            float error = ab[0];

            // check if current error smaller than previous errors
            if (error < smallestError) {
                smallestError = error;
                bestBlock = new float[]{i, ab[0], ab[1], ab[2], ab[3], ab[4], ab[5], ab[6], ab[7], ab[8]};
            }
        }

        // get a
        float a = bestBlock[2] / bestBlock[3];

        // apply threshold
        if (a > 1)
            a = 1;
        if (a < -1)
            a = -1;

        // get b
        float bR = bestBlock[4] - a * bestBlock[5];

        float bG = bestBlock[6] - a * bestBlock[7];

        float bB = bestBlock[8] - a * bestBlock[9];

        return new float[]{bestBlock[0], a, bR, bG, bB};
    }

    /**
     * applies Threshold, so that the color value can only be between 0 and 255
     */
    private static int applyThreshold(int pixelValue) {
        if (pixelValue < 0)
            pixelValue = 0;
        else if (pixelValue > 255)
            pixelValue = 255;
        return pixelValue;
    }

    /**
     * Calculates error, variance and kovariance for colored picture.
     */
    private static float[] getErrorVarianceCovarianceRGB(int[] domain, int[] range, int index,
                                                         DomainBlock domainblock) {

        float domainR = domainblock.averageR;
        float domainG = domainblock.averageG;
        float domainB = domainblock.averageB;

        int[] rangeR = getRGB(range, 0);
        int[] rangeG = getRGB(range, 1);
        int[] rangeB = getRGB(range, 2);

        int rangeRM = getAverage(rangeR);
        int rangeGM = getAverage(rangeG);
        int rangeBM = getAverage(rangeB);

        float kovarianz = 0;
        float varianzSquare = (domainblock.varianceR + domainblock.varianceG + domainblock.averageB);
        float varianzRange = 0;
        float varianzDomain = (float) Math.sqrt(domainblock.variance);

        // iterate domain block
        for (int i = 0; i < range.length; i++) {
            // subtract average value from current value
            float greyD = (((domain[i] >> 16) & 0xff) - domainR) + (((domain[i] >> 8) & 0xff) - domainG)
                    + ((domain[i] & 0xff) - domainB);
            float greyR = (((range[i] >> 16) & 0xff) - rangeRM) + (((range[i] >> 8) & 0xff) - rangeGM)
                    + ((range[i] & 0xff) - rangeBM);

            // calculate variance, covariance
            kovarianz += greyR * greyD;
            varianzRange += greyR;
            varianzDomain += greyD;
        }

        float r;
        float error;

        if (varianzRange == 0 || varianzDomain == 0)
            r = 0;
        else
            r = kovarianz / (varianzRange * varianzDomain);

        r = r * r;
        error = (varianzRange * varianzRange) * (1 - r);

        return new float[]{error, kovarianz, varianzSquare, getAverage(rangeR), domainR, getAverage(rangeG),
                domainG, getAverage(rangeB), domainB};
    }

    public static int[] getRGB(int[] argbBytes, int canal) {
        int[] temp = new int[argbBytes.length];

        // red
        if (canal == 0) {
            for (int i = 0; i < argbBytes.length; i++) {
                temp[i] = (argbBytes[i] >> 16) & 0xff;
            }
        }

        // green
        else if (canal == 1) {
            for (int i = 0; i < argbBytes.length; i++) {
                temp[i] = (argbBytes[i] >> 8) & 0xff;
            }
        }

        // blue
        else if (canal == 2) {
            for (int i = 0; i < argbBytes.length; i++) {
                temp[i] = argbBytes[i] & 0xff;
            }
        }

        return temp;
    }

    /**
     * Calculates the indices from the domain-Kernel to the codebook index.
     */
    private static float[][] calculateIndices(float[][] imgData, int width, int height, int blockgroesse,
                                              int widthKernel) {
        int rangebloeckePerWidth = width / blockgroesse;
        int rangebloeckePerHeight = height / blockgroesse;

        // calculate domainblock per dimension
        int domainbloeckePerWidth = rangebloeckePerWidth * 2 - 3;
        int domainbloeckePerHeight = rangebloeckePerHeight * 2 - 3;

        int i = 0;
        for (int y = 0; y < height; y += blockgroesse) {
            for (int x = 0; x < width; x += blockgroesse) {
                int di = getDomainBlockIndex(x, y, rangebloeckePerWidth, rangebloeckePerHeight, domainbloeckePerWidth);

                // calculate kernel start point
                int dy = (di / domainbloeckePerWidth) - widthKernel / 2;
                int dx = di % domainbloeckePerWidth - widthKernel / 2;

                if (dx < 0)
                    dx = 0;
                if (dy < 0)
                    dy = 0;
                if (dx + widthKernel >= domainbloeckePerWidth)
                    dx = domainbloeckePerWidth - widthKernel;
                if (dy + widthKernel >= domainbloeckePerHeight)
                    dy = domainbloeckePerHeight - widthKernel;

                int yd = (int) (imgData[i][0] / widthKernel);
                int xd = (int) (imgData[i][0] % widthKernel);

                // combine to index
                int result = xd + dx + (yd + dy) * domainbloeckePerWidth;

                imgData[i][0] = result;
                i++;
            }
        }
        return null;
    }

    /**
     * Gets a colored RasterImage and scales it down by factor 2.
     */
    public static RasterImage scaleImageRGB(RasterImage image) {
        RasterImage scaled = new RasterImage(image.width / 2, image.height / 2);
        int i = 0;
        for (int y = 0; y < image.height; y += 2) {
            for (int x = 0; x < image.width; x += 2) {
                int mittelwertR = (image.argb[x + y * image.width] >> 16) & 0xff;
                int mittelwertG = (image.argb[x + y * image.width] >> 8) & 0xff;
                int mittelwertB = image.argb[x + y * image.width] & 0xff;

                if (x + 1 >= image.width) {
                    mittelwertR += 128;
                    mittelwertG += 128;
                    mittelwertB += 128;

                } else {
                    mittelwertR += (image.argb[x + 1 + y * image.width] >> 16) & 0xff;
                    mittelwertG += (image.argb[x + 1 + y * image.width] >> 8) & 0xff;
                    mittelwertB += image.argb[x + 1 + y * image.width] & 0xff;

                    if (y + 1 >= image.height) {
                        mittelwertR += 128;
                        mittelwertG += 128;
                        mittelwertB += 128;
                    } else {
                        mittelwertR += (image.argb[x + (y + 1) * image.width] >> 16) & 0xff;
                        mittelwertG += (image.argb[x + (y + 1) * image.width] >> 8) & 0xff;
                        mittelwertB += image.argb[x + (y + 1) * image.width] & 0xff;

                    }
                }

                if (y + 1 >= image.height) {
                    mittelwertR += 128;
                    mittelwertG += 128;
                    mittelwertB += 128;
                } else {
                    if (x + 1 >= image.height) {
                        mittelwertR += 128;
                        mittelwertG += 128;
                        mittelwertB += 128;
                    } else {
                        mittelwertR += (image.argb[x + (y + 1) * image.width] >> 16) & 0xff;
                        mittelwertG += (image.argb[x + (y + 1) * image.width] >> 8) & 0xff;
                        mittelwertB += image.argb[x + (y + 1) * image.width] & 0xff;
                    }
                }
                // -----

                mittelwertR = mittelwertR / 4;
                mittelwertG = mittelwertG / 4;
                mittelwertB = mittelwertB / 4;

                scaled.argb[i] = 0xff000000 | (mittelwertR << 16) | (mittelwertG << 8) | mittelwertB;
                i++;
            }
        }
        return scaled;

    }

    /**
     * Gets a grey RasterImage and scales it down by factor 2.
     */
    public static RasterImage scaleImage(RasterImage image) {
        RasterImage scaled = new RasterImage(image.width / 2, image.height / 2);
        int i = 0;
        for (int y = 0; y < image.height; y += 2) {
            for (int x = 0; x < image.width; x += 2) {

                // Mittelwert bestimmen
                int mittelwert = (image.argb[x + y * image.width] >> 16) & 0xff;

                // Randbehandlung-----
                if (x + 1 >= image.width) {
                    mittelwert += 128;
                } else {
                    mittelwert += (image.argb[x + 1 + y * image.width] >> 16) & 0xff;
                    if (y + 1 >= image.height)
                        mittelwert += 128;
                    else
                        mittelwert += (image.argb[x + (y + 1) * image.width] >> 16) & 0xff;
                }

                if (y + 1 >= image.height)
                    mittelwert += 128;
                else {
                    if (x + 1 >= image.height)
                        mittelwert += 128;
                    else
                        mittelwert += (image.argb[x + 1 + (y + 1) * image.width] >> 16) & 0xff;
                }
                // -----

                mittelwert = mittelwert / 4;
                scaled.argb[i] = 0xff000000 | (mittelwert << 16) | (mittelwert << 8) | mittelwert;
                i++;
            }
        }
        return scaled;

    }

    /**
     * Gets a grey RasterImage and returns a 2D of array containing a codebook.
     */
    private static DomainBlock[] createCodebuch(RasterImage image) {

        // scale image by factor 2
        image = scaleImage(image);
        int abstand = blockSize / 4;

        // generated codebook size
        DomainBlock[] codebuch = new DomainBlock[(image.width / abstand - 3) * (image.height / abstand - 3)];

        int i = 0;

        // iterate image
        for (int y = 0; y < image.height; y += abstand) {
            for (int x = 0; x < image.width; x += abstand) {

                int[] codebuchblock = new int[blockSize * blockSize];
                // iterate domainblock
                if (y + blockSize <= image.height && x + blockSize <= image.width) {
                    for (int ry = 0; ry < blockSize; ry++) {
                        for (int rx = 0; rx < blockSize; rx++) {

                            codebuchblock[rx + ry * blockSize] = (image.argb[x + rx + (y + ry) * image.width] >> 16)
                                    & 0xff;
                        }
                    }
                    // map domainblock pixel values to domainblock index

                    DomainBlock domainBlock = new DomainBlock(codebuchblock, false);
                    codebuch[i] = domainBlock;

                    i++;
                }
            }
        }
        return codebuch;
    }

    /**
     * Gets a colored RasterImage and returns a 2D of array containing a codebook.
     */
    private static DomainBlock[] createCodebuchRGB(RasterImage image) {
        // scale image by factor 2
        image = scaleImageRGB(image);
        int abstand = blockSize / 4;

        // generated codebook size
        DomainBlock[] codebuch = new DomainBlock[(image.width / abstand - 3) * (image.height / abstand - 3)];

        int i = 0;

        // iterate image
        for (int y = 0; y < image.height; y += abstand) {
            for (int x = 0; x < image.width; x += abstand) {
                int[] codebuchblock = new int[blockSize * blockSize];
                // iterate domainblock
                if (y + blockSize <= image.height && x + blockSize <= image.width) {
                    for (int ry = 0; ry < blockSize; ry++) {
                        for (int rx = 0; rx < blockSize; rx++) {

                            int valueR = (image.argb[x + rx + (y + ry) * image.width] >> 16) & 0xff;
                            int valueG = (image.argb[x + rx + (y + ry) * image.width] >> 8) & 0xff;
                            int valueB = image.argb[x + rx + (y + ry) * image.width] & 0xff;

                            codebuchblock[rx + ry * blockSize] = 0xff000000 | (valueR << 16) | (valueG << 8)
                                    | valueB;
                        }
                    }
                    // map domainblock pixel values to domainblock index
                    DomainBlock domainBlock = new DomainBlock(codebuchblock, true);
                    codebuch[i] = domainBlock;
                    i++;
                }
            }
        }
        return codebuch;
    }

    /**
     * Gets a RasterImage and displays the codebook image generated by it.
     */
    public static RasterImage showCodebuch(RasterImage image) {
        int rangebloeckePerWidth = image.width / blockSize;
        int rangebloeckePerHeight = image.height / blockSize;

        // calculate domainblock per dimension
        int domainbloeckePerWidth = rangebloeckePerWidth * 2 - 3;
        int domainbloeckePerHeight = rangebloeckePerHeight * 2 - 3;
        // generate codebook
        DomainBlock[] codebuch = createCodebuchRGB(image);
        int i = 0;

        // generate image to display
        RasterImage codebuchImage = new RasterImage(domainbloeckePerWidth * blockSize + domainbloeckePerWidth,
                domainbloeckePerHeight * blockSize + domainbloeckePerHeight);

        // iterate image
        for (int y = 0; y < codebuchImage.height; y += blockSize + 1) {
            for (int x = 0; x < codebuchImage.width; x += blockSize + 1) {
                for (int ry = 0; ry < blockSize && y + ry < codebuchImage.height; ry++) {
                    for (int rx = 0; rx < blockSize && x + rx < codebuchImage.width; rx++) {
                        int valueR = (codebuch[i].argb[rx + ry * blockSize] >> 16) & 0xff;
                        int valueG = (codebuch[i].argb[rx + ry * blockSize] >> 8) & 0xff;
                        int valueB = codebuch[i].argb[rx + ry * blockSize] & 0xff;

                        codebuchImage.argb[x + rx + (y + ry) * codebuchImage.width] = 0xff000000 | (valueR << 16)
                                | (valueG << 8) | valueB;
                    }
                }
                i++;
            }
        }
        return codebuchImage;
    }

    /**
     * Generates a grey RasterImage from given width and height.
     */
    public static RasterImage generateGrayImage(int width, int height) {
        RasterImage image = new RasterImage(width, height);
        Arrays.fill(image.argb, 0xff000000 | (128 << 16) | (128 << 8) | 128);
        return image;
    }
}