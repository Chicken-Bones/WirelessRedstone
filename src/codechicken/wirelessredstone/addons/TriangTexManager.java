package codechicken.wirelessredstone.addons;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import codechicken.lib.math.MathHelper;
import codechicken.lib.colour.Colour;
import codechicken.lib.colour.ColourARGB;
import codechicken.lib.colour.ColourRGBA;
import codechicken.lib.render.ManagedTextureFX;
import codechicken.lib.render.TextureUtils;
import codechicken.wirelessredstone.core.RedstoneEther;
import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

public class TriangTexManager
{
    private static Colour[] texRing = new Colour[256];
    private static Colour[] texGrad = new Colour[256];
    private static int[] imageData = new int[256];
    private static ManagedTextureFX[] textures = new ManagedTextureFX[256];

    private static ColourRGBA pr = new ColourRGBA(0xFF0000FF);//pointer colour redstone
    private static ColourRGBA pb = new ColourRGBA(0x0000FFFF);//pointer colour blue
    private static ColourRGBA pg = new ColourRGBA(0x808080FF);//pointer colour grey
    private static ColourRGBA pd = new ColourRGBA(0x404040FF);//pointer colour dark grey
    
    private static HashMap<Integer, Integer> freqslotmap = new HashMap<Integer, Integer>(256);
    private static LinkedList<Integer> freeslots = new LinkedList<Integer>();
    private static HashSet<Integer> activetextures = new HashSet<Integer>(256);
    private static HashSet<Integer> visibletextures = new HashSet<Integer>(256);
    
    private static ColourRGBA[] pointercolours = new ColourRGBA[]{pb,pr,pr,pr,pr,pr,pr,pr,pb,pr,pr,pr,pb,pb};
    private static ColourRGBA[] pointersidecolours = new ColourRGBA[]{pg,pg,pg,pg,pg,pg,pd,pg,pd,pd,pd,pd,pg,pg};
    
    static
    {
        for(int i = 1; i < textures.length; i++)
            freeslots.add(i);
        
        for(int i = 0; i < textures.length; i++)
        {
            textures[i] = new ManagedTextureFX(16, "wrcbe_addons:triang_"+i);
            textures[i].setAtlas(1);
        }
    }
    
    public static void loadTextures()
    {
        texRing = TextureUtils.loadTextureColours(new ResourceLocation("wrcbe_addons", "textures/items/triangRing.png"));
        texGrad = TextureUtils.loadTextureColours(new ResourceLocation("wrcbe_addons", "textures/items/triangGrad.png"));
        processTexture(-1, 0);
    }
    
    public static IIcon getIconFromDamage(int damage)
    {
        return textures[getIconIndexFromDamage(damage)].texture;
    }
    
    public static int getIconIndexFromDamage(int freq)
    {
        if(freq == 0)
            return 0;

        visibletextures.add(freq);
        
        Integer iconindex = freqslotmap.get(freq);
        if(iconindex == null)
            iconindex = allocateSlot(freq);
        
        return iconindex;
    }
    
    private static int allocateSlot(int damage)
    {
        if(freeslots.isEmpty())
            throw new RuntimeException("More than 256 different triangulators!");
        
        int slot = freeslots.remove(0);
        freqslotmap.put(damage, slot);
        return slot;
    }

    private static void processTexture(int freq, int iconindex)
    {
        int colour = freq <= 0 ? 0xFFFFFFFF : RedstoneEther.get(true).getFreqColour(freq);

        mergeTexturesWithColour(new ColourARGB(colour));
        if(freq > 0)
            writePointer(freq);
        
        textures[iconindex].setData(imageData);
    }
    
    private static void mergeTexturesWithColour(ColourARGB texcolour)
    {
        for(int i = 0; i < 256; i++)
        {
            Colour colour;
            if(texGrad[i].a == 0)
                colour = texRing[i];
            else
                colour = texGrad[i].copy().multiply(texcolour);
            
            imageData[i] = colour.argb();
        }
    }
    
    private static void writePointer(int freq)
    {
        if(RedstoneEther.get(true).isPlayerJammed(Minecraft.getMinecraft().thePlayer) || !RedstoneEtherAddons.client().isTriangOn(freq))
            return;
        
        int id = RedstoneEther.get(true).getFreqColourId(freq);
        ColourRGBA pcolour = id < 0 ? pr : pointercolours[id];
        ColourRGBA scolour = id < 0 ? pg : pointersidecolours[id];
        
        float angle = RedstoneEtherAddons.client().getTriangAngle(freq);
        double cos = MathHelper.cos(angle);
        double sin = MathHelper.sin(angle);
        
        for(int i = -4; i <= 4; i++)// for side parts
        {
            int col = (int) (8.5 + cos * i * 0.3);
            int row = (int) (7.5 - sin * i * 0.3 * 0.5);
            int p = row * 16 + col;
            
            imageData[p] = scolour.argb();
        }

        for(int i = -8; i <= 16; i++)
        {
            int col = (int) (8.5 + sin * i * 0.3);
            int row = (int) (7.5 + cos * i * 0.3 * 0.5);
            int p = row * 16 + col;
            
            Colour c = i < 0 ? scolour : pcolour;
            imageData[p] = c.argb();
        }
    }
    
    public static void processAllTextures()
    {
        HashSet<Integer> wasActive = new HashSet<Integer>(activetextures);
        for(int freq : visibletextures)
        {
            int slot = freqslotmap.get(freq);
            if(!wasActive.remove(freq))
            {
                RedstoneEtherAddons.client().setTriangRequired(Minecraft.getMinecraft().thePlayer, freq, true);
                freeslots.remove(slot);
                activetextures.add(freq);
            }

            processTexture(freq, slot);
        }
        
        for(int freq : wasActive)
        {
            RedstoneEtherAddons.client().setTriangRequired(Minecraft.getMinecraft().thePlayer, freq, false);
            freeslots.add(freqslotmap.get(freq));
            activetextures.remove(freq);
        }
        
        visibletextures.clear();
    }
}
