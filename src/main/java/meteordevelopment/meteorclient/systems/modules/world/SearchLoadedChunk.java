package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchLoadedChunk extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> showInChat = sgGeneral.add(new BoolSetting.Builder()
        .name("show-in-chat")
        .description("Displays loaded chunk/player information in the chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyNew = sgGeneral.add(new BoolSetting.Builder()
        .name("only-new")
        .description("Only shows newly loaded chunks/players since enabling the module.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> searchPlayers = sgGeneral.add(new BoolSetting.Builder()
        .name("search-players")
        .description("Searches for players in loaded chunks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showDistance = sgGeneral.add(new BoolSetting.Builder()
        .name("show-distance")
        .description("Shows the distance to detected players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showPlayerPos = sgGeneral.add(new BoolSetting.Builder()
        .name("show-player-pos")
        .description("Shows the exact X/Y/Z coordinates of detected players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Does not detect your own player.")
        .defaultValue(true)
        .build()
    );

    private final Set<ChunkPos> detectedChunks = new HashSet<>();
    private final Set<AbstractClientPlayerEntity> detectedPlayers = new HashSet<>();

    public SearchLoadedChunk() {
        super(Categories.World, "search-loaded-chunk", "Searches loaded chunks and detects players within them.");
    }

    @Override
    public void onActivate() {
        detectedChunks.clear();
        detectedPlayers.clear();
    }

    @Override
    public void onDeactivate() {
        detectedChunks.clear();
        detectedPlayers.clear();
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;

        ChunkPos playerChunkPos = new ChunkPos(mc.player.getBlockPos());
        int range = 10;
        for (int x = playerChunkPos.x - range; x <= playerChunkPos.x + range; x++) {
            for (int z = playerChunkPos.z - range; z <= playerChunkPos.z + range; z++) {
                if (mc.world.getChunkManager().isChunkLoaded(x, z)) {
                    ChunkPos chunkPos = new ChunkPos(x, z);

                    if (!onlyNew.get() || !detectedChunks.contains(chunkPos)) {
                        detectedChunks.add(chunkPos);
                        if (showInChat.get()) {
                            String chunkMsg = String.format("Loaded Chunk - X: %d, Z: %d", x, z);
                            //ChatUtils.sendMsg(Text.literal(chunkMsg));
                        }
                    }

                    if (searchPlayers.get()) detectPlayersInChunkRange(chunkPos);
                }
            }
        }
    }

    private void detectPlayersInChunkRange(ChunkPos chunkPos) {
        if (mc.player == null) return;

        List<AbstractClientPlayerEntity> allPlayers = mc.world.getPlayers();
        for (AbstractClientPlayerEntity player : allPlayers) {
            if (ignoreSelf.get() && player == mc.player) continue;

            ChunkPos playerChunk = new ChunkPos(player.getBlockPos());
            if (playerChunk.x != chunkPos.x || playerChunk.z != chunkPos.z) continue;

            if (onlyNew.get() && detectedPlayers.contains(player)) continue;
            detectedPlayers.add(player);

            StringBuilder playerMsg = new StringBuilder();
            playerMsg.append("Detected Player - ");
            playerMsg.append("Name: ").append(player.getName().getString());
            //playerMsg.append(", Chunk: X:").append(chunkPos.x).append(", Z:").append(chunkPos.z);

            // 新增：输出玩家精确三维坐标
            if (showPlayerPos.get()) {
                double px = player.getX();
                double py = player.getY();
                double pz = player.getZ();
                playerMsg.append(String.format(", Pos: X:%.1f, Y:%.1f, Z:%.1f", px, py, pz));
            }

            if (showDistance.get()) {
                double distance = mc.player.distanceTo(player);
                playerMsg.append(String.format(", Distance: %.1f blocks", distance));
            }

            if (showInChat.get()) {
                ChatUtils.sendMsg(Text.literal(playerMsg.toString()));
            }
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        list.add(theme.settings(this.settings)).expandX();
        return list;
    }
}
