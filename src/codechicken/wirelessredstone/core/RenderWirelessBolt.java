package codechicken.wirelessredstone.core;

import java.util.Iterator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.vec.Vector3;
import codechicken.wirelessredstone.core.WirelessBolt.Segment;

public class RenderWirelessBolt
{
    private static Vector3 getRelativeViewVector(Vector3 pos)
    {
        Entity renderentity = Minecraft.getMinecraft().renderViewEntity;
        return new Vector3((float)renderentity.posX - pos.x, (float)renderentity.posY + renderentity.getEyeHeight() - pos.y, (float)renderentity.posZ - pos.z);
    }
    
    public static void render(float frame, Entity entity)
    {        
        GL11.glPushMatrix();
        RenderUtils.translateToWorldCoords(entity, frame);
        
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        CCRenderState.reset();
        CCRenderState.setBrightness(0xF000F0);
        CCRenderState.changeTexture("wrcbe_core:textures/lightning_glowstone.png");
        CCRenderState.startDrawing(7);
        for(WirelessBolt bolt : WirelessBolt.clientboltlist)
            renderBolt(bolt, frame, ActiveRenderInfo.rotationX, ActiveRenderInfo.rotationXZ, ActiveRenderInfo.rotationZ, ActiveRenderInfo.rotationXY, 0);
        CCRenderState.draw();
        
        CCRenderState.changeTexture("wrcbe_core:textures/lightning_redstone.png");
        CCRenderState.startDrawing(7);
        for(WirelessBolt bolt : WirelessBolt.clientboltlist)
            renderBolt(bolt, frame, ActiveRenderInfo.rotationX, ActiveRenderInfo.rotationXZ, ActiveRenderInfo.rotationZ, ActiveRenderInfo.rotationXY, 1);
        CCRenderState.draw();
        
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);

        GL11.glPopMatrix();
    }
    
    private static void renderBolt(WirelessBolt bolt, float partialframe, float cosyaw, float cospitch, float sinyaw, float cossinpitch, int pass)
    {
        Tessellator t = Tessellator.instance;
        float boltage = bolt.particleAge < 0 ? 0 : (float)bolt.particleAge / (float)bolt.particleMaxAge;
        float mainalpha = 1;
        if(pass == 0)
            mainalpha = (1 - boltage) * 0.4F;
        else
            mainalpha = 1 - boltage * 0.5F;
        
        int expandTime = (int)(bolt.length*WirelessBolt.speed);    
        int renderstart = (int) ((expandTime/2-bolt.particleMaxAge+bolt.particleAge) / (float)(expandTime/2) * bolt.numsegments0);
        int renderend = (int) ((bolt.particleAge+expandTime) / (float)expandTime * bolt.numsegments0);
        
        for(Iterator<Segment> iterator = bolt.segments.iterator(); iterator.hasNext();)
        {
            Segment rendersegment = iterator.next();
            
            if(rendersegment.segmentno < renderstart || rendersegment.segmentno > renderend)
                continue;
            
            Vector3 playervec = getRelativeViewVector(rendersegment.startpoint.point).negate();            
            
            double width = 0.025F * (playervec.mag() / 5+1) * (1+rendersegment.light)*0.5F;
            
            Vector3 diff1 = playervec.copy().crossProduct(rendersegment.prevdiff).normalize().multiply(width / rendersegment.sinprev);
            Vector3 diff2 = playervec.copy().crossProduct(rendersegment.nextdiff).normalize().multiply(width / rendersegment.sinnext);
            
            Vector3 startvec = rendersegment.startpoint.point;
            Vector3 endvec = rendersegment.endpoint.point;
            
            t.setColorRGBA_F(1, 1, 1, mainalpha * rendersegment.light);
            
            t.addVertexWithUV(endvec.x - diff2.x, endvec.y - diff2.y, endvec.z - diff2.z, 0.5, 0);
            t.addVertexWithUV(startvec.x - diff1.x, startvec.y - diff1.y, startvec.z - diff1.z, 0.5, 0);
            t.addVertexWithUV(startvec.x + diff1.x, startvec.y + diff1.y, startvec.z + diff1.z, 0.5, 1);
            t.addVertexWithUV(endvec.x + diff2.x, endvec.y + diff2.y, endvec.z + diff2.z, 0.5, 1);
            
            if(rendersegment.next == null)
            {
                Vector3 roundend = rendersegment.endpoint.point.copy().add(rendersegment.diff.copy().normalize().multiply(width));
                                
                t.addVertexWithUV(roundend.x - diff2.x, roundend.y - diff2.y, roundend.z - diff2.z, 0, 0);
                t.addVertexWithUV(endvec.x - diff2.x, endvec.y - diff2.y, endvec.z - diff2.z, 0.5, 0);
                t.addVertexWithUV(endvec.x + diff2.x, endvec.y + diff2.y, endvec.z + diff2.z, 0.5, 1);
                t.addVertexWithUV(roundend.x + diff2.x, roundend.y + diff2.y, roundend.z + diff2.z, 0, 1);
            }
            
            if(rendersegment.prev == null)
            {
                Vector3 roundend = rendersegment.startpoint.point.copy().subtract(rendersegment.diff.copy().normalize().multiply(width));
                                
                t.addVertexWithUV(startvec.x - diff1.x, startvec.y - diff1.y, startvec.z - diff1.z, 0.5, 0);
                t.addVertexWithUV(roundend.x - diff1.x, roundend.y - diff1.y, roundend.z - diff1.z, 0, 0);
                t.addVertexWithUV(roundend.x + diff1.x, roundend.y + diff1.y, roundend.z + diff1.z, 0, 1);
                t.addVertexWithUV(startvec.x + diff1.x, startvec.y + diff1.y, startvec.z + diff1.z, 0.5, 1);
            }
        }
    }
    
    static double interpPosX;
    static double interpPosY;
    static double interpPosZ;
}
