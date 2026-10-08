package com.kuronami.briskleafdecaytest;

import com.kuronami.briskleafdecay.BriskLeafDecayCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder("brisk_leaf_decay")
@PrefixGameTestTemplate(false)
public class LeafDecayGameTests {

    // randomTickSpeed is world-wide, so tests run in two batches. The GameTest server itself starts at 0.
    private static final String NORMAL = "randomTickSpeedDefault";
    private static final String OFF = "randomTickSpeedZero";

    @BeforeBatch(batch = NORMAL)
    public static void defaultRandomTicks(ServerLevel level) {
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(3, level.getServer());
    }

    @BeforeBatch(batch = OFF)
    public static void noRandomTicks(ServerLevel level) {
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
    }

    /** Set by the build when a same-feature mod is on the classpath (-PcoexistJar). */
    private static boolean expectInactive() {
        return Boolean.getBoolean("brisk_leaf_decay.expectInactive");
    }

    private static BlockState leaf(int distance, boolean persistent) {
        return Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(LeavesBlock.DISTANCE, distance)
                .setValue(LeavesBlock.PERSISTENT, persistent);
    }

    /** A log with leaves around it; returns the leaf positions. */
    private static List<BlockPos> buildBush(GameTestHelper helper, BlockPos log, boolean persistent) {
        helper.setBlock(log, Blocks.OAK_LOG);
        List<BlockPos> leaves = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    int distance = Math.abs(dx) + Math.abs(dz) + dy;
                    if (distance == 0 || distance > 2) {
                        continue;
                    }
                    BlockPos pos = log.offset(dx, dy, dz);
                    helper.setBlock(pos, leaf(distance, persistent));
                    leaves.add(pos);
                }
            }
        }
        return leaves;
    }

    @GameTest(template = "empty7")
    public static void activeMatchesInstalledMods(GameTestHelper helper) {
        boolean expected = !expectInactive();
        if (BriskLeafDecayCommon.isActive() != expected) {
            helper.fail("isActive=" + BriskLeafDecayCommon.isActive() + " but expected " + expected);
        }
        helper.succeed();
    }

    /** Vanilla needs random ticks (about a minute per leaf); 80 ticks is far too short for that. */
    @GameTest(template = "empty7", timeoutTicks = 80, batch = NORMAL)
    public static void cutOffLeavesFallQuickly(GameTestHelper helper) {
        if (expectInactive()) {
            helper.succeed();
            return;
        }
        BlockPos log = new BlockPos(3, 1, 3);
        List<BlockPos> leaves = buildBush(helper, log, false);
        helper.runAfterDelay(5, () -> helper.setBlock(log, Blocks.AIR));
        helper.runAfterDelay(6, () -> helper.succeedWhen(() -> {
            for (BlockPos pos : leaves) {
                helper.assertBlockNotPresent(Blocks.OAK_LEAVES, pos);
            }
        }));
    }

    @GameTest(template = "empty7", timeoutTicks = 100, batch = NORMAL)
    public static void playerPlacedLeavesStay(GameTestHelper helper) {
        BlockPos log = new BlockPos(3, 1, 3);
        List<BlockPos> leaves = buildBush(helper, log, true);
        helper.runAfterDelay(5, () -> helper.setBlock(log, Blocks.AIR));
        helper.runAfterDelay(60, () -> {
            for (BlockPos pos : leaves) {
                helper.assertBlockPresent(Blocks.OAK_LEAVES, pos);
            }
            helper.succeed();
        });
    }

    /** With randomTickSpeed 0 vanilla leaves never decay; the mod must not make them decay either. */
    @GameTest(template = "empty7", timeoutTicks = 100, batch = OFF)
    public static void noDecayWhenRandomTicksAreOff(GameTestHelper helper) {
        if (expectInactive()) {
            // The other mod's own rule applies then (Fast Leaf Decay, for one, still clears leaves at 0).
            helper.succeed();
            return;
        }
        BlockPos log = new BlockPos(3, 1, 3);
        List<BlockPos> leaves = buildBush(helper, log, false);
        helper.runAfterDelay(5, () -> helper.setBlock(log, Blocks.AIR));
        helper.runAfterDelay(60, () -> {
            for (BlockPos pos : leaves) {
                helper.assertBlockPresent(Blocks.OAK_LEAVES, pos);
            }
            helper.succeed();
        });
    }

    /** A booked drop is cancelled when the leaf is connected to a log again by the time it fires. */
    @GameTest(template = "empty7", timeoutTicks = 40, batch = NORMAL)
    public static void reconnectedLeafIsSpared(GameTestHelper helper) {
        if (expectInactive()) {
            helper.succeed();
            return;
        }
        BlockPos log = new BlockPos(3, 1, 3);
        BlockPos leafPos = log.east();
        helper.setBlock(log, Blocks.OAK_LOG);
        ServerLevel level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(leafPos);
        // Stale "cut off" state next to a log, without neighbour updates, then a tick as if one was booked.
        level.setBlock(absolute, leaf(LeavesBlock.DECAY_DISTANCE, false), 2 | 16);
        level.scheduleTick(absolute, Blocks.OAK_LEAVES, 1);
        helper.runAfterDelay(15, () -> {
            helper.assertBlockPresent(Blocks.OAK_LEAVES, leafPos);
            int distance = helper.getBlockState(leafPos).getValue(LeavesBlock.DISTANCE);
            if (distance != 1) {
                helper.fail("leaf distance should be recomputed to 1, was " + distance);
            }
            helper.succeed();
        });
    }

    /** A real worldgen oak: chop every log and the whole canopy should be gone within seconds. */
    @GameTest(template = "empty9", timeoutTicks = 200, batch = NORMAL)
    public static void choppedOakClearsCanopy(GameTestHelper helper) {
        if (expectInactive()) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos base = new BlockPos(4, 1, 4);
        helper.setBlock(base.below(), Blocks.DIRT);
        boolean placed = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolderOrThrow(TreeFeatures.OAK).value()
                .place(level, level.getChunkSource().getGenerator(), level.getRandom(), helper.absolutePos(base));
        if (!placed) {
            helper.fail("oak tree did not generate");
            return;
        }
        List<BlockPos> logs = new ArrayList<>();
        List<BlockPos> leaves = new ArrayList<>();
        helper.forEveryBlockInStructure(pos -> {
            BlockState state = helper.getBlockState(pos);
            if (state.is(BlockTags.LOGS)) {
                logs.add(pos.immutable());
            } else if (state.is(Blocks.OAK_LEAVES)) {
                leaves.add(pos.immutable());
            }
        });
        if (logs.isEmpty() || leaves.size() < 20) {
            helper.fail("unexpected tree shape: logs=" + logs.size() + " leaves=" + leaves.size());
            return;
        }
        helper.runAfterDelay(5, () -> logs.forEach(pos -> helper.setBlock(pos, Blocks.AIR)));
        helper.runAfterDelay(6, () -> helper.succeedWhen(() -> {
            for (BlockPos pos : leaves) {
                helper.assertBlockNotPresent(Blocks.OAK_LEAVES, pos);
            }
        }));
    }
}
