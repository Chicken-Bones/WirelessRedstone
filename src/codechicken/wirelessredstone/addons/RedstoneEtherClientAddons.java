package codechicken.wirelessredstone.addons;

import java.util.HashMap;

import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.MapData;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import codechicken.wirelessredstone.core.IGuiRemoteUseable;
import codechicken.wirelessredstone.core.ItemWirelessFreq;
import codechicken.wirelessredstone.core.RedstoneEther;
import codechicken.wirelessredstone.core.WirelessRedstoneCore;

public class RedstoneEtherClientAddons extends RedstoneEtherAddons
{
    private boolean mousedown;
    private boolean wasmousedown;
    private Remote remote;
    private TriangFreqManager[] triangFreqs;
    private HashMap<Short, ClientMapInfo> clientMapInfoSet = new HashMap<Short, ClientMapInfo>();
    private WirelessMapNodeStorage wirelessmapnodes;
    private EntityREP activeREP;
    private int REPThrowTimeout;
    private int ticksInGui;

    public RedstoneEtherClientAddons() {
        clientMapInfoSet = new HashMap<Short, ClientMapInfo>();
        triangFreqs = new TriangFreqManager[RedstoneEther.numfreqs + 1];
        for (int freq = 1; freq <= RedstoneEther.numfreqs; freq++) {
            triangFreqs[freq] = new TriangFreqManager(freq);
        }
    }

    public void updateTriangulators(EntityPlayer player) {
        for (int freq = 1; freq <= RedstoneEther.numfreqs; freq++) {
            triangFreqs[freq].tickTriang(player);
        }
    }

    public boolean isTriangOn(int freq) {
        return triangFreqs[freq].isTriangOn();
    }

    public float getTriangAngle(int freq) {
        return triangFreqs[freq].getTriangAngle();
    }

    public void setTriangAngle(int freq, float angle) {
        triangFreqs[freq].setTriangAngle(angle);
    }

    public void setTriangRequired(EntityPlayer player, int freq, boolean required) {
        WRAddonCPH.sendSyncTriang(freq, required);
    }

    public boolean isRemoteOn(EntityPlayer player, int freq) {
        return remote == null ? false : remote.getFreq() == freq;
    }

    public int getRemoteFreq(EntityPlayer player) {
        return remote == null ? -1 : remote.getFreq();
    }

    public void checkClicks() {
        wasmousedown = mousedown;
        mousedown = Mouse.isButtonDown(1);
    }

    public void openItemGui(EntityPlayer player) {
        ItemStack helditem = player.inventory.getCurrentItem();
        if (helditem != null && helditem.getItem() instanceof ItemWirelessFreq && mousedown && !wasmousedown && player.isSneaking()) {
            WirelessRedstoneCore.proxy.openItemWirelessGui(player);
        }
    }

    public void processRemote(World world, EntityPlayer player, GuiScreen currentscreen, MovingObjectPosition mop) {
        boolean jammed = RedstoneEther.client().isPlayerJammed(player);

        if (remote != null && //remote is on
                (mousedown == false //mouse released
                        || (currentscreen != null && !(currentscreen instanceof IGuiRemoteUseable))//unsupporting gui open
                        || jammed))//jammed
        {
            deactivateRemote(world, player);
        }

        if (mouseClicked() &&
                remote == null && //not already using a remote
                player.inventory.getCurrentItem() != null && player.inventory.getCurrentItem().getItem() == WirelessRedstoneAddons.remote && //holding a remote
                (currentscreen != null && (currentscreen instanceof IGuiRemoteUseable) && !player.isSneaking()) && //in a remote active gui where onItemRight click won't take it
                ticksInGui > 0 && !jammed) {
            ItemStack stack = player.inventory.getCurrentItem();
            if (stack.getItemDamage() == 0 || ItemWirelessRemote.getTransmitting(stack))
                return;

            activateRemote(world, player);
        }
    }

    public void activateRemote(World world, EntityPlayer player) {
        if (remote != null) {
            if (remote.isBeingHeld())
                return;

            deactivateRemote(world, player);
        }
        if (RedstoneEther.client().isPlayerJammed(player))
            return;

        remote = new Remote(player);
        remote.metaOn();
        WRAddonCPH.sendSetRemote(true);
    }

    public boolean deactivateRemote(World world, EntityPlayer player) {
        if (remote == null)
            return false;

        remote.metaOff();
        remote = null;
        WRAddonCPH.sendSetRemote(false);
        return true;
    }

    public void addSniffer(EntityPlayer player) {
        WRAddonCPH.sendOpenSniffer();
    }

    public void remSniffer(EntityPlayer player) {
        WRAddonCPH.sendCloseSniffer();
    }

    public WirelessMapNodeStorage getMapNodes() {
        if (wirelessmapnodes == null) {
            wirelessmapnodes = new WirelessMapNodeStorage();
        }
        return wirelessmapnodes;
    }

    public void updateSSPMap(World world, EntityPlayer player, MapData mapdata) {
        if (RedstoneEther.get(true).isPlayerJammed(player) || mapdata.dimension != player.dimension) {
            clearMapNodes(player);
            return;
        }
    }

    public void clearMapNodes(EntityPlayer player) {
        WRAddonCPH.sendResetMap();
        wirelessmapnodes = null;
    }

    public boolean mouseClicked() {
        return mousedown && !wasmousedown;
    }

    public ClientMapInfo getMPMapInfo(short mapid) {
        return clientMapInfoSet.get(mapid);
    }

    public void setMPMapInfo(short mapid, ClientMapInfo mapinfo) {
        clientMapInfoSet.put(mapid, mapinfo);
    }

    public void tick() {
        Minecraft mc = Minecraft.getMinecraft();

        checkClicks();
        updateTriangulators(mc.thePlayer);
        openItemGui(mc.thePlayer);
        processRemote(mc.theWorld, mc.thePlayer, mc.currentScreen, mc.objectMouseOver);

        if (REPThrowTimeout > 0)
            REPThrowTimeout--;

        if (mc.currentScreen == null)
            ticksInGui = 0;
        else
            ticksInGui++;
    }

    public boolean detonateREP(EntityPlayer player) {
        if (activeREP == null) {
            return false;
        } else if (activeREP.isDead) {
            activeREP = null;
            return false;
        } else {
            activeREP.detonate();
            activeREP.setDead();
            return true;
        }
    }

    public void throwREP(ItemStack itemstack, World world, EntityPlayer player) {
        if (REPThrowTimeout > 0) {
            return;
        }

        if (!player.capabilities.isCreativeMode) {
            itemstack.stackSize--;
        }
        world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (world.rand.nextFloat() * 0.4F + 0.8F));
        activeREP = new EntityREP(world, player);
        world.spawnEntityInWorld(activeREP);
        REPThrowTimeout = 40;
    }

    public void invalidateREP(EntityPlayer shootingEntity) {
        activeREP = null;
    }
}
