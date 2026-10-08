package com.kuronami.briskleafdecay.clientprobe;

import com.kuronami.briskleafdecay.BriskLeafDecayCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Grows an oak in front of the player, chops every log, and counts the leaves left five seconds later. */
@Mod(value = "brisk_leaf_decay_clientprobe", dist = Dist.CLIENT)
public class LeafDecayClientProbe extends ProbeHarness {

    private BlockPos treeBase;
    private final List<BlockPos> logs = new ArrayList<>();
    private int leavesBefore;
    private int chopTick = -1;

    public LeafDecayClientProbe() {
        super("brisk-leaf-decay");
    }

    private int countLeaves(ServerLevel level) {
        int leaves = 0;
        for (BlockPos pos : BlockPos.betweenClosed(treeBase.offset(-4, 0, -4), treeBase.offset(4, 10, 4))) {
            if (level.getBlockState(pos).is(BlockTags.LEAVES)) {
                leaves++;
            }
        }
        return leaves;
    }

    @Override
    protected void onServerTick(MinecraftServer server, ServerPlayer player, int tick) {
        ServerLevel level = server.overworld();
        if (tick == 40) {
            result.put("active", BriskLeafDecayCommon.isActive());
            BlockPos feet = player.blockPosition();
            treeBase = feet.offset(0, 0, 7);
            // Stand back and face the tree (south, slightly up) so the screenshots show the canopy.
            player.teleportTo(level, feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0F, -20.0F);
            level.setBlockAndUpdate(treeBase.below(), Blocks.DIRT.defaultBlockState());
            boolean placed = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .getHolderOrThrow(TreeFeatures.OAK).value()
                    .place(level, level.getChunkSource().getGenerator(), level.getRandom(), treeBase);
            result.put("treePlaced", placed);
            for (BlockPos pos : BlockPos.betweenClosed(treeBase.offset(-4, 0, -4), treeBase.offset(4, 10, 4))) {
                if (level.getBlockState(pos).is(BlockTags.LOGS)) {
                    logs.add(pos.immutable());
                }
            }
            leavesBefore = countLeaves(level);
            result.put("logs", logs.size());
            result.put("leavesBefore", leavesBefore);
            if (!placed || logs.isEmpty() || leavesBefore < 20) {
                finish(false);
            }
        } else if (tick == 80) {
            screenshot("1-before");
        } else if (tick == 90) {
            logs.forEach(pos -> level.destroyBlock(pos, true, player));
            chopTick = tick;
            screenshot("2-just-chopped");
        } else if (chopTick > 0 && tick == chopTick + 100) {
            int left = countLeaves(level);
            result.put("leavesAfter5s", left);
            screenshot("3-after-5s");
            finish(left == 0 && BriskLeafDecayCommon.isActive());
        }
    }
}
