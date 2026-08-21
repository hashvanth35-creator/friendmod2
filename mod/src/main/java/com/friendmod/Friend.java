package com.friendmod;

import java.util.ArrayList;
import java.util.List;

/** Holds what we currently know about one friend: are they online, and what are they doing. */
public class Friend {
    public String username;
    public transient boolean online = false;
    public transient String locationType = "menu"; // "menu" | "singleplayer" | "multiplayer"
    public transient String locationName = "";
    public transient List<ChatMessage> history = new ArrayList<>();

    public Friend() {
    }

    public Friend(String username) {
        this.username = username;
    }

    public String statusLine() {
        if (!online) return "Offline";
        switch (locationType) {
            case "singleplayer":
                return "Playing: " + locationName;
            case "multiplayer":
                return "Playing on server: " + locationName;
            default:
                return "Online - in menu";
        }
    }
}
