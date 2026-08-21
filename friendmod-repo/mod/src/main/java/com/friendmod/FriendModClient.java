package com.friendmod;

import com.friendmod.gui.FriendsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public class FriendModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FriendManager.init();
        RelayClient.start();

        // Add a "Friends" button to the title screen and the in-game pause menu.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
                int x = scaledWidth - 100;
                int y = 6;
                Button button = Button.builder(Component.literal("Friends"), b -> {
                    client.setScreen(new FriendsScreen(screen));
                }).bounds(x, y, 90, 20).build();
                Screens.getButtons(screen).add(button);
            }
        });

        // When we join a world (singleplayer or multiplayer), tell friends what we're doing.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            updatePresenceFromCurrentState(client);
        });

        // When we leave a world, go back to "online, in menu".
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            RelayClient.updatePresence("menu", "");
        });
    }

    private void updatePresenceFromCurrentState(Minecraft client) {
        if (client.hasSingleplayerServer()) {
            String worldName = client.getSingleplayerServer() != null
                    ? client.getSingleplayerServer().getWorldData().getLevelName()
                    : "a world";
            RelayClient.updatePresence("singleplayer", worldName);
        } else if (client.getCurrentServer() != null) {
            String serverName = client.getCurrentServer().name != null && !client.getCurrentServer().name.isBlank()
                    ? client.getCurrentServer().name
                    : client.getCurrentServer().ip;
            RelayClient.updatePresence("multiplayer", serverName);
        } else {
            RelayClient.updatePresence("multiplayer", "a server");
        }
    }
}
