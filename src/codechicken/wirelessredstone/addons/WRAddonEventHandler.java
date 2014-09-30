package codechicken.wirelessredstone.addons;

import codechicken.core.ClientUtils;
import codechicken.core.ServerUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.world.ChunkEvent.Unload;
import net.minecraftforge.event.world.WorldEvent.Load;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class WRAddonEventHandler
{
    @SubscribeEvent
    public void playerLogin(PlayerLoggedInEvent event) {
        RedstoneEtherAddons.server().onLogin(event.player);
    }

    @SubscribeEvent
    public void playerLogout(PlayerLoggedOutEvent event) {
        RedstoneEtherAddons.server().onLogout(event.player);
    }

    @SubscribeEvent
    public void playerDimensionChange(PlayerChangedDimensionEvent event) {
        RedstoneEtherAddons.server().onDimensionChange(event.player);
    }

    @SubscribeEvent
    public void playerRespawn(PlayerRespawnEvent event) {
        RedstoneEtherAddons.server().onLogin(event.player);
    }

    @SubscribeEvent
    public void worldTick(WorldTickEvent event) {
        if(event.phase == Phase.START)
            RedstoneEtherAddons.server().processSMPMaps(event.world);
    }

    @SubscribeEvent
    public void serverTick(ServerTickEvent event) {
        if(event.phase == Phase.START)
            RedstoneEtherAddons.server().processTrackers();
        else {
            RedstoneEtherAddons.server().tickTriangs();
            RedstoneEtherAddons.server().updateREPTimeouts();
        }
    }

    @SubscribeEvent
    public void clientTick(ClientTickEvent event) {
        if(ClientUtils.inWorld()) {
            if (event.phase == Phase.START)
                TriangTexManager.processAllTextures();
            else
                RedstoneEtherAddons.client().tick();
        }
    }

    @SubscribeEvent
    public void onWorldLoad(Load event) {
        if (event.world.isRemote)
            RedstoneEtherAddons.loadClientManager();
        else
            RedstoneEtherAddons.loadServerWorld();
    }

    @SubscribeEvent
    public void onChunkUnload(Unload event) {
        Chunk chunk = event.getChunk();
        for (int i = 0; i < chunk.entityLists.length; ++i) {
            for (int j = 0; j < chunk.entityLists[i].size(); ++j) {
                Object o = chunk.entityLists[i].get(j);
                if (o instanceof EntityWirelessTracker)
                    ((EntityWirelessTracker) o).onChunkUnload();
            }
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onTextureLoad(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() == 1) {
            RemoteTexManager.load(event.map);
            TriangTexManager.loadTextures();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        if (event.world.isRemote)
            return;

        if (!ServerUtils.mc().isServerRunning())
            RedstoneEtherAddons.unloadServer();
    }
}
