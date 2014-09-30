package codechicken.wirelessredstone.core;

import codechicken.lib.colour.ColourARGB;
import codechicken.core.gui.GuiCCButton;
import codechicken.lib.render.CCRenderState;

import net.minecraft.client.renderer.Tessellator;

public class GuiButtonArrow extends GuiCCButton
{
    public GuiButtonArrow(int x, int y, int w, int h, int arrow)
    {
        super(x, y, w, h, "");
        setArrowDirection(arrow);
    }
    
    public void setArrowDirection(int dir)
    {
        arrowdirection = dir;
    }
    
    @Override
    public void draw(int mousex, int mousey, float frame)
    {
        if(!visible)
            return;
        
        drawArrow(x + width / 2 - 2, y + (height - 8) / 2, getTextColour(mousex, mousey));
    }
    
    private void drawArrow(int x, int y, int colour)
    {
        CCRenderState.changeTexture("wrcbe_core:textures/gui/arrow.png");
        
        new ColourARGB(colour).glColour();
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(x + 0, y + 8, zLevel, arrowdirection * 0.25, 1);
        t.addVertexWithUV(x + 8, y + 8, zLevel, (arrowdirection + 1) * 0.25, 1);
        t.addVertexWithUV(x + 8, y + 0, zLevel, (arrowdirection + 1) * 0.25, 0);
        t.addVertexWithUV(x + 0, y + 0, zLevel, arrowdirection * 0.25, 0);
        t.draw();
    }
    
    int arrowdirection;
}
