package cc.invic;

import cc.invic.schematic.LegacyBlockMapper;
import cc.invic.schematic.MinecraftToHytaleMapper;
import cc.invic.schematic.PasteUtil;
import cc.invic.schematic.SchematicData;
import cc.invic.schematic.SchematicParser;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.task.TaskRegistration;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@SuppressWarnings({"null", "removal"})
public class SchematicLoader extends JavaPlugin {

    private static SchematicLoader instance;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Map<String, SchematicData> playerLoadedSchematics = new HashMap<>();

    public SchematicLoader(@Nonnull JavaPluginInit init) {
        super(init);
    }

    public static SchematicLoader get() {
        return instance;
    }

    public Map<String, SchematicData> getPlayerLoadedSchematics() {
        return playerLoadedSchematics;
    }

    @Override
    protected void setup() {
        instance = this;

        MinecraftToHytaleMapper.setLogger(getLogger());

        saveDefaultResource("hytale_overrides.txt");
        saveDefaultResource("legacy_overrides.txt");

        loadHytaleOverrides(new File(getDataDirectory().toFile(), "hytale_overrides.txt"));
        loadLegacyOverrides(new File(getDataDirectory().toFile(), "legacy_overrides.txt"));

        getCommandRegistry().registerCommand(new ExampleCommand());
        
        File templatesDir = new File(getDataDirectory().toFile(), "schematics");
        getLogger().at(Level.INFO).log(String.valueOf(getDataDirectory()));
        if (!templatesDir.exists())
            templatesDir.mkdirs();

        registerEvents();

        getLogger().at(Level.INFO).log("SchematicLoader setup complete!");
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("SchematicLoader started!");
        
    }

    @Override
    protected void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        getLogger().at(Level.INFO).log("SchematicLoader shutting down!");
    }

    private void saveDefaultResource(String resourceName) {
        File targetFile = new File(getDataDirectory().toFile(), resourceName);
        
        if (targetFile.exists()) {
            return;
        }

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                getLogger().at(Level.WARNING).log("Could not find resource: " + resourceName);
                return;
            }

            Files.copy(in, targetFile.toPath());
            getLogger().at(Level.INFO).log("Saved default resource: " + resourceName);
        } catch (IOException e) {
            getLogger().at(Level.SEVERE).log("Failed to save default resource: " + resourceName, e);
        }
    }

    private void loadHytaleOverrides(File overrideFile) {
        if (!overrideFile.exists()) {
            getLogger().at(Level.INFO).log("No hytale_overrides.txt found, skipping.");
            return;
        }

        int loadedCount = 0;
        int errorCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(overrideFile))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("=", 2);
                if (parts.length != 2) {
                    getLogger().at(Level.WARNING).log("Invalid format in hytale_overrides.txt line " + lineNumber + ": " + line);
                    errorCount++;
                    continue;
                }

                String minecraftKey = parts[0].trim();
                String hytaleValue = parts[1].trim();

                if (!minecraftKey.contains(":")) {
                    getLogger().at(Level.WARNING).log("Invalid Minecraft key in hytale_overrides.txt line " + lineNumber + " (must contain at least one ':'): " + minecraftKey);
                    errorCount++;
                    continue;
                }

                MinecraftToHytaleMapper.addOverride(minecraftKey, hytaleValue);
                loadedCount++;
            }

            getLogger().at(Level.INFO).log("Loaded " + loadedCount + " Hytale overrides (" + errorCount + " errors)");
        } catch (IOException e) {
            getLogger().at(Level.SEVERE).log("Failed to load hytale_overrides.txt", e);
        }
    }

    private void loadLegacyOverrides(File overrideFile) {
        if (!overrideFile.exists()) {
            getLogger().at(Level.INFO).log("No legacy_overrides.txt found, skipping.");
            return;
        }

        int loadedCount = 0;
        int errorCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(overrideFile))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("=", 2);
                if (parts.length != 2) {
                    getLogger().at(Level.WARNING).log("Invalid format in legacy_overrides.txt line " + lineNumber + ": " + line);
                    errorCount++;
                    continue;
                }

                String legacyKey = parts[0].trim();
                String modernValue = parts[1].trim();

                long colonCount = legacyKey.chars().filter(ch -> ch == ':').count();
                if (colonCount != 1) {
                    getLogger().at(Level.WARNING).log("Invalid legacy key in legacy_overrides.txt line " + lineNumber + " (must have exactly one ':'): " + legacyKey);
                    errorCount++;
                    continue;
                }

                LegacyBlockMapper.addOverride(legacyKey, modernValue);
                loadedCount++;
            }

            getLogger().at(Level.INFO).log("Loaded " + loadedCount + " legacy overrides (" + errorCount + " errors)");
        } catch (IOException e) {
            getLogger().at(Level.SEVERE).log("Failed to load legacy_overrides.txt", e);
        }
    }

    private void registerEvents() {
        // Listen for living entity block use events (doors, etc.)
        this.getEntityStoreRegistry().registerSystem(new BlockBreakEventsystem());
        getEventRegistry().registerGlobal(
            BreakBlockEvent.class,
            this::onBlockBreak
        );
    }

    private void onBlockBreak(BreakBlockEvent event)
    {
        getLogger().atInfo().log(event.getBlockType().toString());

    }


    // Main /example command collection
    class ExampleCommand extends AbstractCommandCollection {

        ExampleCommand() {
            super("schem", "example.commands.desc");

            this.addSubCommand(new InfoCommand());
            this.addSubCommand(new SchematicListCommand());
            this.addSubCommand(new LoadCommand());
            this.addSubCommand(new PasteCommand());
        }

        // /example info - shows plugin information
        class InfoCommand extends CommandBase {

            InfoCommand() {
                super("info", "example.commands.info.desc");
            }

            @Override
            protected void executeSync(@Nonnull CommandContext context) {
                context.sendMessage(Message.translation(""));
                context.sendMessage(Message.translation("========== SchematicLoader Plugin =========="));
                context.sendMessage(Message.translation("Version: 1.0.0"));
                context.sendMessage(Message.translation("Made by: Invictable"));
                context.sendMessage(Message.translation("====================================="));
            }
        }

//        // /example tools - gives stone tools (once per player)
//        class ToolsCommand extends AbstractPlayerCommand {
//
//            ToolsCommand() {
//                super("tools", "example.commands.tools.desc");
//            }
//
//            @Override
//            protected void execute(@Nonnull CommandContext context,
//                                   @Nonnull Store<EntityStore> store,
//                                   @Nonnull Ref<EntityStore> ref,
//                                   @Nonnull PlayerRef playerRef,
//                                   @Nonnull World world) {
//
//                String username = playerRef.getUsername();
//
//                if (playersReceivedTools.contains(username)) {
//                    context.sendMessage(Message.translation("You have already received your starter tools!"));
//                    return;
//                }
//
//                Vector3d pos = playerRef.getTransform().getPosition();
//                int baseX = (int) pos.getX();
//                int baseY = (int) pos.getY();
//                int baseZ = (int) pos.getZ();
//
//                context.sendMessage(Message.translation("Placing 5 blocks, one every 3 seconds..."));
//
//                for (int i = 0; i < 5; i++) {
//                    final int iteration = i;
//                    long delaySeconds = i * 3;
//
//                    scheduler.schedule(
//                        () -> {
//                            world.setBlock(baseX + iteration, baseY, baseZ, "Rock_Stone");
//                            getLogger().at(Level.INFO).log("Placed block " + (iteration + 1) + " for player: " + username);
//                        },
//                        delaySeconds,
//                        TimeUnit.SECONDS
//                    );
//                }
//
//                Player player = store.getComponent(ref, Player.getComponentType());
//                if (player == null) {
//                    context.sendMessage(Message.translation("Error: Could not access player data."));
//                    return;
//                }
//
//                Inventory inventory = player.getInventory();
//
//                List<ItemStack> tools = Arrays.asList(
//                    new ItemStack("Tool_Pickaxe_Crude", 1),
//                    new ItemStack("Tool_Hatchet_Crude", 1),
//                    new ItemStack("Tool_Shovel_Crude", 1),
//                    new ItemStack("Weapon_Axe_Crude", 1)
//                );
//
//                inventory.getStorage().addItemStacks(tools);
//                player.sendInventory();
//
//                playersReceivedTools.add(username);
//
//                context.sendMessage(Message.translation("You received your starter crude tools!"));
//                context.sendMessage(Message.translation("- Crude Pickaxe"));
//                context.sendMessage(Message.translation("- Crude Hatchet"));
//                context.sendMessage(Message.translation("- Crude Shovel"));
//                context.sendMessage(Message.translation("- Crude Axe"));
//
//                getLogger().at(Level.INFO).log("Gave starter tools to player: %s", username);
//            }
//        }

        class SchematicListCommand extends CommandBase {

            SchematicListCommand() {
                super("list", "Lists schematics present in folder. Restart after adding more.");
                this.requirePermission("schematic.*");
            }

            @Override
            protected void executeSync(@Nonnull CommandContext context) {

                File templatesDir = new File(getDataDirectory().toFile(), "schematics");
                File[] files = templatesDir.listFiles();
                
                List<String> schematicFiles = new ArrayList<>();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && (file.getName().endsWith(".schematic") || file.getName().endsWith(".schem"))) {
                            schematicFiles.add(file.getName());
                        }
                    }
                }

                if (schematicFiles.isEmpty()) {
                    context.sendMessage(Message.translation("No schematics found in the schematics folder."));
                    return;
                }

                context.sendMessage(Message.translation(""));
                context.sendMessage(Message.translation("========== Available Schematics =========="));
                context.sendMessage(Message.translation("Total schematics: " + schematicFiles.size()));

                for (String fileName : schematicFiles) {
                    context.sendMessage(Message.translation("  - " + fileName).color(Color.YELLOW));
                }

                context.sendMessage(Message.translation("Use /schem load <name> to load a schematic"));
                context.sendMessage(Message.translation("====================================="));
            }
        }

        class LoadCommand extends AbstractCommandCollection {

            LoadCommand() {
                super("load", "example.commands.load.desc");
                
                File templatesDir = new File(getDataDirectory().toFile(), "schematics");
                File[] files = templatesDir.listFiles();
                
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && (file.getName().endsWith(".schematic") || file.getName().endsWith(".schem"))) {
                            this.addSubCommand(new LoadSchematicCommand(file.getName(), file));
                        }
                    }
                }
            }
        }

        private static List<String> loadingPlayers = new ArrayList<>();
        class LoadSchematicCommand extends AbstractPlayerCommand {
            private final File schematicFile;

            LoadSchematicCommand(String schematicName, File schematicFile) {
                super(schematicName, "Load schematic: " + schematicName);
                this.schematicFile = schematicFile;
                this.requirePermission("schematic.*");
            }

            @Override
            protected void execute(@Nonnull CommandContext context,
                                   @Nonnull Store<EntityStore> store,
                                   @Nonnull Ref<EntityStore> ref,
                                   @Nonnull PlayerRef playerRef,
                                   @Nonnull World world) {

                String username = playerRef.getUsername();
                if(loadingPlayers.contains(username))
                {
                    context.sendMessage(Message.translation("You're already loading a schematic! Please wait."));
                    return;
                }
                context.sendMessage(Message.translation("Loading schematic: " + schematicFile.getName() + "..."));
                loadingPlayers.add(username);

                scheduler.submit(() -> {
                    try {
                        SchematicParser parser = new SchematicParser(getLogger());
                        SchematicData data = parser.parseSchematic(schematicFile);
                        
                        playerLoadedSchematics.put(username, data);
                        
                        context.sendMessage(Message.translation("Successfully loaded schematic: " + schematicFile.getName()));
                        context.sendMessage(Message.translation("Dimensions: " + data.getWidth() + "x" + 
                            data.getHeight() + "x" + data.getLength()));
                        context.sendMessage(Message.translation("Total blocks: " + data.getTotalBlocks()));
                        
                        if (data.getVersion() != -1) {
                            context.sendMessage(Message.translation("Version: " + data.getVersion()));
                        }
                        if (data.getDataVersion() != -1) {
                            context.sendMessage(Message.translation("DataVersion: " + data.getDataVersion()));
                        }
                        
                        context.sendMessage(Message.translation("Use /schem paste to paste it at your location"));
                        
                        getLogger().at(Level.INFO).log("Player " + username + " loaded schematic: " + schematicFile.getName());
                        loadingPlayers.remove(username);
                    } catch (Exception e) {
                        context.sendMessage(Message.translation("Failed to load schematic: " + e.getMessage()));
                        loadingPlayers.remove(username);
                        getLogger().at(Level.SEVERE).log("Failed to parse schematic for player " + username + ": " + schematicFile.getName(), e);
                    }
                });
            }
        }

        class PasteCommand extends AbstractPlayerCommand {

            PasteCommand() {
                super("paste", "example.commands.paste.desc");
                this.requirePermission("schematic.*");
            }

            @Override
            protected void execute(@Nonnull CommandContext context,
                                   @Nonnull Store<EntityStore> store,
                                   @Nonnull Ref<EntityStore> ref,
                                   @Nonnull PlayerRef playerRef,
                                   @Nonnull World world) {

                String username = playerRef.getUsername();

                if (!playerLoadedSchematics.containsKey(username)) {
                    context.sendMessage(Message.translation("No schematic loaded!"));
                    context.sendMessage(Message.translation("Use /schem load <name> to load a schematic first"));
                    context.sendMessage(Message.translation("Use /schem list to see available schematics"));
                    return;
                }

                SchematicData data = playerLoadedSchematics.get(username);
                Vector3d pos = playerRef.getTransform().getPosition();

                context.sendMessage(Message.translation("Pasting schematic: " + data.getFileName()));
                context.sendMessage(Message.translation("Total blocks: " + data.getTotalBlocks()));


                PasteUtil.paste(world, pos, data, scheduler);

                getLogger().at(Level.INFO).log("Player " + username + " pasted schematic: " + data.getFileName());
            }
        }
    }
}
