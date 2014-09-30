package codechicken.wirelessredstone.addons;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import codechicken.core.ClientUtils;
import codechicken.wirelessredstone.core.*;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.IItemRenderer;

public class WirelessMapRenderer implements IItemRenderer
{
    private void renderPass(int xCenter, int zCenter, int scale, WirelessMapNodeStorage mapstorage, long worldTime, float size, float alpha, float light) {
        Tessellator tessellator = Tessellator.instance;
        float blockscale = 1 << scale;

        for (FreqCoord node : mapstorage.nodes) {
            float relx = node.x / blockscale + 64;
            float relz = node.z / blockscale + 64;

            int colour = RedstoneEther.client().getFreqColour(node.freq);
            if (colour == 0xFFFFFFFF) {
                colour = 0xFFFF0000;
            }
            float r = ((colour >> 16) & 0xFF) / 255F * light;
            float g = ((colour >> 8) & 0xFF) / 255F * light;
            float b = (colour & 0xFF) / 255F * light;
            tessellator.setColorRGBA_F(r, g, b, alpha);

            float rot = RedstoneEther.getRotation(ClientUtils.getRenderTime(), node.freq);
            float xrot = (float) (Math.sin(rot) * size);
            float zrot = (float) (Math.cos(rot) * size);

            tessellator.addVertex(relx - zrot, relz + xrot, -0.01);
            tessellator.addVertex(relx + xrot, relz + zrot, -0.01);
            tessellator.addVertex(relx + zrot, relz - xrot, -0.01);
            tessellator.addVertex(relx - xrot, relz - zrot, -0.01);
        }

        for (FreqCoord node : mapstorage.devices) {
            float relx = (node.x - xCenter) / blockscale + 64;
            float relz = (node.z - zCenter) / blockscale + 64;

            int colour = RedstoneEther.client().getFreqColour(node.freq);
            if (colour == 0xFFFFFFFF) {
                colour = 0xFFFF0000;
            }
            float r = ((colour >> 16) & 0xFF) / 255F * light;
            float g = ((colour >> 8) & 0xFF) / 255F * light;
            float b = (colour & 0xFF) / 255F * light;
            tessellator.setColorRGBA_F(r, g, b, alpha);

            float rot = RedstoneEther.getRotation(ClientUtils.getRenderTime(), node.freq);
            float xrot = (float) (Math.sin(rot) * size);
            float zrot = (float) (Math.cos(rot) * size);

            tessellator.addVertex(relx - zrot, relz + xrot, -0.01);
            tessellator.addVertex(relx + xrot, relz + zrot, -0.01);
            tessellator.addVertex(relx + zrot, relz - xrot, -0.01);
            tessellator.addVertex(relx - xrot, relz - zrot, -0.01);
        }
    }

    public void renderMap(EntityPlayer player, MapData mapData, boolean inFrame) {
        Minecraft.getMinecraft().entityRenderer.getMapItemRenderer().func_148250_a(mapData, inFrame);

        WirelessMapNodeStorage mapstorage = RedstoneEtherAddons.client().getMapNodes();
        Tessellator tessellator = Tessellator.instance;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        float light = 1;
        long worldTime = player.worldObj.getTotalWorldTime();

        ItemStack currentitem = player.inventory.getCurrentItem();
        if (currentitem == null || currentitem.getItem() != WirelessRedstoneAddons.wirelessMap)
            return;

        ClientMapInfo mapinfo = RedstoneEtherAddons.client().getMPMapInfo((short) currentitem.getItemDamage());
        if (mapinfo == null)
            return;

        int xCenter = mapinfo.xCenter;
        int zCenter = mapinfo.zCenter;
        int scale = mapinfo.scale;

        tessellator.startDrawingQuads();
        renderPass(xCenter, zCenter, scale, mapstorage, worldTime, 0.75F, 1F, light * 0.5F);
        renderPass(xCenter, zCenter, scale, mapstorage, worldTime, 0.6F, 1F, light);
        tessellator.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type == ItemRenderType.FIRST_PERSON_MAP;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        renderMap((EntityPlayer) data[0], (MapData) data[2], false);
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return true;
    }
}
