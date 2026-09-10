package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
import cn.sd.jrz.alltheimbaium.setup.KillLootEstimator;
import cn.sd.jrz.alltheimbaium.setup.MobFarmCatalog;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 生物农场方块物品。
 * <p>
 * 收容发生在手持阶段：未收容时右击使用会尝试捕捉附近（半径 {@link MobFarmBlock#getCaptureRadius()}）
 * 最近的有效生物并写入物品 NBT，且此时不能当作方块放下；已收容后右击才会当作方块放下。
 * 每台机器只能收容一次（不允许取消/再次收容）。
 */
public class MobFarmItem extends BlockItem {
    private static final Logger log = LoggerFactory.getLogger(MobFarmItem.class);

    public MobFarmItem(Block block) {
        super(block, new Properties().rarity(Rarity.RARE).fireResistant());
    }

    /**
     * 该物品是否已收容生物（查看 BlockEntityTag.entityTag）
     */
    public static boolean hasCreature(ItemStack stack) {
        try {
            if (!stack.hasTag()) {
                return false;
            }
            CompoundTag tag = stack.getTagElement("BlockEntityTag");
            return tag != null && tag.contains("entityTag", Tag.TAG_COMPOUND)
                    && tag.getCompound("entityTag").contains("id", Tag.TAG_STRING);
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        try {
            ItemStack stack = player.getItemInHand(hand);
            if (!hasCreature(stack)) {
                // 未收容：先尝试收容；空中右键无可放置面时收容失败则反馈
                boolean captured = false;
                if (!level.isClientSide) {
                    captured = tryCaptureInHand(player, stack);
                }
                if (!captured && !level.isClientSide) {
                    player.sendSystemMessage(Component.translatable("chat.alltheimbaium.mob_farm.capture_fail"));
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
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
                if (context.getLevel().isClientSide) {
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
            List<LivingEntity> list = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive()
                            && !(e instanceof Player)
                            && !e.isRemoved()
                            && (MobFarmCatalog.isWhitelisted(e.getType())
                            || KillLootEstimator.hasAnyDrop(serverLevel, e.getType())));
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
            CompoundTag tag = new CompoundTag();
            if (!nearest.save(tag) || !tag.contains("id", Tag.TAG_STRING)) {
                tag = new CompoundTag();
                tag.putString("id", EntityType.getKey(nearest.getType()).toString());
            }
            CompoundTag blockTag = stack.getOrCreateTagElement("BlockEntityTag");
            blockTag.put("entityTag", tag);
            String name = nearest.getName().getString();
            nearest.discard();
            player.sendSystemMessage(Component.translatable("chat.alltheimbaium.mob_farm.capture", name));
            return true;
        } catch (Throwable e) {
            log.error("MobFarmItem.tryCaptureInHand error", e);
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        try {
            long level = MobFarmBlock.getInitialLevel();
            String containedName = null;
            ListTag rows = null;
            if (stack.hasTag()) {
                CompoundTag tag = stack.getTagElement("BlockEntityTag");
                if (tag != null) {
                    if (tag.contains("level", Tag.TAG_LONG)) {
                        level = Tool.suit(tag.getLong("level"));
                    }
                    if (tag.contains("entityTag", Tag.TAG_COMPOUND)) {
                        String id = tag.getCompound("entityTag").getString("id");
                        Optional<EntityType<?>> type = EntityType.byString(id);
                        if (type.isPresent()) {
                            containedName = type.get().getDescription().getString();
                        }
                    }
                    if (tag.contains("rows", Tag.TAG_LIST)) {
                        rows = (ListTag) tag.get("rows");
                    }
                }
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
     * 把产物行压成"§e速率/秒§7 名称"列表；速率 = 权重 × 等级 ÷ 500 件/秒
     */
    @Nonnull
    private static List<String> collectProducts(@Nullable ListTag rows, long level) {
        List<String> products = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return products;
        }
        for (int i = 0; i < rows.size(); i++) {
            try {
                CompoundTag c = rows.getCompound(i);
                ItemStack rowStack = ItemStack.of(c);
                if (rowStack.isEmpty()) {
                    continue;
                }
                long weight = c.contains("Weight", Tag.TAG_LONG) ? Tool.suit(c.getLong("Weight")) : 0;
                if (weight <= 0) {
                    continue;
                }
                BigDecimal speed = new BigDecimal(weight).multiply(new BigDecimal(level))
                        .divide(new BigDecimal(500), 3, RoundingMode.HALF_UP);
                products.add("§e" + speed.stripTrailingZeros().toPlainString() + "/秒§7 " + rowStack.getHoverName().getString());
            } catch (Throwable ignored) {
            }
        }
        return products;
    }
}
