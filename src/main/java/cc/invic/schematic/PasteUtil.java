package cc.invic.schematic;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

public class PasteUtil {

    private static final int BLOCKS_PER_BATCH = 500000;

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

            int worldX = baseX + loc.getX();
            int worldY = baseY + loc.getY();
            int worldZ = baseZ + loc.getZ();

            placements.add(new BlockPlacement(worldX, worldY, worldZ, hytaleBlock, block.hasRotation(), block.getBlockData()));
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

            for (BlockPlacement placement : batch) {
                world.setBlock(placement.x, placement.y, placement.z, placement.blockType);
                if(placement.hasRotation)
                {
                    int rotation = placement.data;
                    // TODO: rotate this block
                    // 0 = north
                    // 1 = east
                    // 2 = south
                    // 3 = west
                    //
                }
            }
        }
    }

    private static class BlockPlacement {
        final int x, y, z;
        final String blockType;
        final int data;
        final boolean hasRotation;

        BlockPlacement(int x, int y, int z, String blockType, boolean hasRotation,int data) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockType = blockType;
            this.data = data;
            this.hasRotation=hasRotation;
        }

        @Override
        public String toString(){
            return "{loc=x"+x+","+y+","+z+",block="+blockType+",hasRotation="+hasRotation+",data="+data+"}";

        }
    }
}
