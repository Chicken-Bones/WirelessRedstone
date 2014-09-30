package codechicken.wirelessredstone.addons;

import codechicken.lib.colour.Colour;
import codechicken.lib.colour.ColourARGB;
import codechicken.lib.render.TextureDataHolder;
import codechicken.lib.render.TextureSpecial;
import codechicken.lib.render.TextureUtils;
import codechicken.wirelessredstone.core.*;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

public class RemoteTexManager
{
    private static Colour texGrad[];
    private static Colour texOff[];
    private static Colour texOn[];
    private static int[] imageData = new int[256];
    private static TextureSpecial[] icons = new TextureSpecial[(RedstoneEther.numcolours+1)*2];
    
    public static void load(TextureMap registrar)
    {
        for(int i = 0; i < icons.length; i++)
            icons[i] = TextureUtils.getTextureSpecial(registrar, "wrcbe_addons:remote_"+i);
        
        texOn = TextureUtils.loadTextureColours(new ResourceLocation("wrcbe_addons", "textures/items/remoteOn.png"));
        texOff = TextureUtils.loadTextureColours(new ResourceLocation("wrcbe_addons", "textures/items/remoteOff.png"));
        texGrad = TextureUtils.loadTextureColours(new ResourceLocation("wrcbe_addons", "textures/items/remoteGrad.png"));

        for(int i = 0; i < RedstoneEther.numcolours; i++)
        {
            processTexture(RedstoneEther.colours[i], false, getIconIndex(i, false));
            processTexture(RedstoneEther.colours[i], true, getIconIndex(i, true));
        }
        processTexture(0xFFFFFFFF, false, getIconIndex(-1, false));
        processTexture(0xFFFFFFFF, true, getIconIndex(-1, true));
    }
    
    private static void processTexture(int colour, boolean on, int i)
    {
        mergeTexturesWithColour(new ColourARGB(colour), on);
        icons[i].addTexture(new TextureDataHolder(imageData, 16).copyData());
    }

    public static IIcon getIcon(int colourid, boolean on)
    {
        return icons[getIconIndex(colourid, on)];
    }
    
    public static int getIconIndex(int colourid, boolean on)
    {
        return colourid + 1 + (on ? RedstoneEther.numcolours+1 : 0);
    }
    
    private static void mergeTexturesWithColour(Colour texcolour, boolean on)
    {
        for(int i = 0; i < 256; i++)
        {
            Colour colour;
            if(texGrad[i].a == 0)
                colour = on ? texOn[i] : texOff[i];
            else
                colour = texGrad[i].copy().multiply(texcolour);
            
            imageData[i] = colour.argb();
        }
    }
}
