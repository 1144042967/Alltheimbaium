package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
import cn.sd.jrz.alltheimbaium.entity.AutoFarmlandEntity;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.block.CropBlock;
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
 * 自动耕地 GUI（资源农场式，复用 mob_farm 布局）。
 * 无标记槽/能量：顶部显示当前作物与等级进度，27 行产物槽（AE 大数）、六向输出按钮、输出总开关。
 */
@OnlyIn(Dist.CLIENT)
public class AutoFarmlandScreen extends AbstractContainerScreen<AutoFarmlandMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("alltheimbaium", "textures/gui/auto_farmland_gui.png");

    private static final int BTN_W = 48;
    private static final int BTN_H = 16;
    private static final int[] BTN_XS = {8, 64, 120};
    private static final int[] BTN_YS = {102, 123};
    private static final int TOOL_BTN_W = 48;
    private static final int TOOL_BTN_H = 12;
    private static final int OUTPUT_BTN_X = 176 - TOOL_BTN_W - 8;
    private static final int OUTPUT_BTN_Y = 145;
    private static final int PROGRESS_X = 8;
    private static final int PROGRESS_Y = 34;
    private static final int PROGRESS_W = 160; // 去掉标记槽后加宽进度条，横跨顶部信息区（可随 GUI 微调）
    private static final int PROGRESS_H = 4;
    private static final float COUNT_SCALE = 0.5F;

    private final FaceButton[] faceButtons = new FaceButton[6];
    private StateButton outputButton;
    private boolean spaceDown = false;

    public AutoFarmlandScreen(AutoFarmlandMenu menu, Inventory playerInventory, Component title) {
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
                    button -> sendButton(AutoFarmlandMenu.BUTTON_DIR_BASE + direction.ordinal()));
            this.addRenderableWidget(this.faceButtons[i]);
        }
        this.outputButton = new StateButton(this.leftPos + OUTPUT_BTN_X, this.topPos + OUTPUT_BTN_Y, TOOL_BTN_W, TOOL_BTN_H,
                this.menu.isOutputEnabled(),
                Component.translatable("screen.alltheimbaium.mob_farm.output"),
                button -> sendButton(AutoFarmlandMenu.BUTTON_OUTPUT));
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
            for (int i = 0; i < AutoFarmlandMenu.MAX_PRODUCTS; i++) {
                Slot slot = this.menu.slots.get(AutoFarmlandMenu.SLOT_PRODUCT_BASE + i);
                if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    int id;
                    if (hasShiftDown()) {
                        id = AutoFarmlandMenu.BUTTON_EXTRACT_STACK_BASE + i;
                    } else if (this.spaceDown) {
                        id = AutoFarmlandMenu.BUTTON_EXTRACT_ALL_BASE + i;
                    } else {
                        id = AutoFarmlandMenu.BUTTON_EXTRACT_ONE_BASE + i;
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
    protected void renderLabels(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFAA00, true);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x000000, false);
        Component crop = Component.translatable("screen.alltheimbaium.auto_farmland.crop", currentCropName());
        guiGraphics.drawString(this.font, crop, 8, 14, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, Component.translatable("screen.alltheimbaium.mob_farm.level_progress",
                this.menu.getLevel(), growthPercent()), 8, 24, 0xFFFFFF, true);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.flush();
        drawSlotCounts(guiGraphics);
        this.outputButton.setState(this.menu.isOutputEnabled());
        for (FaceButton faceButton : this.faceButtons) {
            if (faceButton.isHovered()) {
                guiGraphics.renderTooltip(this.font, faceButton.buildTooltip(), Optional.empty(), mouseX, mouseY);
            }
        }
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Slot slot = findHoveredSlot(mouseX, mouseY);
        if (slot != null && !slot.getItem().isEmpty()) {
            int idx = slot.index;
            if (idx >= AutoFarmlandMenu.SLOT_PRODUCT_BASE && idx < AutoFarmlandMenu.SLOT_PRODUCT_BASE + AutoFarmlandMenu.MAX_PRODUCTS) {
                int i = idx - AutoFarmlandMenu.SLOT_PRODUCT_BASE;
                List<Component> lines = new ArrayList<>();
                lines.add(slot.getItem().getHoverName().copy().withStyle(ChatFormatting.WHITE));
                lines.add(Component.translatable("screen.alltheimbaium.mob_farm.count", this.menu.getProductStock(i)));
                lines.add(Component.translatable("screen.alltheimbaium.auto_farmland.efficiency", efficiencyPercent()));
                guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private Component currentCropName() {
        if (this.minecraft != null && this.minecraft.level != null && this.menu.entity != null) {
            BlockState st = this.minecraft.level.getBlockState(this.menu.entity.getBlockPos().above());
            if (st.getBlock() instanceof CropBlock) {
                return st.getBlock().getName();
            }
        }
        return Component.translatable("screen.alltheimbaium.auto_farmland.no_crop");
    }

    private long efficiencyPercent() {
        return 100 + (Math.max(1, this.menu.getLevel()) - 1);
    }

    private void drawSlotCounts(GuiGraphics guiGraphics) {
        for (int i = 0; i < AutoFarmlandMenu.MAX_PRODUCTS; i++) {
            long stock = this.menu.getProductStock(i);
            if (stock <= 0) {
                continue;
            }
            Slot slot = this.menu.slots.get(AutoFarmlandMenu.SLOT_PRODUCT_BASE + i);
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y + 12;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 300);
            guiGraphics.pose().scale(COUNT_SCALE, COUNT_SCALE, 1.0F);
            guiGraphics.drawString(this.font, formatCount(stock), (int) (x / COUNT_SCALE), (int) (y / COUNT_SCALE), 0xFFFFFF, true);
            guiGraphics.pose().popPose();
        }
    }

    @Nullable
    private Slot findHoveredSlot(int mouseX, int mouseY) {
        for (Slot slot : this.menu.slots) {
            if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private int growthPercent() {
        long second = Math.max(1, MobFarmBlock.getLevelUpIntervalSeconds());
        double percent = this.menu.getTickCount() / (second * 20.0) * 100.0;
        return (int) Math.max(0, Math.min(100, percent));
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

    private static boolean isChinese() {
        return Minecraft.getInstance().getLanguageManager().getSelected().startsWith("zh");
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

    private ItemStack getNeighborIcon(Direction direction) {
        Item item = getNeighborState(direction).getBlock().asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private Component getNeighborName(Direction direction) {
        return getNeighborState(direction).getBlock().getName();
    }

    private net.minecraft.world.level.block.state.BlockState getNeighborState(Direction direction) {
        if (this.minecraft != null && this.minecraft.level != null && this.menu.entity != null) {
            return this.minecraft.level.getBlockState(this.menu.entity.getBlockPos().relative(direction));
        }
        return Blocks.AIR.defaultBlockState();
    }

    private class FaceButton extends SimpleButton {
        private final Direction direction;

        FaceButton(int x, int y, Direction direction, OnPress onPress) {
            super(x, y, BTN_W, BTN_H, Component.literal(""), onPress);
            this.direction = direction;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int state = AutoFarmlandScreen.this.menu.getDirectionState(this.direction);
            int color;
            if (state == AutoFarmlandEntity.STATE_DISABLED) {
                color = 0xFFAA0000;
            } else if (state == AutoFarmlandEntity.STATE_RANDOM) {
                color = 0xFF00AA00;
            } else {
                color = 0xFF3A3A6B;
            }
            renderButton(guiGraphics, color);
            ItemStack neighborIcon = AutoFarmlandScreen.this.getNeighborIcon(this.direction);
            boolean isChinese = isChinese();
            String dirName = Component.translatable("screen.alltheimbaium.mob_farm.face." + this.direction.getName()).getString();
            if (!isChinese) {
                dirName = directionSymbol(this.direction);
            }
            String stateText = null;
            ItemStack slotIcon = ItemStack.EMPTY;
            String slotText = null;
            if (state >= AutoFarmlandEntity.STATE_SLOT_BASE) {
                int slot = state - AutoFarmlandEntity.STATE_SLOT_BASE;
                slotIcon = AutoFarmlandScreen.this.menu.getProductStack(slot);
                if (slotIcon.isEmpty()) {
                    slotText = String.valueOf(slot + 1);
                }
            } else if (state == AutoFarmlandEntity.STATE_RANDOM) {
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
            contentW += hasTargetIcon ? 16 : AutoFarmlandScreen.this.font.width(dirName);
            contentW += AutoFarmlandScreen.this.font.width("←") + 2;
            contentW += hasSlotIcon ? 16 : AutoFarmlandScreen.this.font.width(rightText);
            contentW += 2;
            int x = this.getX() + Math.max(1, (BTN_W - contentW) / 2);
            int iconY = this.getY();
            int textY = this.getY() + 4;
            if (hasTargetIcon) {
                guiGraphics.renderItem(neighborIcon, x, iconY);
                x += 18;
            } else {
                guiGraphics.drawString(AutoFarmlandScreen.this.font, dirName, x, textY, 0xFFFFFFFF, true);
                x += AutoFarmlandScreen.this.font.width(dirName) + 2;
            }
            guiGraphics.drawString(AutoFarmlandScreen.this.font, "←", x, textY, 0xFFFFFFFF, true);
            x += AutoFarmlandScreen.this.font.width("←") + 2;
            if (hasSlotIcon) {
                guiGraphics.renderItem(slotIcon, x, iconY);
            } else {
                guiGraphics.drawString(AutoFarmlandScreen.this.font, rightText, x, textY, 0xFFFFFFFF, true);
            }
        }

        List<Component> buildTooltip() {
            int state = AutoFarmlandScreen.this.menu.getDirectionState(this.direction);
            ItemStack neighborIcon = AutoFarmlandScreen.this.getNeighborIcon(this.direction);
            String dirName = Component.translatable("screen.alltheimbaium.mob_farm.face." + this.direction.getName()).getString();
            List<Component> lines = new ArrayList<>();
            if (!neighborIcon.isEmpty()) {
                lines.add(Component.translatable("screen.alltheimbaium.mob_farm.tooltip_target",
                        AutoFarmlandScreen.this.getNeighborName(this.direction)));
            } else {
                lines.add(Component.translatable("screen.alltheimbaium.mob_farm.tooltip_target",
                        Component.translatable("screen.alltheimbaium.mob_farm.tooltip_no_target")));
            }
            lines.add(Component.translatable("screen.alltheimbaium.mob_farm.tooltip_direction", dirName));
            if (state >= AutoFarmlandEntity.STATE_SLOT_BASE) {
                int slot = state - AutoFarmlandEntity.STATE_SLOT_BASE;
                ItemStack materialIcon = AutoFarmlandScreen.this.menu.getProductStack(slot);
                if (!materialIcon.isEmpty()) {
                    lines.add(Component.translatable("screen.alltheimbaium.mob_farm.tooltip_material", materialIcon.getHoverName()));
                } else {
                    lines.add(Component.translatable("screen.alltheimbaium.mob_farm.tooltip_material",
                            Component.translatable("screen.alltheimbaium.mob_farm.slot_number", slot + 1)));
                }
            } else if (state == AutoFarmlandEntity.STATE_RANDOM) {
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
            guiGraphics.drawCenteredString(AutoFarmlandScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }
}
