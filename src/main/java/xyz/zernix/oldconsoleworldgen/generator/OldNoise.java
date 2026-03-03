package xyz.zernix.oldconsoleworldgen.generator;

final class OldJavaRandom {
    private long seed;
    private boolean haveNextNextGaussian;
    private double nextNextGaussian;

    OldJavaRandom(long seed) {
        setSeed(seed);
    }

    void setSeed(long seed) {
        this.seed = (seed ^ 0x5DEECE66DL) & ((1L << 48) - 1);
        haveNextNextGaussian = false;
    }

    int nextInt() {
        return next(32);
    }

    int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        if ((bound & -bound) == bound) {
            return (int) ((bound * (long) next(31)) >> 31);
        }

        int bits;
        int value;
        do {
            bits = next(31);
            value = bits % bound;
        } while (bits - value + (bound - 1) < 0);
        return value;
    }

    long nextLong() {
        return ((long) next(32) << 32) + next(32);
    }

    float nextFloat() {
        return next(24) / (float) (1 << 24);
    }

    double nextDouble() {
        return (((long) next(26) << 27) + next(27)) / (double) (1L << 53);
    }

    private int next(int bits) {
        seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
        return (int) (seed >>> (48 - bits));
    }
}

final class OldImprovedNoise {
    private final int[] p = new int[512];
    private final double xo;
    private final double yo;
    private final double zo;

    OldImprovedNoise(OldJavaRandom random) {
        xo = random.nextDouble() * 256.0;
        yo = random.nextDouble() * 256.0;
        zo = random.nextDouble() * 256.0;

        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }

        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256 - i) + i;
            int value = p[i];
            p[i] = p[j];
            p[j] = value;
            p[i + 256] = p[i];
        }
    }

    void add(double[] buffer, double x, double y, double z, int xSize, int ySize, int zSize, double xs, double ys, double zs, double pow) {
        if (ySize == 1) {
            int index = 0;
            double scale = 1.0 / pow;
            for (int xx = 0; xx < xSize; xx++) {
                double xPos = x + xx * xs + xo;
                int xf = floor(xPos);
                int X = xf & 255;
                xPos -= xf;
                double u = fade(xPos);

                for (int zz = 0; zz < zSize; zz++) {
                    double zPos = z + zz * zs + zo;
                    int zf = floor(zPos);
                    int Z = zf & 255;
                    zPos -= zf;
                    double w = fade(zPos);

                    int A = p[X];
                    int AA = p[A] + Z;
                    int B = p[X + 1];
                    int BA = p[B] + Z;

                    double vv0 = lerp(u, grad2(p[AA], xPos, zPos), grad(p[BA], xPos - 1.0, 0.0, zPos));
                    double vv2 = lerp(u, grad(p[AA + 1], xPos, 0.0, zPos - 1.0), grad(p[BA + 1], xPos - 1.0, 0.0, zPos - 1.0));
                    buffer[index++] += lerp(w, vv0, vv2) * scale;
                }
            }
            return;
        }

        int index = 0;
        double scale = 1.0 / pow;
        int yOld = Integer.MIN_VALUE;
        int A = 0;
        int AA = 0;
        int AB = 0;
        int B = 0;
        int BA = 0;
        int BB = 0;
        double vv0 = 0.0;
        double vv1 = 0.0;
        double vv2 = 0.0;
        double vv3 = 0.0;

        for (int xx = 0; xx < xSize; xx++) {
            double xPos = x + xx * xs + xo;
            int xf = floor(xPos);
            int X = xf & 255;
            xPos -= xf;
            double u = fade(xPos);

            for (int zz = 0; zz < zSize; zz++) {
                double zPos = z + zz * zs + zo;
                int zf = floor(zPos);
                int Z = zf & 255;
                zPos -= zf;
                double w = fade(zPos);

                for (int yy = 0; yy < ySize; yy++) {
                    double yPos = y + yy * ys + yo;
                    int yf = floor(yPos);
                    int Y = yf & 255;
                    yPos -= yf;
                    double v = fade(yPos);

                    if (yy == 0 || Y != yOld) {
                        yOld = Y;
                        A = p[X] + Y;
                        AA = p[A] + Z;
                        AB = p[A + 1] + Z;
                        B = p[X + 1] + Y;
                        BA = p[B] + Z;
                        BB = p[B + 1] + Z;
                        vv0 = lerp(u, grad(p[AA], xPos, yPos, zPos), grad(p[BA], xPos - 1.0, yPos, zPos));
                        vv1 = lerp(u, grad(p[AB], xPos, yPos - 1.0, zPos), grad(p[BB], xPos - 1.0, yPos - 1.0, zPos));
                        vv2 = lerp(u, grad(p[AA + 1], xPos, yPos, zPos - 1.0), grad(p[BA + 1], xPos - 1.0, yPos, zPos - 1.0));
                        vv3 = lerp(u, grad(p[AB + 1], xPos, yPos - 1.0, zPos - 1.0), grad(p[BB + 1], xPos - 1.0, yPos - 1.0, zPos - 1.0));
                    }

                    double value0 = lerp(v, vv0, vv1);
                    double value1 = lerp(v, vv2, vv3);
                    buffer[index++] += lerp(w, value0, value1) * scale;
                }
            }
        }
    }

    private static int floor(double value) {
        int floor = (int) value;
        return value < floor ? floor - 1 : floor;
    }

    private static double fade(double value) {
        return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad2(int hash, double x, double z) {
        int h = hash & 15;
        double u = (1 - ((h & 8) >> 3)) * x;
        double v = h < 4 ? 0.0 : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}

final class OldPerlinNoise {
    private final OldImprovedNoise[] noiseLevels;

    OldPerlinNoise(OldJavaRandom random, int levels) {
        noiseLevels = new OldImprovedNoise[levels];
        for (int i = 0; i < levels; i++) {
            noiseLevels[i] = new OldImprovedNoise(random);
        }
    }

    double[] getRegion(double[] buffer, int x, int y, int z, int xSize, int ySize, int zSize, double xScale, double yScale, double zScale) {
        if (buffer == null || buffer.length < xSize * ySize * zSize) {
            buffer = new double[xSize * ySize * zSize];
        } else {
            java.util.Arrays.fill(buffer, 0.0);
        }

        double pow = 1.0;
        for (OldImprovedNoise noiseLevel : noiseLevels) {
            double xx = x * pow * xScale;
            double yy = y * pow * yScale;
            double zz = z * pow * zScale;

            long xb = lfloor(xx);
            long zb = lfloor(zz);
            xx -= xb;
            zz -= zb;
            xb %= 16777216L;
            zb %= 16777216L;
            xx += xb;
            zz += zb;

            noiseLevel.add(buffer, xx, yy, zz, xSize, ySize, zSize, xScale * pow, yScale * pow, zScale * pow, pow);
            pow /= 2.0;
        }

        return buffer;
    }

    double[] getRegion(double[] buffer, int x, int z, int xSize, int zSize, double xScale, double zScale, double ignoredPow) {
        return getRegion(buffer, x, 10, z, xSize, 1, zSize, xScale, 1.0, zScale);
    }

    private static long lfloor(double value) {
        long floor = (long) value;
        return value < floor ? floor - 1L : floor;
    }
}
