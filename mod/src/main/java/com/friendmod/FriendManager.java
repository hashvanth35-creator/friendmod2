package com.friendmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything about "who are my friends, are they online, and what have we said to each other"
 * lives here, and gets saved to plain JSON files under .minecraft/config/friendmod/.
 */
public class FriendManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configDir;
    private static Path friendsFile;
    private static Path chatsDir;
    private static Path settingsFile;

    public static final Map<String, Friend> friends = new LinkedHashMap<>();
    public static Settings settings = new Settings();

    public static class Settings {
        public String relayServerUrl = "wss://friendmod.onrender.com";
    }

    public static void init() {
        configDir = FabricLoader.getInstance().getConfigDir().resolve("friendmod");
        friendsFile = configDir.resolve("friends.json");
        chatsDir = configDir.resolve("chats");
        settingsFile = configDir.resolve("settings.json");

        try {
            Files.createDirectories(chatsDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        loadSettings();
        loadFriends();
    }

    // ---------- settings ----------

    private static void loadSettings() {
        try {
            if (Files.exists(settingsFile)) {
                String json = Files.readString(settingsFile);
                Settings loaded = GSON.fromJson(json, Settings.class);
                if (loaded != null) settings = loaded;
            } else {
                saveSettings();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveSettings() {
        try {
            Files.writeString(settingsFile, GSON.toJson(settings));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------- friend list ----------

    private static void loadFriends() {
        try {
            if (Files.exists(friendsFile)) {
                String json = Files.readString(friendsFile);
                Type type = new TypeToken<List<String>>() {}.getType();
                List<String> names = GSON.fromJson(json, type);
                if (names != null) {
                    for (String name : names) {
                        Friend f = new Friend(name);
                        f.history.addAll(loadChatHistory(name));
                        friends.put(name.toLowerCase(), f);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveFriendsList() {
        try {
            Files.writeString(friendsFile, GSON.toJson(friends.values().stream().map(f -> f.username).toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addFriend(String username) {
        String key = username.toLowerCase();
        if (friends.containsKey(key)) return;
        Friend f = new Friend(username);
        f.history.addAll(loadChatHistory(username));
        friends.put(key, f);
        saveFriendsList();
    }

    public static void removeFriend(String username) {
        friends.remove(username.toLowerCase());
        saveFriendsList();
    }

    public static Friend get(String username) {
        return friends.get(username.toLowerCase());
    }

    // ---------- presence ----------

    public static void setPresence(String username, boolean online, String locationType, String locationName) {
        Friend f = friends.get(username.toLowerCase());
        if (f == null) return; // not on our friend list, ignore
        f.online = online;
        f.locationType = locationType;
        f.locationName = locationName;
    }

    // ---------- chat ----------

    private static Path chatFile(String friendUsername) {
        return chatsDir.resolve(friendUsername.toLowerCase() + ".json");
    }

    private static List<ChatMessage> loadChatHistory(String friendUsername) {
        try {
            Path file = chatFile(friendUsername);
            if (Files.exists(file)) {
                String json = Files.readString(file);
                Type type = new TypeToken<List<ChatMessage>>() {}.getType();
                List<ChatMessage> list = GSON.fromJson(json, type);
                if (list != null) return list;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new java.util.ArrayList<>();
    }

    private static void saveChatHistory(String friendUsername, List<ChatMessage> history) {
        try {
            Files.writeString(chatFile(friendUsername), GSON.toJson(history));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Call this both when we send a message and when we receive one, so history + the file stay in sync. */
    public static void recordMessage(String otherUsername, ChatMessage message) {
        Friend f = friends.get(otherUsername.toLowerCase());
        if (f == null) {
            // Message from someone not yet on our list - add them automatically
            addFriend(otherUsername);
            f = friends.get(otherUsername.toLowerCase());
        }
        f.history.add(message);
        saveChatHistory(otherUsername, f.history);
    }
}
