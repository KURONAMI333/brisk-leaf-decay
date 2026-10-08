package com.kuronami.briskleafdecay.mixin;

import com.kuronami.briskleafdecay.BriskLeafDecayCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla leaves only learn they are cut off through a scheduled tick (which recomputes DISTANCE),
 * and then wait for a random tick to actually fall, which takes minutes on average.
 * <p>
 * Here the scheduled tick does both: when a leaf ends up at the decay distance it books one more
 * tick a few ticks later, and that tick drops it through the block's own {@code randomTick}, so
 * loot tables and modded leaf behaviour stay exactly as they would be on a natural decay.
 * <p>
 * With {@code randomTickSpeed} at 0 vanilla leaves never decay, so this does nothing there either.
 */
@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin extends Block {

    private LeavesBlockMixin(Properties properties) {
        super(properties);
    }

    @Shadow
    protected abstract boolean decaying(BlockState state);

    @Shadow
    private static BlockState updateDistance(BlockState state, LevelAccessor level, BlockPos pos) {
        throw new AssertionError();
    }

    @Unique
    private static boolean brisk_leaf_decay$enabled(ServerLevel level) {
        return BriskLeafDecayCommon.isActive() && level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING) > 0;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void brisk_leaf_decay$dropIfStillCutOff(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!brisk_leaf_decay$enabled(level) || !this.decaying(state)) {
            return;
        }
        // Recheck against the current neighbours: a log placed since the tick was booked saves the leaf.
        if (this.decaying(updateDistance(state, level, pos))) {
            this.randomTick(state, level, pos, random);
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void brisk_leaf_decay$bookDrop(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!brisk_leaf_decay$enabled(level)) {
            return;
        }
        BlockState now = level.getBlockState(pos);
        if (now.is(this) && this.decaying(now)) {
            int spread = BriskLeafDecayCommon.MAX_DELAY_TICKS - BriskLeafDecayCommon.MIN_DELAY_TICKS + 1;
            level.scheduleTick(pos, this, BriskLeafDecayCommon.MIN_DELAY_TICKS + random.nextInt(spread));
        }
    }
}
