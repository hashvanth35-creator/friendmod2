package com.friendmod.gui;

import com.friendmod.FriendManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AddFriendScreen extends Screen {
    private final FriendsScreen parent;
    private EditBox usernameBox;

    public AddFriendScreen(FriendsScreen parent) {
        super(Component.literal("Add Friend"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int boxWidth = 200;
        int left = (this.width - boxWidth) / 2;
        int y = this.height / 2 - 10;

        usernameBox = new EditBox(this.font, left, y, boxWidth, 20, Component.literal("Username"));
        usernameBox.setMaxLength(32);
        this.addRenderableWidget(usernameBox);
        this.setInitialFocus(usernameBox);

        this.addRenderableWidget(Button.builder(Component.literal("Add"), b -> confirm())
                .bounds(left, y + 30, 95, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b ->
                this.minecraft.setScreen(parent)
        ).bounds(left + 105, y + 30, 95, 20).build());
    }

    private void confirm() {
        String name = usernameBox.getValue().trim();
        if (!name.isEmpty()) {
            FriendManager.addFriend(name);
        }
        this.minecraft.setScreen(new FriendsScreen(parent.getParentScreen()));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter / numpad enter
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, "Friend's Minecraft username", this.width / 2, this.height / 2 - 26, 0xFFFFFF);
    }
}
