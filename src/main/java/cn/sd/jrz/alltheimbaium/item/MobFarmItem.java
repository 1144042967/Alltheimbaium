package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
import cn.sd.jrz.alltheimbaium.setup.KillLootEstimator;
import cn.sd.jrz.alltheimbaium.setup.MobFarmCatalog;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 生物农场方块物品。
 * <p>
 * 收容发生在手持阶段：未收容时右击使用会尝试捕捉附近（半径 {@link MobFarmBlock#getCaptureRadius()}）
 * 最近的有效生物并写入物品 NBT，且此时不能当作方块放下；已收容后右击才会当作方块放下。
 * 每台机器只能收容一次（不允许取消/再次收容）。
 */
public class MobFarmItem extends BlockItem {
    private static final Logger log = LoggerFactory.getLogger(MobFarmItem.class);

    /** 产物行在方块实体数据里的键（与 MobFarmEntity 的 KEY_ROWS 一致） */
    private static final String ROWS_KEY = "rows";

    /**
     * 26.x：注册 id 与 block. 语言键前缀由 Registration 的 blockItemProps(key) 灌进属性里，这里只加品级
     */
    public MobFarmItem(Block block, Item.Properties properties) {
        super(block, properties.rarity(Rarity.RARE).fireResistant());
    }

    /**
     * 该物品是否已收容生物（查看 BlockEntityTag.entityTag）
     */
    public static boolean hasCreature(ItemStack stack) {
        try {
            CompoundTag tag = Tool.getBlockEntityTag(stack);
            // 26.x：CompoundTag 的取值方法一律返回 Optional，"按类型判断"的 contains 重载已删除，
            // 复合标签改用 getCompoundOrEmpty
            return tag != null && tag.getCompoundOrEmpty("entityTag").contains("id");
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * 26.x：{@code InteractionResultHolder} 已删除，改用 sealed 的 {@link InteractionResult}。
     * 原 {@code sidedSuccess(stack, isClientSide)} 统一记为 {@code SUCCESS.heldItemTransformedTo(stack)}——
     * 客户端挥手动画由 SUCCESS 自带的 SwingSource.CLIENT 负责，服务端照常结算。
     */
    @Override
    @Nonnull
    public InteractionResult use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        try {
            ItemStack stack = player.getItemInHand(hand);
            if (!hasCreature(stack)) {
                // 未收容：先尝试收容；空中右键无可放置面时收容失败则反馈
                boolean captured = false;
                if (!level.isClientSide()) {
                    captured = tryCaptureInHand(player, stack);
                }
                if (!captured && !level.isClientSide()) {
                    player.sendSystemMessage(Component.translatable("chat.alltheimbaium.mob_farm.capture_fail"));
                }
                return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
            }
            return super.use(level, player, hand);
        } catch (Throwable e) {
            log.error("MobFarmItem.use error", e);
        }
        return super.use(level, player, hand);
    }

    @Override
    @Nonnull
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        try {
            ItemStack stack = context.getItemInHand();
            if (!hasCreature(stack)) {
                // 客户端只表示"已处理"，由服务端决定收容或放置，避免两端重复
                if (context.getLevel().isClientSide()) {
                    return InteractionResult.SUCCESS;
                }
                // 未收容：先尝试收容；收容失败再当作方块放下
                if (tryCaptureInHand(context.getPlayer(), stack)) {
                    return InteractionResult.SUCCESS;
                }
                return super.useOn(context);
            }
            return super.useOn(context);
        } catch (Throwable e) {
            log.error("MobFarmItem.useOn error", e);
        }
        return super.useOn(context);
    }

    /**
     * 手持收容：捕捉玩家周围半径内最近的有效生物（白名单，或采样有击杀掉落）。
     *
     * @return 是否收容成功
     */
    private boolean tryCaptureInHand(@Nullable Player player, ItemStack stack) {
        try {
            if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
                return false;
            }
            int radius = Math.max(1, MobFarmBlock.getCaptureRadius());
            AABB box = player.getBoundingBox().inflate(radius);
            // 只收真正的生物（Mob）：盔甲架这类 LivingEntity 有击杀战利品表（掉落自身），
            // 单靠 hasAnyDrop 过滤会把它一并放行，等于可以"养殖"盔甲架。
            // 白名单里的实体不受此限——模组加的自定义非 Mob 生物写进 signature_whitelist 仍可收容。
            List<LivingEntity> list = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive()
                            && !(e instanceof Player)
                            && !e.isRemoved()
                            && (MobFarmCatalog.isWhitelisted(e.getType())
                            || (e instanceof Mob && KillLootEstimator.hasAnyDrop(serverLevel, e.getType()))));
            if (list.isEmpty()) {
                return false;
            }
            LivingEntity nearest = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity e : list) {
                double d = player.distanceToSqr(e);
                if (d < best) {
                    best = d;
                    nearest = e;
                }
            }
            if (nearest == null) {
                return false;
            }
            // 26.x：Entity.save(CompoundTag) 已删除，改写成 ValueOutput；buildResult() 拿到的就是活标签
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, serverLevel.registryAccess());
            CompoundTag tag;
            if (!nearest.save(output) || !output.buildResult().contains("id")) {
                tag = new CompoundTag();
                tag.putString("id", EntityType.getKey(nearest.getType()).toString());
            } else {
                tag = output.buildResult();
            }
            // 1.21：组件不是活引用，读出来是副本，改完必须写回
            CompoundTag blockTag = Tool.getBlockEntityTagOrEmpty(stack);
            blockTag.put("entityTag", tag);
            // 必须带方块实体类型：BLOCK_ENTITY_DATA 组件要求标签里有 id，缺了存档即崩
            Tool.setBlockEntityTag(stack, Registration.MOB_FARM_ENTITY.get(), blockTag);
            String name = nearest.getName().getString();
            nearest.discard();
            player.sendSystemMessage(Component.translatable("chat.alltheimbaium.mob_farm.capture", name));
            return true;
        } catch (Throwable e) {
            log.error("MobFarmItem.tryCaptureInHand error", e);
        }
        return false;
    }

    /**
     * 26.x：tooltip 出口由 List&lt;Component&gt; 换成 Consumer&lt;Component&gt;，@OnlyIn 已删除
     */
    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context,
                                @Nonnull TooltipDisplay display, @Nonnull Consumer<Component> tooltip,
                                @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        try {
            long level = MobFarmBlock.getInitialLevel();
            String containedName = null;
            // 26.x：产物行不再手写 NBT，统一由方块实体的 ValueOutput + Tool.WeightedToolRow 记录类型托管，
            // 这里用 TagValueInput 把物品上的 BlockEntityTag 当成 ValueInput 读回来（与实体侧对称）
            HolderLookup.Provider registries = registriesOf(context);
            List<Tool.WeightedToolRow> rows = new ArrayList<>();
            CompoundTag tag = Tool.getBlockEntityTag(stack);
            if (tag != null) {
                if (tag.contains("level")) {
                    level = Tool.suit(tag.getLongOr("level", level));
                }
                String containedId = tag.getCompoundOrEmpty("entityTag").getStringOr("id", "");
                if (!containedId.isEmpty()) {
                    Optional<EntityType<?>> type = EntityType.byString(containedId);
                    if (type.isPresent()) {
                        containedName = type.get().getDescription().getString();
                    }
                }
                rows = Tool.readRows(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag),
                        ROWS_KEY, Tool.WeightedToolRow.CODEC);
            }
            Tip tip = Tip.of(tooltip)
                    .head(stack, "tip.alltheimbaium.type.agriculture")
                    .summary("item.alltheimbaium.mob_farm.summary");
            if (containedName != null) {
                tip.state("item.alltheimbaium.mob_farm.state.contained", containedName);
            }
            tip.state("item.alltheimbaium.mob_farm.state.level", level);
            // 产物表最多 27 行，压成一行展示，避免把 tooltip 撑爆
            List<String> products = collectProducts(rows, level);
            if (!products.isEmpty()) {
                tip.raw(Tip.inline(products, "tip.alltheimbaium.more"));
            }
            tip.usage("item.alltheimbaium.mob_farm.usage.1",
                            "item.alltheimbaium.mob_farm.usage.2",
                            "item.alltheimbaium.mob_farm.usage.3",
                            "item.alltheimbaium.mob_farm.usage.4")
                    .params("item.alltheimbaium.mob_farm.param.1")
                    .warn("item.alltheimbaium.mob_farm.warn.1");
        } catch (Throwable e) {
            log.error("MobFarmItem.appendHoverText error", e);
        }
    }

    /**
     * tooltip 上下文带注册表访问器（物品组件里的注册名靠它解析）；客户端拿不到时回退到 Tool 的兜底
     */
    @Nonnull
    private static HolderLookup.Provider registriesOf(@Nonnull Item.TooltipContext context) {
        HolderLookup.Provider registries = context.registries();
        return registries != null ? registries : Tool.registries();
    }

    /**
     * 把产物行压成"§e速率/秒§7 名称"列表；速率 = 权重 × 等级 ÷ 500 件/秒
     */
    @Nonnull
    private static List<String> collectProducts(@Nonnull List<Tool.WeightedToolRow> rows, long level) {
        List<String> products = new ArrayList<>();
        for (Tool.WeightedToolRow row : rows) {
            try {
                if (row == null || row.item() == null || row.item().isEmpty() || row.weight() <= 0) {
                    continue;
                }
                BigDecimal speed = new BigDecimal(row.weight()).multiply(new BigDecimal(level))
                        .divide(new BigDecimal(500), 3, RoundingMode.HALF_UP);
                products.add("§e" + speed.stripTrailingZeros().toPlainString()
                        + Component.translatable("item.alltheimbaium.tooltip.per_second").getString()
                        + "§7 " + row.item().getHoverName().getString());
            } catch (Throwable ignored) {
            }
        }
        return products;
    }
}
