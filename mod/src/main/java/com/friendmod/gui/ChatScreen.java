package com.friendmod.gui;

import com.friendmod.ChatMessage;
import com.friendmod.Friend;
import com.friendmod.FriendManager;
import com.friendmod.RelayClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/** Chat window for one friend. Re-reads FriendManager's history each render, so incoming messages just appear. */
public class ChatScreen extends Screen {
    private final Screen parent;
    private final Friend friend;
    private EditBox inputBox;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

    public ChatScreen(Screen parent, Friend friend) {
        super(Component.literal("Chat - " + friend.username));
        this.parent = parent;
        this.friend = friend;
    }

    @Override
    protected void init() {
        int boxWidth = Math.min(360, this.width - 40);
        int left = (this.width - boxWidth) / 2;
        int y = this.height - 46;

        inputBox = new EditBox(this.font, left, y, boxWidth - 70, 20, Component.literal("Message"));
        inputBox.setMaxLength(256);
        this.addRenderableWidget(inputBox);
        this.setInitialFocus(inputBox);

        this.addRenderableWidget(Button.builder(Component.literal("Send"), b -> sendMessage())
                .bounds(left + boxWidth - 65, y, 65, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Back"), b ->
                this.minecraft.setScreen(parent)
        ).bounds(left, this.height - 22, 80, 20).build());
    }

    private void sendMessage() {
        String text = inputBox.getValue().trim();
        if (text.isEmpty()) return;

        RelayClient.sendChat(friend.username, text);

        String me = this.minecraft.getUser() != null ? this.minecraft.getUser().getName() : "me";
        ChatMessage sent = new ChatMessage(me, friend.username, text, System.currentTimeMillis());
        FriendManager.recordMessage(friend.username, sent);

        inputBox.setValue("");
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter
            sendMessage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);

        graphics.drawCenteredString(this.font, friend.username + " - " + friend.statusLine(), this.width / 2, 12, 0xFFFFFF);

        // Draw the last N messages, newest at the bottom, just above the input box.
        int bottom = this.height - 56;
        int lineHeight = this.font.lineHeight + 2;
        int maxLines = (bottom - 30) / lineHeight;

        var history = friend.history;
        int start = Math.max(0, history.size() - maxLines);
        int y = bottom - Math.min(maxLines, history.size()) * lineHeight;

        String me = this.minecraft.getUser() != null ? this.minecraft.getUser().getName() : "me";
        for (int i = start; i < history.size(); i++) {
            ChatMessage m = history.get(i);
            boolean fromMe = m.from.equalsIgnoreCase(me);
            String time = TIME_FORMAT.format(new Date(m.timestamp));
            String line = "[" + time + "] " + (fromMe ? "You" : m.from) + ": " + m.message;
            int color = fromMe ? 0xAAFFAA : 0xFFFFFF;
            graphics.drawString(this.font, line, 20, y, color, false);
            y += lineHeight;
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
