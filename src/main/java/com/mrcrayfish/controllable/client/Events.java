package com.mrcrayfish.controllable.client;

import com.mrcrayfish.controllable.Buttons;
import com.mrcrayfish.controllable.Controllable;
import com.mrcrayfish.controllable.event.ControllerInputEvent;
import com.mrcrayfish.controllable.event.ControllerMoveEvent;
import com.mrcrayfish.controllable.event.ControllerTurnEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Author: MrCrayfish
 */
@SideOnly(Side.CLIENT)
public class Events
{
    private boolean sneaking = false;

    private float prevXAxis;
    private float prevYAxis;
    private int prevTargetMouseX;
    private int prevTargetMouseY;
    private int targetMouseX;
    private int targetMouseY;

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event)
    {
        this.prevTargetMouseX = targetMouseX;
        this.prevTargetMouseY = targetMouseY;

        Controller controller = Controllable.getController();
        if(controller == null)
            return;

        while(Controllers.next())
        {
            if(Controllers.isEventButton() && Controllers.getEventSource() == controller)
            {
                int button = Controllers.getEventControlIndex();
                boolean state = Controllers.getEventButtonState();
                if(!MinecraftForge.EVENT_BUS.post(new ControllerInputEvent(button, state)))
                {
                    handleMinecraftInput(button, state);
                }
            }
        }

        if(event.phase == TickEvent.Phase.END)
            return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if(player == null)
            return;

        if(mc.currentScreen == null)
        {
            if(!MinecraftForge.EVENT_BUS.post(new ControllerTurnEvent()))
            {
                /* Handles rotating the yaw of player */
                if(controller.getZAxisValue() != 0.0F || controller.getRZAxisValue() != 0.0F)
                {
                    float rotationYaw = 20.0F * (controller.getZAxisValue() > 0.0F ? 1 : -1) * Math.abs(controller.getZAxisValue());
                    float rotationPitch = 15.0F * (controller.getRZAxisValue() > 0.0F ? 1 : -1) * Math.abs(controller.getRZAxisValue());
                    player.turn(rotationYaw, -rotationPitch);
                }
            }
        }
        else
        {
            if(controller.getXAxisValue() != 0.0F || controller.getYAxisValue() != 0.0F)
            {
                if(prevXAxis == 0.0F && prevYAxis == 0.0F)
                {
                    prevTargetMouseX = targetMouseX = Mouse.getX();
                    prevTargetMouseY = targetMouseY = Mouse.getY();
                }
                targetMouseX += 20 * (controller.getXAxisValue() > 0.0F ? 1 : -1) * Math.abs(controller.getXAxisValue());
                targetMouseY += 20 * (controller.getYAxisValue() > 0.0F ? -1 : 1) * Math.abs(controller.getYAxisValue());
            }
            /*if(controller.getXAxisValue() != 0.0F)
            {
                if(prevXAxis == 0.0F)
                {
                    prevTargetMouseX = targetMouseX = Mouse.getX();
                }
                targetMouseX += 20 * (controller.getXAxisValue() > 0.0F ? 1 : -1) * Math.abs(controller.getXAxisValue());
            }
            if(controller.getYAxisValue() != 0.0F)
            {
                if(prevYAxis == 0.0F)
                {
                    System.out.println("YO");
                    prevTargetMouseY = targetMouseY = Mouse.getY();
                }
                targetMouseY += 20 * (controller.getYAxisValue() > 0.0F ? -1 : 1) * Math.abs(controller.getYAxisValue());
            }*/
            prevXAxis = controller.getXAxisValue();
            prevYAxis = controller.getYAxisValue();
        }
    }

    @SubscribeEvent
    public void onRenderScreen(RenderWorldLastEvent event)
    {
        if(Minecraft.getMinecraft().currentScreen != null && (targetMouseX != prevTargetMouseX || targetMouseY != prevTargetMouseY))
        {
            int mouseX = (int) (prevTargetMouseX + (targetMouseX - prevTargetMouseX) * event.getPartialTicks() + 0.5F);
            int mouseY = (int) (prevTargetMouseY + (targetMouseY - prevTargetMouseY) * event.getPartialTicks() + 0.5F);
            Mouse.setCursorPosition(mouseX, mouseY);
        }
    }

    @SubscribeEvent
    public void onInputUpdate(InputUpdateEvent event)
    {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if(player == null)
            return;

        Controller controller = Controllable.getController();
        if(controller == null)
            return;

        event.getMovementInput().sneak = sneaking;

        if(Minecraft.getMinecraft().currentScreen == null)
        {
            if(!MinecraftForge.EVENT_BUS.post(new ControllerMoveEvent()))
            {
                if(controller.getYAxisValue() != 0.0F)
                {
                    int dir = controller.getYAxisValue() > 0.0F ? -1 : 1;
                    event.getMovementInput().forwardKeyDown = dir > 0;
                    event.getMovementInput().backKeyDown = dir < 0;
                    event.getMovementInput().moveForward = dir * Math.abs(controller.getYAxisValue());

                    if(event.getMovementInput().sneak)
                    {
                        event.getMovementInput().moveForward *= 0.3D;
                    }
                }

                if(controller.getXAxisValue() != 0.0F)
                {
                    int dir = controller.getXAxisValue() > 0.0F ? -1 : 1;
                    event.getMovementInput().rightKeyDown = dir < 0;
                    event.getMovementInput().leftKeyDown = dir > 0;
                    event.getMovementInput().moveStrafe = dir * Math.abs(controller.getXAxisValue());

                    if(event.getMovementInput().sneak)
                    {
                        event.getMovementInput().moveStrafe *= 0.3D;
                    }
                }
            }

            if(controller.isButtonPressed(Buttons.A))
            {
                event.getMovementInput().jump = true;
            }
        }

        Minecraft mc = Minecraft.getMinecraft();
        if(controller.isButtonPressed(Buttons.LEFT_TRIGGER) && mc.rightClickDelayTimer == 0 && !mc.player.isHandActive())
        {
            mc.rightClickMouse();
        }
    }

    private void handleMinecraftInput(int button, boolean state)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if(state)
        {
            if(button == Buttons.Y)
            {
                if(mc.currentScreen == null)
                {
                    if (mc.playerController.isRidingHorse())
                    {
                        mc.player.sendHorseInventory();
                    }
                    else
                    {
                        mc.getTutorial().openInventory();
                        mc.displayGuiScreen(new GuiInventory(mc.player));
                    }
                    prevTargetMouseX = targetMouseX = Mouse.getX();
                    prevTargetMouseY = targetMouseY = Mouse.getY();
                }
                else
                {
                    mc.player.closeScreen();
                }
            }
            else if(button == Buttons.LEFT_THUMB_STICK)
            {
                if(mc.currentScreen == null)
                {
                    sneaking = !sneaking;
                }
            }
            else if(button == Buttons.LEFT_BUMPER)
            {
                if(mc.currentScreen == null)
                {
                    mc.player.inventory.changeCurrentItem(1);
                }
            }
            else if(button == Buttons.RIGHT_BUMPER)
            {
                if(mc.currentScreen == null)
                {
                    mc.player.inventory.changeCurrentItem(-1);
                }
            }
            else if(button == Buttons.A)
            {
                invokeMouseClick(mc.currentScreen, 0);
            }
            else if(button == Buttons.X)
            {
                invokeMouseClick(mc.currentScreen, 1);
            }
            else
            {
                Controller controller = Controllable.getController();
                if(controller == null)
                    return;

                if(!mc.player.isHandActive() && mc.currentScreen == null)
                {
                    if(button == Buttons.RIGHT_TRIGGER)
                    {
                        mc.clickMouse();
                    }
                    if(button == Buttons.LEFT_TRIGGER)
                    {
                        mc.rightClickMouse();
                    }
                    if(button == Buttons.X)
                    {
                        mc.middleClickMouse();
                    }
                }
            }
        }
    }

    public static boolean isLeftClicking()
    {
        Minecraft mc = Minecraft.getMinecraft();
        boolean isLeftClicking = mc.gameSettings.keyBindAttack.isKeyDown();
        Controller controller = Controllable.getController();
        if(controller != null) isLeftClicking |= controller.isButtonPressed(Buttons.RIGHT_TRIGGER);
        return mc.currentScreen == null && isLeftClicking && mc.inGameHasFocus;
    }

    /**
     * Invokes a mouse click in a GUI. This is modified version that is designed for controllers.
     * Upon clicking, mouse released is called straight away to make sure dragging doesn't happen.
     *
     * @param gui the gui instance
     * @param button the button to click with
     */
    private void invokeMouseClick(GuiScreen gui, int button)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if(gui != null)
        {
            int guiX = Mouse.getX() * gui.width / mc.displayWidth;
            int guiY = gui.height - Mouse.getY() * gui.height / mc.displayHeight - 1;

            try
            {
                long start = System.nanoTime();
                Class<?> clazz = GuiScreen.class;
                Field eventButton = clazz.getDeclaredField("eventButton");
                eventButton.setAccessible(true);
                eventButton.set(gui, button);

                Field lastMouseEvent = clazz.getDeclaredField("lastMouseEvent");
                lastMouseEvent.setAccessible(true);
                lastMouseEvent.set(gui, System.currentTimeMillis());

                Method mouseClicked = clazz.getDeclaredMethod("mouseClicked", int.class, int.class, int.class);
                mouseClicked.setAccessible(true);
                mouseClicked.invoke(gui, guiX, guiY, button);

                //Resets the mouse straight away
                eventButton.set(gui, -1);
                Method mouseReleased = clazz.getDeclaredMethod("mouseReleased", int.class, int.class, int.class);
                mouseReleased.setAccessible(true);
                mouseReleased.invoke(gui, guiX, guiY, button);

                System.out.println(System.nanoTime() - start);
            }
            catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e)
            {
                e.printStackTrace();
            }
        }
    }
}