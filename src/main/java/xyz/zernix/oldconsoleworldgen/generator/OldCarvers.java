package xyz.zernix.oldconsoleworldgen.generator;

abstract class OldLargeFeature {
    protected final int radius = 8;
    protected final OldJavaRandom random = new OldJavaRandom(0L);
    protected long seed;

    void apply(long seed, int chunkX, int chunkZ, OldChunkBuffer buffer) {
        this.seed = seed;
        random.setSeed(seed);
        long xScale = random.nextLong();
        long zScale = random.nextLong();
        for (int x = chunkX - radius; x <= chunkX + radius; x++) {
            for (int z = chunkZ - radius; z <= chunkZ + radius; z++) {
                random.setSeed((x * xScale) ^ (z * zScale) ^ seed);
                addFeature(x, z, chunkX, chunkZ, buffer);
            }
        }
    }

    protected abstract void addFeature(int startChunkX, int startChunkZ, int targetChunkX, int targetChunkZ, OldChunkBuffer buffer);
}

final class OldLargeCaveFeature extends OldLargeFeature {
    @Override
    protected void addFeature(int startChunkX, int startChunkZ, int targetChunkX, int targetChunkZ, OldChunkBuffer buffer) {
        int caves = random.nextInt(random.nextInt(random.nextInt(40) + 1) + 1);
        if (random.nextInt(15) != 0) {
            caves = 0;
        }

        for (int cave = 0; cave < caves; cave++) {
            double xCave = startChunkX * 16.0 + random.nextInt(16);
            double yCave = random.nextInt(random.nextInt(OldChunkBuffer.GEN_DEPTH - 8) + 8);
            double zCave = startChunkZ * 16.0 + random.nextInt(16);

            int tunnels = 1;
            if (random.nextInt(4) == 0) {
                addRoom(random.nextLong(), targetChunkX, targetChunkZ, buffer, xCave, yCave, zCave);
                tunnels += random.nextInt(4);
            }

            for (int i = 0; i < tunnels; i++) {
                float yRot = random.nextFloat() * (float) Math.PI * 2.0f;
                float xRot = ((random.nextFloat() - 0.5f) * 2.0f) / 8.0f;
                float thickness = random.nextFloat() * 2.0f + random.nextFloat();
                if (random.nextInt(10) == 0) {
                    thickness *= random.nextFloat() * random.nextFloat() * 3.0f + 1.0f;
                }
                addTunnel(random.nextLong(), targetChunkX, targetChunkZ, buffer, xCave, yCave, zCave, thickness, yRot, xRot, 0, 0, 1.0);
            }
        }
    }

    private void addRoom(long seed, int chunkX, int chunkZ, OldChunkBuffer buffer, double xRoom, double yRoom, double zRoom) {
        addTunnel(seed, chunkX, chunkZ, buffer, xRoom, yRoom, zRoom, 1.0f + random.nextFloat() * 6.0f, 0.0f, 0.0f, -1, -1, 0.5);
    }

    private void addTunnel(long tunnelSeed, int chunkX, int chunkZ, OldChunkBuffer buffer, double xCave, double yCave, double zCave,
                           float thickness, float yRot, float xRot, int step, int dist, double yScale) {
        double xMid = chunkX * 16.0 + 8.0;
        double zMid = chunkZ * 16.0 + 8.0;

        float yRota = 0.0f;
        float xRota = 0.0f;
        OldJavaRandom tunnelRandom = new OldJavaRandom(tunnelSeed);

        if (dist <= 0) {
            int max = radius * 16 - 16;
            dist = max - tunnelRandom.nextInt(max / 4);
        }

        boolean singleStep = false;
        if (step == -1) {
            step = dist / 2;
            singleStep = true;
        }

        int splitPoint = tunnelRandom.nextInt(dist / 2) + dist / 4;
        boolean steep = tunnelRandom.nextInt(6) == 0;

        for (; step < dist; step++) {
            double rad = 1.5 + Math.sin(step * Math.PI / dist) * thickness;
            double yRad = rad * yScale;

            float xc = (float) Math.cos(xRot);
            float xs = (float) Math.sin(xRot);
            xCave += Math.cos(yRot) * xc;
            yCave += xs;
            zCave += Math.sin(yRot) * xc;

            xRot *= steep ? 0.92f : 0.7f;
            xRot += xRota * 0.1f;
            yRot += yRota * 0.1f;
            xRota *= 0.90f;
            yRota *= 0.75f;
            xRota += (tunnelRandom.nextFloat() - tunnelRandom.nextFloat()) * tunnelRandom.nextFloat() * 2.0f;
            yRota += (tunnelRandom.nextFloat() - tunnelRandom.nextFloat()) * tunnelRandom.nextFloat() * 4.0f;

            if (!singleStep && step == splitPoint && thickness > 1.0f && dist > 0) {
                addTunnel(tunnelRandom.nextLong(), chunkX, chunkZ, buffer, xCave, yCave, zCave,
                        tunnelRandom.nextFloat() * 0.5f + 0.5f, yRot - (float) Math.PI / 2.0f, xRot / 3.0f, step, dist, 1.0);
                addTunnel(tunnelRandom.nextLong(), chunkX, chunkZ, buffer, xCave, yCave, zCave,
                        tunnelRandom.nextFloat() * 0.5f + 0.5f, yRot + (float) Math.PI / 2.0f, xRot / 3.0f, step, dist, 1.0);
                return;
            }

            if (!singleStep && tunnelRandom.nextInt(4) == 0) {
                continue;
            }

            double xd = xCave - xMid;
            double zd = zCave - zMid;
            double remaining = dist - step;
            double rr = thickness + 18.0;
            if (xd * xd + zd * zd - remaining * remaining > rr * rr) {
                return;
            }

            if (xCave < xMid - 16.0 - rad * 2.0 || zCave < zMid - 16.0 - rad * 2.0
                    || xCave > xMid + 16.0 + rad * 2.0 || zCave > zMid + 16.0 + rad * 2.0) {
                continue;
            }

            int x0 = Math.max(0, floor(xCave - rad) - chunkX * 16 - 1);
            int x1 = Math.min(16, floor(xCave + rad) - chunkX * 16 + 1);
            int y0 = Math.max(1, floor(yCave - yRad) - 1);
            int y1 = Math.min(OldChunkBuffer.GEN_DEPTH - 8, floor(yCave + yRad) + 1);
            int z0 = Math.max(0, floor(zCave - rad) - chunkZ * 16 - 1);
            int z1 = Math.min(16, floor(zCave + rad) - chunkZ * 16 + 1);

            if (detectWater(buffer, x0, x1, y0, y1, z0, z1)) {
                continue;
            }

            carveEllipsoid(buffer, chunkX, chunkZ, xCave, yCave, zCave, rad, yRad, x0, x1, y0, y1, z0, z1, false, null);
            if (singleStep) {
                break;
            }
        }
    }

    static void carveEllipsoid(OldChunkBuffer buffer, int chunkX, int chunkZ, double xCave, double yCave, double zCave,
                               double rad, double yRad, int x0, int x1, int y0, int y1, int z0, int z1,
                               boolean canyon, float[] rs) {
        for (int xx = x0; xx < x1; xx++) {
            double xd = ((xx + chunkX * 16 + 0.5) - xCave) / rad;
            for (int zz = z0; zz < z1; zz++) {
                double zd = ((zz + chunkZ * 16 + 0.5) - zCave) / rad;
                boolean hasGrass = false;
                if (xd * xd + zd * zd >= 1.0) {
                    continue;
                }

                for (int yy = y1 - 1; yy >= y0; yy--) {
                    double yd = (yy + 0.5 - yCave) / yRad;
                    double test = canyon ? ((xd * xd + zd * zd) * rs[yy] + (yd * yd / 6.0)) : (xd * xd + yd * yd + zd * zd);
                    if ((!canyon && yd > -0.7 && test < 1.0) || (canyon && test < 1.0)) {
                        int block = buffer.getBlock(xx, yy, zz);
                        if (block == OldBlockIds.GRASS) {
                            hasGrass = true;
                        }
                        if (block == OldBlockIds.STONE || block == OldBlockIds.DIRT || block == OldBlockIds.GRASS) {
                            if (yy < 10) {
                                buffer.setBlock(xx, yy, zz, OldBlockIds.LAVA);
                            } else {
                                buffer.setBlock(xx, yy, zz, OldBlockIds.AIR);
                                if (hasGrass && yy > 0 && buffer.getBlock(xx, yy - 1, zz) == OldBlockIds.DIRT) {
                                    buffer.setBlock(xx, yy - 1, zz, buffer.getBiome(xx, zz).topBlockId());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static boolean detectWater(OldChunkBuffer buffer, int x0, int x1, int y0, int y1, int z0, int z1) {
        for (int xx = x0; xx < x1; xx++) {
            for (int zz = z0; zz < z1; zz++) {
                for (int yy = y1 + 1; yy >= y0 - 1; yy--) {
                    if (yy < 0 || yy >= OldChunkBuffer.GEN_DEPTH) {
                        continue;
                    }
                    int block = buffer.getBlock(xx, yy, zz);
                    if (block == OldBlockIds.WATER) {
                        return true;
                    }
                    if (yy != y0 - 1 && xx != x0 && xx != x1 - 1 && zz != z0 && zz != z1 - 1) {
                        yy = y0;
                    }
                }
            }
        }
        return false;
    }

    private static int floor(double value) {
        int floor = (int) value;
        return value < floor ? floor - 1 : floor;
    }
}

final class OldCanyonFeature extends OldLargeFeature {
    private final float[] rs = new float[OldChunkBuffer.GEN_DEPTH];

    @Override
    protected void addFeature(int startChunkX, int startChunkZ, int targetChunkX, int targetChunkZ, OldChunkBuffer buffer) {
        if (random.nextInt(50) != 0) {
            return;
        }

        double xCave = startChunkX * 16.0 + random.nextInt(16);
        double yCave = random.nextInt(random.nextInt(40) + 8) + 20;
        double zCave = startChunkZ * 16.0 + random.nextInt(16);
        float yRot = random.nextFloat() * (float) Math.PI * 2.0f;
        float xRot = ((random.nextFloat() - 0.5f) * 2.0f) / 8.0f;
        float thickness = (random.nextFloat() * 2.0f + random.nextFloat()) * 2.0f;
        addTunnel(random.nextLong(), targetChunkX, targetChunkZ, buffer, xCave, yCave, zCave, thickness, yRot, xRot, 0, 0, 3.0);
    }

    private void addTunnel(long tunnelSeed, int chunkX, int chunkZ, OldChunkBuffer buffer, double xCave, double yCave, double zCave,
                           float thickness, float yRot, float xRot, int step, int dist, double yScale) {
        OldJavaRandom tunnelRandom = new OldJavaRandom(tunnelSeed);
        double xMid = chunkX * 16.0 + 8.0;
        double zMid = chunkZ * 16.0 + 8.0;
        float yRota = 0.0f;
        float xRota = 0.0f;

        if (dist <= 0) {
            int max = radius * 16 - 16;
            dist = max - tunnelRandom.nextInt(max / 4);
        }

        boolean singleStep = false;
        if (step == -1) {
            step = dist / 2;
            singleStep = true;
        }

        float f = 1.0f;
        for (int i = 0; i < OldChunkBuffer.GEN_DEPTH; i++) {
            if (i == 0 || tunnelRandom.nextInt(3) == 0) {
                f = 1.0f + (tunnelRandom.nextFloat() * tunnelRandom.nextFloat());
            }
            rs[i] = f * f;
        }

        for (; step < dist; step++) {
            double rad = 1.5 + Math.sin(step * Math.PI / dist) * thickness;
            double yRad = rad * yScale;

            rad *= tunnelRandom.nextFloat() * 0.25 + 0.75;
            yRad *= tunnelRandom.nextFloat() * 0.25 + 0.75;

            float xc = (float) Math.cos(xRot);
            float xs = (float) Math.sin(xRot);
            xCave += Math.cos(yRot) * xc;
            yCave += xs;
            zCave += Math.sin(yRot) * xc;

            xRot *= 0.7f;
            xRot += xRota * 0.05f;
            yRot += yRota * 0.05f;
            xRota *= 0.80f;
            yRota *= 0.50f;
            xRota += (tunnelRandom.nextFloat() - tunnelRandom.nextFloat()) * tunnelRandom.nextFloat() * 2.0f;
            yRota += (tunnelRandom.nextFloat() - tunnelRandom.nextFloat()) * tunnelRandom.nextFloat() * 4.0f;

            if (!singleStep && tunnelRandom.nextInt(4) == 0) {
                continue;
            }

            double xd = xCave - xMid;
            double zd = zCave - zMid;
            double remaining = dist - step;
            double rr = thickness + 18.0;
            if (xd * xd + zd * zd - remaining * remaining > rr * rr) {
                return;
            }

            if (xCave < xMid - 16.0 - rad * 2.0 || zCave < zMid - 16.0 - rad * 2.0
                    || xCave > xMid + 16.0 + rad * 2.0 || zCave > zMid + 16.0 + rad * 2.0) {
                continue;
            }

            int x0 = Math.max(0, floor(xCave - rad) - chunkX * 16 - 1);
            int x1 = Math.min(16, floor(xCave + rad) - chunkX * 16 + 1);
            int y0 = Math.max(1, floor(yCave - yRad) - 1);
            int y1 = Math.min(OldChunkBuffer.GEN_DEPTH - 8, floor(yCave + yRad) + 1);
            int z0 = Math.max(0, floor(zCave - rad) - chunkZ * 16 - 1);
            int z1 = Math.min(16, floor(zCave + rad) - chunkZ * 16 + 1);

            if (OldLargeCaveFeature.detectWater(buffer, x0, x1, y0, y1, z0, z1)) {
                continue;
            }

                OldLargeCaveFeature.carveEllipsoid(buffer, chunkX, chunkZ, xCave, yCave, zCave, rad, yRad, x0, x1, y0, y1, z0, z1, true, rs);
                if (singleStep) {
                    break;
                }
        }
    }

    private static int floor(double value) {
        int floor = (int) value;
        return value < floor ? floor - 1 : floor;
    }
}
