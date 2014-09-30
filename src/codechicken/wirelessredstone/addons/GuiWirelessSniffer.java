package codechicken.wirelessredstone.addons;

import codechicken.lib.colour.Colour;
import codechicken.lib.colour.ColourARGB;
import codechicken.lib.config.ConfigTag;
import codechicken.lib.render.CCRenderState;
import codechicken.wirelessredstone.core.*;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class GuiWirelessSniffer extends GuiScreen
{
    public GuiWirelessSniffer()
    {
        page = 0;
        ethercopy = new byte[RedstoneEther.numfreqs >> 3];
        brightness = new byte[RedstoneEther.numfreqs];
        title = "Wireless Sniffer";
    }

    public void initGui()
    {
        buttonList.add(new GuiButton(0, width / 2 + 40, height / 2 + 75, 50, 20, "Next"));
        buttonList.add(new GuiButton(1, width / 2 - 90, height / 2 + 75, 50, 20, "Prev"));

        super.initGui();
    }

    protected void actionPerformed(GuiButton guibutton)
    {
        switch(guibutton.id)
        {
            case 0:
                page++;
            break;

            case 1:
                page--;
            break;
        }

        if(page < 0)
        {
            page = RedstoneEther.numfreqs / 1000 - 1;
        }
        else if(page > RedstoneEther.numfreqs / 1000 - 1)
        {
            page = 0;
        }
    }
    
    public int getFreqMouseOver(int mousex, int mousey)
    {
        int xfreq = (mousex - ((width - xSize) / 2 + 8)) / (blocksize+1);
        int yfreq = (mousey - ((height - ySize) / 2 + 24)) / (blocksize+1);
        if(xfreq < 0 || xfreq >= 40 || yfreq < 0 || yfreq >= 25)
        {
            return 0;
        }
        return page * 1000 + 1 + yfreq * 40 + xfreq;
    }
    
    public void drawScreen(int mousex, int mousey, float partialframe)
    {
        drawDefaultBackground();

        int backtexx = (width - xSize) / 2;
        int backtexy = (height - ySize) / 2;
        int startfreq = page * 1000 + 1;
        int endfreq = (page+1) * 1000;
        if(endfreq > RedstoneEther.numfreqs)
            endfreq = RedstoneEther.numfreqs;
        
        GL11.glColor4f(1, 1, 1, 1);
        CCRenderState.changeTexture("wrcbe_addons:textures/gui/sniffer.png");
        drawTexturedModalRect(backtexx, backtexy, 0, 0, xSize, ySize);
        
        GL11.glPushMatrix();
            GL11.glTranslatef(backtexx, backtexy, 0.0F);
    
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);

            fontRendererObj.drawString(title, xSize / 2 - fontRendererObj.getStringWidth(title) / 2, 10, 0x404040);
    
            drawFrequencies(8, 24, startfreq);
            
            String rangestring = ""+startfreq+" - "+endfreq;
            fontRendererObj.drawString(rangestring, xSize / 2 - fontRendererObj.getStringWidth(rangestring) / 2, 181, 0x404040);
        GL11.glPopMatrix();
        
        int freq = getFreqMouseOver(mousex, mousey);
        if(freq != 0)
        {
            String freqname = getFreqTip(freq);
            int width = fontRendererObj.getStringWidth(freqname);
            drawGradientRect(mousex, mousey - 12, mousex + width + 3, mousey, 0xc0000000, 0xc0000000);
            fontRendererObj.drawStringWithShadow(freqname, mousex + 2, mousey - 10, -1);
        }
        
        super.drawScreen(mousex, mousey, partialframe);

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public String getFreqTip(int freq)
    {
        String name = RedstoneEther.get(true).getFreqName(freq);
        return Integer.toString(freq) + ((name == null || name.equals("")) ? "" : (" "+name));
    }
    
    public Colour getColour(int freq)
    {
        if(RedstoneEther.get(true).isPlayerJammed(mc.thePlayer) || !RedstoneEther.get(true).canBroadcastOnFrequency(mc.thePlayer, freq))
            return colourJammed;
        
        return colourOff.copy().interpolate(colourOn, brightness[freq-1] / 64F);
    }
    
    public Colour getBorder(int freq)
    {
        if(RedstoneEther.get(true).isPlayerJammed(mc.thePlayer) || !RedstoneEther.get(true).canBroadcastOnFrequency(mc.thePlayer, freq))
        {
            return borderJammed;
        }
        else if(RedstoneEther.get(true).getFreqColourId(freq) != -1)
        {
            return new ColourARGB(RedstoneEther.get(true).getFreqColour(freq));
        }
        else
        {
            return borderOff.copy().interpolate(borderOn, brightness[freq-1] / 64F);
        }
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }
    
    private void drawFrequencies(int xstart, int ystart, int freqstart)
    {        
        int freq = freqstart;
        for(int yfreq = 0; yfreq < 25; yfreq++)
        {
            for(int xfreq = 0; xfreq < 40; xfreq++)
            {
                if(freq > RedstoneEther.numfreqs)
                {
                    return;
                }
                
                int x = xstart + (blocksize+1) * xfreq;
                int y = ystart + (blocksize+1) * yfreq;
                drawRect(x, y, x + blocksize, y + blocksize, getBorder(freq).argb());
                drawRect(x + 1, y + 1, x + blocksize - 1, y + blocksize - 1, getColour(freq).argb());
                
                freq++;
            }
        }
    }

    public void onGuiClosed()
    {
        RedstoneEtherAddons.client().remSniffer(mc.thePlayer);
    }
    
    public void updateScreen()
    {
        super.updateScreen();
        for(int freq = 0; freq < RedstoneEther.numfreqs; freq++)
        {
            if((ethercopy[freq >> 3] & (1 << (freq & 7))) != 0)
            {
                if(brightness[freq] < 1)
                {
                    brightness[freq] = 1;
                }
                brightness[freq] = 64;
                if((brightness[freq] & 0xFF) > 64)
                {
                    brightness[freq] = 64;
                }
            }
            else
            {
                brightness[freq] *= 0.9;
            }
        }
    }
    
    public void setEtherCopy(byte[] copy)
    {
        ethercopy = copy;
    }

    public void setEtherFreq(int freq, boolean state)
    {
        freq--;
        if(state)
        {
            ethercopy[freq >> 3] |= 1 << (freq & 7);
            brightness[freq] = 64;
        }
        else
        {
            ethercopy[freq >> 3] &= ((1 << (freq & 7)) ^ 0xFF);
        }
    }
    
    public static void loadColours(ConfigTag addonconfig)
    {
        ConfigTag snifferconifg = addonconfig.getTag("sniffer.gui").useBraces();
        ConfigTag colourconfig = snifferconifg.getTag("colour").setPosition(0).setComment("Colours are in 0xAARRGGBB format:Alpha should be FF");
        ConfigTag borderconfig = snifferconifg.getTag("border").setPosition(1).setNewLine(true);
        
        colourOn = new ColourARGB(colourconfig.getTag("on").setPosition(0).setComment("").getHexValue(0xffFF0000));
        colourOff = new ColourARGB(colourconfig.getTag("off").setPosition(1).getHexValue(0xff700000));
        colourJammed = new ColourARGB(colourconfig.getTag("jammed").setPosition(2).getHexValue(0xff707070));
        
        colourPOn = new ColourARGB(colourconfig.getTag("private.on").setPosition(0).getHexValue(0xff40F000));
        colourPOff = new ColourARGB(colourconfig.getTag("private.off").setPosition(1).getHexValue(0xff40A000));
        
        borderOn = new ColourARGB(borderconfig.getTag("on").setPosition(0).getHexValue(0xffEE0000));
        borderOff = new ColourARGB(borderconfig.getTag("off").setPosition(1).getHexValue(0xff500000));
        borderJammed =  new ColourARGB(borderconfig.getTag("jammed").setPosition(2).getHexValue(0xff505050));
        
        borderPOn = new ColourARGB(borderconfig.getTag("private.on").setPosition(0).getHexValue(0xff20E000));
        borderPOff = new ColourARGB(borderconfig.getTag("private.off").setPosition(1).getHexValue(0xff209000));
    }

    public String title;
    
    private int page;
    private byte[] ethercopy;    
    protected byte[] brightness;
    
    public static int xSize = 256;
    public static int ySize = 200;
    public static ColourARGB colourOn;
    public static ColourARGB colourOff;
    public static ColourARGB colourPOn;
    public static ColourARGB colourPOff;
    public static ColourARGB colourJammed;
    public static ColourARGB borderOn;
    public static ColourARGB borderOff;
    public static ColourARGB borderPOn;
    public static ColourARGB borderPOff;
    public static ColourARGB borderJammed;
    
    public static final int blocksize = 5;
}
