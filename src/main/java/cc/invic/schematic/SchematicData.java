package cc.invic.schematic;

import java.util.HashMap;
import java.util.Map;

public class SchematicData {
    private final String fileName;
    private final int width;
    private final int height;
    private final int length;
    private final String materials;
    private final Map<BlockLocation, BlockData> blocks;
    private final Map<String, Integer> schematicaMapping;
    private final int weOriginX;
    private final int weOriginY;
    private final int weOriginZ;
    private final int weOffsetX;
    private final int weOffsetY;
    private final int weOffsetZ;
    private final int version;
    private final int dataVersion;

    public SchematicData(String fileName, int width, int height, int length, String materials,
                        Map<BlockLocation, BlockData> blocks, Map<String, Integer> schematicaMapping,
                        int weOriginX, int weOriginY, int weOriginZ,
                        int weOffsetX, int weOffsetY, int weOffsetZ,
                        int version, int dataVersion) {
        this.fileName = fileName;
        this.width = width;
        this.height = height;
        this.length = length;
        this.materials = materials;
        this.blocks = blocks;
        this.schematicaMapping = schematicaMapping != null ? schematicaMapping : new HashMap<>();
        this.weOriginX = weOriginX;
        this.weOriginY = weOriginY;
        this.weOriginZ = weOriginZ;
        this.weOffsetX = weOffsetX;
        this.weOffsetY = weOffsetY;
        this.weOffsetZ = weOffsetZ;
        this.version = version;
        this.dataVersion = dataVersion;
    }

    public String getFileName() {
        return fileName;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLength() {
        return length;
    }

    public String getMaterials() {
        return materials;
    }

    public Map<BlockLocation, BlockData> getBlocks() {
        return blocks;
    }

    public int getTotalBlocks() {
        return blocks.size();
    }

    public Map<String, Integer> getSchematicaMapping() {
        return schematicaMapping;
    }

    public int getWeOriginX() {
        return weOriginX;
    }

    public int getWeOriginY() {
        return weOriginY;
    }

    public int getWeOriginZ() {
        return weOriginZ;
    }

    public int getWeOffsetX() {
        return weOffsetX;
    }

    public int getWeOffsetY() {
        return weOffsetY;
    }

    public int getWeOffsetZ() {
        return weOffsetZ;
    }

    public boolean hasWorldEditOrigin() {
        return weOriginX != 0 || weOriginY != 0 || weOriginZ != 0;
    }

    public boolean hasWorldEditOffset() {
        return true;//weOffsetX != 0 || weOffsetY != 0 || weOffsetZ != 0;
    }

    public int getVersion() {
        return version;
    }

    public int getDataVersion() {
        return dataVersion;
    }
}
