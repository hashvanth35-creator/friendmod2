package com.friendmod;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Talks to the relay server over a WebSocket. Uses java.net.http.WebSocket, which
 * ships with the JDK, so we don't need to bundle any extra networking library.
 */
public class RelayClient {
    private static final Gson GSON = new Gson();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "friendmod-relay");
        t.setDaemon(true);
        return t;
    });

    private static WebSocket socket;
    private static volatile boolean shouldReconnect = true;
    private static String lastLocationType = "menu";
    private static String lastLocationName = "";

    public static void start() {
        shouldReconnect = true;
        connect();
    }

    public static void stop() {
        shouldReconnect = false;
        if (socket != null) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "closing");
        }
    }

    private static void connect() {
        String url = FriendManager.settings.relayServerUrl;
        if (url == null || url.isBlank()) return;

        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<WebSocket> future = client.newWebSocketBuilder()
                .buildAsync(URI.create(url), new Listener());

        future.whenComplete((ws, err) -> {
            if (err != null) {
                System.out.println("[FriendMod] Could not connect to relay server, retrying in 10s: " + err.getMessage());
                scheduleReconnect();
            } else {
                socket = ws;
                sendHello();
            }
        });
    }

    private static void scheduleReconnect() {
        if (!shouldReconnect) return;
        SCHEDULER.schedule(RelayClient::connect, 10, TimeUnit.SECONDS);
    }

    private static String ownUsername() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getUser() == null) return "player";
        return mc.getUser().getName();
    }

    private static void sendHello() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "hello");
        o.addProperty("from", ownUsername());
        JsonObject loc = new JsonObject();
        loc.addProperty("type", lastLocationType);
        loc.addProperty("name", lastLocationName);
        o.add("location", loc);
        send(o);
    }

    /** Call whenever our own status changes: entering a world, joining a server, or going back to the menu. */
    public static void updatePresence(String locationType, String locationName) {
        lastLocationType = locationType;
        lastLocationName = locationName;
        JsonObject o = new JsonObject();
        o.addProperty("type", "presence");
        o.addProperty("from", ownUsername());
        JsonObject loc = new JsonObject();
        loc.addProperty("type", locationType);
        loc.addProperty("name", locationName);
        o.add("location", loc);
        send(o);
    }

    public static void sendChat(String toUsername, String message) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "chat");
        o.addProperty("from", ownUsername());
        o.addProperty("to", toUsername);
        o.addProperty("message", message);
        send(o);
    }

    private static void send(JsonObject o) {
        if (socket == null) return;
        socket.sendText(GSON.toJson(o), true);
    }

    private static class Listener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            webSocket.request(1);
            if (last) {
                String full = buffer.toString();
                buffer.setLength(0);
                handleMessage(full);
            }
            return null;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("[FriendMod] Disconnected from relay server: " + reason);
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.out.println("[FriendMod] Relay connection error: " + error.getMessage());
            scheduleReconnect();
        }
    }

    private static void handleMessage(String json) {
        JsonObject o;
        try {
            o = GSON.fromJson(json, JsonObject.class);
        } catch (Exception e) {
            return;
        }
        if (o == null || !o.has("type")) return;

        String type = o.get("type").getAsString();
        String from = o.has("from") ? o.get("from").getAsString() : null;
        if (from == null) return;

        Minecraft mc = Minecraft.getInstance();

        if (type.equals("presence")) {
            String status = o.get("status").getAsString();
            JsonObject loc = o.has("location") ? o.getAsJsonObject("location") : null;
            String locType = loc != null ? loc.get("type").getAsString() : "menu";
            String locName = loc != null && loc.has("name") ? loc.get("name").getAsString() : "";
            mc.execute(() -> FriendManager.setPresence(from, status.equals("online"), locType, locName));
        } else if (type.equals("chat")) {
            String message = o.get("message").getAsString();
            long timestamp = o.has("timestamp") ? o.get("timestamp").getAsLong() : System.currentTimeMillis();
            String ownName = ownUsername();
            ChatMessage cm = new ChatMessage(from, ownName, message, timestamp);
            mc.execute(() -> FriendManager.recordMessage(from, cm));
        }
    }
}
