package cc.invic.schematic;

import cc.invic.SchematicLoader;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.Tag;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class SchematicParser {

    private final HytaleLogger logger;
    private static final Map<String, String> TILE_ENTITY_TO_MODERN_BLOCK = new HashMap<>();
    
    static {
        TILE_ENTITY_TO_MODERN_BLOCK.put("Beacon", "minecraft:beacon");
        TILE_ENTITY_TO_MODERN_BLOCK.put("Bed", "minecraft:red_bed");
        TILE_ENTITY_TO_MODERN_BLOCK.put("Chest", "minecraft:chest");
        TILE_ENTITY_TO_MODERN_BLOCK.put("Comparator", "minecraft:comparator");
        TILE_ENTITY_TO_MODERN_BLOCK.put("Control", "minecraft:command_block");
        TILE_ENTITY_TO_MODERN_BLOCK.put("DLDetector", "minecraft:daylight_detector");
        TILE_ENTITY_TO_MODERN_BLOCK.put("Dropper", "minecraft:dropper");
        TILE_ENTITY_TO_MODERN_BLOCK.put("EnchantTable", "minecraft:enchanting_table");
        TILE_ENTITY_TO_MODERN_BLOCK.put("EnderChest", "minecraft:ender_chest");
        TILE_ENTITY_TO_MODERN_BLOCK.put("Furnace", "minecraft:furnace");
        TILE_ENTITY_TO_MODERN_BLOCK.put("Hopper", "minecraft:hopper");
        TILE_ENTITY_TO_MODERN_BLOCK.put("Music", "minecraft:jukebox");
        TILE_ENTITY_TO_MODERN_BLOCK.put("RecordPlayer", "minecraft:jukebox");
        TILE_ENTITY_TO_MODERN_BLOCK.put("MobSpawner", "minecraft:spawner");
        TILE_ENTITY_TO_MODERN_BLOCK.put("Sign", "minecraft:oak_sign");
        TILE_ENTITY_TO_MODERN_BLOCK.put("Skull", "minecraft:skeleton_skull");
        TILE_ENTITY_TO_MODERN_BLOCK.put("Trap", "minecraft:dispenser");
        TILE_ENTITY_TO_MODERN_BLOCK.put("Cauldron", "minecraft:brewing_stand");
        TILE_ENTITY_TO_MODERN_BLOCK.put("Torch", "minecraft:torch");
    }

    public SchematicParser(HytaleLogger logger) {
        this.logger = logger;
    }

    public SchematicData parseSchematic(File schematicFile) throws IOException {
        logger.at(Level.INFO).log("Parsing schematic: " + schematicFile.getName());

        String fileName = schematicFile.getName().toLowerCase();
        
        if (fileName.endsWith(".schem")) {
            return parseSpongeSchematic(schematicFile);
        } else if (fileName.endsWith(".schematic")) {
            return parseLegacySchematic(schematicFile);
        } else {
            throw new IOException("Unknown schematic format: " + fileName);
        }
    }

    private SchematicData parseLegacySchematic(File schematicFile) throws IOException {
        logger.at(Level.INFO).log("Parsing legacy .schematic format");

        NamedTag namedTag = NBTUtil.read(schematicFile);
        CompoundTag root = (CompoundTag) namedTag.getTag();

        int width = root.getShort("Width");
        int height = root.getShort("Height");
        int length = root.getShort("Length");
        String materials = root.getString("Materials");

        logger.at(Level.INFO).log("Dimensions: " + width + "x" + height + "x" + length);
        logger.at(Level.INFO).log("Materials: " + materials);

        byte[] blocks = root.getByteArray("Blocks");
        byte[] blockData = root.getByteArray("Data");
        byte[] addBlocks = root.containsKey("AddBlocks") ? root.getByteArray("AddBlocks") : null;

        //   Map<String, Integer> schematicaMapping = parseSchematicaMapping(root);

        int weOriginX = root.containsKey("WEOriginX") ? root.getInt("WEOriginX") : 0;
        int weOriginY = root.containsKey("WEOriginY") ? root.getInt("WEOriginY") : 0;
        int weOriginZ = root.containsKey("WEOriginZ") ? root.getInt("WEOriginZ") : 0;

        int weOffsetX = root.containsKey("WEOffsetX") ? root.getInt("WEOffsetX") : 0;
        int weOffsetY = root.containsKey("WEOffsetY") ? root.getInt("WEOffsetY") : 0;
        int weOffsetZ = root.containsKey("WEOffsetZ") ? root.getInt("WEOffsetZ") : 0;

        if (weOriginX != 0 || weOriginY != 0 || weOriginZ != 0) {
            logger.at(Level.INFO).log("WorldEdit Origin: (" + weOriginX + ", " + weOriginY + ", " + weOriginZ + ")");
        }

        if (weOffsetX != 0 || weOffsetY != 0 || weOffsetZ != 0) {
            logger.at(Level.INFO).log("WorldEdit Offset: (" + weOffsetX + ", " + weOffsetY + ", " + weOffsetZ + ")");
        }

        Map<BlockLocation, BlockData> blockMap = parseLegacyBlocks(blocks, blockData, addBlocks, width, height, length);
        
      //  parseTileEntities(root, blockMap);

        return new SchematicData(
            schematicFile.getName(),
            width, height, length,
            materials,
            blockMap,
            null,
            weOriginX, weOriginY, weOriginZ,
            weOffsetX, weOffsetY, weOffsetZ,
            0,
            -1
        );
    }   

    private Map<BlockLocation, BlockData> parseLegacyBlocks(byte[] blocks, byte[] blockData, byte[] addBlocks,
                                                            int width, int height, int length) {
        Map<BlockLocation, BlockData> blockMap = new HashMap<>();
        int totalBlocks = width * height * length;

        logger.at(Level.INFO).log("Parsing " + totalBlocks + " blocks...");

        for (int index = 0; index < blocks.length && index < totalBlocks; index++) {
            int x = index % width;
            int z = (index / width) % length;
            int y = index / (width * length);

            int blockId = blocks[index] & 0xFF;

            if (addBlocks != null) {
                int addBlocksIndex = index / 2;
                if (addBlocksIndex < addBlocks.length) {
                    int addBlockValue = addBlocks[addBlocksIndex] & 0xFF;
                    if (index % 2 == 0) {
                        blockId |= (addBlockValue & 0xF0) << 4;
                    } else {
                        blockId |= (addBlockValue & 0x0F) << 8;
                    }
                }
            }

            int data = blockData[index] & 0x0F;

//            if(blockId == 46)
//                logger.at(Level.INFO).log("Found a chest "+data);
//            if(blockId == 163)
//                logger.at(Level.INFO).log("Found a acacia stairs"+data);


            if (blockId != -1) {
                String modernName = LegacyBlockMapper.getModernBlock(blockId, data);

                if (MinecraftToHytaleMapper.getHytaleBlock(modernName).equals("skip"))
                    continue;

                BlockLocation location = new BlockLocation(x, y, z);
                boolean hasRotation = false;
                BlockData block;
                if(LegacyBlockMapper.dataIsRotation(blockId,data))
                {
                    hasRotation=true;
                    int extra = -1;
                    if(modernName.contains("stair"))
                    {
                        // stairs rotation data is 0-3. starting at 4, pattern repeats, but for upside down stairs.
                        if(data > 3)
                            extra = 1; // upside down
                        else extra = 0;//normal
                    }
                    else if(modernName.contains("slab"))
                    {
                        // slab types are 1-7. after 7, they repeat but for upside down slab variants
                        if(data>7)
                            extra = 1; // upsidedown
                        else extra = 0; //normal
                    }
                    block = new BlockData(blockId, adjustRotation(modernName,data), modernName, hasRotation,extra);

                }
                else
                {
                    int extra = -1;
                    if(modernName.contains("slab"))
                    {
                        // slab types are 1-7. after 7, they repeat but for upside down slab variants
                        if(data>7)
                            extra = 1; // upside down
                        else extra = 0; // normal
                    }
                    hasRotation=false;
                    block = new BlockData(blockId, data, modernName, hasRotation,extra);

                }
                 blockMap.put(location, block);
            }
        }

        logger.at(Level.INFO).log("Parsed " + blockMap.size() + " blocks");
        return blockMap;
    }

    // Different blocks have rotation data saved in unexpected orders
    private int adjustRotation(String blockName,int rotationData)
    {
        if(blockName.contains("stairs"))
        {
            // 4 -> 0
            // for upside down
            if(rotationData > 3)
                rotationData-=4;
            return switch (rotationData) {
                case 1 -> 3;      // west
                case 2 -> 2;   // South
                case 0 -> 1;  // East
                case 3 -> 0;  // North
                default -> 0;
            };
        }
        else if(blockName.contains("chest"))
        {
            return switch (rotationData) {
                case 0 -> 0;      // north
                case 1 -> 0;   // north
                case 2 -> 0;  // north
                case 3 -> 2;  // south
                case 4 -> 3;  // west
                case 5 -> 1;  // east
                default -> 0;
            };
        }
//        else if(blockName.contains("torch"))
//        {
//            return switch (rotationData) {
//                case 0 -> 5;      // up
//                case 1 -> 1;   // east
//                case 2 -> 3;  // west
//                case 3 -> 2;  // south
//                case 4 -> 0;  // north
//                default -> 0;
//            };
//        }
        return rotationData;
    }

//    private Map<String, Integer> parseSchematicaMapping(CompoundTag root) {
//        Map<String, Integer> mapping = new HashMap<>();
//
//        if (root.containsKey("SchematicaMapping")) {
//            CompoundTag mappingTag = root.getCompoundTag("SchematicaMapping");
//            logger.at(Level.INFO).log("SchematicaMapping found:");
//
//            for (Map.Entry<String, Tag<?>> entry : mappingTag.entrySet()) {
//                String blockName = entry.getKey();
//                Tag<?> tag = entry.getValue();
//                int blockId = 0;
//
//                if (tag instanceof net.querz.nbt.tag.IntTag) {
//                    blockId = ((net.querz.nbt.tag.IntTag) tag).asInt();
//                } else if (tag instanceof net.querz.nbt.tag.ShortTag) {
//                    blockId = ((net.querz.nbt.tag.ShortTag) tag).asShort();
//                } else if (tag instanceof net.querz.nbt.tag.ByteTag) {
//                    blockId = ((net.querz.nbt.tag.ByteTag) tag).asByte();
//                }
//
//                mapping.put(blockName, blockId);
//                logger.at(Level.INFO).log("  " + blockName + " -> " + blockId);
//            }
//        }
//
//        return mapping;
//    }
//    private void parseTileEntities(CompoundTag root, Map<BlockLocation, BlockData> blockMap) {
//        if (!root.containsKey("TileEntities")) {
//            return;
//        }
//
//        ListTag<?> tileEntitiesList = (ListTag<?>) root.get("TileEntities");
//        if (tileEntitiesList == null || tileEntitiesList.size() == 0) {
//            return;
//        }
//
//        logger.at(Level.INFO).log("Parsing " + tileEntitiesList.size() + " tile entities...");
//        int addedCount = 0;
//        int skippedUnknown = 0;
//        int skippedExisting = 0;
//
//        for (Tag<?> tag : tileEntitiesList) {
//            if (!(tag instanceof CompoundTag)) {
//               // logger.at(Level.WARNING).log("Tile entity not a compound tag");
//                continue;
//            }
//
//            CompoundTag tileEntity = (CompoundTag) tag;
//
//            if (!tileEntity.containsKey("x") || !tileEntity.containsKey("y") || !tileEntity.containsKey("z")) {
//              //  logger.at(Level.WARNING).log("No x y and z for tile entity ");
//                continue;
//            }
//
//            int x = tileEntity.getInt("x");
//            int y = tileEntity.getInt("y");
//            int z = tileEntity.getInt("z");
//
//            String tileEntityId = null;
//            if (tileEntity.containsKey("id")) {
//                Tag<?> idTag = tileEntity.get("id");
//                if (idTag instanceof net.querz.nbt.tag.StringTag) {
//                    tileEntityId = ((net.querz.nbt.tag.StringTag) idTag).getValue();
//                }
//            }
//
//            if (tileEntityId == null) {
//              //  logger.at(Level.WARNING).log("No tile entity ID at (" + x + ", " + y + ", " + z + ")");
//                continue;
//            }
//
//            String modernBlockName = TILE_ENTITY_TO_MODERN_BLOCK.get(tileEntityId);
//            if (modernBlockName == null) {
//              //  logger.at(Level.WARNING).log("Unknown tile entity type: " + tileEntityId + " at (" + x + ", " + y + ", " + z + ")");
//                skippedUnknown++;
//                continue;
//            }
//
//            BlockLocation location = new BlockLocation(x, y, z);
//
//            BlockData blockData = new BlockData(0, 0, modernBlockName,false);
//            blockMap.put(location, blockData);
//            addedCount++;
//        }
//
//        logger.at(Level.INFO).log("Tile entity summary: " + addedCount + " added, " + skippedExisting + " skipped (existing), " + skippedUnknown + " skipped (unknown)");
//    }

    private SchematicData parseSpongeSchematic(File schematicFile) throws IOException {
        logger.at(Level.INFO).log("Parsing modern sponge .schem format");

        NamedTag namedTag = NBTUtil.read(schematicFile);
        CompoundTag root = (CompoundTag) namedTag.getTag();
        
        CompoundTag schematic = root.getCompoundTag("Schematic");
        if (schematic == null) {
            schematic = root;
        }

        int version = schematic.containsKey("Version") ? schematic.getInt("Version") : -1;
        int dataVersion = schematic.containsKey("DataVersion") ? schematic.getInt("DataVersion") : -1;
        
        int width = schematic.getShort("Width");
        int height = schematic.getShort("Height");
        int length = schematic.getShort("Length");

        logger.at(Level.INFO).log("Dimensions: " + width + "x" + height + "x" + length);
        logger.at(Level.INFO).log("Version: " + version + ", DataVersion: " + dataVersion);

        int weOriginX = 0;
        int weOriginY = 0;
        int weOriginZ = 0;
        
        if (schematic.containsKey("Offset")) {
            int[] offset = schematic.getIntArray("Offset");
            if (offset.length >= 3) {
                weOriginX = offset[0];
                weOriginY = offset[1];
                weOriginZ = offset[2];
                logger.at(Level.INFO).log("Offset: (" + weOriginX + ", " + weOriginY + ", " + weOriginZ + ")");
            }
            else
                logger.at(Level.INFO).log("badly sized offset: "+offset.length);
        }
        else
            logger.at(Level.INFO).log("no offset");


        CompoundTag blocks = schematic.getCompoundTag("Blocks");
        
        Map<Integer, String> palette = parseSpongeBlockPalette(blocks);
        byte[] blockData = blocks.getByteArray("Data");
        
        Map<BlockLocation, BlockData> blockMap = parseSpongeBlocks(blockData, palette, width, height, length);
        
       // parseSpongeBlockEntities(blocks, blockMap);

        return new SchematicData(
            schematicFile.getName(),
            width, height, length,
            "Sponge",
            blockMap,
            null,
            weOriginX, weOriginY, weOriginZ,
            0, 0, 0,
            version,
            dataVersion
        );
    }

    private Map<Integer, String> parseSpongeBlockPalette(CompoundTag schematic) {
        Map<Integer, String> palette = new HashMap<>();
        
        if (!schematic.containsKey("Palette")) {
            logger.at(Level.WARNING).log("No Palette found in schematic");
            return palette;
        }

        CompoundTag paletteTag = schematic.getCompoundTag("Palette");
        logger.at(Level.INFO).log("Parsing palette with " + paletteTag.size() + " entries");

        for (Map.Entry<String, Tag<?>> entry : paletteTag.entrySet()) {
            String blockState = entry.getKey();
            int index = 0;
            
            Tag<?> tag = entry.getValue();
            if (tag instanceof net.querz.nbt.tag.IntTag) {
                index = ((net.querz.nbt.tag.IntTag) tag).asInt();
            } else if (tag instanceof net.querz.nbt.tag.ShortTag) {
                index = ((net.querz.nbt.tag.ShortTag) tag).asShort();
            } else if (tag instanceof net.querz.nbt.tag.ByteTag) {
                index = ((net.querz.nbt.tag.ByteTag) tag).asByte();
            }
            
            palette.put(index, blockState);
        }

        return palette;
    }

    private Map<BlockLocation, BlockData> parseSpongeBlocks(byte[] data, Map<Integer, String> palette,
                                                            int width, int height, int length) {
        Map<BlockLocation, BlockData> blockMap = new HashMap<>();
        int totalBlocks = width * height * length;

        logger.at(Level.INFO).log("Parsing " + totalBlocks + " blocks from Sponge format...");

        int[] blockIndices = decodeVarintArray(data, totalBlocks);

        for (int index = 0; index < blockIndices.length && index < totalBlocks; index++) {
            int x = index % width;
            int z = (index / width) % length;
            int y = index / (width * length);

            int paletteIndex = blockIndices[index];
            String blockState = palette.get(paletteIndex);
            
            if (blockState == null || blockState.isEmpty()) {
                continue;
            }

            if (!blockState.contains(":")) {
                blockState = "minecraft:" + blockState;
            }

            String blockName = blockState;
            if (MinecraftToHytaleMapper.getHytaleBlock(blockName).equals("skip"))
               continue;

            int rotationData = 0;
            boolean hasRotation = false;

            int extra = -1;
            if (blockState.contains("[")) {
                int bracketIndex = blockState.indexOf('[');
                blockName = blockState.substring(0, bracketIndex);
                String properties = blockState.substring(bracketIndex + 1, blockState.length() - 1);
                
                rotationData = parseRotationFromProperties(properties);
                if (rotationData != -1) {
                    hasRotation = true;
                }
                extra = parseExtraData(properties);
            }

                BlockLocation location = new BlockLocation(x, y, z);
                BlockData block = new BlockData(0, rotationData, blockName, hasRotation,extra);
                blockMap.put(location, block);
        }

        logger.at(Level.INFO).log("Parsed " + blockMap.size() + " blocks");
        return blockMap;
    }

    private int[] decodeVarintArray(byte[] data, int expectedLength) {
        int[] result = new int[expectedLength];
        int resultIndex = 0;
        int dataIndex = 0;

        while (dataIndex < data.length && resultIndex < expectedLength) {
            int value = 0;
            int shift = 0;
            byte currentByte;

            do {
                if (dataIndex >= data.length) break;
                currentByte = data[dataIndex++];
                value |= (currentByte & 0x7F) << shift;
                shift += 7;
            } while ((currentByte & 0x80) != 0);

            result[resultIndex++] = value;
        }

        return result;
    }

    private int parseExtraData(String properties) {
        String[] props = properties.split(",");

        for (String prop : props) {
            String[] keyValue = prop.split("=");
            if (keyValue.length != 2) continue;

            String key = keyValue[0].trim();
            String value = keyValue[1].trim();

            if (key.equals("type")) { // slabs
                switch (value) {
                    case "bottom": return 0;
                    case "top": return 1;
                    case "double": return 2;
                }
            }
            if (key.equals("half")) { // stairs
                switch (value) {
                    case "bottom": return 0;
                    case "top": return 1;
                    case "double": return 2;
                }
            }
        }

        return -1;
    }

    private int parseRotationFromProperties(String properties) {
        String[] props = properties.split(",");
        
        for (String prop : props) {
            String[] keyValue = prop.split("=");
            if (keyValue.length != 2) continue;
            
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();
            
            if (key.equals("facing")) {
                switch (value) {
                    case "north": return 0;
                    case "east": return 1;
                    case "south": return 2;
                    case "west": return 3;
                    case "up": return 4;
                    case "down": return 5;
                }
            } else if (key.equals("rotation")) { // signs
                try {
                    int rotation = Integer.parseInt(value);
                    return rotation % 4;
                } catch (NumberFormatException e) {
                    logger.at(Level.WARNING).log("Invalid rotation value: " + value);
                }
            }
        }
        
        return -1;
    }

    private void parseSpongeBlockEntities(CompoundTag schematic, Map<BlockLocation, BlockData> blockMap) {
        if (!schematic.containsKey("BlockEntities")) {
            return;
        }

        ListTag<?> blockEntitiesList = (ListTag<?>) schematic.get("BlockEntities");
        if (blockEntitiesList == null || blockEntitiesList.size() == 0) {
            return;
        }

        logger.at(Level.INFO).log("Parsing " + blockEntitiesList.size() + " block entities from Sponge format...");
        int processedCount = 0;

        for (Tag<?> tag : blockEntitiesList) {
            if (!(tag instanceof CompoundTag)) {
                continue;
            }

            CompoundTag blockEntity = (CompoundTag) tag;
            
            if (!blockEntity.containsKey("Pos")) {
                continue;
            }

            int[] pos = blockEntity.getIntArray("Pos");
            if (pos.length < 3) {
                continue;
            }

            int x = pos[0];
            int y = pos[1];
            int z = pos[2];
            
            String blockEntityId = null;
            if (blockEntity.containsKey("Id")) {
                Tag<?> idTag = blockEntity.get("Id");
                if (idTag instanceof net.querz.nbt.tag.StringTag) {
                    blockEntityId = ((net.querz.nbt.tag.StringTag) idTag).getValue();
                }
            }

            if (blockEntityId != null) {
                BlockLocation location = new BlockLocation(x, y, z);
                if (blockMap.containsKey(location)) {
                    processedCount++;
                }
            }
        }

        logger.at(Level.INFO).log("Processed " + processedCount + " block entities");
    }

}
