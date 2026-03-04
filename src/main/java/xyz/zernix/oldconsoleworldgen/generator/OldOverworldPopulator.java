package xyz.zernix.oldconsoleworldgen.generator;

import org.allaymc.api.block.property.enums.PillarAxis;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.world.chunk.UnsafeChunk;
import org.allaymc.api.world.generator.context.PopulateContext;
import org.allaymc.api.world.generator.function.Populator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class OldOverworldPopulator implements Populator {
    private final GeneratorConfig config;
    private final OldBiomeSource biomeSource;

    public OldOverworldPopulator(GeneratorConfig config) {
        this.config = config;
        this.biomeSource = new OldBiomeSource(config.seed(), config.largeBiomes());
    }

    @Override
    public synchronized boolean apply(PopulateContext context) {
        int chunkX = context.getCurrentChunk().getX();
        int chunkZ = context.getCurrentChunk().getZ();
        int xo = chunkX * 16;
        int zo = chunkZ * 16;

        OldJavaRandom random = createChunkRandom(chunkX, chunkZ);
        OldWorldAccess world = new OldWorldAccess(context, config.seaLevel(), biomeSource);
        OldBiome biome = biomeSource.getBiome(xo + 16, zo + 16);
        generateLakes(world, random, xo, zo);
        consumeMonsterRoomAttempts(random, xo, zo);
        decorateBiome(world, random, biome, xo, zo);
        return true;
    }

    private OldJavaRandom createChunkRandom(int chunkX, int chunkZ) {
        OldJavaRandom random = new OldJavaRandom(config.seed());
        long xScale = (random.nextLong() / 2L) * 2L + 1L;
        long zScale = (random.nextLong() / 2L) * 2L + 1L;
        random.setSeed((((long) chunkX * xScale) + ((long) chunkZ * zScale)) ^ config.seed());
        return random;
    }

    private void generateLakes(OldWorldAccess world, OldJavaRandom random, int xo, int zo) {
        if (random.nextInt(4) == 0) {
            int x = xo + random.nextInt(16) + 8;
            int y = random.nextInt(OldChunkBuffer.GEN_DEPTH);
            int z = zo + random.nextInt(16) + 8;
            new OldLakeFeature(OldWorldAccess.WATER).place(world, random, x, y, z);
        }

        if (random.nextInt(8) == 0) {
            int x = xo + random.nextInt(16) + 8;
            int y = random.nextInt(random.nextInt(OldChunkBuffer.GEN_DEPTH - 8) + 8);
            int z = zo + random.nextInt(16) + 8;
            if (y < world.seaLevel() || random.nextInt(10) == 0) {
                new OldLakeFeature(OldWorldAccess.LAVA).place(world, random, x, y, z);
            }
        }
    }

    private void consumeMonsterRoomAttempts(OldJavaRandom random, int xo, int zo) {
        for (int i = 0; i < 8; i++) {
            random.nextInt(16);
            random.nextInt(OldChunkBuffer.GEN_DEPTH);
            random.nextInt(16);
        }
    }

    private void decorateBiome(OldWorldAccess world, OldJavaRandom random, OldBiome biome, int xo, int zo) {
        OldDecoratorConfig decorator = OldDecoratorConfig.forBiome(biome);
        decorateOres(world, random, biome, xo, zo);

        OldFeature sandFeature = new OldBlobReplaceFeature(7, OldWorldAccess.SAND, Set.of(BlockTypes.DIRT, BlockTypes.GRASS_BLOCK));
        OldFeature clayFeature = new OldBlobReplaceFeature(4, OldWorldAccess.CLAY, Set.of(BlockTypes.DIRT, BlockTypes.CLAY));
        OldFeature gravelFeature = new OldBlobReplaceFeature(6, OldWorldAccess.GRAVEL, Set.of(BlockTypes.DIRT, BlockTypes.GRASS_BLOCK));

        for (int i = 0; i < decorator.sandCount(); i++) {
            int x = xo + random.nextInt(16) + 8;
            int z = zo + random.nextInt(16) + 8;
            sandFeature.place(world, random, x, world.getTopSolidBlock(x, z), z);
        }

        for (int i = 0; i < decorator.clayCount(); i++) {
            int x = xo + random.nextInt(16) + 8;
            int z = zo + random.nextInt(16) + 8;
            clayFeature.place(world, random, x, world.getTopSolidBlock(x, z), z);
        }

        for (int i = 0; i < decorator.gravelCount(); i++) {
            int x = xo + random.nextInt(16) + 8;
            int z = zo + random.nextInt(16) + 8;
            gravelFeature.place(world, random, x, world.getTopSolidBlock(x, z), z);
        }

        int forests = decorator.treeCount();
        if (random.nextInt(10) == 0) {
            forests += 1;
        }

        for (int i = 0; i < forests; i++) {
            int x = xo + random.nextInt(16) + 8;
            int z = zo + random.nextInt(16) + 8;
            selectTreeFeature(biome, random).place(world, random, x, world.getHeightmap(x, z), z);
        }

        for (int i = 0; i < decorator.hugeMushrooms(); i++) {
            int x = xo + random.nextInt(16) + 8;
            int z = zo + random.nextInt(16) + 8;
            new OldHugeMushroomFeature().place(world, random, x, world.getHeightmap(x, z), z);
        }

        OldFeature dandelion = new OldPlantScatterFeature(OldWorldAccess.DANDELION, 64, OldPlantSupport.GRASSY);
        OldFeature poppy = new OldPlantScatterFeature(OldWorldAccess.POPPY, 64, OldPlantSupport.GRASSY);
        OldFeature brownMushroom = new OldPlantScatterFeature(OldWorldAccess.BROWN_MUSHROOM, 64, OldPlantSupport.MUSHROOM);
        OldFeature redMushroom = new OldPlantScatterFeature(OldWorldAccess.RED_MUSHROOM, 64, OldPlantSupport.MUSHROOM);
        OldFeature deadBush = new OldPlantScatterFeature(OldWorldAccess.DEADBUSH, 64, OldPlantSupport.DEAD_BUSH);

        for (int i = 0; i < decorator.flowerCount(); i++) {
            int x = xo + random.nextInt(16) + 8;
            int y = random.nextInt(OldChunkBuffer.GEN_DEPTH);
            int z = zo + random.nextInt(16) + 8;
            dandelion.place(world, random, x, y, z);

            if (random.nextInt(4) == 0) {
                x = xo + random.nextInt(16) + 8;
                y = random.nextInt(OldChunkBuffer.GEN_DEPTH);
                z = zo + random.nextInt(16) + 8;
                poppy.place(world, random, x, y, z);
            }
        }

        for (int i = 0; i < decorator.grassCount(); i++) {
            int x = xo + random.nextInt(16) + 8;
            int y = random.nextInt(OldChunkBuffer.GEN_DEPTH);
            int z = zo + random.nextInt(16) + 8;
            selectGrassFeature(biome, random).place(world, random, x, y, z);
        }

        for (int i = 0; i < decorator.deadBushCount(); i++) {
            int x = xo + random.nextInt(16) + 8;
            int y = random.nextInt(OldChunkBuffer.GEN_DEPTH);
            int z = zo + random.nextInt(16) + 8;
            deadBush.place(world, random, x, y, z);
        }

        OldFeature waterlilyFeature = new OldWaterlilyFeature();
        for (int i = 0; i < decorator.waterlilyCount(); i++) {
            int x = xo + random.nextInt(16) + 8;
            int z = zo + random.nextInt(16) + 8;
            int y = random.nextInt(OldChunkBuffer.GEN_DEPTH);
            while (y > 0 && world.isAir(x, y - 1, z)) {
                y--;
            }
            waterlilyFeature.place(world, random, x, y, z);
        }

        for (int i = 0; i < decorator.mushroomCount(); i++) {
            if (random.nextInt(4) == 0) {
                int x = xo + random.nextInt(16) + 8;
                int z = zo + random.nextInt(16) + 8;
                brownMushroom.place(world, random, x, world.getHeightmap(x, z), z);
            }
            if (random.nextInt(8) == 0) {
                int x = xo + random.nextInt(16) + 8;
                int y = random.nextInt(OldChunkBuffer.GEN_DEPTH);
                int z = zo + random.nextInt(16) + 8;
                redMushroom.place(world, random, x, y, z);
            }
        }

        if (random.nextInt(4) == 0) {
            int x = xo + random.nextInt(16) + 8;
            int y = random.nextInt(OldChunkBuffer.GEN_DEPTH);
            int z = zo + random.nextInt(16) + 8;
            brownMushroom.place(world, random, x, y, z);
        }

        if (random.nextInt(8) == 0) {
            int x = xo + random.nextInt(16) + 8;
            int y = random.nextInt(OldChunkBuffer.GEN_DEPTH);
            int z = zo + random.nextInt(16) + 8;
            redMushroom.place(world, random, x, y, z);
        }

        OldFeature reedsFeature = new OldReedsFeature();
        for (int i = 0; i < decorator.reedsCount(); i++) {
            int x = xo + random.nextInt(16) + 8;
            int z = zo + random.nextInt(16) + 8;
            int y = random.nextInt(OldChunkBuffer.GEN_DEPTH);
            reedsFeature.place(world, random, x, y, z);
        }

        for (int i = 0; i < 10; i++) {
            int x = xo + random.nextInt(16) + 8;
            int y = random.nextInt(OldChunkBuffer.GEN_DEPTH);
            int z = zo + random.nextInt(16) + 8;
            reedsFeature.place(world, random, x, y, z);
        }

        if (random.nextInt(32) == 0) {
            int x = xo + random.nextInt(16) + 8;
            int y = random.nextInt(OldChunkBuffer.GEN_DEPTH);
            int z = zo + random.nextInt(16) + 8;
            new OldPumpkinFeature().place(world, random, x, y, z);
        }

        OldFeature cactusFeature = new OldCactusFeature();
        for (int i = 0; i < decorator.cactusCount(); i++) {
            int x = xo + random.nextInt(16) + 8;
            int y = random.nextInt(OldChunkBuffer.GEN_DEPTH);
            int z = zo + random.nextInt(16) + 8;
            cactusFeature.place(world, random, x, y, z);
        }

        if (decorator.liquids()) {
            OldFeature waterSpring = new OldSpringFeature(OldWorldAccess.WATER);
            for (int i = 0; i < 50; i++) {
                int x = xo + random.nextInt(16) + 8;
                int y = random.nextInt(random.nextInt(OldChunkBuffer.GEN_DEPTH - 8) + 8);
                int z = zo + random.nextInt(16) + 8;
                waterSpring.place(world, random, x, y, z);
            }

            OldFeature lavaSpring = new OldSpringFeature(OldWorldAccess.LAVA);
            for (int i = 0; i < 20; i++) {
                int x = xo + random.nextInt(16) + 8;
                int y = random.nextInt(random.nextInt(random.nextInt(OldChunkBuffer.GEN_DEPTH - 16) + 8) + 8);
                int z = zo + random.nextInt(16) + 8;
                lavaSpring.place(world, random, x, y, z);
            }
        }

        if (decorator.jungleVines()) {
            OldFeature vines = new OldVinesFeature();
            for (int i = 0; i < 50; i++) {
                int x = xo + random.nextInt(16) + 8;
                int z = zo + random.nextInt(16) + 8;
                vines.place(world, random, x, OldChunkBuffer.GEN_DEPTH / 2, z);
            }
        }
    }

    private void decorateOres(OldWorldAccess world, OldJavaRandom random, OldBiome biome, int xo, int zo) {
        decorateDepthSpan(world, random, xo, zo, 20, new OldOreFeature(OldWorldAccess.DIRT, 32), 0, OldChunkBuffer.GEN_DEPTH);
        decorateDepthSpan(world, random, xo, zo, 10, new OldOreFeature(OldWorldAccess.GRAVEL, 32), 0, OldChunkBuffer.GEN_DEPTH);
        decorateDepthSpan(world, random, xo, zo, 20, new OldOreFeature(OldWorldAccess.COAL_ORE, 16), 0, OldChunkBuffer.GEN_DEPTH);
        decorateDepthSpan(world, random, xo, zo, 20, new OldOreFeature(OldWorldAccess.IRON_ORE, 8), 0, OldChunkBuffer.GEN_DEPTH / 2);
        decorateDepthSpan(world, random, xo, zo, 2, new OldOreFeature(OldWorldAccess.GOLD_ORE, 8), 0, OldChunkBuffer.GEN_DEPTH / 4);
        decorateDepthSpan(world, random, xo, zo, 8, new OldOreFeature(OldWorldAccess.REDSTONE_ORE, 7), 0, OldChunkBuffer.GEN_DEPTH / 8);
        decorateDepthSpan(world, random, xo, zo, 1, new OldOreFeature(OldWorldAccess.DIAMOND_ORE, 7), 0, OldChunkBuffer.GEN_DEPTH / 8);
        decorateDepthAverage(world, random, xo, zo, 1, new OldOreFeature(OldWorldAccess.LAPIS_ORE, 6), OldChunkBuffer.GEN_DEPTH / 8, OldChunkBuffer.GEN_DEPTH / 8);

        if (biome == OldBiome.EXTREME_HILLS || biome == OldBiome.EXTREME_HILLS_EDGE) {
            int emeraldCount = 3 + random.nextInt(6);
            for (int i = 0; i < emeraldCount; i++) {
                int x = xo + random.nextInt(16);
                int y = random.nextInt((OldChunkBuffer.GEN_DEPTH / 4) - 4) + 4;
                int z = zo + random.nextInt(16);
                if (world.getType(x, y, z) == BlockTypes.STONE) {
                    world.setBlockState(x, y, z, OldWorldAccess.EMERALD_ORE);
                }
            }
        }
    }

    private void decorateDepthSpan(OldWorldAccess world, OldJavaRandom random, int xo, int zo, int count, OldFeature feature, int y0, int y1) {
        for (int i = 0; i < count; i++) {
            int x = xo + random.nextInt(16);
            int y = random.nextInt(y1 - y0) + y0;
            int z = zo + random.nextInt(16);
            feature.place(world, random, x, y, z);
        }
    }

    private void decorateDepthAverage(OldWorldAccess world, OldJavaRandom random, int xo, int zo, int count, OldFeature feature, int yMid, int ySpan) {
        for (int i = 0; i < count; i++) {
            int x = xo + random.nextInt(16);
            int y = random.nextInt(ySpan) + random.nextInt(ySpan) + (yMid - ySpan);
            int z = zo + random.nextInt(16);
            feature.place(world, random, x, y, z);
        }
    }

    private OldFeature selectTreeFeature(OldBiome biome, OldJavaRandom random) {
        if (biome == OldBiome.FOREST || biome == OldBiome.FOREST_HILLS) {
            if (random.nextInt(5) == 0) {
                return new OldBirchTreeFeature();
            }
            if (random.nextInt(10) == 0) {
                return new OldFancyTreeFeature();
            }
            return new OldTreeFeature(4, OldWorldAccess.OAK_LOG, OldWorldAccess.OAK_LEAVES, false);
        }

        if (biome == OldBiome.TAIGA || biome == OldBiome.TAIGA_HILLS) {
            if (random.nextInt(3) == 0) {
                return new OldPineTreeFeature();
            }
            return new OldSpruceTreeFeature();
        }

        if (biome == OldBiome.SWAMPLAND) {
            return new OldSwampTreeFeature();
        }

        if (biome == OldBiome.JUNGLE || biome == OldBiome.JUNGLE_HILLS) {
            if (random.nextInt(10) == 0) {
                return new OldFancyTreeFeature();
            }
            if (random.nextInt(2) == 0) {
                return new OldGroundBushFeature(OldWorldAccess.JUNGLE_LOG, OldWorldAccess.OAK_LEAVES);
            }
            if (random.nextInt(3) == 0) {
                return new OldMegaJungleTreeFeature(10 + random.nextInt(20));
            }
            return new OldTreeFeature(4 + random.nextInt(7), OldWorldAccess.JUNGLE_LOG, OldWorldAccess.JUNGLE_LEAVES, true);
        }

        if (random.nextInt(10) == 0) {
            return new OldFancyTreeFeature();
        }
        return new OldTreeFeature(4, OldWorldAccess.OAK_LOG, OldWorldAccess.OAK_LEAVES, false);
    }

    private OldFeature selectGrassFeature(OldBiome biome, OldJavaRandom random) {
        if (biome == OldBiome.JUNGLE || biome == OldBiome.JUNGLE_HILLS) {
            if (random.nextInt(4) == 0) {
                return new OldTallGrassFeature(OldWorldAccess.FERN);
            }
        }
        return new OldTallGrassFeature(OldWorldAccess.TALL_GRASS);
    }
}

record OldDecoratorConfig(
        int waterlilyCount,
        int treeCount,
        int flowerCount,
        int grassCount,
        int deadBushCount,
        int mushroomCount,
        int reedsCount,
        int cactusCount,
        int gravelCount,
        int sandCount,
        int clayCount,
        int hugeMushrooms,
        boolean liquids,
        boolean jungleVines
) {
    static OldDecoratorConfig forBiome(OldBiome biome) {
        int waterlilyCount = 0;
        int treeCount = 0;
        int flowerCount = 2;
        int grassCount = 1;
        int deadBushCount = 0;
        int mushroomCount = 0;
        int reedsCount = 0;
        int cactusCount = 0;
        int gravelCount = 1;
        int sandCount = 3;
        int clayCount = 1;
        int hugeMushrooms = 0;
        boolean liquids = true;
        boolean jungleVines = false;

        if (biome == OldBiome.FOREST || biome == OldBiome.FOREST_HILLS) {
            treeCount = 10;
            grassCount = 2;
        } else if (biome == OldBiome.TAIGA || biome == OldBiome.TAIGA_HILLS) {
            treeCount = 10;
            grassCount = 1;
        } else if (biome == OldBiome.DESERT || biome == OldBiome.DESERT_HILLS) {
            treeCount = -999;
            deadBushCount = 2;
            reedsCount = 50;
            cactusCount = 10;
        } else if (biome == OldBiome.PLAINS) {
            treeCount = -999;
            flowerCount = 4;
            grassCount = 10;
        } else if (biome == OldBiome.SWAMPLAND) {
            treeCount = 2;
            flowerCount = -999;
            deadBushCount = 1;
            mushroomCount = 8;
            reedsCount = 10;
            clayCount = 1;
            waterlilyCount = 4;
        } else if (biome == OldBiome.MUSHROOM_ISLAND || biome == OldBiome.MUSHROOM_ISLAND_SHORE) {
            treeCount = -100;
            flowerCount = -100;
            grassCount = -100;
            mushroomCount = 1;
            hugeMushrooms = 1;
        } else if (biome == OldBiome.BEACH) {
            treeCount = -999;
            deadBushCount = 0;
            reedsCount = 0;
            cactusCount = 0;
        } else if (biome == OldBiome.JUNGLE || biome == OldBiome.JUNGLE_HILLS) {
            treeCount = 50;
            grassCount = 25;
            flowerCount = 4;
            jungleVines = true;
        }

        return new OldDecoratorConfig(
                waterlilyCount,
                treeCount,
                flowerCount,
                grassCount,
                deadBushCount,
                mushroomCount,
                reedsCount,
                cactusCount,
                gravelCount,
                sandCount,
                clayCount,
                hugeMushrooms,
                liquids,
                jungleVines
        );
    }
}

enum OldPlantSupport {
    GRASSY,
    MUSHROOM,
    DEAD_BUSH
}

interface OldFeature {
    boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z);
}

final class OldWorldAccess {
    static final BlockState AIR = BlockTypes.AIR.getDefaultState();
    static final BlockState STONE = BlockTypes.STONE.getDefaultState();
    static final BlockState DIRT = BlockTypes.DIRT.getDefaultState();
    static final BlockState GRASS = BlockTypes.GRASS_BLOCK.getDefaultState();
    static final BlockState WATER = BlockTypes.WATER.getDefaultState();
    static final BlockState LAVA = BlockTypes.LAVA.getDefaultState();
    static final BlockState SAND = BlockTypes.SAND.getDefaultState();
    static final BlockState GRAVEL = BlockTypes.GRAVEL.getDefaultState();
    static final BlockState CLAY = BlockTypes.CLAY.getDefaultState();
    static final BlockState COAL_ORE = BlockTypes.COAL_ORE.getDefaultState();
    static final BlockState IRON_ORE = BlockTypes.IRON_ORE.getDefaultState();
    static final BlockState GOLD_ORE = BlockTypes.GOLD_ORE.getDefaultState();
    static final BlockState REDSTONE_ORE = BlockTypes.REDSTONE_ORE.getDefaultState();
    static final BlockState DIAMOND_ORE = BlockTypes.DIAMOND_ORE.getDefaultState();
    static final BlockState LAPIS_ORE = BlockTypes.LAPIS_ORE.getDefaultState();
    static final BlockState EMERALD_ORE = BlockTypes.EMERALD_ORE.getDefaultState();
    static final BlockState DANDELION = BlockTypes.DANDELION.getDefaultState();
    static final BlockState POPPY = BlockTypes.POPPY.getDefaultState();
    static final BlockState BROWN_MUSHROOM = BlockTypes.BROWN_MUSHROOM.getDefaultState();
    static final BlockState RED_MUSHROOM = BlockTypes.RED_MUSHROOM.getDefaultState();
    static final BlockState DEADBUSH = BlockTypes.DEADBUSH.getDefaultState();
    static final BlockState TALL_GRASS = BlockTypes.TALL_GRASS.getDefaultState();
    static final BlockState FERN = BlockTypes.FERN.getDefaultState();
    static final BlockState REEDS = BlockTypes.REEDS.getDefaultState();
    static final BlockState CACTUS = BlockTypes.CACTUS.getDefaultState();
    static final BlockState WATERLILY = BlockTypes.WATERLILY.getDefaultState();
    static final BlockState PUMPKIN = BlockTypes.PUMPKIN.getDefaultState();
    static final BlockState BROWN_MUSHROOM_BLOCK = BlockTypes.BROWN_MUSHROOM_BLOCK.getDefaultState();
    static final BlockState RED_MUSHROOM_BLOCK = BlockTypes.RED_MUSHROOM_BLOCK.getDefaultState();
    static final BlockState MUSHROOM_STEM = BlockTypes.MUSHROOM_STEM.getDefaultState().setPropertyValue(BlockPropertyTypes.HUGE_MUSHROOM_BITS, 10);
    static final BlockState OAK_LOG = vertical(BlockTypes.OAK_LOG.getDefaultState());
    static final BlockState OAK_LOG_X = axis(BlockTypes.OAK_LOG.getDefaultState(), PillarAxis.X);
    static final BlockState OAK_LOG_Z = axis(BlockTypes.OAK_LOG.getDefaultState(), PillarAxis.Z);
    static final BlockState BIRCH_LOG = vertical(BlockTypes.BIRCH_LOG.getDefaultState());
    static final BlockState SPRUCE_LOG = vertical(BlockTypes.SPRUCE_LOG.getDefaultState());
    static final BlockState JUNGLE_LOG = vertical(BlockTypes.JUNGLE_LOG.getDefaultState());
    static final BlockState OAK_LEAVES = persistent(BlockTypes.OAK_LEAVES.getDefaultState());
    static final BlockState BIRCH_LEAVES = persistent(BlockTypes.BIRCH_LEAVES.getDefaultState());
    static final BlockState SPRUCE_LEAVES = persistent(BlockTypes.SPRUCE_LEAVES.getDefaultState());
    static final BlockState JUNGLE_LEAVES = persistent(BlockTypes.JUNGLE_LEAVES.getDefaultState());
    static final BlockState VINE_SOUTH = BlockTypes.VINE.getDefaultState().setPropertyValue(BlockPropertyTypes.VINE_DIRECTION_BITS, 1);
    static final BlockState VINE_WEST = BlockTypes.VINE.getDefaultState().setPropertyValue(BlockPropertyTypes.VINE_DIRECTION_BITS, 2);
    static final BlockState VINE_NORTH = BlockTypes.VINE.getDefaultState().setPropertyValue(BlockPropertyTypes.VINE_DIRECTION_BITS, 4);
    static final BlockState VINE_EAST = BlockTypes.VINE.getDefaultState().setPropertyValue(BlockPropertyTypes.VINE_DIRECTION_BITS, 8);

    private static final Set<BlockType<?>> LEAVES = Set.of(BlockTypes.OAK_LEAVES, BlockTypes.BIRCH_LEAVES, BlockTypes.SPRUCE_LEAVES, BlockTypes.JUNGLE_LEAVES);
    private static final Set<BlockType<?>> LOGS = Set.of(BlockTypes.OAK_LOG, BlockTypes.BIRCH_LOG, BlockTypes.SPRUCE_LOG, BlockTypes.JUNGLE_LOG);

    private final PopulateContext context;
    private final int seaLevel;
    private final OldBiomeSource biomeSource;

    OldWorldAccess(PopulateContext context, int seaLevel, OldBiomeSource biomeSource) {
        this.context = context;
        this.seaLevel = seaLevel;
        this.biomeSource = biomeSource;
    }

    int seaLevel() {
        return seaLevel;
    }

    OldBiome getBiome(int x, int z) {
        return biomeSource.getBiome(x, z);
    }

    BlockState getBlockState(int x, int y, int z) {
        if (y < 0 || y >= OldChunkBuffer.GEN_DEPTH) {
            return AIR;
        }
        return context.getBlockState(x, y, z);
    }

    BlockType<?> getType(int x, int y, int z) {
        return getBlockState(x, y, z).getBlockType();
    }

    void setBlockState(int x, int y, int z, BlockState state) {
        if (y < 0 || y >= OldChunkBuffer.GEN_DEPTH) {
            return;
        }
        context.setBlockState(x, y, z, state);
    }

    void scheduleFluidUpdate(int x, int y, int z) {
        if (y < 0 || y >= OldChunkBuffer.GEN_DEPTH) {
            return;
        }
        UnsafeChunk chunk = getChunk(x, z);
        if (chunk == null) {
            return;
        }
        int localX = x & 15;
        int localZ = z & 15;
        if (!chunk.hasScheduledUpdate(localX, y, localZ)) {
            chunk.addScheduledUpdate(localX, y, localZ, 0L);
        }
    }

    boolean isAir(int x, int y, int z) {
        return getType(x, y, z) == BlockTypes.AIR;
    }

    boolean isSolid(int x, int y, int z) {
        return getBlockState(x, y, z).getBlockStateData().isSolid();
    }

    boolean isLeaves(int x, int y, int z) {
        return isLeaves(getType(x, y, z));
    }

    boolean isWater(int x, int y, int z) {
        BlockType<?> type = getType(x, y, z);
        return type == BlockTypes.WATER || type == BlockTypes.FLOWING_WATER;
    }

    boolean isLava(int x, int y, int z) {
        BlockType<?> type = getType(x, y, z);
        return type == BlockTypes.LAVA || type == BlockTypes.FLOWING_LAVA;
    }

    boolean isLiquid(int x, int y, int z) {
        return isWater(x, y, z) || isLava(x, y, z);
    }

    boolean isReplaceableByTree(int x, int y, int z) {
        BlockType<?> type = getType(x, y, z);
        return type == BlockTypes.AIR || isLeaves(type) || type == BlockTypes.GRASS_BLOCK || type == BlockTypes.DIRT || isLog(type) || type == BlockTypes.REEDS;
    }

    boolean isGroundForTree(int x, int y, int z) {
        BlockType<?> type = getType(x, y, z);
        return type == BlockTypes.GRASS_BLOCK || type == BlockTypes.DIRT;
    }

    boolean canPlacePlant(int x, int y, int z, OldPlantSupport support) {
        if (!isAir(x, y, z)) {
            return false;
        }
        BlockType<?> below = getType(x, y - 1, z);
        return switch (support) {
            case GRASSY -> below == BlockTypes.GRASS_BLOCK || below == BlockTypes.DIRT;
            case MUSHROOM -> below == BlockTypes.MYCELIUM || below == BlockTypes.GRASS_BLOCK || below == BlockTypes.DIRT || getBlockState(x, y - 1, z).getBlockStateData().isSolid();
            case DEAD_BUSH -> below == BlockTypes.SAND;
        };
    }

    boolean canPlaceReeds(int x, int y, int z) {
        if (!isAir(x, y, z)) {
            return false;
        }
        BlockType<?> below = getType(x, y - 1, z);
        if (below != BlockTypes.GRASS_BLOCK && below != BlockTypes.DIRT && below != BlockTypes.SAND && below != BlockTypes.REEDS) {
            return false;
        }
        return isWater(x - 1, y - 1, z) || isWater(x + 1, y - 1, z) || isWater(x, y - 1, z - 1) || isWater(x, y - 1, z + 1);
    }

    boolean canPlaceCactus(int x, int y, int z) {
        if (!isAir(x, y, z)) {
            return false;
        }
        BlockType<?> below = getType(x, y - 1, z);
        if (below != BlockTypes.SAND && below != BlockTypes.CACTUS) {
            return false;
        }
        return !isSolid(x - 1, y, z) && !isSolid(x + 1, y, z) && !isSolid(x, y, z - 1) && !isSolid(x, y, z + 1);
    }

    boolean canPlaceWaterlily(int x, int y, int z) {
        return isAir(x, y, z) && isWater(x, y - 1, z);
    }

    int getHeightmap(int x, int z) {
        for (int y = OldChunkBuffer.GEN_DEPTH - 1; y > 0; y--) {
            if (!isAir(x, y, z)) {
                return y + 1;
            }
        }
        return 0;
    }

    int getTopSolidBlock(int x, int z) {
        for (int y = OldChunkBuffer.GEN_DEPTH - 1; y > 0; y--) {
            BlockState state = getBlockState(x, y, z);
            if (state.getBlockType() == BlockTypes.AIR || !state.getBlockStateData().isSolid() || isLeaves(state.getBlockType())) {
                continue;
            }
            return y + 1;
        }
        return -1;
    }

    int descendUntilGround(int x, int y, int z) {
        while ((isAir(x, y, z) || isLeaves(x, y, z)) && y > 0) {
            y--;
        }
        return y;
    }

    boolean canSeeSky(int x, int y, int z) {
        for (int yy = y; yy < OldChunkBuffer.GEN_DEPTH; yy++) {
            if (!isAir(x, yy, z)) {
                return false;
            }
        }
        return true;
    }

    BlockState getBiomeSurfaceBlock(int x, int z) {
        return getBiome(x, z).topBlockId() == OldBlockIds.MYCELIUM
                ? BlockTypes.MYCELIUM.getDefaultState()
                : GRASS;
    }

    static boolean isLeaves(BlockType<?> type) {
        return LEAVES.contains(type);
    }

    static boolean isLog(BlockType<?> type) {
        return LOGS.contains(type);
    }

    private UnsafeChunk getChunk(int x, int z) {
        if ((x >> 4) == context.getCurrentChunk().getX() && (z >> 4) == context.getCurrentChunk().getZ()) {
            return context.getCurrentChunk();
        }
        return context.getChunkSource().getChunk(x >> 4, z >> 4);
    }

    private static BlockState persistent(BlockState state) {
        return state.setPropertyValue(BlockPropertyTypes.PERSISTENT_BIT, true);
    }

    private static BlockState vertical(BlockState state) {
        return state.setPropertyValue(BlockPropertyTypes.PILLAR_AXIS, PillarAxis.Y);
    }

    private static BlockState axis(BlockState state, PillarAxis axis) {
        return state.setPropertyValue(BlockPropertyTypes.PILLAR_AXIS, axis);
    }
}

final class OldOreFeature implements OldFeature {
    private final BlockState ore;
    private final int count;

    OldOreFeature(BlockState ore, int count) {
        this.ore = ore;
        this.count = count;
    }

    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        float dir = random.nextFloat() * (float) Math.PI;
        double x0 = x + 8 + Math.sin(dir) * count / 8.0;
        double x1 = x + 8 - Math.sin(dir) * count / 8.0;
        double z0 = z + 8 + Math.cos(dir) * count / 8.0;
        double z1 = z + 8 - Math.cos(dir) * count / 8.0;
        double y0 = y + random.nextInt(3) - 2;
        double y1 = y + random.nextInt(3) - 2;

        for (int d = 0; d <= count; d++) {
            double xx = x0 + (x1 - x0) * d / count;
            double yy = y0 + (y1 - y0) * d / count;
            double zz = z0 + (z1 - z0) * d / count;
            double ss = random.nextDouble() * count / 16.0;
            double r = (Math.sin(d * Math.PI / count) + 1.0) * ss + 1.0;
            double hr = (Math.sin(d * Math.PI / count) + 1.0) * ss + 1.0;

            int xt0 = floor(xx - r / 2.0);
            int yt0 = floor(yy - hr / 2.0);
            int zt0 = floor(zz - r / 2.0);
            int xt1 = floor(xx + r / 2.0);
            int yt1 = floor(yy + hr / 2.0);
            int zt1 = floor(zz + r / 2.0);

            for (int x2 = xt0; x2 <= xt1; x2++) {
                double xd = ((x2 + 0.5) - xx) / (r / 2.0);
                if (xd * xd >= 1.0) {
                    continue;
                }
                for (int y2 = yt0; y2 <= yt1; y2++) {
                    double yd = ((y2 + 0.5) - yy) / (hr / 2.0);
                    if (xd * xd + yd * yd >= 1.0) {
                        continue;
                    }
                    for (int z2 = zt0; z2 <= zt1; z2++) {
                        double zd = ((z2 + 0.5) - zz) / (r / 2.0);
                        if (xd * xd + yd * yd + zd * zd < 1.0 && world.getType(x2, y2, z2) == BlockTypes.STONE) {
                            world.setBlockState(x2, y2, z2, ore);
                        }
                    }
                }
            }
        }

        return true;
    }

    private static int floor(double value) {
        int floor = (int) value;
        return value < floor ? floor - 1 : floor;
    }
}

final class OldBlobReplaceFeature implements OldFeature {
    private final int radius;
    private final BlockState replacement;
    private final Set<BlockType<?>> targets;

    OldBlobReplaceFeature(int radius, BlockState replacement, Set<BlockType<?>> targets) {
        this.radius = radius;
        this.replacement = replacement;
        this.targets = targets;
    }

    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        if (y < 0 || !world.isWater(x, y, z)) {
            return false;
        }
        int r = random.nextInt(radius - 2) + 2;
        int yr = replacement == OldWorldAccess.CLAY ? 1 : 2;
        for (int xx = x - r; xx <= x + r; xx++) {
            for (int zz = z - r; zz <= z + r; zz++) {
                int xd = xx - x;
                int zd = zz - z;
                if (xd * xd + zd * zd > r * r) {
                    continue;
                }
                for (int yy = y - yr; yy <= y + yr; yy++) {
                    if (targets.contains(world.getType(xx, yy, zz))) {
                        world.setBlockState(xx, yy, zz, replacement);
                    }
                }
            }
        }
        return true;
    }
}

final class OldPlantScatterFeature implements OldFeature {
    private final BlockState plant;
    private final int tries;
    private final OldPlantSupport support;

    OldPlantScatterFeature(BlockState plant, int tries, OldPlantSupport support) {
        this.plant = plant;
        this.tries = tries;
        this.support = support;
    }

    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        for (int i = 0; i < tries; i++) {
            int x2 = x + random.nextInt(8) - random.nextInt(8);
            int y2 = y + random.nextInt(4) - random.nextInt(4);
            int z2 = z + random.nextInt(8) - random.nextInt(8);
            if (world.canPlacePlant(x2, y2, z2, support)) {
                world.setBlockState(x2, y2, z2, plant);
            }
        }
        return true;
    }
}

final class OldTallGrassFeature implements OldFeature {
    private final BlockState grass;

    OldTallGrassFeature(BlockState grass) {
        this.grass = grass;
    }

    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        y = world.descendUntilGround(x, y, z);
        for (int i = 0; i < 128; i++) {
            int x2 = x + random.nextInt(8) - random.nextInt(8);
            int y2 = y + random.nextInt(4) - random.nextInt(4);
            int z2 = z + random.nextInt(8) - random.nextInt(8);
            if (world.canPlacePlant(x2, y2, z2, OldPlantSupport.GRASSY)) {
                world.setBlockState(x2, y2, z2, grass);
            }
        }
        return true;
    }
}

final class OldReedsFeature implements OldFeature {
    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        for (int i = 0; i < 20; i++) {
            int x2 = x + random.nextInt(4) - random.nextInt(4);
            int z2 = z + random.nextInt(4) - random.nextInt(4);
            if (world.canPlaceReeds(x2, y, z2)) {
                int h = 2 + random.nextInt(random.nextInt(3) + 1);
                for (int yy = 0; yy < h && world.canPlaceReeds(x2, y + yy, z2); yy++) {
                    world.setBlockState(x2, y + yy, z2, OldWorldAccess.REEDS);
                }
            }
        }
        return true;
    }
}

final class OldCactusFeature implements OldFeature {
    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        for (int i = 0; i < 10; i++) {
            int x2 = x + random.nextInt(8) - random.nextInt(8);
            int y2 = y + random.nextInt(4) - random.nextInt(4);
            int z2 = z + random.nextInt(8) - random.nextInt(8);
            if (world.isAir(x2, y2, z2)) {
                int h = 1 + random.nextInt(random.nextInt(3) + 1);
                for (int yy = 0; yy < h && world.canPlaceCactus(x2, y2 + yy, z2); yy++) {
                    world.setBlockState(x2, y2 + yy, z2, OldWorldAccess.CACTUS);
                }
            }
        }
        return true;
    }
}

final class OldWaterlilyFeature implements OldFeature {
    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        for (int i = 0; i < 10; i++) {
            int x2 = x + random.nextInt(8) - random.nextInt(8);
            int y2 = y + random.nextInt(4) - random.nextInt(4);
            int z2 = z + random.nextInt(8) - random.nextInt(8);
            if (world.canPlaceWaterlily(x2, y2, z2)) {
                world.setBlockState(x2, y2, z2, OldWorldAccess.WATERLILY);
            }
        }
        return true;
    }
}

final class OldSpringFeature implements OldFeature {
    private final BlockState liquid;

    OldSpringFeature(BlockState liquid) {
        this.liquid = liquid;
    }

    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        if (world.getType(x, y + 1, z) != BlockTypes.STONE) {
            return false;
        }
        if (world.getType(x, y - 1, z) != BlockTypes.STONE) {
            return false;
        }
        BlockType<?> center = world.getType(x, y, z);
        if (center != BlockTypes.AIR && center != BlockTypes.STONE) {
            return false;
        }

        int rockCount = 0;
        if (world.getType(x - 1, y, z) == BlockTypes.STONE) rockCount++;
        if (world.getType(x + 1, y, z) == BlockTypes.STONE) rockCount++;
        if (world.getType(x, y, z - 1) == BlockTypes.STONE) rockCount++;
        if (world.getType(x, y, z + 1) == BlockTypes.STONE) rockCount++;

        int holeCount = 0;
        if (world.isAir(x - 1, y, z)) holeCount++;
        if (world.isAir(x + 1, y, z)) holeCount++;
        if (world.isAir(x, y, z - 1)) holeCount++;
        if (world.isAir(x, y, z + 1)) holeCount++;

        if (rockCount == 3 && holeCount == 1) {
            world.setBlockState(x, y, z, liquid);
            world.scheduleFluidUpdate(x, y, z);
        }
        return true;
    }
}

final class OldLakeFeature implements OldFeature {
    private static final int WIDTH = 16;
    private static final int HEIGHT = 8;

    private final BlockState liquid;

    OldLakeFeature(BlockState liquid) {
        this.liquid = liquid;
    }

    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        x -= 8;
        z -= 8;
        while (y > 5 && world.isAir(x, y, z)) {
            y--;
        }
        if (y <= 4) {
            return false;
        }

        y -= 4;

        boolean[] mask = new boolean[WIDTH * WIDTH * HEIGHT];
        int spots = random.nextInt(4) + 4;
        for (int i = 0; i < spots; i++) {
            double xr = random.nextDouble() * 6.0 + 3.0;
            double yr = random.nextDouble() * 4.0 + 2.0;
            double zr = random.nextDouble() * 6.0 + 3.0;

            double xp = random.nextDouble() * (16.0 - xr - 2.0) + 1.0 + xr / 2.0;
            double yp = random.nextDouble() * (8.0 - yr - 4.0) + 2.0 + yr / 2.0;
            double zp = random.nextDouble() * (16.0 - zr - 2.0) + 1.0 + zr / 2.0;

            for (int xx = 1; xx < 15; xx++) {
                for (int zz = 1; zz < 15; zz++) {
                    for (int yy = 1; yy < 7; yy++) {
                        double xd = (xx - xp) / (xr / 2.0);
                        double yd = (yy - yp) / (yr / 2.0);
                        double zd = (zz - zp) / (zr / 2.0);
                        if (xd * xd + yd * yd + zd * zd < 1.0) {
                            mask[index(xx, zz, yy)] = true;
                        }
                    }
                }
            }
        }

        for (int xx = 0; xx < 16; xx++) {
            for (int zz = 0; zz < 16; zz++) {
                for (int yy = 0; yy < 8; yy++) {
                    if (!isBoundary(mask, xx, zz, yy)) {
                        continue;
                    }
                    int wx = x + xx;
                    int wy = y + yy;
                    int wz = z + zz;
                    if (yy >= 4 && world.isLiquid(wx, wy, wz)) {
                        return false;
                    }
                    if (yy < 4 && !world.isSolid(wx, wy, wz) && world.getType(wx, wy, wz) != liquid.getBlockType()) {
                        return false;
                    }
                }
            }
        }

        for (int xx = 0; xx < 16; xx++) {
            for (int zz = 0; zz < 16; zz++) {
                for (int yy = 0; yy < 8; yy++) {
                    if (!mask[index(xx, zz, yy)]) {
                        continue;
                    }
                    int wx = x + xx;
                    int wy = y + yy;
                    int wz = z + zz;
                    if (yy >= 4) {
                        world.setBlockState(wx, wy, wz, OldWorldAccess.AIR);
                    } else {
                        world.setBlockState(wx, wy, wz, liquid);
                        world.scheduleFluidUpdate(wx, wy, wz);
                    }
                }
            }
        }

        for (int xx = 0; xx < 16; xx++) {
            for (int zz = 0; zz < 16; zz++) {
                for (int yy = 4; yy < 8; yy++) {
                    if (!mask[index(xx, zz, yy)]) {
                        continue;
                    }
                    int wx = x + xx;
                    int wy = y + yy;
                    int wz = z + zz;
                    if (world.getType(wx, wy - 1, wz) == BlockTypes.DIRT && world.canSeeSky(wx, wy, wz)) {
                        world.setBlockState(wx, wy - 1, wz, world.getBiomeSurfaceBlock(wx, wz));
                    }
                }
            }
        }

        if (liquid.getBlockType() == BlockTypes.LAVA) {
            for (int xx = 0; xx < 16; xx++) {
                for (int zz = 0; zz < 16; zz++) {
                    for (int yy = 0; yy < 8; yy++) {
                        if (!isBoundary(mask, xx, zz, yy)) {
                            continue;
                        }
                        int wx = x + xx;
                        int wy = y + yy;
                        int wz = z + zz;
                        if ((yy < 4 || random.nextInt(2) != 0) && world.isSolid(wx, wy, wz)) {
                            world.setBlockState(wx, wy, wz, OldWorldAccess.STONE);
                        }
                    }
                }
            }
        }

        return true;
    }

    private static int index(int x, int z, int y) {
        return ((x * WIDTH) + z) * HEIGHT + y;
    }

    private static boolean isBoundary(boolean[] mask, int x, int z, int y) {
        if (mask[index(x, z, y)]) {
            return false;
        }
        return (x < 15 && mask[index(x + 1, z, y)])
                || (x > 0 && mask[index(x - 1, z, y)])
                || (z < 15 && mask[index(x, z + 1, y)])
                || (z > 0 && mask[index(x, z - 1, y)])
                || (y < 7 && mask[index(x, z, y + 1)])
                || (y > 0 && mask[index(x, z, y - 1)]);
    }
}

final class OldPumpkinFeature implements OldFeature {
    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        for (int i = 0; i < 64; i++) {
            int x2 = x + random.nextInt(8) - random.nextInt(8);
            int y2 = y + random.nextInt(4) - random.nextInt(4);
            int z2 = z + random.nextInt(8) - random.nextInt(8);
            if (world.isAir(x2, y2, z2) && world.getType(x2, y2 - 1, z2) == BlockTypes.GRASS_BLOCK) {
                world.setBlockState(x2, y2, z2, OldWorldAccess.PUMPKIN);
            }
        }
        return true;
    }
}

final class OldVinesFeature implements OldFeature {
    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        int ox = x;
        int oz = z;
        while (y < OldChunkBuffer.GEN_DEPTH) {
            if (world.isAir(x, y, z)) {
                if (world.isSolid(x, y, z + 1)) {
                    world.setBlockState(x, y, z, OldWorldAccess.VINE_NORTH);
                } else if (world.isSolid(x, y, z - 1)) {
                    world.setBlockState(x, y, z, OldWorldAccess.VINE_SOUTH);
                } else if (world.isSolid(x + 1, y, z)) {
                    world.setBlockState(x, y, z, OldWorldAccess.VINE_WEST);
                } else if (world.isSolid(x - 1, y, z)) {
                    world.setBlockState(x, y, z, OldWorldAccess.VINE_EAST);
                }
            } else {
                x = ox + random.nextInt(4) - random.nextInt(4);
                z = oz + random.nextInt(4) - random.nextInt(4);
            }
            y++;
        }
        return true;
    }
}

final class OldHugeMushroomFeature implements OldFeature {
    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        int type = random.nextInt(2);
        int height = random.nextInt(3) + 4;
        if (y < 1 || y + height + 1 >= OldChunkBuffer.GEN_DEPTH) {
            return false;
        }

        for (int yy = y; yy <= y + 1 + height; yy++) {
            int r = yy <= y + 3 ? 0 : 3;
            for (int xx = x - r; xx <= x + r; xx++) {
                for (int zz = z - r; zz <= z + r; zz++) {
                    BlockType<?> block = world.getType(xx, yy, zz);
                    if (block != BlockTypes.AIR && !OldWorldAccess.isLeaves(block)) {
                        return false;
                    }
                }
            }
        }

        BlockType<?> below = world.getType(x, y - 1, z);
        if (below != BlockTypes.DIRT && below != BlockTypes.GRASS_BLOCK && below != BlockTypes.MYCELIUM) {
            return false;
        }

        int low = type == 1 ? y + height - 3 : y + height;
        BlockState cap = type == 0 ? OldWorldAccess.BROWN_MUSHROOM_BLOCK : OldWorldAccess.RED_MUSHROOM_BLOCK;
        for (int yy = low; yy <= y + height; yy++) {
            int offs = type == 0 ? 3 : (yy < y + height ? 2 : 1);
            for (int xx = x - offs; xx <= x + offs; xx++) {
                for (int zz = z - offs; zz <= z + offs; zz++) {
                    int data = 5;
                    if (xx == x - offs) data--;
                    if (xx == x + offs) data++;
                    if (zz == z - offs) data -= 3;
                    if (zz == z + offs) data += 3;
                    if (type == 0 || yy < y + height) {
                        if ((xx == x - offs || xx == x + offs) && (zz == z - offs || zz == z + offs)) {
                            continue;
                        }
                        if (xx == x - (offs - 1) && zz == z - offs) data = 1;
                        if (xx == x - offs && zz == z - (offs - 1)) data = 1;
                        if (xx == x + (offs - 1) && zz == z - offs) data = 3;
                        if (xx == x + offs && zz == z - (offs - 1)) data = 3;
                        if (xx == x - (offs - 1) && zz == z + offs) data = 7;
                        if (xx == x - offs && zz == z + (offs - 1)) data = 7;
                        if (xx == x + (offs - 1) && zz == z + offs) data = 9;
                        if (xx == x + offs && zz == z + (offs - 1)) data = 9;
                    }
                    if (data == 5 && yy < y + height) {
                        data = 0;
                    }
                    if (data != 0 && !world.isSolid(xx, yy, zz)) {
                        world.setBlockState(xx, yy, zz, cap.setPropertyValue(BlockPropertyTypes.HUGE_MUSHROOM_BITS, data));
                    }
                }
            }
        }

        for (int yy = 0; yy < height; yy++) {
            if (!world.isSolid(x, y + yy, z)) {
                world.setBlockState(x, y + yy, z, OldWorldAccess.MUSHROOM_STEM);
            }
        }
        return true;
    }
}

abstract class OldAbstractTreeFeature implements OldFeature {
    protected void replaceWithDirt(OldWorldAccess world, int x, int y, int z) {
        world.setBlockState(x, y, z, OldWorldAccess.DIRT);
    }

    protected void placeLeaf(OldWorldAccess world, int x, int y, int z, BlockState leaves) {
        if (!world.isSolid(x, y, z)) {
            world.setBlockState(x, y, z, leaves);
        }
    }

    protected void placeLog(OldWorldAccess world, int x, int y, int z, BlockState log) {
        BlockType<?> block = world.getType(x, y, z);
        if (block == BlockTypes.AIR || OldWorldAccess.isLeaves(block) || world.isWater(x, y, z)) {
            world.setBlockState(x, y, z, log);
        }
    }

    protected void addVine(OldWorldAccess world, int x, int y, int z, BlockState vine) {
        if (!world.isAir(x, y, z)) {
            return;
        }
        world.setBlockState(x, y, z, vine);
        int remaining = 4;
        while (remaining-- > 0 && world.isAir(x, --y, z)) {
            world.setBlockState(x, y, z, vine);
        }
    }
}

final class OldTreeFeature extends OldAbstractTreeFeature {
    private final int baseHeight;
    private final BlockState log;
    private final BlockState leaves;
    private final boolean jungleVines;

    OldTreeFeature(int baseHeight, BlockState log, BlockState leaves, boolean jungleVines) {
        this.baseHeight = baseHeight;
        this.log = log;
        this.leaves = leaves;
        this.jungleVines = jungleVines;
    }

    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        int treeHeight = random.nextInt(3) + baseHeight;
        if (y < 1 || y + treeHeight + 1 > OldChunkBuffer.GEN_DEPTH) {
            return false;
        }

        for (int yy = y; yy <= y + 1 + treeHeight; yy++) {
            int r = yy == y ? 0 : (yy >= y + treeHeight - 1 ? 2 : 1);
            for (int xx = x - r; xx <= x + r; xx++) {
                for (int zz = z - r; zz <= z + r; zz++) {
                    if (!world.isReplaceableByTree(xx, yy, zz)) {
                        return false;
                    }
                }
            }
        }

        if (!world.isGroundForTree(x, y - 1, z) || y >= OldChunkBuffer.GEN_DEPTH - treeHeight - 1) {
            return false;
        }

        replaceWithDirt(world, x, y - 1, z);

        for (int yy = y + treeHeight; yy >= y - 3 + treeHeight; yy--) {
            int yo = yy - (y + treeHeight);
            int offs = 1 - yo / 2;
            for (int xx = x - offs; xx <= x + offs; xx++) {
                int xo = xx - x;
                for (int zz = z - offs; zz <= z + offs; zz++) {
                    int zo = zz - z;
                    if (Math.abs(xo) == offs && Math.abs(zo) == offs && (random.nextInt(2) == 0 || yo == 0)) {
                        continue;
                    }
                    placeLeaf(world, xx, yy, zz, leaves);
                }
            }
        }

        for (int hh = 0; hh < treeHeight; hh++) {
            placeLog(world, x, y + hh, z, log);
            if (jungleVines && hh > 0) {
                if (random.nextInt(3) > 0) addVine(world, x - 1, y + hh, z, OldWorldAccess.VINE_EAST);
                if (random.nextInt(3) > 0) addVine(world, x + 1, y + hh, z, OldWorldAccess.VINE_WEST);
                if (random.nextInt(3) > 0) addVine(world, x, y + hh, z - 1, OldWorldAccess.VINE_SOUTH);
                if (random.nextInt(3) > 0) addVine(world, x, y + hh, z + 1, OldWorldAccess.VINE_NORTH);
            }
        }

        if (jungleVines) {
            for (int yy = y - 3 + treeHeight; yy <= y + treeHeight; yy++) {
                int yo = yy - (y + treeHeight);
                int offs = 2 - yo / 2;
                for (int xx = x - offs; xx <= x + offs; xx++) {
                    for (int zz = z - offs; zz <= z + offs; zz++) {
                        if (OldWorldAccess.isLeaves(world.getType(xx, yy, zz))) {
                            if (random.nextInt(4) == 0) addVine(world, xx - 1, yy, zz, OldWorldAccess.VINE_EAST);
                            if (random.nextInt(4) == 0) addVine(world, xx + 1, yy, zz, OldWorldAccess.VINE_WEST);
                            if (random.nextInt(4) == 0) addVine(world, xx, yy, zz - 1, OldWorldAccess.VINE_SOUTH);
                            if (random.nextInt(4) == 0) addVine(world, xx, yy, zz + 1, OldWorldAccess.VINE_NORTH);
                        }
                    }
                }
            }
        }

        return true;
    }
}

final class OldFancyTreeFeature extends OldAbstractTreeFeature {
    private static final byte[] AXIS_CONVERSION_ARRAY = {2, 0, 0, 1, 2, 1};

    private final OldJavaRandom innerRandom = new OldJavaRandom(0L);
    private final int[] origin = new int[3];
    private int height;
    private int trunkHeight;
    private final double trunkHeightScale = 0.618;
    private final double branchDensity = 1.0;
    private final double branchSlope = 0.381;
    private final double widthScale = 1.0;
    private final double foliageDensity = 1.0;
    private final int trunkWidth = 1;
    private final int heightVariance = 12;
    private final int foliageHeight = 4;
    private List<int[]> foliageCoords = List.of();
    private OldWorldAccess currentWorld;

    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        currentWorld = world;
        innerRandom.setSeed(random.nextLong());
        origin[0] = x;
        origin[1] = y;
        origin[2] = z;
        height = 5 + innerRandom.nextInt(heightVariance);
        foliageCoords = List.of();

        if (!checkLocation(world)) {
            return false;
        }

        prepare();
        makeFoliage(world);
        makeTrunk(world);
        makeBranches(world);
        return true;
    }

    private void prepare() {
        trunkHeight = (int) (height * trunkHeightScale);
        if (trunkHeight >= height) {
            trunkHeight = height - 1;
        }
        int clustersPerY = (int) (1.382 + Math.pow(foliageDensity * height / 13.0, 2.0));
        if (clustersPerY < 1) {
            clustersPerY = 1;
        }

        List<int[]> coords = new ArrayList<>(clustersPerY * height);
        int y = origin[1] + height - foliageHeight;
        int trunkTop = origin[1] + trunkHeight;
        int relativeY = y - origin[1];

        coords.add(new int[]{origin[0], y, origin[2], trunkTop});
        y--;

        while (relativeY >= 0) {
            float shapeFactor = treeShape(relativeY);
            if (shapeFactor < 0.0f) {
                y--;
                relativeY--;
                continue;
            }

            int num = 0;
            double originOffset = 0.5;
            while (num < clustersPerY) {
                double radius = widthScale * (shapeFactor * (innerRandom.nextFloat() + 0.328));
                double angle = innerRandom.nextFloat() * 2.0 * Math.PI;
                int x = floor(radius * Math.sin(angle) + origin[0] + originOffset);
                int z = floor(radius * Math.cos(angle) + origin[2] + originOffset);
                int[] checkStart = {x, y, z};
                int[] checkEnd = {x, y + foliageHeight, z};
                if (checkLine(currentWorld, checkStart, checkEnd) == -1) {
                    int[] branchBase = {origin[0], origin[1], origin[2]};
                    double distance = Math.sqrt(
                            Math.pow(Math.abs(origin[0] - checkStart[0]), 2.0)
                                    + Math.pow(Math.abs(origin[2] - checkStart[2]), 2.0)
                    );
                    double branchHeight = distance * branchSlope;
                    if ((checkStart[1] - branchHeight) > trunkTop) {
                        branchBase[1] = trunkTop;
                    } else {
                        branchBase[1] = (int) (checkStart[1] - branchHeight);
                    }
                    if (checkLine(currentWorld, branchBase, checkStart) == -1) {
                        coords.add(new int[]{x, y, z, branchBase[1]});
                    }
                }
                num++;
            }
            y--;
            relativeY--;
        }

        foliageCoords = coords;
    }

    private float treeShape(int y) {
        if (y < (height * 0.3f)) {
            return -1.618f;
        }
        float radius = height / 2.0f;
        float adjacent = (height / 2.0f) - y;
        float distance;
        if (adjacent == 0.0f) {
            distance = radius;
        } else if (Math.abs(adjacent) >= radius) {
            distance = 0.0f;
        } else {
            distance = (float) Math.sqrt(Math.pow(Math.abs(radius), 2.0) - Math.pow(Math.abs(adjacent), 2.0));
        }
        return distance * 0.5f;
    }

    private float foliageShape(int y) {
        if (y < 0 || y >= foliageHeight) {
            return -1.0f;
        }
        if (y == 0 || y == foliageHeight - 1) {
            return 2.0f;
        }
        return 3.0f;
    }

    private void foliageCluster(OldWorldAccess world, int x, int y, int z) {
        for (int currentY = y + foliageHeight - 1; currentY >= y; currentY--) {
            float radius = foliageShape(currentY - y);
            crossSection(world, x, currentY, z, radius, (byte) 1, OldWorldAccess.OAK_LEAVES);
        }
    }

    private void crossSection(OldWorldAccess world, int x, int y, int z, float radius, byte direction, BlockState blockState) {
        int rad = (int) (radius + 0.618);
        byte secIdx1 = AXIS_CONVERSION_ARRAY[direction];
        byte secIdx2 = AXIS_CONVERSION_ARRAY[direction + 3];
        int[] center = {x, y, z};
        int[] position = {0, 0, 0};
        position[direction] = center[direction];
        for (int offset1 = -rad; offset1 <= rad; offset1++) {
            position[secIdx1] = center[secIdx1] + offset1;
            for (int offset2 = -rad; offset2 <= rad; offset2++) {
                double distance = Math.pow(Math.abs(offset1) + 0.5, 2.0) + Math.pow(Math.abs(offset2) + 0.5, 2.0);
                if (distance > radius * radius) {
                    continue;
                }
                position[secIdx2] = center[secIdx2] + offset2;
                BlockType<?> current = world.getType(position[0], position[1], position[2]);
                if (current != BlockTypes.AIR && !OldWorldAccess.isLeaves(current)) {
                    continue;
                }
                world.setBlockState(position[0], position[1], position[2], blockState);
            }
        }
    }

    private void limb(OldWorldAccess world, int[] start, int[] end, boolean leaves) {
        int[] delta = {0, 0, 0};
        byte primaryIndex = 0;
        for (byte i = 0; i < 3; i++) {
            delta[i] = end[i] - start[i];
            if (Math.abs(delta[i]) > Math.abs(delta[primaryIndex])) {
                primaryIndex = i;
            }
        }
        if (delta[primaryIndex] == 0) {
            return;
        }

        byte secIdx1 = AXIS_CONVERSION_ARRAY[primaryIndex];
        byte secIdx2 = AXIS_CONVERSION_ARRAY[primaryIndex + 3];
        int primarySign = delta[primaryIndex] > 0 ? 1 : -1;
        double secFactor1 = (double) delta[secIdx1] / (double) delta[primaryIndex];
        double secFactor2 = (double) delta[secIdx2] / (double) delta[primaryIndex];
        int[] coordinate = {0, 0, 0};
        for (int primaryOffset = 0, endOffset = delta[primaryIndex] + primarySign; primaryOffset != endOffset; primaryOffset += primarySign) {
            coordinate[primaryIndex] = floor(start[primaryIndex] + primaryOffset + 0.5);
            coordinate[secIdx1] = floor(start[secIdx1] + (primaryOffset * secFactor1) + 0.5);
            coordinate[secIdx2] = floor(start[secIdx2] + (primaryOffset * secFactor2) + 0.5);
            if (leaves) {
                world.setBlockState(coordinate[0], coordinate[1], coordinate[2], OldWorldAccess.OAK_LEAVES);
            } else {
                world.setBlockState(coordinate[0], coordinate[1], coordinate[2], oakLogForAxis(coordinate, start));
            }
        }
    }

    private void makeFoliage(OldWorldAccess world) {
        for (int[] coord : foliageCoords) {
            foliageCluster(world, coord[0], coord[1], coord[2]);
        }
    }

    private boolean trimBranches(int localY) {
        return localY >= (height * 0.2);
    }

    private void makeTrunk(OldWorldAccess world) {
        int[] start = {origin[0], origin[1], origin[2]};
        int[] end = {origin[0], origin[1] + trunkHeight, origin[2]};
        limb(world, start, end, false);
        if (trunkWidth == 2) {
            start[0] += 1;
            end[0] += 1;
            limb(world, start, end, false);
            start[2] += 1;
            end[2] += 1;
            limb(world, start, end, false);
            start[0] -= 1;
            end[0] -= 1;
            limb(world, start, end, false);
        }
    }

    private void makeBranches(OldWorldAccess world) {
        int[] base = {origin[0], origin[1], origin[2]};
        for (int[] coord : foliageCoords) {
            int[] end = {coord[0], coord[1], coord[2]};
            base[1] = coord[3];
            int localY = base[1] - origin[1];
            if (trimBranches(localY)) {
                limb(world, base, end, false);
            }
        }
    }

    private int checkLine(OldWorldAccess world, int[] start, int[] end) {
        int[] delta = {0, 0, 0};
        byte primaryIndex = 0;
        for (byte i = 0; i < 3; i++) {
            delta[i] = end[i] - start[i];
            if (Math.abs(delta[i]) > Math.abs(delta[primaryIndex])) {
                primaryIndex = i;
            }
        }
        if (delta[primaryIndex] == 0) {
            return -1;
        }

        byte secIdx1 = AXIS_CONVERSION_ARRAY[primaryIndex];
        byte secIdx2 = AXIS_CONVERSION_ARRAY[primaryIndex + 3];
        int primarySign = delta[primaryIndex] > 0 ? 1 : -1;
        double secFactor1 = (double) delta[secIdx1] / (double) delta[primaryIndex];
        double secFactor2 = (double) delta[secIdx2] / (double) delta[primaryIndex];
        int[] coordinate = {0, 0, 0};
        int primaryOffset = 0;
        int endOffset = delta[primaryIndex] + primarySign;
        while (primaryOffset != endOffset) {
            coordinate[primaryIndex] = start[primaryIndex] + primaryOffset;
            coordinate[secIdx1] = floor(start[secIdx1] + (primaryOffset * secFactor1));
            coordinate[secIdx2] = floor(start[secIdx2] + (primaryOffset * secFactor2));
            BlockType<?> material = world.getType(coordinate[0], coordinate[1], coordinate[2]);
            if (material != BlockTypes.AIR && !OldWorldAccess.isLeaves(material)) {
                break;
            }
            primaryOffset += primarySign;
        }
        return primaryOffset == endOffset ? -1 : Math.abs(primaryOffset);
    }

    private boolean checkLocation(OldWorldAccess world) {
        int[] start = {origin[0], origin[1], origin[2]};
        int[] end = {origin[0], origin[1] + height - 1, origin[2]};
        BlockType<?> base = world.getType(origin[0], origin[1] - 1, origin[2]);
        if (base != BlockTypes.GRASS_BLOCK && base != BlockTypes.DIRT) {
            return false;
        }
        int allowedHeight = checkLine(world, start, end);
        if (allowedHeight == -1) {
            return true;
        }
        if (allowedHeight < 6) {
            return false;
        }
        height = allowedHeight;
        return true;
    }

    private static int floor(double value) {
        int floor = (int) value;
        return value < floor ? floor - 1 : floor;
    }

    private static BlockState oakLogForAxis(int[] coordinate, int[] start) {
        int xDiff = Math.abs(coordinate[0] - start[0]);
        int zDiff = Math.abs(coordinate[2] - start[2]);
        int maxDiff = Math.max(xDiff, zDiff);
        if (maxDiff > 0) {
            if (xDiff == maxDiff) {
                return OldWorldAccess.OAK_LOG_X;
            }
            if (zDiff == maxDiff) {
                return OldWorldAccess.OAK_LOG_Z;
            }
        }
        return OldWorldAccess.OAK_LOG;
    }
}

final class OldBirchTreeFeature extends OldAbstractTreeFeature {
    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        int treeHeight = random.nextInt(3) + 5;
        if (y < 1 || y + treeHeight + 1 > OldChunkBuffer.GEN_DEPTH) {
            return false;
        }
        for (int yy = y; yy <= y + 1 + treeHeight; yy++) {
            int r = yy == y ? 0 : (yy >= y + treeHeight - 1 ? 2 : 1);
            for (int xx = x - r; xx <= x + r; xx++) {
                for (int zz = z - r; zz <= z + r; zz++) {
                    BlockType<?> type = world.getType(xx, yy, zz);
                    if (type != BlockTypes.AIR && !OldWorldAccess.isLeaves(type)) {
                        return false;
                    }
                }
            }
        }
        if (!world.isGroundForTree(x, y - 1, z) || y >= OldChunkBuffer.GEN_DEPTH - treeHeight - 1) {
            return false;
        }
        replaceWithDirt(world, x, y - 1, z);
        for (int yy = y - 3 + treeHeight; yy <= y + treeHeight; yy++) {
            int yo = yy - (y + treeHeight);
            int offs = 1 - yo / 2;
            for (int xx = x - offs; xx <= x + offs; xx++) {
                int xo = xx - x;
                for (int zz = z - offs; zz <= z + offs; zz++) {
                    int zo = zz - z;
                    if (Math.abs(xo) == offs && Math.abs(zo) == offs && (random.nextInt(2) == 0 || yo == 0)) {
                        continue;
                    }
                    placeLeaf(world, xx, yy, zz, OldWorldAccess.BIRCH_LEAVES);
                }
            }
        }
        for (int hh = 0; hh < treeHeight; hh++) {
            placeLog(world, x, y + hh, z, OldWorldAccess.BIRCH_LOG);
        }
        return true;
    }
}

final class OldSpruceTreeFeature extends OldAbstractTreeFeature {
    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        int treeHeight = random.nextInt(4) + 6;
        int trunkHeight = 1 + random.nextInt(2);
        int topHeight = treeHeight - trunkHeight;
        int leafRadius = 2 + random.nextInt(2);
        if (y < 1 || y + treeHeight + 1 > OldChunkBuffer.GEN_DEPTH) {
            return false;
        }
        for (int yy = y; yy <= y + 1 + treeHeight; yy++) {
            int r = (yy - y) < trunkHeight ? 0 : leafRadius;
            for (int xx = x - r; xx <= x + r; xx++) {
                for (int zz = z - r; zz <= z + r; zz++) {
                    BlockType<?> type = world.getType(xx, yy, zz);
                    if (type != BlockTypes.AIR && !OldWorldAccess.isLeaves(type)) {
                        return false;
                    }
                }
            }
        }
        if (!world.isGroundForTree(x, y - 1, z) || y >= OldChunkBuffer.GEN_DEPTH - treeHeight - 1) {
            return false;
        }
        replaceWithDirt(world, x, y - 1, z);
        int currentRadius = random.nextInt(2);
        int maxRadius = 1;
        int minRadius = 0;
        for (int heightPos = 0; heightPos <= topHeight; heightPos++) {
            int yy = y + treeHeight - heightPos;
            for (int xx = x - currentRadius; xx <= x + currentRadius; xx++) {
                int xo = xx - x;
                for (int zz = z - currentRadius; zz <= z + currentRadius; zz++) {
                    int zo = zz - z;
                    if (Math.abs(xo) == currentRadius && Math.abs(zo) == currentRadius && currentRadius > 0) {
                        continue;
                    }
                    placeLeaf(world, xx, yy, zz, OldWorldAccess.SPRUCE_LEAVES);
                }
            }
            if (currentRadius >= maxRadius) {
                currentRadius = minRadius;
                minRadius = 1;
                maxRadius = Math.min(maxRadius + 1, leafRadius);
            } else {
                currentRadius++;
            }
        }
        int topOffset = random.nextInt(3);
        for (int hh = 0; hh < treeHeight - topOffset; hh++) {
            placeLog(world, x, y + hh, z, OldWorldAccess.SPRUCE_LOG);
        }
        return true;
    }
}

final class OldPineTreeFeature extends OldAbstractTreeFeature {
    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        int treeHeight = random.nextInt(5) + 7;
        int trunkHeight = treeHeight - random.nextInt(2) - 3;
        int topHeight = treeHeight - trunkHeight;
        int topRadius = 1 + random.nextInt(topHeight + 1);
        if (y < 1 || y + treeHeight + 1 > OldChunkBuffer.GEN_DEPTH) {
            return false;
        }
        for (int yy = y; yy <= y + 1 + treeHeight; yy++) {
            int r = (yy - y) < trunkHeight ? 0 : topRadius;
            for (int xx = x - r; xx <= x + r; xx++) {
                for (int zz = z - r; zz <= z + r; zz++) {
                    BlockType<?> type = world.getType(xx, yy, zz);
                    if (type != BlockTypes.AIR && !OldWorldAccess.isLeaves(type)) {
                        return false;
                    }
                }
            }
        }
        if (!world.isGroundForTree(x, y - 1, z) || y >= OldChunkBuffer.GEN_DEPTH - treeHeight - 1) {
            return false;
        }
        replaceWithDirt(world, x, y - 1, z);
        int currentRadius = 0;
        for (int yy = y + treeHeight; yy >= y + trunkHeight; yy--) {
            for (int xx = x - currentRadius; xx <= x + currentRadius; xx++) {
                int xo = xx - x;
                for (int zz = z - currentRadius; zz <= z + currentRadius; zz++) {
                    int zo = zz - z;
                    if (Math.abs(xo) == currentRadius && Math.abs(zo) == currentRadius && currentRadius > 0) {
                        continue;
                    }
                    placeLeaf(world, xx, yy, zz, OldWorldAccess.SPRUCE_LEAVES);
                }
            }
            if (currentRadius >= 1 && yy == y + trunkHeight + 1) {
                currentRadius--;
            } else if (currentRadius < topRadius) {
                currentRadius++;
            }
        }
        for (int hh = 0; hh < treeHeight - 1; hh++) {
            placeLog(world, x, y + hh, z, OldWorldAccess.SPRUCE_LOG);
        }
        return true;
    }
}

final class OldSwampTreeFeature extends OldAbstractTreeFeature {
    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        int treeHeight = random.nextInt(4) + 5;
        while (world.isWater(x, y - 1, z)) {
            y--;
        }
        if (y < 1 || y + treeHeight + 1 > OldChunkBuffer.GEN_DEPTH) {
            return false;
        }
        for (int yy = y; yy <= y + 1 + treeHeight; yy++) {
            int r = yy == y ? 0 : (yy >= y + treeHeight - 1 ? 3 : 1);
            for (int xx = x - r; xx <= x + r; xx++) {
                for (int zz = z - r; zz <= z + r; zz++) {
                    BlockType<?> block = world.getType(xx, yy, zz);
                    if (block == BlockTypes.AIR || OldWorldAccess.isLeaves(block)) {
                        continue;
                    }
                    if ((block == BlockTypes.WATER || block == BlockTypes.FLOWING_WATER) && yy <= y) {
                        continue;
                    }
                    return false;
                }
            }
        }
        if (!world.isGroundForTree(x, y - 1, z) || y >= OldChunkBuffer.GEN_DEPTH - treeHeight - 1) {
            return false;
        }
        replaceWithDirt(world, x, y - 1, z);
        for (int yy = y - 3 + treeHeight; yy <= y + treeHeight; yy++) {
            int yo = yy - (y + treeHeight);
            int offs = 2 - yo / 2;
            for (int xx = x - offs; xx <= x + offs; xx++) {
                int xo = xx - x;
                for (int zz = z - offs; zz <= z + offs; zz++) {
                    int zo = zz - z;
                    if (Math.abs(xo) == offs && Math.abs(zo) == offs && (random.nextInt(2) == 0 || yo == 0)) {
                        continue;
                    }
                    placeLeaf(world, xx, yy, zz, OldWorldAccess.OAK_LEAVES);
                }
            }
        }
        for (int hh = 0; hh < treeHeight; hh++) {
            placeLog(world, x, y + hh, z, OldWorldAccess.OAK_LOG);
        }
        for (int yy = y - 3 + treeHeight; yy <= y + treeHeight; yy++) {
            int yo = yy - (y + treeHeight);
            int offs = 2 - yo / 2;
            for (int xx = x - offs; xx <= x + offs; xx++) {
                for (int zz = z - offs; zz <= z + offs; zz++) {
                    if (OldWorldAccess.isLeaves(world.getType(xx, yy, zz))) {
                        if (random.nextInt(4) == 0) addVine(world, xx - 1, yy, zz, OldWorldAccess.VINE_EAST);
                        if (random.nextInt(4) == 0) addVine(world, xx + 1, yy, zz, OldWorldAccess.VINE_WEST);
                        if (random.nextInt(4) == 0) addVine(world, xx, yy, zz - 1, OldWorldAccess.VINE_SOUTH);
                        if (random.nextInt(4) == 0) addVine(world, xx, yy, zz + 1, OldWorldAccess.VINE_NORTH);
                    }
                }
            }
        }
        return true;
    }
}

final class OldGroundBushFeature extends OldAbstractTreeFeature {
    private final BlockState log;
    private final BlockState leaves;

    OldGroundBushFeature(BlockState log, BlockState leaves) {
        this.log = log;
        this.leaves = leaves;
    }

    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        y = world.descendUntilGround(x, y, z);
        BlockType<?> block = world.getType(x, y, z);
        if (block != BlockTypes.DIRT && block != BlockTypes.GRASS_BLOCK) {
            return false;
        }
        y++;
        world.setBlockState(x, y, z, log);
        for (int yy = y; yy <= y + 2; yy++) {
            int yo = yy - y;
            int offs = 2 - yo;
            for (int xx = x - offs; xx <= x + offs; xx++) {
                int xo = xx - x;
                for (int zz = z - offs; zz <= z + offs; zz++) {
                    int zo = zz - z;
                    if (Math.abs(xo) == offs && Math.abs(zo) == offs && random.nextInt(2) == 0) {
                        continue;
                    }
                    placeLeaf(world, xx, yy, zz, leaves);
                }
            }
        }
        return true;
    }
}

final class OldMegaJungleTreeFeature extends OldAbstractTreeFeature {
    private final int baseHeight;

    OldMegaJungleTreeFeature(int baseHeight) {
        this.baseHeight = baseHeight;
    }

    @Override
    public boolean place(OldWorldAccess world, OldJavaRandom random, int x, int y, int z) {
        int treeHeight = random.nextInt(3) + baseHeight;
        if (y < 1 || y + treeHeight + 1 > OldChunkBuffer.GEN_DEPTH) {
            return false;
        }
        for (int yy = y; yy <= y + 1 + treeHeight; yy++) {
            int r = yy == y ? 1 : 2;
            for (int xx = x - r; xx <= x + r; xx++) {
                for (int zz = z - r; zz <= z + r; zz++) {
                    if (!world.isReplaceableByTree(xx, yy, zz)) {
                        return false;
                    }
                }
            }
        }
        if (!world.isGroundForTree(x, y - 1, z) || !world.isGroundForTree(x + 1, y - 1, z)
                || !world.isGroundForTree(x, y - 1, z + 1) || !world.isGroundForTree(x + 1, y - 1, z + 1)) {
            return false;
        }

        replaceWithDirt(world, x, y - 1, z);
        replaceWithDirt(world, x + 1, y - 1, z);
        replaceWithDirt(world, x, y - 1, z + 1);
        replaceWithDirt(world, x + 1, y - 1, z + 1);

        placeLeaves(world, random, x, z, y + treeHeight, 2);
        int branchHeight = y + treeHeight - 2 - random.nextInt(4);
        while (branchHeight > y + treeHeight / 2) {
            float angle = random.nextFloat() * (float) Math.PI * 2.0f;
            int bx = x + (int) (0.5f + Math.cos(angle) * 4.0f);
            int bz = z + (int) (0.5f + Math.sin(angle) * 4.0f);
            placeLeaves(world, random, bx, bz, branchHeight, 0);
            for (int b = 0; b < 5; b++) {
                bx = x + (int) (1.5f + Math.cos(angle) * b);
                bz = z + (int) (1.5f + Math.sin(angle) * b);
                world.setBlockState(bx, branchHeight - 3 + b / 2, bz, OldWorldAccess.JUNGLE_LOG);
            }
            branchHeight -= 2 + random.nextInt(4);
        }

        for (int hh = 0; hh < treeHeight; hh++) {
            placeLog(world, x, y + hh, z, OldWorldAccess.JUNGLE_LOG);
            placeLog(world, x + 1, y + hh, z, OldWorldAccess.JUNGLE_LOG);
            placeLog(world, x, y + hh, z + 1, OldWorldAccess.JUNGLE_LOG);
            placeLog(world, x + 1, y + hh, z + 1, OldWorldAccess.JUNGLE_LOG);
        }
        return true;
    }

    private void placeLeaves(OldWorldAccess world, OldJavaRandom random, int x, int z, int top, int baseRadius) {
        for (int yy = top; yy >= top - 2; yy--) {
            int yo = yy - top;
            int radius = baseRadius + 1 - yo;
            for (int xx = x - radius; xx <= x + radius + 1; xx++) {
                int xo = xx - x;
                for (int zz = z - radius; zz <= z + radius + 1; zz++) {
                    int zo = zz - z;
                    if ((xo < 0 && zo < 0) && (xo * xo + zo * zo) > (radius * radius)) {
                        continue;
                    }
                    if ((xo > 0 || zo > 0) && (xo * xo + zo * zo) > ((radius + 1) * (radius + 1))) {
                        continue;
                    }
                    if (random.nextInt(4) == 0 && (xo * xo + zo * zo) > ((radius - 1) * (radius - 1))) {
                        continue;
                    }
                    placeLeaf(world, xx, yy, zz, OldWorldAccess.JUNGLE_LEAVES);
                }
            }
        }
    }
}
