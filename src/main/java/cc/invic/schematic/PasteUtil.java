package cc.invic.schematic;

import cc.invic.SchematicLoader;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PasteUtil {

    private static final int BLOCKS_PER_BATCH = 100000;
    private static final long DELAY_BETWEEN_BATCHES_MS = 500;

    public static void paste(World world, Vector3d pasteSource, SchematicData data, ScheduledExecutorService scheduler) {
        if (data == null) {
            return;
        }

        int originX = 0;
        int originY = 0;
        int originZ = 0;

//        if (data.hasWorldEditOrigin()) {
//            originX = data.getWeOriginX();
//            originY = data.getWeOriginY();
//            originZ = data.getWeOriginZ();
//        }

        int offsetX = 0;
        int offsetY = 0;
        int offsetZ = 0;

        if (data.hasWorldEditOffset()) {
            offsetX = data.getWeOffsetX();
            offsetY = data.getWeOffsetY();
            offsetZ = data.getWeOffsetZ();
        }

        int baseX = (int) pasteSource.getX() - originX + offsetX;
        int baseY = (int) pasteSource.getY() - originY + offsetY;
        int baseZ = (int) pasteSource.getZ() - originZ + offsetZ;

        List<BlockPlacement> placements = new ArrayList<>();

        for (Map.Entry<BlockLocation, BlockData> entry : data.getBlocks().entrySet()) {
            BlockLocation loc = entry.getKey();
            BlockData block = entry.getValue();
            
            String hytaleBlock = block.getHytaleName();
            if (hytaleBlock == null || hytaleBlock.isEmpty()) {
                continue;
            }

            if(hytaleBlock.equals("skip"))
                continue;

            int worldX = baseX + loc.getX();
            int worldY = baseY + loc.getY();
            int worldZ = baseZ + loc.getZ();

            placements.add(new BlockPlacement(worldX, worldY, worldZ, hytaleBlock, block.hasRotation(), block.getBlockData(),block.getExtra()));
        }

        pasteAsync(world, placements, scheduler);
    }

    private static void pasteAsync(World world, List<BlockPlacement> placements, ScheduledExecutorService scheduler) {
        int totalBlocks = placements.size();
        int batches = (int) Math.ceil((double) totalBlocks / BLOCKS_PER_BATCH);
    
        for (int i = 0; i < batches; i++) {
            int startIndex = i * BLOCKS_PER_BATCH;
            int endIndex = Math.min(startIndex + BLOCKS_PER_BATCH, totalBlocks);
            List<BlockPlacement> batch = placements.subList(startIndex, endIndex);
            long delay = i * DELAY_BETWEEN_BATCHES_MS;
    
            scheduler.schedule(() -> {
                CompletableFuture.runAsync(() -> {
                    for (BlockPlacement placement : batch) {
                        if(placement.hasRotation) {
                            // Convert Minecraft rotation to Hytale rotation
                            Rotation yaw = getYawFromMinecraftData(placement.data,placement.blockType);
                            Rotation pitch = getPitchFromMinecraftData(placement.extra);
                            Rotation roll = Rotation.None;

                            long chunkIndex = ChunkUtil.indexChunkFromBlock(placement.x, placement.z);
                            WorldChunk chunk = world.getNonTickingChunk(chunkIndex);
                            if (chunk != null) {
//                                if(!placement.blockType.contains("Torch"))
//                                    SchematicLoader.get().getLogger().atInfo().log("rotation: "+pitch.toString()+ " block: "+placement.blockType);
                                chunk.placeBlock(placement.x, placement.y, placement.z, 
                                                placement.blockType, yaw, pitch, roll);
                            }
                        } else {
                            world.setBlock(placement.x, placement.y, placement.z, placement.blockType);
                        }
                    }
                }, world);
            }, delay, TimeUnit.MILLISECONDS);
        }
    }

    private static Rotation getPitchFromMinecraftData(int data)
    {
        switch(data) {
            case 0: return Rotation.None;
            case 1: return Rotation.Ninety;
            default: return Rotation.None;
        }
    }

    private static Rotation getYawFromMinecraftData(int data,String blockType) {
        // Minecraft rotation data:
        // 0 = north, 1 = east, 2 = south, 3 = west
        // 4 = up?, 5 = down/standing
        if(!blockType.contains("Chest"))
        {
            switch(data) {
                case 0: return Rotation.None;
                case 3: return Rotation.Ninety;
                case 2: return Rotation.OneEighty;
                case 1: return Rotation.TwoSeventy;
                default: return Rotation.None;
            }
        }
        else // chests are rotated the opposite of what is expected
        {
            switch(data) {
                case 2: return Rotation.None;
                case 1: return Rotation.Ninety;
                case 0: return Rotation.OneEighty;
                case 3: return Rotation.TwoSeventy;
                default: return Rotation.None;
            }
        }

    }

    private static class BlockPlacement {
        final int x, y, z;
        final String blockType;
        final int data;
        final boolean hasRotation;
        final int extra;

        BlockPlacement(int x, int y, int z, String blockType, boolean hasRotation,int data,int extra) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockType = blockType;
            this.data = data;
            this.hasRotation=hasRotation;
            this.extra = extra;
        }

        @Override
        public String toString(){
            return "{loc=x"+x+","+y+","+z+",block="+blockType+",hasRotation="+hasRotation+",data="+data+"}";

        }
    }
}
