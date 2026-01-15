package cc.invic;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class BlockBreakEventsystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    protected BlockBreakEventsystem()
    {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl BreakBlockEvent event)
    {
        Ref<EntityStore> entityStoreRef = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(entityStoreRef, Player.getComponentType());
        if(player ==null)
            return;

       // player.sendMessage(Message.raw(event.getBlockType().toString() + " mined"));
        //SchematicLoader.get().getLogger().atInfo().log(event.getBlockType().toString());
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery()
    {
        return PlayerRef.getComponentType();
    }
}
