package codechicken.wirelessredstone.logic;

import org.lwjgl.opengl.GL11;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

public class ItemWirelessRenderer implements IItemRenderer
{
    private WirelessPart[] renderParts = new WirelessPart[3];
    
    public ItemWirelessRenderer()
    {
        for(int i = 0; i < renderParts.length; i++)
            renderParts[i] = ItemWirelessPart.getPart(i);
    }
    
    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type)
    {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper)
    {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType t, ItemStack item, Object... data)
    {
        GL11.glPushMatrix();
        if(t == ItemRenderType.ENTITY)
            GL11.glScaled(0.5, 0.5, 0.5);
        if(t == ItemRenderType.INVENTORY || t == ItemRenderType.ENTITY)
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        
        RenderWireless.renderInv(renderParts[item.getItemDamage()]);
        GL11.glPopMatrix();
    }

}
