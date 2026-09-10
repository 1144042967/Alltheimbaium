package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.InstantFurnaceEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 零刻熔炉 GUI（AE 大数版，参考方块生成机）。
 * <p>
 * 顶部标题 + FE 能量条；中间两行输入格（暖色带，可投料/取回）与两行输出格（冷色带，取成品）共 36 格；
 * 格子左下角以 AE 风格小字显示存量（1.1K/2.1M…）。输入区与输出区之间一排：6 个六面推送开关 + 交换按钮。
 * 交互：单击格取 1、Shift 取 1 组、空格取满；手中持原料点击输入格 = 整组投料并入输入行。
 */
@OnlyIn(Dist.CLIENT)
public class InstantFurnaceScreen extends AbstractContainerScreen<InstantFurnaceMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("alltheimbaium", "textures/gui/instant_furnace_gui.png");

    // FE 能量条（代码绘制）
    private static final int ENERGY_X = 8;
    private static final int ENERGY_Y = 16;
    private static final int ENERGY_W = 160;
    private static final int ENERGY_H = 6;
    private static final int ENERGY_TRACK_COLOR = 0xFF373737;
    private static final int ENERGY_FILL_COLOR = 0xFFFF8000;

    // 控制区（贴图中部留白带 y61~81）：六面开关（16 方块）+ 交换按钮
    private static final int FACE_Y = 63;
    private static final int FACE_SIZE = 16;
    private static final int FACE_STEP = 18;
    private static final int FACE_X_BASE = 8;
    private static final int SWAP_X = 122;
    private static final int SWAP_Y = 63;
    private static final int SWAP_W = 45;
    private static final int SWAP_H = 16;

    private static final float COUNT_SCALE = 0.5F;

    private final FaceButton[] faceButtons = new FaceButton[6];
    private Button swapButton;
    private boolean spaceDown = false;

    public InstantFurnaceScreen(InstantFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = InstantFurnaceMenu.IMAGE_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 6; i++) {
            Direction direction = Direction.values()[i];
            this.faceButtons[i] = new FaceButton(this.leftPos + FACE_X_BASE + i * FACE_STEP + i, this.topPos + FACE_Y,
                    direction, button -> sendButton(InstantFurnaceMenu.BUTTON_FACE_BASE + direction.ordinal()));
            this.addRenderableWidget(this.faceButtons[i]);
        }
        // 交换输入/输出
        this.swapButton = Button.builder(Component.translatable("screen.alltheimbaium.instant_furnace.swap"),
                        button -> sendButton(InstantFurnaceMenu.BUTTON_SWAP))
                .bounds(this.leftPos + SWAP_X, this.topPos + SWAP_Y, SWAP_W, SWAP_H)
                .build();
        this.addRenderableWidget(this.swapButton);
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

    /**
     * 拦截输入/输出 36 个格子的点击：
     * 输入格：持物→整组投料；空手→取回原料（1/组/满）。
     * 输出格：空手→取成品（1/组/满）；持物→忽略。
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < InstantFurnaceEntity.MAX_TYPES * 2; i++) {
                Slot slot = this.menu.slots.get(i);
                if (!this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    continue;
                }
                boolean isInput = i < InstantFurnaceEntity.MAX_TYPES;
                int cell = isInput ? i : i - InstantFurnaceEntity.MAX_TYPES;
                if (isInput && !this.menu.getCarried().isEmpty()) {
                    // 投料：手中物品整组并入输入行；本地随即清空光标
                    sendButton(InstantFurnaceMenu.BUTTON_DEPOSIT_INPUT);
                    this.menu.setCarried(ItemStack.EMPTY);
                    return true;
                }
                if (isInput) {
                    sendButton(pickExtractButton(InstantFurnaceMenu.BUTTON_INPUT_ONE_BASE,
                            InstantFurnaceMenu.BUTTON_INPUT_STACK_BASE, InstantFurnaceMenu.BUTTON_INPUT_ALL_BASE, cell));
                } else if (this.menu.getCarried().isEmpty()) {
                    sendButton(pickExtractButton(InstantFurnaceMenu.BUTTON_OUTPUT_ONE_BASE,
                            InstantFurnaceMenu.BUTTON_OUTPUT_STACK_BASE, InstantFurnaceMenu.BUTTON_OUTPUT_ALL_BASE, cell));
                } else {
                    return true; // 输出格不可投料
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int pickExtractButton(int oneBase, int stackBase, int allBase, int cell) {
        if (hasShiftDown()) {
            return stackBase + cell;
        }
        if (this.spaceDown) {
            return allBase + cell;
        }
        return oneBase + cell;
    }

    @Override
    protected void renderLabels(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 标题：颜色由菜单标题组件携带品级样式决定，此处带阴影保证在浅色底上可读
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, true);
        // 物品栏标签：保持原版观感，不加阴影
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderBackground(guiGraphics);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        // FE 能量条
        int trackLeft = this.leftPos + ENERGY_X;
        int trackTop = this.topPos + ENERGY_Y;
        guiGraphics.fill(trackLeft, trackTop, trackLeft + ENERGY_W, trackTop + ENERGY_H, ENERGY_TRACK_COLOR);
        int max = Math.max(1, this.menu.getMaxEnergy());
        int energy = Math.max(0, Math.min(max, this.menu.getEnergy()));
        if (energy > 0) {
            int fill = ENERGY_W * energy / max;
            guiGraphics.fill(trackLeft, trackTop, trackLeft + fill, trackTop + ENERGY_H, ENERGY_FILL_COLOR);
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 交换按钮为一次性操作，点击后无需保持焦点，避免残留原版按钮的白色聚焦描边
        if (this.swapButton != null) {
            this.swapButton.setFocused(false);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.flush();
        drawSlotCounts(guiGraphics);
        // FE 能量条 hover
        if (this.isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, buildEnergyTooltip(), Optional.empty(), mouseX, mouseY);
        }
        // 六面开关 tooltip
        for (FaceButton faceButton : this.faceButtons) {
            if (faceButton.isHovered()) {
                guiGraphics.renderTooltip(this.font, faceButton.buildTooltip(), Optional.empty(), mouseX, mouseY);
            }
        }
        // 最后再画一次悬停 tooltip，使其显示在数量小字之上（行格定制提示 / 其它槽默认提示）
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Slot slot = findRowSlot(mouseX, mouseY);
        if (slot != null) {
            int idx = slot.index;
            boolean isInput = idx < InstantFurnaceEntity.MAX_TYPES;
            int cell = isInput ? idx : idx - InstantFurnaceEntity.MAX_TYPES;
            ItemStack cur = isInput ? this.menu.getInputStack(cell) : this.menu.getOutputStack(cell);
            List<Component> lines = new ArrayList<>();
            if (isInput && !this.menu.getCarried().isEmpty()) {
                // 投料提示
                lines.add(Component.translatable("screen.alltheimbaium.instant_furnace.deposit_hint",
                        this.menu.getCarried().getHoverName()));
            } else if (!cur.isEmpty()) {
                lines.add(cur.getHoverName().copy().withStyle(style -> style.withColor(0xFFFFFF)));
                lines.add(Component.translatable("screen.alltheimbaium.instant_furnace.row_count",
                        isInput ? this.menu.getInputStock(cell) : this.menu.getOutputStock(cell)));
                lines.add(Component.translatable("screen.alltheimbaium.instant_furnace.extract_usage"));
            } else {
                return;
            }
            guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
            return;
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private Slot findRowSlot(int mouseX, int mouseY) {
        for (int i = 0; i < InstantFurnaceEntity.MAX_TYPES * 2; i++) {
            Slot slot = this.menu.slots.get(i);
            if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    /**
     * 在 36 个格子左下角绘制 AE 风格存量小字
     */
    private void drawSlotCounts(GuiGraphics guiGraphics) {
        for (int i = 0; i < InstantFurnaceEntity.MAX_TYPES; i++) {
            drawCount(guiGraphics, this.menu.getInputStock(i), this.menu.slots.get(i));
            drawCount(guiGraphics, this.menu.getOutputStock(i), this.menu.slots.get(InstantFurnaceEntity.MAX_TYPES + i));
        }
    }

    private void drawCount(GuiGraphics guiGraphics, long stock, Slot slot) {
        if (stock <= 0) {
            return;
        }
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y + 12;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);
        guiGraphics.pose().scale(COUNT_SCALE, COUNT_SCALE, 1.0F);
        guiGraphics.drawString(this.font, formatCount(stock), (int) (x / COUNT_SCALE), (int) (y / COUNT_SCALE), 0xFFFFFF, true);
        guiGraphics.pose().popPose();
    }

    private List<Component> buildEnergyTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("screen.alltheimbaium.instant_furnace.energy_tooltip",
                String.format("%,d", this.menu.getEnergy()), String.format("%,d", this.menu.getMaxEnergy())));
        lines.add(Component.translatable("screen.alltheimbaium.instant_furnace.energy_usage",
                String.format("%,d", this.menu.getEnergyPerSmelt())));
        return lines;
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
        if (value < 1_000_000_000_000_000_000L) {
            return String.format("%.1fP", value / 1_000_000_000_000_000.0);
        }
        return String.format("%.1fE", value / 1_000_000_000_000_000_000.0);
    }

    private static String directionArrow(Direction direction) {
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
     * 六面推送开关：绿=推送、红=禁用
     */
    private class FaceButton extends Button {
        private final Direction direction;

        FaceButton(int x, int y, Direction direction, OnPress onPress) {
            super(x, y, FACE_SIZE, FACE_SIZE, Component.literal(""), onPress, DEFAULT_NARRATION);
            this.direction = direction;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int state = InstantFurnaceScreen.this.menu.getDirectionState(this.direction);
            int color = state == InstantFurnaceEntity.STATE_DISABLED ? 0xFFAA0000 : 0xFF00AA00;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            int borderColor = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor);
            guiGraphics.drawCenteredString(InstantFurnaceScreen.this.font, directionArrow(this.direction),
                    this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }

        List<Component> buildTooltip() {
            int state = InstantFurnaceScreen.this.menu.getDirectionState(this.direction);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("screen.alltheimbaium.instant_furnace.face." + this.direction.getName()));
            lines.add(state == InstantFurnaceEntity.STATE_DISABLED
                    ? Component.translatable("screen.alltheimbaium.instant_furnace.disabled")
                    : Component.translatable("screen.alltheimbaium.instant_furnace.push"));
            return lines;
        }
    }
}
