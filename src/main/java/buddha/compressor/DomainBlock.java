package buddha.compressor;

public class DomainBlock {
    public int[] argb;
    public int average;

    public float variance;

    public int averageR;
    public int averageG;
    public int averageB;

    public float varianceR;
    public float varianceG;
    public float varianceB;

    public DomainBlock(int[] argb, boolean rgb) {
        this.argb = argb;
        this.average = average(argb);

        if (!rgb) {
            this.variance = variance(average, argb);
            return;
        }

        this.averageR = average(extractRGB(argb, 0));
        this.varianceR = variance(averageR, extractRGB(argb, 0));

        this.averageG = average(extractRGB(argb, 1));
        this.varianceG = variance(averageG, extractRGB(argb, 1));

        this.averageB = average(extractRGB(argb, 2));
        this.varianceB = variance(averageB, extractRGB(argb, 2));
    }

    public int[] extractRGB(int[] argb, int canal) {
        var temp = new int[argb.length];

        switch (canal) {
            // Red
            case 0 -> {
                for (int i = 0; i < argb.length; i++)
                    temp[i] = (argb[i] >> 16) & 0xff;
            }

            // Green
            case 1 -> {
                for (int i = 0; i < argb.length; i++)
                    temp[i] = (argb[i] >> 8) & 0xff;
            }

            // Blue
            case 2 -> {
                for (int i = 0; i < argb.length; i++)
                    temp[i] = argb[i] & 0xff;
            }
        }

        return temp;
    }

    public int average(int[] values) {
        int sum = 0;
        for (int value : values)
            sum += value;

        return sum / values.length;
    }

    public float variance(int average, int[] block) {
        float variantsDomain = 0;
        for (int value : block) {
            float grey = value - average;
            variantsDomain += grey * grey;
        }

        return variantsDomain;
    }
}