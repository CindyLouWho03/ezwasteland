package com.ezrol.terry.minecraft.wastelands.gui;

import com.ezrol.terry.minecraft.wastelands.api.Param;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.resource.language.I18n;

import java.util.List;

public class WastelandParamListWidget extends EntryListWidget<WastelandParamListWidget.Entry> {

    public WastelandParamListWidget(MinecraftClient client, int int_1, int int_2, int int_3, int int_4, int int_5) {
        super(client, int_1, int_2, int_3, int_4, int_5);
    }

    private ButtonWidget nextWidget(int id, String type, int x, int y, int w, int h, Param p){
        switch (p.getType()){
            case BOOLEAN:
                return( new BooleanParamButton(id, type, x, y, w, h, (Param.BooleanParam)p));
            case INTEGER:
                return( new IntParamButton(id, type, x, y, w, h, (Param.IntegerParam)p));
            case FLOAT:
                return( new FloatParamButton(id, type, x, y, w, h, (Param.FloatParam)p));
            default:
                throw(new IllegalArgumentException("Unsupported Parameter Type"));
        }
    }
    /** Add a group of parameters with a title to the list widget
     *
     * @param startId first ID to add to the screen
     * @param title title of the seciton
     * @param sectionParams the parameters in the section to create widgets for
     * @return the last id used +1
     */
    public int addGroup(int startId, String title, List<Param> sectionParams){
        int curid = startId;
        ButtonWidget a = null;
        ButtonWidget b = null;
        int xoff = (width - getEntryWidth())/2;

        addEntry(new Entry("ezwastelands.config." + title + ".title"));

        for(Param p : sectionParams){
            if(p.getType() != Param.ParamTypes.STRING){
                if(a==null){
                    a= nextWidget(curid,title,xoff,0,(getEntryWidth()/2)-5,20,p);
                }
                else{
                    b= nextWidget(curid,title,(width/2)+5,0,(getEntryWidth()/2)-5,20,p);
                }
                curid++;
            }
            else{
                //we have a string
                //TODO implement me
            }
            if(a!= null && b!= null){
                addEntry(new Entry(a, b));
                a=null;
                b=null;
            }
        }
        if(a != null){
            addEntry(new Entry(a, null));
        }
        return curid;
    }

    /** Hoverably button **/
    private abstract class HoverableWidget extends ButtonWidget{
        long hoverStart = -1;
        int lastX, lastY;
        HoverableWidget(int id, int x, int y, int w, int h, String t){
            super(id,x,y,w,h,t);
            lastX = -1;
            lastY = -1;
        }
        @Override
        public void draw(int mouseX, int mouseY, float partialTicks) {
            super.draw(mouseX, mouseY, partialTicks);
            this.drawHover(mouseX, mouseY);
        }

        protected void drawHover(int mouseX, int mouseY) {
            boolean hovered = isHovered();

            if(hovered && (hoverStart == -1 || lastX != mouseX || lastY != mouseY)){
                hoverStart = System.currentTimeMillis();
                lastX = mouseX;
                lastY = mouseY;
            }
            else if(hovered && (System.currentTimeMillis() - hoverStart) > 1800){
                //draw hover text
                String text = getHoverText();
                int w = client.fontRenderer.getStringWidth(text);

                drawRect(x+6,y-3,x+6+w+6,y-3+13,0xFF0000CC);
                drawRect(x+7,y-2,x+7+w+4,y-2+11,0xFF000000);
                drawString(client.fontRenderer, text, x+8, y-1,0xCCFFCC);
            }
            if(!hovered){
                hoverStart = -1;
            }
        }

        abstract public String getHoverText();
    }
    /** Simple true/false toggle **/
    private class BooleanParamButton extends HoverableWidget{
        private Param.BooleanParam param;
        private final String type;

        BooleanParamButton(int id, String type, int x, int y, int width, int height, Param.BooleanParam p){
            super(id,x,y,width,height,"");
            this.type = type;
            param = p;
            setBtnText();
        }

        private void setBtnText(){
            String value = param.get() ?
                    I18n.translate("config.ezwastelands.boolean.true") :
                    I18n.translate("config.ezwastelands.boolean.false");
            String name = I18n.translate("config.ezwastelands." + type + "." + param.getName() + ".name");

            text = name + ": " + value;
        }

        @Override
        public String getHoverText(){
            return(I18n.translate(param.getComment()));
        }

        @Override
        public void onPressed(double var1, double var3) {
            boolean newval = !param.get();
            param.set(newval);
            setBtnText();
        }


    }
    /** A Integer Slider **/
    private class IntParamButton extends HoverableWidget{
        Param.IntegerParam param;
        String type;
        boolean isPressed=false;

        IntParamButton(int id, String type, int x, int y, int width, int height, Param.IntegerParam p){
            super(id,x,y,width,height,"");
            this.type = type;
            param = p;
            setBtnText();
        }

        @Override
        public String getHoverText(){
            return(I18n.translate(param.getComment()));
        }

        private float valOffset(){
            float val = param.get();
            val -= param.getMin();
            val /= (param.getMax() - param.getMin());
            return val;
        }

        /** Force button background style **/
        @Override
        protected int getTextureId(boolean boolean_1) {
            return 0;
        }

        @Override
        public void onPressed(double double_1, double double_2) {
            super.onPressed(double_1, double_2);
            isPressed=true;
        }

        @Override
        public void onReleased(double double_1, double double_2) {
            isPressed=false;
            super.onReleased(double_1, double_2);
        }

        /** Draw the custom background for the slider **/
        @Override
        protected void drawBackground(MinecraftClient mc, int mouseX, int mouseY) {
            boolean interaction = (isHovered() && enabled && isPressed);

            if(mouseX < x || mouseX > (x+width)){
                interaction = false;
                isPressed = false;
            }

            if(interaction){
                double pos = mouseX - x;
                int range = param.getMax() - param.getMin();
                int oldVal = param.get();
                int newVal;

                pos = pos / (double)width;
                if(pos>1.0){
                    pos = 1.0;
                }
                if(pos < 0.0){
                    pos = 0.0;
                }
                newVal = param.getMin() + ((int)(pos * ((double)range + 0.5)));

                if(newVal > param.getMax()){
                    newVal = param.getMax();
                }
                if(newVal != oldVal) {
                    param.set(newVal);
                    setBtnText();
                }
            }

            mc.getTextureManager().bindTexture(WIDGET_TEX);
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            drawTexturedRect(x + (int)(valOffset() * (double)(width - 8)), y, 0, 66, 4, 20);
            drawTexturedRect(x + (int)(valOffset() * (double)(width - 8)) + 4, y, 196, 66, 4, 20);
        }

        private void setBtnText(){
            String name = I18n.translate("config.ezwastelands." + type + "." + param.getName() + ".name");

            text = name + ": " + param.get();
        }
    }

    /** A Float Slider **/
    private class FloatParamButton extends HoverableWidget{
        Param.FloatParam param;
        String type;
        boolean isPressed=false;

        FloatParamButton(int id, String type, int x, int y, int width, int height, Param.FloatParam p){
            super(id,x,y,width,height,"");
            this.type = type;
            param = p;
            setBtnText();
        }

        @Override
        public String getHoverText(){
            return(I18n.translate(param.getComment()));
        }

        private float valOffset(){
            float val = param.get();
            val -= param.getMin();
            val /= (param.getMax() - param.getMin());
            return val;
        }

        /** Force button background style **/
        @Override
        protected int getTextureId(boolean boolean_1) {
            return 0;
        }

        @Override
        public void onPressed(double double_1, double double_2) {
            super.onPressed(double_1, double_2);
            isPressed=true;
        }

        @Override
        public void onReleased(double double_1, double double_2) {
            isPressed=false;
            super.onReleased(double_1, double_2);
        }

        /** Draw the custom background for the slider **/
        @Override
        protected void drawBackground(MinecraftClient mc, int mouseX, int mouseY) {
            boolean interaction = (isHovered() && enabled && isPressed);

            if(mouseX < x || mouseX > (x+width)){
                interaction = false;
                isPressed = false;
            }

            if(interaction){
                double pos = mouseX - x;
                float range = param.getMax() - param.getMin();
                float oldVal = param.get();
                float newVal;

                pos = pos / (double)(width-2);
                if(pos>1.0){
                    pos = 1.0;
                }
                if(pos < 0.0){
                    pos = 0.0;
                }
                newVal = param.getMin() + ((float)(pos * ((double)range)));

                if(newVal > param.getMax()){
                    newVal = param.getMax();
                }
                if(newVal != oldVal) {
                    param.set(newVal);
                    setBtnText();
                }
            }

            mc.getTextureManager().bindTexture(WIDGET_TEX);
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            drawTexturedRect(x + (int)(valOffset() * (double)(width - 8)), y, 0, 66, 4, 20);
            drawTexturedRect(x + (int)(valOffset() * (double)(width - 8)) + 4, y, 196, 66, 4, 20);
        }

        private void setBtnText(){
            String name = I18n.translate("config.ezwastelands." + type + "." + param.getName() + ".name");

            text = name + ": " + param.get();
        }
    }

    /** width of the entry list **/
    @Override
    public int getEntryWidth() {
        if(width < 422){
            return width - 12;
        }
        return 410;
    }

    /** Location of the scrollbar **/
    @Override
    protected int getScrollbarPosition() {
        return (width / 2) + (getEntryWidth() / 2) + 2;
    }

    public class Entry extends EntryListWidget.Entry<WastelandParamListWidget.Entry>{
        private ButtonWidget widget1=null;
        private ButtonWidget widget2=null;
        private String title=null;
        private ButtonWidget inputbox=null;

        public Entry(String title){
            //a simple title widget
            this.title = title;
        }
        public Entry(ButtonWidget a, ButtonWidget b){
            this.widget1 = a;
            this.widget2 = b;
        }

        @Override
        public void mouseMoved(double x, double y) {
            super.mouseMoved(x,y);
            if(x < width / 2 && widget1 != null){
                widget1.mouseMoved(x,y);
            }
            if(x > width / 2 && widget2 != null){
                widget2.mouseMoved(x,y);
            }
        }

        @Override
        public boolean mouseClicked(double x, double y, int i) {
            boolean r=false;
            if(x < width / 2 && widget1 != null){
                r=widget1.mouseClicked(x,y,i);
            }
            if(x > width / 2 && widget2 != null){
                r=widget2.mouseClicked(x,y,i);
            }
            if(!r){
                r=super.mouseClicked(x,y,i);
            }
            return r;
        }

        @Override
        public boolean mouseReleased(double x, double y, int i) {
            boolean r=false;
            if(x < width / 2 && widget1 != null){
                r=widget1.mouseReleased(x,y,i);
            }
            if(x > width / 2 && widget2 != null){
                r=widget2.mouseReleased(x,y,i);
            }
            if(!r){
                r=super.mouseReleased(x,y,i);
            }
            return r;
        }

        @Override
        public boolean mouseDragged(double x, double y, int i, double x2, double y2) {
            boolean r=false;
            if(x < width / 2 && widget1 != null){
                r=widget1.mouseDragged(x,y,i,x2,y2);
            }
            if(x > width / 2 && widget2 != null){
                r=widget2.mouseDragged(x,y,i,x2,y2);
            }
            if(!r){
                r=super.mouseDragged(x,y,i,x2,y2);
            }
            return r;
        }

        @Override
        public boolean keyPressed(int int_1, int int_2, int int_3) {
            return false;
        }

        @Override
        public boolean keyReleased(int int_1, int int_2, int int_3) {
            return false;
        }

        @Override
        public boolean charTyped(char char_1, int int_1) {
            return false;
        }

        @Override
        public void draw(int var1, int var2, int mouseX, int mouseY, boolean var5, float subticks) {
            int widgety = this.getY();
            if(widget1 == null && inputbox == null){
                //Full width title
                drawStringCentered(client.fontRenderer, I18n.translate(title), width /2, widgety + 4, 0xFFFFFF);
            }
            if(widget1 != null){
                //one or two widgets
                widget1.y = widgety;
                widget1.draw(mouseX, mouseY, subticks);

                if(widget2 != null){
                    widget2.y = widgety;
                    widget2.draw(mouseX,mouseY,subticks);
                }
            }
        }
    }
}
