package codechicken.wirelessredstone.logic;

import net.minecraftforge.client.MinecraftForgeClient;

import static codechicken.wirelessredstone.logic.WirelessRedstoneLogic.*;

public class WRLogicClientProxy extends WRLogicProxy
{
    @Override
    public void init() {
        super.init();
        MinecraftForgeClient.registerItemRenderer(itemwireless, new ItemWirelessRenderer());
    }
}
