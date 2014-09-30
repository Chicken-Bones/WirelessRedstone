package codechicken.wirelessredstone.core;

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
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

public class WRCoreEventHandler
{
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.world.isRemote)
            RedstoneEther.loadClientEther(event.world);
        else
            RedstoneEther.loadServerWorld(event.world);
    }

    @SubscribeEvent
    public void onChunkDataLoad(ChunkDataEvent.Load event) {
        RedstoneEther.loadServerWorld(event.world);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote)
            return;

        RedstoneEther.unloadServerWorld(event.world);

        if (!ServerUtils.mc().isServerRunning())
            RedstoneEther.unloadServer();
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event) {
        if (event.world.isRemote || RedstoneEther.server() == null)
            return;

        RedstoneEther.server().saveEther(event.world);
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.world.isRemote)
            return;

        if (RedstoneEther.server() != null)//new world
        {
            RedstoneEther.loadServerWorld(event.world);
            RedstoneEther.server().verifyChunkTransmitters(event.world, event.getChunk().xPosition, event.getChunk().zPosition);
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        RenderWirelessBolt.render(event.partialTicks, Minecraft.getMinecraft().renderViewEntity);
    }

    @SubscribeEvent
    public void playerLogin(PlayerLoggedInEvent event) {
        RedstoneEther.server().resetPlayer(event.player);
    }

    @SubscribeEvent
    public void playerDimensionChange(PlayerChangedDimensionEvent event) {
        RedstoneEther.server().resetPlayer(event.player);
    }

    @SubscribeEvent
    public void playerLogout(PlayerLoggedOutEvent event) {
        RedstoneEther.server().removePlayer(event.player);
    }

    @SubscribeEvent
    public void playerRespawn(PlayerRespawnEvent event) {
        RedstoneEther.server().resetPlayer(event.player);
    }

    @SubscribeEvent
    public void clientTick(ClientTickEvent event) {
        if(event.phase == Phase.START)
            WirelessBolt.update(WirelessBolt.clientboltlist);
    }

    @SubscribeEvent
    public void serverTick(ServerTickEvent event) {
        if(event.phase == Phase.START)
            WirelessBolt.update(WirelessBolt.serverboltlist);
    }

    @SubscribeEvent
    public void serverTick(WorldTickEvent event) {
        if(event.phase == Phase.END && !event.world.isRemote)
            RedstoneEther.server().tick(event.world);
    }
}
