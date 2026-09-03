package dev.chestprofiles.mixin;

import dev.chestprofiles.client.SlotLock;
import dev.chestprofiles.client.KeybindManager;
import dev.chestprofiles.client.config.ProfileConfig;
import dev.chestprofiles.client.engine.TransferEngine;
import dev.chestprofiles.client.gui.ProfilePanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Shadow
    protected int imageWidth;

    @Shadow
    protected int imageHeight;

    @Shadow
    @Final
    protected net.minecraft.world.inventory.AbstractContainerMenu menu;

    @Unique
    private ProfilePanel chestprofile$panel;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private ProfilePanel chestprofile$panel() {
        if (chestprofile$panel == null) {
            chestprofile$panel = new ProfilePanel((AbstractContainerScreen<?>) (Object) this);
        }
        return chestprofile$panel;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void chestprofile$init(CallbackInfo callbackInfo) {
        ProfilePanel panel = chestprofile$panel();
        panel.setPosition(this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        String chestKey = chestprofile$chestKey();
        panel.setChestKey(chestKey);
        if (chestKey != null) {
            ProfileConfig.instance.applyChestAssignment(chestKey);
        }
    }

    @Unique
    private String chestprofile$chestKey() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.hitResult == null
                || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockHitResult blockHit = (BlockHitResult) minecraft.hitResult;
        BlockPos blockPosition = blockHit.getBlockPos();
        if (!(minecraft.level.getBlockEntity(blockPosition) instanceof MenuProvider)) {
            return null;
        }
        BlockState blockState = minecraft.level.getBlockState(blockPosition);
        if (blockState.getBlock() instanceof ChestBlock && blockState.hasProperty(ChestBlock.TYPE)) {
            ChestType chestType = blockState.getValue(ChestBlock.TYPE);
            if (chestType == ChestType.RIGHT) {
                Direction connected = ChestBlock.getConnectedDirection(blockState);
                blockPosition = blockPosition.relative(connected);
            }
        }
        return ProfileConfig.chestKey(
                minecraft.level.dimension().identifier().toString(),
                blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
    }

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void chestprofile$extractPanelBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callbackInfo) {
        ProfilePanel panel = chestprofile$panel();
        panel.setPosition(this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        panel.renderBackground(graphics, mouseX, mouseY);
        panel.renderFillButton(graphics, mouseX, mouseY);
        panel.renderChestPhantoms(graphics);
        panel.renderOverlay(graphics, mouseX, mouseY);
        if (this.menu instanceof ChestMenu && SlotLock.isAltDown()) {
            SlotLock.render(graphics, this.leftPos, this.topPos, this.menu);
        }
    }

        @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void chestprofile$mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (event.hasAltDown() && event.button() == 0
                && SlotLock.clickToggle((AbstractContainerScreen<?>) (Object) this, this.leftPos, this.topPos, event.x(), event.y())) {
            callbackInfoReturnable.setReturnValue(true);
            return;
        }
        ProfilePanel panel = chestprofile$panel();
        if (panel.fillButtonClicked(event.x(), event.y(), event.button())
                || panel.mouseClicked(event.x(), event.y(), event.button())) {
            callbackInfoReturnable.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void chestprofile$mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (chestprofile$panel().mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            callbackInfoReturnable.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void chestprofile$mouseDragged(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (chestprofile$panel().mouseDragged(event)) {
            callbackInfoReturnable.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void chestprofile$mouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (chestprofile$panel().mouseReleased(event)) {
            callbackInfoReturnable.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void chestprofile$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (KeybindManager.TRANSFER.matches(event)) {
            if (TransferEngine.startFill((AbstractContainerScreen<?>) (Object) this)) {
                callbackInfoReturnable.setReturnValue(true);
            }
            return;
        }
        if (KeybindManager.TOGGLE_PANEL.matches(event)) {
            ProfileConfig.instance.panelEnabled = !ProfileConfig.instance.panelEnabled;
            ProfileConfig.save();
            callbackInfoReturnable.setReturnValue(true);
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void chestprofile$slotClicked(Slot slot, int slotId, int button, ContainerInput containerInput, CallbackInfo callbackInfo) {
        AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) (Object) this;
        if (TransferEngine.isActiveFor(containerScreen) && slotId < 0) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "containerTick", at = @At("HEAD"))
    private void chestprofile$containerTick(CallbackInfo callbackInfo) {
        TransferEngine.tick((AbstractContainerScreen<?>) (Object) this);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void chestprofile$removed(CallbackInfo callbackInfo) {
        TransferEngine.onScreenClosed((AbstractContainerScreen<?>) (Object) this);
    }
}
