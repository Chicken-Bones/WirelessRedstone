package codechicken.wirelessredstone.logic;

import java.util.Arrays;
import java.util.Map;

import codechicken.lib.lighting.PlanarLightModel;
import codechicken.lib.render.*;
import codechicken.lib.render.uv.MultiIconTransformation;
import org.lwjgl.opengl.GL11;

import codechicken.lib.math.MathHelper;
import codechicken.lib.colour.ColourRGBA;
import codechicken.lib.lighting.LightModel;
import codechicken.lib.lighting.LightModel.Light;
import codechicken.lib.vec.Scale;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import static codechicken.lib.vec.Vector3.*;
import static codechicken.lib.vec.Rotation.*;

public class RenderWireless
{
    private static MultiIconTransformation model_icont;
    private static MultiIconTransformation base_icont[] = new MultiIconTransformation[2];
    private static CCModel[][] models = new CCModel[3][24];

    private static LightModel lm = new LightModel()
            .setAmbient(new Vector3(0.7, 0.7, 0.7))
            .addLight(new Light(new Vector3(0.2, 1, -0.7))
                    .setDiffuse(new Vector3(0.3, 0.3, 0.3)))
            .addLight(new Light(new Vector3(-0.2, 1, 0.7))
                    .setDiffuse(new Vector3(0.3, 0.3, 0.3)))
            .addLight(new Light(new Vector3(0.7, -1, -0.2))
                    .setDiffuse(new Vector3(0.2, 0.2, 0.2)))
            .addLight(new Light(new Vector3(-0.7, -1, 0.2))
                    .setDiffuse(new Vector3(0.2, 0.2, 0.2)));
    private static PlanarLightModel rlm = lm.reducePlanar();

    static {
        Map<String, CCModel> modelMap = CCModel.parseObjModels(
                new ResourceLocation("wrcbe_logic", "models/models.obj"), 7, null);
        CCModel tstand = setTex(modelMap.get("TStand"), 2);
        CCModel jstand = setTex(tstand.copy(), 1);
        CCModel rstand = setTex(modelMap.get("RStand"), 2);
        CCModel rdish = modelMap.get("RDish");

        models[0][0] = tstand;
        models[1][0] = CCModel.combine(Arrays.asList(rstand, rdish));
        models[2][0] = jstand;

        for (int i = 0; i < 3; i++)
            models[i][0].computeNormals();

        for (int j = 1; j < 24; j++) {
            Transformation t = sideOrientation(j >> 2, j & 3).at(center);
            for (int i = 0; i < models.length; i++)
                models[i][j] = models[i][0].copy().apply(t);
        }

        for (int j = 0; j < 24; j++)
            for (int i = 0; i < 3; i++)
                models[i][j].computeLighting(lm);
    }

    private static CCModel setTex(CCModel model, int index) {
        for (Vertex5 v : model.verts)
            v.uv.tex = index;

        return model;
    }

    public static void loadIcons(IIconRegister r) {
        IIcon base = r.registerIcon("wrcbe_logic:base");
        IIcon on = r.registerIcon("wrcbe_logic:on");
        IIcon off = r.registerIcon("wrcbe_logic:off");
        IIcon blaze = r.registerIcon("wrcbe_logic:blaze");
        IIcon obsidian = r.registerIcon("obsidian");

        model_icont = new MultiIconTransformation(base, blaze, obsidian);
        base_icont[0] = new MultiIconTransformation(base, off, base, base, base, base);
        base_icont[1] = new MultiIconTransformation(base, on, base, base, base, base);
    }

    public static void renderInv(WirelessPart p) {
        CCRenderState.reset();
        CCRenderState.useNormals = true;
        CCRenderState.pullLightmap();
        CCRenderState.startDrawing(7);
        CCRenderState.setPipeline(base_icont[0]);
        BlockRenderer.renderCuboid(WirelessPart.baseBounds(0), 0);
        models[p.modelId()][0].render(model_icont);
        CCRenderState.draw();

        renderPearl(zero, p);
    }

    public static void renderWorld(WirelessPart p) {
        CCRenderState.setBrightness(p.world(), p.x(), p.y(), p.z());

        Transformation t = new Translation(p.x(), p.y(), p.z());
        CCRenderState.setPipeline(p.rotationT().at(center).with(t), base_icont[p.textureSet()], rlm);
        BlockRenderer.renderCuboid(p.baseRenderBounds, p.baseRenderMask);
        models[p.modelId()][p.side() << 2 | p.rotation()].render(t, model_icont);
    }

    public static void renderFreq(Vector3 pos, TransceiverPart p) {
        GL11.glPushMatrix();

        pos.copy().add(center).translation().glApply();
        p.rotationT().glApply();

        renderFreq(p.getFreq());
        GL11.glRotatef(180, 0, 1, 0);
        renderFreq(p.getFreq());

        GL11.glPopMatrix();
    }

    private static void renderFreq(int freq) {
        float scale = 1 / 64F;

        GL11.glPushMatrix();
        GL11.glRotatef(90, 0, 1, 0);
        GL11.glRotatef(90, 1, 0, 0);
        GL11.glTranslated(0, -5 / 16D, 0.374);
        GL11.glScalef(scale, scale, scale);

        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String s = Integer.toString(freq);
        GL11.glDepthMask(false);
        font.drawString(s, -font.getStringWidth(s) / 2, 0, 0);
        GL11.glDepthMask(true);

        GL11.glPopMatrix();
    }

    public static void renderPearl(Vector3 pos, WirelessPart p) {
        GL11.glPushMatrix();

        pos.translation().glApply();
        p.rotationT().at(center).glApply();
        p.getPearlPos().translation().glApply();
        p.getPearlRotation().glApply();
        new Scale(p.getPearlScale()).glApply();
        float light = 1;
        if (p.tile() != null) {
            GL11.glRotatef((float) (p.getPearlSpin() * MathHelper.todeg), 0, 1, 0);
            light = p.getPearlLight();
        }

        GL11.glDisable(GL11.GL_LIGHTING);
        CCRenderState.reset();
        CCRenderState.changeTexture("wrcbe_core:textures/hedronmap.png");
        CCRenderState.pullLightmap();
        CCRenderState.setColour(new ColourRGBA(light, light, light, 1).rgba());
        CCRenderState.startDrawing(4);
        CCModelLibrary.icosahedron4.render();
        CCRenderState.draw();
        GL11.glEnable(GL11.GL_LIGHTING);

        GL11.glPopMatrix();
    }

    public static IIcon getBreakingIcon(int tex) {
        return base_icont[tex].icons[1];
    }
}
