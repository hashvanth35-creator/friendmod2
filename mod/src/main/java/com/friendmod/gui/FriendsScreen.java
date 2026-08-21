package com.friendmod.gui;

import com.friendmod.Friend;
import com.friendmod.FriendManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class FriendsScreen extends Screen {
    private final Screen parent;
    private static final int ROW_HEIGHT = 24;
    private static final int LIST_TOP = 40;

    public FriendsScreen(Screen parent) {
        super(Component.literal("Friends"));
        this.parent = parent;
    }

    /** The screen (title screen / pause menu) that was open before this one, so we can refresh and reopen. */
    public Screen getParentScreen() {
        return parent;
    }

    @Override
    protected void init() {
        super.init();

        int listWidth = 260;
        int left = (this.width - listWidth) / 2;

        List<Friend> friendList = new ArrayList<>(FriendManager.friends.values());
        int y = LIST_TOP;
        for (Friend friend : friendList) {
            int rowY = y;
            Button row = Button.builder(
                    Component.literal(friend.username + "  -  " + friend.statusLine()),
                    b -> this.minecraft.setScreen(new ChatScreen(this, friend))
            ).bounds(left, rowY, listWidth, 20).build();
            this.addRenderableWidget(row);
            y += ROW_HEIGHT;
        }

        // Add Friend button
        this.addRenderableWidget(Button.builder(Component.literal("Add Friend"), b ->
                this.minecraft.setScreen(new AddFriendScreen(this))
        ).bounds(left, this.height - 52, 120, 20).build());

        // Done button
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b ->
                this.minecraft.setScreen(parent)
        ).bounds(left + 140, this.height - 52, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        if (FriendManager.friends.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    "No friends added yet - click Add Friend below",
                    this.width / 2, LIST_TOP + 10, 0xAAAAAA);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
