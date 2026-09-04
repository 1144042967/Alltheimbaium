package cn.sd.jrz.alltheimbaium.setup;

import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 击杀战利品表采样估算：对某实体类型的击杀战利品表跑固定次数，统计每种物品每次击杀的平均掉落量，
 * 用于把"生物本身的击杀掉落"折算成确定性产物的权重。结果按实体类型全局缓存。
 */
public final class KillLootEstimator {
    private static final Logger log = LoggerFactory.getLogger(KillLootEstimator.class);
    private static final Map<EntityType<?>, List<SampledDrop>> CACHE = new ConcurrentHashMap<>();

    private KillLootEstimator() {
    }

    /** 一次击杀掉落的平均估算：物品 + 每次平均数量 */
    public record SampledDrop(@Nonnull Item item, double avgPerKill) {
    }

    /**
     * 估算某实体击杀掉落（服务端）。无击杀掉落或采样失败返回空列表。
     */
    @Nonnull
    public static List<SampledDrop> estimate(ServerLevel serverLevel, @Nonnull EntityType<?> type) {
        return CACHE.computeIfAbsent(type, t -> doEstimate(serverLevel, t));
    }

    /**
     * 该实体是否有（采样得到的）击杀掉落，用作空罐捕捉"有击杀掉落"判定
     */
    public static boolean hasAnyDrop(ServerLevel serverLevel, @Nonnull EntityType<?> type) {
        try {
            return !estimate(serverLevel, type).isEmpty();
        } catch (Throwable e) {
            log.error("KillLootEstimator.hasAnyDrop error", e);
        }
        return false;
    }

    @Nonnull
    private static List<SampledDrop> doEstimate(ServerLevel serverLevel, EntityType<?> type) {
        List<SampledDrop> result = new ArrayList<>();
        try {
            ResourceLocation lootId = type.getDefaultLootTable();
            if (lootId == null) {
                return result;
            }
            LootTable table = serverLevel.getServer().getLootData().getLootTable(lootId);
            int kills = Math.max(1, MobFarmBlock.getSampleKills());
            Entity probe = null;
            try {
                probe = type.create(serverLevel);
            } catch (Throwable ignored) {
                // 无法创建探测实例时仍可尝试掷表
            }
            LootParams params = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(BlockPos.ZERO))
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, probe)
                    .withOptionalParameter(LootContextParams.DAMAGE_SOURCE, serverLevel.damageSources().generic())
                    .create(LootContextParamSets.ENTITY);
            Map<Item, Long> acc = new HashMap<>();
            long emptyRolls = 0;
            for (int i = 0; i < kills; i++) {
                List<ItemStack> drops = table.getRandomItems(params);
                if (drops.isEmpty()) {
                    emptyRolls++;
                    continue;
                }
                for (ItemStack stack : drops) {
                    if (!stack.isEmpty()) {
                        acc.merge(stack.getItem(), (long) stack.getCount(), Long::sum);
                    }
                }
            }
            // 有掉落时按 成功击杀 统计平均（避免把"可能不掉"的击杀拉低主掉落权重）
            long effective = kills - emptyRolls;
            if (effective <= 0) {
                return result;
            }
            acc.forEach((item, count) -> result.add(new SampledDrop(item, count / (double) effective)));
        } catch (Throwable e) {
            log.warn("KillLootEstimator.doEstimate error for {}", type, e);
        }
        return result;
    }
}
