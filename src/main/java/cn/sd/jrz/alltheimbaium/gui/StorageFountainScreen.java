package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.block.StorageFountainBlock;
import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 存储方块制造机 GUI（新材质 GUI，176 宽）。
 * <p>
 * 黑色背景区域：第一行增长进度条，第二行增长百分比，第三行下次增长数值，第四行产量；
 * 右下角为标记槽（放入物品标记/取消标记）。下方为 9 个已标记物品槽（单击提取 1 个、shift 提取 1 组、空格提取到背包满，
 * 槽位左下角以 AE2 风格缩写显示数量）。再下方为六面输出状态按钮（随机/禁用/槽1~槽9，槽位状态显示对应物品图标），
 * 左下物品栏标签 + 右侧"输出"总开关按钮，最下方为玩家物品栏。
 */
@OnlyIn(Dist.CLIENT)
public class StorageFountainScreen extends AbstractContainerScreen<StorageFountainMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("alltheimbaium", "textures/gui/storage_fountain_gui.png");
    /** 黑色信息面板上的浅色文字 */
    private static final int TEXT_COLOR = 0xC6C6C6;

    // 增长进度条（黑色背景区域第一行）
    private static final int PROGRESS_X = 10;
    private static final int PROGRESS_Y = 21;
    private static final int PROGRESS_W = 156;
    private static final int PROGRESS_H = 4;
    // 信息文字行（第二/三/四行）
    private static final int INFO_X = 10;
    private static final int INFO_Y = 30;
    private static final int INFO_LINE = 12;

    // 六面状态按钮（2 行 x 3 列）
    private static final int BTN_W = 48;
    private static final int BTN_H = 16;
    private static final int[] BTN_XS = {8, 64, 120};
    private static final int[] BTN_YS = {93, 114};
    // "输出"总开关按钮（左下物品栏标签右侧靠右）
    private static final int OUTPUT_BTN_W = 48;
    private static final int OUTPUT_BTN_H = 13;
    private static final int OUTPUT_BTN_X = 176 - OUTPUT_BTN_W - 8;
    private static final int OUTPUT_BTN_Y = 135;

    // 已标记物品槽左下角数量文字偏移
    private static final int COUNT_X = 0;
    private static final int COUNT_Y = 12;
    /** AE2 风格数量文字缩放（0.5 倍小字体，绘制在槽位左下角且位于图标之上） */
    private static final float COUNT_SCALE = 0.5F;

    private final FaceButton[] faceButtons = new FaceButton[6];
    private StateButton outputButton;
    /** 空格键是否按下（空格+单击物品槽 = 提取到背包满） */
    private boolean spaceDown = false;

    public StorageFountainScreen(StorageFountainMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 233;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // 六个面状态按钮，按钮 id 与 Direction.values() 顺序一致（0~5）
        for (int i = 0; i < 6; i++) {
            Direction direction = Direction.values()[i];
            this.faceButtons[i] = new FaceButton(this.leftPos + BTN_XS[i % 3], this.topPos + BTN_YS[i / 3], direction,
                    button -> sendButton(StorageFountainMenu.BUTTON_DIR_BASE + direction.ordinal()));
            this.addRenderableWidget(this.faceButtons[i]);
        }
        // "输出"总开关按钮
        this.outputButton = new StateButton(this.leftPos + OUTPUT_BTN_X, this.topPos + OUTPUT_BTN_Y, OUTPUT_BTN_W, OUTPUT_BTN_H,
                this.menu.isOutputEnabled(),
                Component.translatable("screen.alltheimbaium.storage_fountain.output"),
                button -> sendButton(StorageFountainMenu.BUTTON_OUTPUT));
        this.addRenderableWidget(this.outputButton);
    }

    /**
     * 发送容器按钮点击到服务端
     */
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

    /**
     * 拦截已标记物品槽的点击：单击提取一个、Shift+单击提取一组、空格+单击提取到背包满
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < 9; i++) {
                Slot slot = this.menu.slots.get(1 + i);
                if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    int id;
                    if (hasShiftDown()) {
                        id = StorageFountainMenu.BUTTON_EXTRACT_STACK_BASE + i;
                    } else if (this.spaceDown) {
                        id = StorageFountainMenu.BUTTON_EXTRACT_ALL_BASE + i;
                    } else {
                        id = StorageFountainMenu.BUTTON_EXTRACT_ONE_BASE + i;
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
        // 主背景（槽位框、黑色信息面板、标记槽、物品栏槽位均已绘制在图上）
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        // 第一行：增长进度条
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
        // 标题与物品栏标签：亮色背景上用深色文字
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        // 黑色信息面板上的四行信息（局部坐标）：产量/下次增长均以 /tick 为单位。
        // 下次增长 = 下次要增长的数值（增量），不是增长后的值。
        long output = this.menu.getOutput();
        long step = this.menu.getStep();
        double carry = StorageFountainBlock.getCarry();
        double currentRate = output / carry;
        double nextIncrease = step / carry;
        guiGraphics.drawString(this.font, Component.translatable("screen.alltheimbaium.storage_fountain.growth", growthPercent()), INFO_X, INFO_Y, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.alltheimbaium.storage_fountain.next", formatRate(nextIncrease)), INFO_X, INFO_Y + INFO_LINE, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.alltheimbaium.storage_fountain.output_rate", formatRate(currentRate)), INFO_X, INFO_Y + INFO_LINE * 2, TEXT_COLOR, false);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 先刷新已绘制的内容（物品图标已通过 renderItem 内部 flush 到屏幕），
        // 确保随后绘制的数量文字位于物品图标之上
        guiGraphics.flush();
        drawSlotCounts(guiGraphics);
        // 刷新开关状态
        this.outputButton.setState(this.menu.isOutputEnabled());
        // 渲染六面按钮 tooltip（始终显示完整的输出目的/输出方向/输出材料说明）
        for (FaceButton faceButton : this.faceButtons) {
            if (faceButton.isHovered()) {
                guiGraphics.renderTooltip(this.font, faceButton.buildTooltip(), Optional.empty(), mouseX, mouseY);
            }
        }
        // 渲染鼠标悬浮物品的信息提示窗
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * 绘制 9 个已标记物品槽左下角的存量数量（AE2 风格缩写，如 1.1K、2.1M）。
     * 参考 AE2 的显示方式：缩小字体（0.5 倍）。
     * 深度层级：物品 z≈250 < 数量文字 z=300 < tooltip 背景 z=400，
     * 因此数量文字盖在物品之上，又位于 tooltip 背景之下（tooltip 显示时背景可覆盖它）。
     */
    private void drawSlotCounts(GuiGraphics guiGraphics) {
        for (int i = 0; i < 9; i++) {
            long units = this.menu.getMarkedCount(i);
            if (units <= 0) {
                continue;
            }
            long items = units / StorageFountainBlock.getCarry();
            if (items <= 0) {
                continue;
            }
            String text = formatCount(items);
            Slot slot = this.menu.slots.get(1 + i);
            int x = this.leftPos + slot.x + COUNT_X;
            int y = this.topPos + slot.y + COUNT_Y;
            guiGraphics.pose().pushPose();
            // 缩放 XY 实现小字体（坐标相应放大绘制）
            guiGraphics.pose().translate(0, 0, 300);
            guiGraphics.pose().scale(COUNT_SCALE, COUNT_SCALE, 1.0F);
            guiGraphics.drawString(this.font, text, (int) (x / COUNT_SCALE), (int) (y / COUNT_SCALE), 0xFFFFFF, true);
            guiGraphics.pose().popPose();
        }
    }

    /**
     * 计算增长进度百分比（0-100）
     */
    private int growthPercent() {
        long second = Math.max(1, this.menu.getGrowthIntervalSeconds());
        double percent = this.menu.getTickCount() / (second * 20.0) * 100.0;
        return (int) Math.max(0, Math.min(100, percent));
    }

    /**
     * 产量/下次增长格式化（单位：物品/每 tick），小数值保留更多精度，大数值用单位缩写
     */
    private static String formatRate(double itemsPerTick) {
        if (itemsPerTick < 10) {
            return String.format("%.3f", itemsPerTick);
        }
        if (itemsPerTick < 100) {
            return String.format("%.2f", itemsPerTick);
        }
        if (itemsPerTick < 1000) {
            return String.format("%.1f", itemsPerTick);
        }
        if (itemsPerTick < 1_000_000) {
            return String.format("%.1fK", itemsPerTick / 1000.0);
        }
        if (itemsPerTick < 1_000_000_000) {
            return String.format("%.1fM", itemsPerTick / 1_000_000.0);
        }
        if (itemsPerTick < 1_000_000_000_000L) {
            return String.format("%.1fG", itemsPerTick / 1_000_000_000.0);
        }
        return String.format("%.1fT", itemsPerTick / 1_000_000_000_000.0);
    }

    /**
     * 存量数量单位缩写：参考 AE2，1.1K、2.1M 等
     */
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
        if (value < 1_000_000_000_000_000_000L) {
            return String.format("%.1fP", value / 1_000_000_000_000_000.0);
        }
        return String.format("%.1fE", value / 1_000_000_000_000_000_000.0);
    }

    /**
     * 获取指定方向相邻方块的物品图标（无方块或方块无对应物品时返回空）。
     * 用于六面按钮显示"输出目的"贴图。
     */
    private ItemStack getNeighborIcon(Direction direction) {
        Item item = getNeighborState(direction).getBlock().asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /**
     * 获取指定方向相邻方块的显示名（无方块时返回方块自身名，仅供有贴图时使用）
     */
    private Component getNeighborName(Direction direction) {
        return getNeighborState(direction).getBlock().getName();
    }

    /**
     * 获取指定方向相邻方块状态（客户端世界不可用/无机器时返回空气）
     */
    private BlockState getNeighborState(Direction direction) {
        if (this.minecraft != null && this.minecraft.level != null && this.menu.entity != null) {
            return this.minecraft.level.getBlockState(this.menu.entity.getBlockPos().relative(direction));
        }
        return Blocks.AIR.defaultBlockState();
    }

    /**
     * 非中文语言下用符号表示方向，保证按钮宽度可显示
     */
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

    /**
     * 当前 GUI 语言是否为中文（中文使用文字，其他语言用符号）
     */
    private static boolean isChinese() {
        return Minecraft.getInstance().getLanguageManager().getSelected().startsWith("zh");
    }

    /**
     * 六面输出状态按钮：显示为 [相邻方块贴图+方向名] · [随机/禁用/槽位贴图+槽号]，
     * 点击循环切换 11 个状态；按钮上有贴图时 hover 显示 tooltip（输出目的/输出方向/输出材料）。
     */
    private class FaceButton extends SimpleButton {
        private final Direction direction;

        FaceButton(int x, int y, Direction direction, OnPress onPress) {
            super(x, y, BTN_W, BTN_H, Component.literal(""), onPress);
            this.direction = direction;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int state = StorageFountainScreen.this.menu.getDirectionState(this.direction);
            // 背景色：禁用=红，随机=绿，槽位=蓝灰
            int color;
            if (state == StorageFountainEntity.STATE_DISABLED) {
                color = 0xFFAA0000;
            } else if (state == StorageFountainEntity.STATE_RANDOM) {
                color = 0xFF00AA00;
            } else {
                color = 0xFF3A3A6B;
            }
            renderButton(guiGraphics, color);
            // 内容 = [输出目的(相邻贴图/方向名)] ← [输出材料(槽位贴图/槽号 或 随机/禁用)]，
            // 有贴图时省略对应文字（方向名/槽号），整体在按钮内左右居中
            ItemStack neighborIcon = StorageFountainScreen.this.getNeighborIcon(this.direction);
            // 中文用文字，其他语言用符号表示方向/随机/禁用，保证按钮宽度可显示
            String dirName = Component.translatable("screen.alltheimbaium.storage_fountain.face." + this.direction.getName()).getString();
            boolean isChinese = isChinese();
            if (!isChinese) {
                dirName = directionSymbol(this.direction);
            }
            String stateText = null;
            ItemStack slotIcon = ItemStack.EMPTY;
            String slotText = null;
            if (state >= StorageFountainEntity.STATE_SLOT_BASE) {
                int slot = state - StorageFountainEntity.STATE_SLOT_BASE;
                slotIcon = StorageFountainScreen.this.menu.getMarkedStack(slot);
                if (slotIcon.isEmpty()) {
                    // 对应槽位未标记（无贴图）时显示槽号兜底
                    slotText = String.valueOf(slot + 1);
                }
            } else if (state == StorageFountainEntity.STATE_RANDOM) {
                stateText = isChinese
                        ? Component.translatable("screen.alltheimbaium.storage_fountain.random").getString()
                        : "?";
            } else {
                stateText = isChinese
                        ? Component.translatable("screen.alltheimbaium.storage_fountain.disabled").getString()
                        : "×";
            }
            boolean hasTargetIcon = !neighborIcon.isEmpty();
            boolean hasSlotIcon = !slotIcon.isEmpty();
            String rightText = hasSlotIcon ? null : (slotText != null ? slotText : stateText);
            // 计算内容总宽（贴图 16px；各元素间留 2px）
            int contentW = 0;
            contentW += hasTargetIcon ? 16 : StorageFountainScreen.this.font.width(dirName);
            contentW += StorageFountainScreen.this.font.width("←") + 2;
            contentW += hasSlotIcon ? 16 : StorageFountainScreen.this.font.width(rightText);
            contentW += 2;
            int x = this.getX() + Math.max(1, (BTN_W - contentW) / 2);
            int iconY = this.getY();
            int textY = this.getY() + 4;
            // 左段：相邻方块贴图（有贴图时不显示方向名）
            if (hasTargetIcon) {
                guiGraphics.renderItem(neighborIcon, x, iconY);
                x += 18;
            } else {
                guiGraphics.drawString(StorageFountainScreen.this.font, dirName, x, textY, 0xFFFFFFFF, true);
                x += StorageFountainScreen.this.font.width(dirName) + 2;
            }
            // 左箭头：表示右段物品输出到左段目的
            guiGraphics.drawString(StorageFountainScreen.this.font, "←", x, textY, 0xFFFFFFFF, true);
            x += StorageFountainScreen.this.font.width("←") + 2;
            // 右段：槽位贴图（有贴图时不显示槽号）或 随机/禁用
            if (hasSlotIcon) {
                guiGraphics.renderItem(slotIcon, x, iconY);
            } else {
                guiGraphics.drawString(StorageFountainScreen.this.font, rightText, x, textY, 0xFFFFFFFF, true);
            }
        }

        /**
         * 构建 hover tooltip：所有情况下都返回完整的 [输出目的/输出方向/输出材料] 三行说明
         */
        List<Component> buildTooltip() {
            int state = StorageFountainScreen.this.menu.getDirectionState(this.direction);
            ItemStack neighborIcon = StorageFountainScreen.this.getNeighborIcon(this.direction);
            String dirName = Component.translatable("screen.alltheimbaium.storage_fountain.face." + this.direction.getName()).getString();
            List<Component> lines = new ArrayList<>();
            // 输出目的：相邻方块名，无相邻方块时提示无目标
            if (!neighborIcon.isEmpty()) {
                lines.add(Component.translatable("screen.alltheimbaium.storage_fountain.tooltip_target",
                        StorageFountainScreen.this.getNeighborName(this.direction)));
            } else {
                lines.add(Component.translatable("screen.alltheimbaium.storage_fountain.tooltip_target",
                        Component.translatable("screen.alltheimbaium.storage_fountain.tooltip_no_target")));
            }
            // 输出方向
            lines.add(Component.translatable("screen.alltheimbaium.storage_fountain.tooltip_direction", dirName));
            // 输出材料：槽位物品名/槽号，随机，禁用
            if (state >= StorageFountainEntity.STATE_SLOT_BASE) {
                int slot = state - StorageFountainEntity.STATE_SLOT_BASE;
                ItemStack materialIcon = StorageFountainScreen.this.menu.getMarkedStack(slot);
                if (!materialIcon.isEmpty()) {
                    lines.add(Component.translatable("screen.alltheimbaium.storage_fountain.tooltip_material", materialIcon.getHoverName()));
                } else {
                    lines.add(Component.translatable("screen.alltheimbaium.storage_fountain.tooltip_material",
                            Component.translatable("screen.alltheimbaium.storage_fountain.slot_number", slot + 1)));
                }
            } else if (state == StorageFountainEntity.STATE_RANDOM) {
                lines.add(Component.translatable("screen.alltheimbaium.storage_fountain.tooltip_material",
                        Component.translatable("screen.alltheimbaium.storage_fountain.random")));
            } else {
                lines.add(Component.translatable("screen.alltheimbaium.storage_fountain.tooltip_material",
                        Component.translatable("screen.alltheimbaium.storage_fountain.disabled")));
            }
            return lines;
        }
    }

    /**
     * 带状态颜色的开关按钮（开=绿色，关=红色）
     */
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

    /**
     * 带边框与居中文字的通用按钮
     */
    private abstract class SimpleButton extends Button {
        SimpleButton(int x, int y, int width, int height, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        }

        protected void renderButton(GuiGraphics guiGraphics, int color) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            // 1px 边框（鼠标悬浮时边框变亮，用于指示可交互）
            int borderColor = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor);
            guiGraphics.drawCenteredString(StorageFountainScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }
}
