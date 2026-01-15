package cc.invic.schematic;

public class BlockData {
    private final int blockId;
    private final int blockData;
    private final String modernName;
    private final String hytaleName;
    private final boolean hasRotation;
    private final int extra;


    public BlockData(int blockId, int blockData, String modernName,boolean hasRotation, int extra) {
        this.blockId = blockId;
        this.blockData = blockData;
        this.modernName = modernName;
        this.hytaleName = MinecraftToHytaleMapper.getHytaleBlock(modernName);
        this.hasRotation = hasRotation;
        this.extra = extra;
    }

    public int getBlockId() {
        return blockId;
    }

    public int getBlockData() {
        return blockData;
    }

    public String getModernName() {
        return modernName;
    }

    public String getHytaleName() {
        return hytaleName;
    }

    public boolean hasRotation()
    {
        return hasRotation;
    }

    /**
     * -1 = no data
     * 0 = bottom half stair / slab
     * 1 = top half stair / slab
     * 2 = full block slab (probably wont be used)
     * @return
     */
    public int getExtra()
    {
        return extra;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(modernName);
        if (hytaleName != null && !hytaleName.isEmpty()) {
            sb.append(" -> Hytale: ").append(hytaleName);
        }
        if(modernName != null) {
            sb.append(" -> Modern Minecraft: ").append(modernName);
        }
        if(blockId != 0) {
            sb.append(" -> Legacy ID: ").append(blockId);
            sb.append(" -> Legacy Data: ").append(blockData);
        }
        sb.append(" -> hasRotation?: "+hasRotation );
        return sb.toString();
    }
}


