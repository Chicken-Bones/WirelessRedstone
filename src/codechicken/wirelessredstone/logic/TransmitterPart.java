package codechicken.wirelessredstone.logic;

import net.minecraft.item.ItemStack;

import codechicken.core.ClientUtils;
import codechicken.lib.data.MCDataInput;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.wirelessredstone.core.*;
import net.minecraft.util.StatCollector;

import static codechicken.lib.vec.Rotation.*;
import static codechicken.lib.vec.Vector3.*;

public class TransmitterPart extends TransceiverPart
{
    public static Cuboid6[] extensionBB = new Cuboid6[24];

    static {
        Cuboid6 base = new Cuboid6(7 / 16D, 1 / 8D, 2 / 8D, 9 / 16D, 7 / 8D, 3 / 8D);
        for (int s = 0; s < 6; s++)
            for (int r = 0; r < 4; r++)
                extensionBB[s << 2 | r] = base.copy().apply(sideOrientation(s, r).at(center));
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(WirelessRedstoneLogic.itemwireless, 1, 0);
    }

    @Override
    public void onNeighborChanged() {
        if (dropIfCantStay())
            return;

        int gettingPowered = getPoweringLevel();
        if (!active() && gettingPowered > 0)
            trySetState(true);
        else if (active() && gettingPowered == 0)
            trySetState(false);
    }

    private void trySetState(boolean on) {
        setActive(on);
        if (!disabled() || !on) {
            changeSpinState(on);
            RedstoneEther.server().setTransmitter(world(), x(), y(), z(), currentfreq, active());
        }
        updateChange();
    }

    private void changeSpinState(boolean on) {
        if (on && spinoffset < 0)//turning on
        {
            int time = (int) ((world().getTotalWorldTime() + spinoffset) % 100000);
            spinoffset = time;
        } else if (!on && spinoffset >= 0)//turning off
        {
            int time = (int) ((world().getTotalWorldTime() - spinoffset) % 100000);
            spinoffset = -time;
        }
    }

    @Override
    public void jamTile() {
        super.jamTile();
        updateChange();
    }

    @Override
    public void unjamTile() {
        super.unjamTile();
        addToEther();
        onNeighborChanged();
    }

    @Override
    public Vector3 getFocalPoint() {
        return new Vector3(0.3125, 0.24, 0).apply(rotationT());
    }

    @Override
    public Vector3 getPearlPos() {
        return new Vector3(0.5, 0.74 + getFloating() * 0.04, 5 / 16D);
    }

    @Override
    public double getPearlSpin() {
        if (spinoffset < 0)
            return RedstoneEther.getRotation(-spinoffset, currentfreq);

        return RedstoneEther.getRotation(ClientUtils.getRenderTime() - spinoffset, currentfreq);
    }

    public float getPearlLight() {
        float light = world().getLightBrightness(x(), y(), z());
        if ((deadmap & 1) == 1 || (deadmap == 0 && (disabled() || !active() || currentfreq == 0))) {
            light = (light + 1) * 0.25F;
        } else {
            light = (light + 1) * 0.5F;
        }
        return light;
    }

    @Override
    public void setFreq(int newfreq) {
        super.setFreq(newfreq);

        changeSpinState(active());
    }

    private void resetRotation() {
        spinoffset = active() ? 0 : -1;
    }

    @Override
    public void addToEther() {
        resetRotation();
        updateChange();
        RedstoneEther.server().setTransmitter(world(), x(), y(), z(), currentfreq, active() && !disabled());
    }

    @Override
    public void removeFromEther() {
        RedstoneEther.server().remTransmitter(world(), x(), y(), z(), currentfreq);
    }

    @Override
    public String getGuiName() {
        return StatCollector.translateToLocal("item.wrcbe_logic:wirelesspart|0.name");
    }

    @Override
    public String getType() {
        return "wrcbe-tran";
    }

    @Override
    public void read(MCDataInput packet) {
        super.read(packet);
        changeSpinState(active());
    }

    @Override
    public Cuboid6 getExtensionBB() {
        return extensionBB[shape()];
    }

    @Override
    public int modelId() {
        return 0;
    }
}
