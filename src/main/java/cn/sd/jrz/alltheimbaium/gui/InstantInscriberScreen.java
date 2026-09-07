package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.InstantInscriberEntity;
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
 * 零刻压印器 GUI（AE 大数版）。
 * <p>
 * 顶部标题 + FE 能量条；两行输入格（18，可投料/取回）、一行输出格（9，取成品）；
 * 中间一排：6 个六面推送开关 + 模式切换按钮（显示当前 压板/组装，点击切换）。格内 AE 风格存量小字。
 */
@OnlyIn(Dist.CLIENT)
public class InstantInscriberScreen extends AbstractContainerScreen<InstantInscriberMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("alltheimbaium", "textures/gui/instant_inscriber_gui.png");

    private static final int ENERGY_X = 8;
    private static final int ENERGY_Y = 16;
    private static final int ENERGY_W = 160;
    private static final int ENERGY_H = 6;
    private static final int ENERGY_TRACK_COLOR = 0xFF373737;
    private static final int ENERGY_FILL_COLOR = 0xFFFF8000;

    private static final int FACE_Y = 66;
    private static final int FACE_SIZE = 16;
    private static final int FACE_STEP = 18;
    private static final int FACE_X_BASE = 8;
    private static final int MODE_X = 121;
    private static final int MODE_Y = 66;
    private static final int MODE_W = 47;
    private static final int MODE_H = 16;

    private static final float COUNT_SCALE = 0.5F;

    private final FaceButton[] faceButtons = new FaceButton[6];
    private Button modeButton;
    private boolean spaceDown = false;

    public InstantInscriberScreen(InstantInscriberMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 233;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 6; i++) {
            Direction direction = Direction.values()[i];
            this.faceButtons[i] = new FaceButton(this.leftPos + FACE_X_BASE + i * FACE_STEP + i, this.topPos + FACE_Y,
                    direction, button -> sendButton(InstantInscriberMenu.BUTTON_FACE_BASE + direction.ordinal()));
            this.addRenderableWidget(this.faceButtons[i]);
        }
        // 模式切换：显示当前模式，点击切换（hover 说明目标模式）
        this.modeButton = Button.builder(Component.literal(""),
                        button -> sendButton(InstantInscriberMenu.BUTTON_MODE))
                .bounds(this.leftPos + MODE_X, this.topPos + MODE_Y, MODE_W, MODE_H)
                .build();
        this.addRenderableWidget(this.modeButton);
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
            int totalCells = InstantInscriberEntity.INPUT_MAX_TYPES + InstantInscriberEntity.OUTPUT_MAX_TYPES;
            for (int i = 0; i < totalCells; i++) {
                Slot slot = this.menu.slots.get(i);
                if (!this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    continue;
                }
                boolean isInput = i < InstantInscriberEntity.INPUT_MAX_TYPES;
                int cell = isInput ? i : i - InstantInscriberEntity.INPUT_MAX_TYPES;
                if (isInput && !this.menu.getCarried().isEmpty()) {
                    sendButton(InstantInscriberMenu.BUTTON_DEPOSIT_INPUT);
                    this.menu.setCarried(ItemStack.EMPTY);
                    return true;
                }
                if (isInput) {
                    sendButton(pickExtractButton(InstantInscriberMenu.BUTTON_INPUT_ONE_BASE,
                            InstantInscriberMenu.BUTTON_INPUT_STACK_BASE, InstantInscriberMenu.BUTTON_INPUT_ALL_BASE, cell));
                } else if (this.menu.getCarried().isEmpty()) {
                    sendButton(pickExtractButton(InstantInscriberMenu.BUTTON_OUTPUT_ONE_BASE,
                            InstantInscriberMenu.BUTTON_OUTPUT_STACK_BASE, InstantInscriberMenu.BUTTON_OUTPUT_ALL_BASE, cell));
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
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderBackground(guiGraphics);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
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
        // 模式按钮文案随时刷新（显示当前模式）
        this.modeButton.setMessage(Component.translatable("screen.alltheimbaium.instant_inscriber.mode." + modeKey(this.menu.getMode())));
        // 模式按钮点击后无需保持焦点，避免残留原版按钮的白色聚焦描边
        this.modeButton.setFocused(false);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.flush();
        drawSlotCounts(guiGraphics);
        // FE 能量条 hover
        if (this.isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, buildEnergyTooltip(), Optional.empty(), mouseX, mouseY);
        }
        // 模式按钮 hover：目标模式说明
        if (this.modeButton.isHovered()) {
            int next = this.menu.getMode() == InstantInscriberEntity.MODE_ASSEMBLY
                    ? InstantInscriberEntity.MODE_INSCRIBE : InstantInscriberEntity.MODE_ASSEMBLY;
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("screen.alltheimbaium.instant_inscriber.mode_tooltip.current",
                    Component.translatable("screen.alltheimbaium.instant_inscriber.mode." + modeKey(this.menu.getMode()))));
            lines.add(Component.translatable("screen.alltheimbaium.instant_inscriber.mode_tooltip.next",
                    Component.translatable("screen.alltheimbaium.instant_inscriber.mode." + modeKey(next))));
            guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
        // 六面开关 tooltip
        for (FaceButton faceButton : this.faceButtons) {
            if (faceButton.isHovered()) {
                guiGraphics.renderTooltip(this.font, faceButton.buildTooltip(), Optional.empty(), mouseX, mouseY);
            }
        }
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static String modeKey(int mode) {
        return mode == InstantInscriberEntity.MODE_ASSEMBLY ? "assemble" : "press";
    }

    @Override
    protected void renderTooltip(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Slot slot = findRowSlot(mouseX, mouseY);
        if (slot != null) {
            int idx = slot.index;
            boolean isInput = idx < InstantInscriberEntity.INPUT_MAX_TYPES;
            int cell = isInput ? idx : idx - InstantInscriberEntity.INPUT_MAX_TYPES;
            ItemStack cur = isInput ? this.menu.getInputStack(cell) : this.menu.getOutputStack(cell);
            List<Component> lines = new ArrayList<>();
            if (isInput && !this.menu.getCarried().isEmpty()) {
                lines.add(Component.translatable("screen.alltheimbaium.instant_inscriber.deposit_hint",
                        this.menu.getCarried().getHoverName()));
            } else if (!cur.isEmpty()) {
                lines.add(cur.getHoverName().copy().withStyle(style -> style.withColor(0xFFFFFF)));
                lines.add(Component.translatable("screen.alltheimbaium.instant_inscriber.row_count",
                        isInput ? this.menu.getInputStock(cell) : this.menu.getOutputStock(cell)));
                lines.add(Component.translatable("screen.alltheimbaium.instant_inscriber.extract_usage"));
            } else {
                return;
            }
            guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
            return;
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private Slot findRowSlot(int mouseX, int mouseY) {
        int totalCells = InstantInscriberEntity.INPUT_MAX_TYPES + InstantInscriberEntity.OUTPUT_MAX_TYPES;
        for (int i = 0; i < totalCells; i++) {
            Slot slot = this.menu.slots.get(i);
            if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private void drawSlotCounts(GuiGraphics guiGraphics) {
        for (int i = 0; i < InstantInscriberEntity.INPUT_MAX_TYPES; i++) {
            drawCount(guiGraphics, this.menu.getInputStock(i), this.menu.slots.get(i));
        }
        for (int i = 0; i < InstantInscriberEntity.OUTPUT_MAX_TYPES; i++) {
            drawCount(guiGraphics, this.menu.getOutputStock(i),
                    this.menu.slots.get(InstantInscriberEntity.INPUT_MAX_TYPES + i));
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
        lines.add(Component.translatable("screen.alltheimbaium.instant_inscriber.energy_tooltip",
                String.format("%,d", this.menu.getEnergy()), String.format("%,d", this.menu.getMaxEnergy())));
        lines.add(Component.translatable("screen.alltheimbaium.instant_inscriber.energy_usage",
                String.format("%,d", this.menu.getEnergyPerOp())));
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

    private class FaceButton extends Button {
        private final Direction direction;

        FaceButton(int x, int y, Direction direction, OnPress onPress) {
            super(x, y, FACE_SIZE, FACE_SIZE, Component.literal(""), onPress, DEFAULT_NARRATION);
            this.direction = direction;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int state = InstantInscriberScreen.this.menu.getDirectionState(this.direction);
            int color = state == InstantInscriberEntity.STATE_DISABLED ? 0xFFAA0000 : 0xFF00AA00;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            int borderColor = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor);
            guiGraphics.drawCenteredString(InstantInscriberScreen.this.font, directionArrow(this.direction),
                    this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }

        List<Component> buildTooltip() {
            int state = InstantInscriberScreen.this.menu.getDirectionState(this.direction);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("screen.alltheimbaium.instant_inscriber.face." + this.direction.getName()));
            lines.add(state == InstantInscriberEntity.STATE_DISABLED
                    ? Component.translatable("screen.alltheimbaium.instant_inscriber.disabled")
                    : Component.translatable("screen.alltheimbaium.instant_inscriber.push"));
            return lines;
        }
    }
}
