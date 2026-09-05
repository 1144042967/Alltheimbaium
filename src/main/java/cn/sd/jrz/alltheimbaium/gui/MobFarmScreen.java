package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
import cn.sd.jrz.alltheimbaium.entity.MobFarmEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 生物农场 GUI（176 宽）。
 * <p>
 * 顶部：标题、收容生物与等级/升级进度；右上标记槽(刷怪蛋/特征物收容)与使用槽(物品自动模拟右击)。
 * 中部：27 个产物行虚拟槽（单击取 1、Shift 取 1 组、空格取到背包满，左下角 AE2 风格缩写）。
 * 下部：六面输出状态按钮 + 主动输出开关 + 清空收容物按钮；最下方玩家背包。
 */
@OnlyIn(Dist.CLIENT)
public class MobFarmScreen extends AbstractContainerScreen<MobFarmMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("alltheimbaium", "textures/gui/mob_farm_gui.png");
    private static final int DARK_TEXT = 0x404040;

    // 六面状态按钮
    private static final int BTN_W = 48;
    private static final int BTN_H = 16;
    private static final int[] BTN_XS = {8, 64, 120};
    private static final int[] BTN_YS = {102, 123};
    // 输出按钮：放在物品栏标签行右侧，右缘与物品栏最右侧(176-8)对齐
    private static final int TOOL_BTN_W = 48;
    private static final int TOOL_BTN_H = 12;
    private static final int OUTPUT_BTN_X = 176 - TOOL_BTN_W - 8;
    private static final int OUTPUT_BTN_Y = 145;

    // 信息/进度条（renderBg 屏幕坐标用这些常量 + leftPos/topPos）
    private static final int INFO_X = 8;
    private static final int PROGRESS_X = 8;
    private static final int PROGRESS_Y = 34;
    private static final int PROGRESS_W = 132;
    private static final int PROGRESS_H = 4;

    private static final float COUNT_SCALE = 0.5F;

    private final FaceButton[] faceButtons = new FaceButton[6];
    private StateButton outputButton;
    private boolean spaceDown = false;

    public MobFarmScreen(MobFarmMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 242;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 6; i++) {
            Direction direction = Direction.values()[i];
            this.faceButtons[i] = new FaceButton(this.leftPos + BTN_XS[i % 3], this.topPos + BTN_YS[i / 3], direction,
                    button -> sendButton(MobFarmMenu.BUTTON_DIR_BASE + direction.ordinal()));
            this.addRenderableWidget(this.faceButtons[i]);
        }
        this.outputButton = new StateButton(this.leftPos + OUTPUT_BTN_X, this.topPos + OUTPUT_BTN_Y, TOOL_BTN_W, TOOL_BTN_H,
                this.menu.isOutputEnabled(),
                Component.translatable("screen.alltheimbaium.mob_farm.output"),
                button -> sendButton(MobFarmMenu.BUTTON_OUTPUT));
        this.addRenderableWidget(this.outputButton);
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.connection.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, id));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            this.spaceDown = true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            this.spaceDown = false;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < MobFarmMenu.MAX_PRODUCTS; i++) {
                Slot slot = this.menu.slots.get(MobFarmMenu.SLOT_PRODUCT_BASE + i);
                if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    int id;
                    if (hasShiftDown()) {
                        id = MobFarmMenu.BUTTON_EXTRACT_STACK_BASE + i;
                    } else if (this.spaceDown) {
                        id = MobFarmMenu.BUTTON_EXTRACT_ALL_BASE + i;
                    } else {
                        id = MobFarmMenu.BUTTON_EXTRACT_ONE_BASE + i;
                    }
                    sendButton(id);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderBackground(guiGraphics);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        // 升级进度条
        int trackLeft = this.leftPos + PROGRESS_X;
        int trackTop = this.topPos + PROGRESS_Y;
        guiGraphics.fill(trackLeft, trackTop, trackLeft + PROGRESS_W, trackTop + PROGRESS_H, 0xFF555555);
        int percent = growthPercent();
        if (percent > 0) {
            int fill = PROGRESS_W * percent / 100;
            guiGraphics.fill(trackLeft, trackTop, trackLeft + fill, trackTop + PROGRESS_H, 0xFF00AA00);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 标题：金色，无阴影；收容/等级描述：白字 + 黑阴影（同补给箱风格）
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFAA00, false);
        // 物品栏标签：黑色、不加阴影
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x000000, false);
        // 收容生物
        int containedId = this.menu.getContainedEntityId();
        Component contained;
        if (containedId > 0) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.byId(containedId);
            contained = type == null ? Component.literal("?") : type.getDescription();
        } else {
            contained = Component.translatable("screen.alltheimbaium.mob_farm.empty");
        }
        guiGraphics.drawString(this.font, Component.translatable("screen.alltheimbaium.mob_farm.contained", contained), INFO_X, 14, 0xFFFFFF, true);
        // 等级 + 升级百分比
        guiGraphics.drawString(this.font, Component.translatable("screen.alltheimbaium.mob_farm.level_progress",
                this.menu.getLevel(), growthPercent()), INFO_X, 24, 0xFFFFFF, true);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.flush();
        drawSlotCounts(guiGraphics);
        this.outputButton.setState(this.menu.isOutputEnabled());
        // 六面按钮 tooltip
        for (FaceButton faceButton : this.faceButtons) {
            if (faceButton.isHovered()) {
                guiGraphics.renderTooltip(this.font, faceButton.buildTooltip(), Optional.empty(), mouseX, mouseY);
            }
        }
        renderSpecialSlotTooltips(guiGraphics, mouseX, mouseY);
        // 经由本类重载：产物槽显示"数量+速度"，其它槽走默认
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /** 找到鼠标悬浮的槽位（在渲染 tooltip 时机，容器内部 hoveredSlot 不可靠，自行用 isHovering 判断） */
    @Nullable
    private Slot findHoveredSlot(int mouseX, int mouseY) {
        for (Slot slot : this.menu.slots) {
            if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    /** 产物槽自定义 tooltip（数量 + 生成速度），覆盖默认单件 tooltip */
    @Override
    protected void renderTooltip(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Slot slot = this.findHoveredSlot(mouseX, mouseY);
        if (slot != null && !slot.getItem().isEmpty()) {
            int idx = slot.index;
            if (idx >= MobFarmMenu.SLOT_PRODUCT_BASE && idx < MobFarmMenu.SLOT_PRODUCT_BASE + MobFarmMenu.MAX_PRODUCTS) {
                int i = idx - MobFarmMenu.SLOT_PRODUCT_BASE;
                long stock = this.menu.getProductStock(i);
                long weight = this.menu.getProductWeight(i);
                int mode = this.menu.getRowMode(i);
                int toolStatus = this.menu.getToolStatus();
                List<Component> lines = new ArrayList<>();
                lines.add(slot.getItem().getHoverName().copy().withStyle(ChatFormatting.WHITE));
                lines.add(Component.translatable("screen.alltheimbaium.mob_farm.count", stock));
                if (mode == 1) {
                    // 使用槽正在产出该物品：标注来源 + 速度与刷怪蛋一致
                    lines.add(Component.translatable("screen.alltheimbaium.mob_farm.tool_from"));
                    lines.add(Component.translatable("screen.alltheimbaium.mob_farm.rate",
                            formatSpeed(Math.max(1, weight), this.menu.getLevel())));
                } else if (mode == 2) {
                    // 使用槽工具行但当前不产出（缺少工具/工具不符）：生成速度 0
                    lines.add(Component.translatable(toolStatus == 1
                            ? "screen.alltheimbaium.mob_farm.missing_tool"
                            : "screen.alltheimbaium.mob_farm.wrong_tool"));
                    lines.add(Component.translatable("screen.alltheimbaium.mob_farm.rate_zero"));
                } else if (weight > 0 && this.menu.getContainedEntityId() > 0) {
                    lines.add(Component.translatable("screen.alltheimbaium.mob_farm.rate", formatSpeed(weight, this.menu.getLevel())));
                } else if (weight <= 0) {
                    lines.add(Component.translatable("screen.alltheimbaium.mob_farm.manual"));
                }
                guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderSpecialSlotTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 收容/使用合一槽（槽空时按状态显示说明）
        Slot special = this.menu.slots.get(MobFarmMenu.SLOT_SPECIAL);
        if (!special.hasItem() && this.isHovering(special.x, special.y, 16, 16, mouseX, mouseY)) {
            boolean contained = this.menu.getContainedEntityId() > 0;
            guiGraphics.renderTooltip(this.font, List.of(
                    Component.translatable(contained
                            ? "screen.alltheimbaium.mob_farm.use_tooltip.1"
                            : "screen.alltheimbaium.mob_farm.marker_tooltip.1"),
                    Component.translatable(contained
                            ? "screen.alltheimbaium.mob_farm.use_tooltip.2"
                            : "screen.alltheimbaium.mob_farm.marker_tooltip.2")
            ), Optional.empty(), mouseX, mouseY);
        }
    }

    /** 绘制 27 个产物槽左下角的存量缩写（AE2 风格） */
    private void drawSlotCounts(GuiGraphics guiGraphics) {
        for (int i = 0; i < MobFarmMenu.MAX_PRODUCTS; i++) {
            long stock = this.menu.getProductStock(i);
            if (stock <= 0) {
                continue;
            }
            String text = formatCount(stock);
            Slot slot = this.menu.slots.get(MobFarmMenu.SLOT_PRODUCT_BASE + i);
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y + 12;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 300);
            guiGraphics.pose().scale(COUNT_SCALE, COUNT_SCALE, 1.0F);
            guiGraphics.drawString(this.font, text, (int) (x / COUNT_SCALE), (int) (y / COUNT_SCALE), 0xFFFFFF, true);
            guiGraphics.pose().popPose();
        }
    }

    private int growthPercent() {
        long second = Math.max(1, MobFarmBlock.getLevelUpIntervalSeconds());
        double percent = this.menu.getTickCount() / (second * 20.0) * 100.0;
        return (int) Math.max(0, Math.min(100, percent));
    }

    /** 生成速度格式化：重量×等级 /500 (件/秒) */
    private static String formatSpeed(long weight, long level) {
        BigDecimal speed = new BigDecimal(weight).multiply(new BigDecimal(level))
                .divide(new BigDecimal(500), 3, RoundingMode.HALF_UP);
        return speed.stripTrailingZeros().toPlainString();
    }

    private static String formatCount(long value) {
        if (value < 1000) {
            return String.valueOf(value);
        }
        if (value < 1_000_000) {
            return String.format("%.1fK", value / 1000.0);
        }
        if (value < 1_000_000_000) {
            return String.format("%.1fM", value / 1_000_000.0);
        }
        if (value < 1_000_000_000_000L) {
            return String.format("%.1fG", value / 1_000_000_000.0);
        }
        if (value < 1_000_000_000_000_000L) {
            return String.format("%.1fT", value / 1_000_000_000_000.0);
        }
        return String.format("%.1fP", value / 1_000_000_000_000_000.0);
    }

    private ItemStack getNeighborIcon(Direction direction) {
        Item item = getNeighborState(direction).getBlock().asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private Component getNeighborName(Direction direction) {
        return getNeighborState(direction).getBlock().getName();
    }

    private BlockState getNeighborState(Direction direction) {
        if (this.minecraft != null && this.minecraft.level != null && this.menu.entity != null) {
            return this.minecraft.level.getBlockState(this.menu.entity.getBlockPos().relative(direction));
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static String directionSymbol(Direction direction) {
        return switch (direction) {
            case DOWN -> "↓";
            case UP -> "↑";
            case NORTH -> "▲";
            case SOUTH -> "▼";
            case WEST -> "◀";
            case EAST -> "▶";
        };
    }

    private static boolean isChinese() {
        return Minecraft.getInstance().getLanguageManager().getSelected().startsWith("zh");
    }

    /** 六面输出状态按钮（随机/禁用/槽1~槽27 循环），显示相邻目标贴图 + 输出材料贴图/文字 */
    private class FaceButton extends SimpleButton {
        private final Direction direction;

        FaceButton(int x, int y, Direction direction, OnPress onPress) {
            super(x, y, BTN_W, BTN_H, Component.literal(""), onPress);
            this.direction = direction;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int state = MobFarmScreen.this.menu.getDirectionState(this.direction);
            int color;
            if (state == MobFarmEntity.STATE_DISABLED) {
                color = 0xFFAA0000;
            } else if (state == MobFarmEntity.STATE_RANDOM) {
                color = 0xFF00AA00;
            } else {
                color = 0xFF3A3A6B;
            }
            renderButton(guiGraphics, color);
            ItemStack neighborIcon = MobFarmScreen.this.getNeighborIcon(this.direction);
            boolean isChinese = isChinese();
            String dirName = Component.translatable("screen.alltheimbaium.mob_farm.face." + this.direction.getName()).getString();
            if (!isChinese) {
                dirName = directionSymbol(this.direction);
            }
            String stateText = null;
            ItemStack slotIcon = ItemStack.EMPTY;
            String slotText = null;
            if (state >= MobFarmEntity.STATE_SLOT_BASE) {
                int slot = state - MobFarmEntity.STATE_SLOT_BASE;
                slotIcon = MobFarmScreen.this.menu.getProductStack(slot);
                if (slotIcon.isEmpty()) {
                    slotText = String.valueOf(slot + 1);
                }
            } else if (state == MobFarmEntity.STATE_RANDOM) {
                stateText = isChinese
                        ? Component.translatable("screen.alltheimbaium.mob_farm.random").getString()
                        : "?";
            } else {
                stateText = isChinese
                        ? Component.translatable("screen.alltheimbaium.mob_farm.disabled").getString()
                        : "×";
            }
            boolean hasTargetIcon = !neighborIcon.isEmpty();
            boolean hasSlotIcon = !slotIcon.isEmpty();
            String rightText = hasSlotIcon ? null : (slotText != null ? slotText : stateText);
            int contentW = 0;
            contentW += hasTargetIcon ? 16 : MobFarmScreen.this.font.width(dirName);
            contentW += MobFarmScreen.this.font.width("←") + 2;
            contentW += hasSlotIcon ? 16 : MobFarmScreen.this.font.width(rightText);
            contentW += 2;
            int x = this.getX() + Math.max(1, (BTN_W - contentW) / 2);
            int iconY = this.getY();
            int textY = this.getY() + 4;
            if (hasTargetIcon) {
                guiGraphics.renderItem(neighborIcon, x, iconY);
                x += 18;
            } else {
                guiGraphics.drawString(MobFarmScreen.this.font, dirName, x, textY, 0xFFFFFFFF, true);
                x += MobFarmScreen.this.font.width(dirName) + 2;
            }
            guiGraphics.drawString(MobFarmScreen.this.font, "←", x, textY, 0xFFFFFFFF, true);
            x += MobFarmScreen.this.font.width("←") + 2;
            if (hasSlotIcon) {
                guiGraphics.renderItem(slotIcon, x, iconY);
            } else {
                guiGraphics.drawString(MobFarmScreen.this.font, rightText, x, textY, 0xFFFFFFFF, true);
            }
        }

        List<Component> buildTooltip() {
            int state = MobFarmScreen.this.menu.getDirectionState(this.direction);
            ItemStack neighborIcon = MobFarmScreen.this.getNeighborIcon(this.direction);
            String dirName = Component.translatable("screen.alltheimbaium.mob_farm.face." + this.direction.getName()).getString();
            List<Component> lines = new ArrayList<>();
            if (!neighborIcon.isEmpty()) {
                lines.add(Component.translatable("screen.alltheimbaium.mob_farm.tooltip_target",
                        MobFarmScreen.this.getNeighborName(this.direction)));
            } else {
                lines.add(Component.translatable("screen.alltheimbaium.mob_farm.tooltip_target",
                        Component.translatable("screen.alltheimbaium.mob_farm.tooltip_no_target")));
            }
            lines.add(Component.translatable("screen.alltheimbaium.mob_farm.tooltip_direction", dirName));
            if (state >= MobFarmEntity.STATE_SLOT_BASE) {
                int slot = state - MobFarmEntity.STATE_SLOT_BASE;
                ItemStack materialIcon = MobFarmScreen.this.menu.getProductStack(slot);
                if (!materialIcon.isEmpty()) {
                    lines.add(Component.translatable("screen.alltheimbaium.mob_farm.tooltip_material", materialIcon.getHoverName()));
                } else {
                    lines.add(Component.translatable("screen.alltheimbaium.mob_farm.tooltip_material",
                            Component.translatable("screen.alltheimbaium.mob_farm.slot_number", slot + 1)));
                }
            } else if (state == MobFarmEntity.STATE_RANDOM) {
                lines.add(Component.translatable("screen.alltheimbaium.mob_farm.tooltip_material",
                        Component.translatable("screen.alltheimbaium.mob_farm.random")));
            } else {
                lines.add(Component.translatable("screen.alltheimbaium.mob_farm.tooltip_material",
                        Component.translatable("screen.alltheimbaium.mob_farm.disabled")));
            }
            return lines;
        }
    }

    private class StateButton extends SimpleButton {
        private boolean state;

        StateButton(int x, int y, int width, int height, boolean initial, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress);
            this.state = initial;
        }

        void setState(boolean state) {
            this.state = state;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButton(guiGraphics, this.state ? 0xFF00AA00 : 0xFFAA0000);
        }
    }

    private abstract class SimpleButton extends Button {
        SimpleButton(int x, int y, int width, int height, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        }

        protected void renderButton(GuiGraphics guiGraphics, int color) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            int borderColor = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor);
            guiGraphics.drawCenteredString(MobFarmScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }
}
