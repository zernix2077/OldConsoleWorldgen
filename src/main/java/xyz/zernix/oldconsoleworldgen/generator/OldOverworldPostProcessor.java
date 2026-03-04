package xyz.zernix.oldconsoleworldgen.generator;

import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.world.generator.context.PostProcessContext;
import org.allaymc.api.world.generator.function.PostProcessor;

public final class OldOverworldPostProcessor implements PostProcessor {
    private final GeneratorConfig config;

    public OldOverworldPostProcessor(GeneratorConfig config) {
        this.config = config;
    }

    @Override
    public boolean apply(PostProcessContext context) {
        var chunk = context.getCurrentChunk();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                var biome = OldBiome.byAllayBiome(chunk.getBiome(x, Math.max(0, config.seaLevel()), z));
                if (!biome.hasSnow()) {
                    continue;
                }

                int topY = Math.min(OldChunkBuffer.GEN_DEPTH - 1, chunk.getHeight(x, z));
                var topBlock = chunk.getBlockState(x, topY, z).getBlockType();
                if (topBlock == BlockTypes.WATER || topBlock == BlockTypes.FLOWING_WATER) {
                    chunk.setBlockState(x, topY, z, BlockTypes.ICE.getDefaultState());
                    topBlock = BlockTypes.ICE;
                }

                int snowY = topBlock == BlockTypes.AIR ? topY : topY + 1;
                if (snowY <= OldChunkBuffer.GEN_DEPTH - 1
                        && chunk.getBlockState(x, snowY, z).getBlockType() == BlockTypes.AIR
                        && canSupportSnow(chunk.getBlockState(x, snowY - 1, z))) {
                    chunk.setBlockState(x, snowY, z, BlockTypes.SNOW_LAYER.getDefaultState());
                }
            }
        }
        return true;
    }

    private static boolean canSupportSnow(org.allaymc.api.block.type.BlockState below) {
        var type = below.getBlockType();
        if (type == BlockTypes.AIR || type == BlockTypes.ICE) {
            return false;
        }
        if (type == BlockTypes.OAK_LEAVES
                || type == BlockTypes.BIRCH_LEAVES
                || type == BlockTypes.SPRUCE_LEAVES
                || type == BlockTypes.JUNGLE_LEAVES) {
            return true;
        }
        return below.getBlockStateData().collisionShape().isFull(BlockFace.UP)
                && below.getBlockStateData().isSolid();
    }
}
