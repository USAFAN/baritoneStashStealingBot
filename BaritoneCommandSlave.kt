package com.lambda.client.module.modules.misc



import com.lambda.client.event.SafeClientEvent
import com.lambda.client.event.events.PacketEvent
import com.lambda.client.module.Category
import com.lambda.client.module.Module
import com.lambda.client.util.BaritoneUtils
import com.lambda.client.util.items.allSlots
import com.lambda.client.util.text.MessageDetection
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.util.threads.safeListener
import com.lambda.event.listener.listener
import net.minecraft.network.play.server.SPacketChat
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.nio.charset.Charset
import kotlin.random.Random
import net.minecraft.init.Items

import com.lambda.client.util.items.*

object BaritoneCommandSlave : Module(
    name = "BaritoneCommandSlave",
    category = Category.MISC,
    description = "moves to incoming baritone coords for stashmoverbot"
) {
    private val pearlSpotXbringBack = setting("pearlSpotXbringBack", 60, -9999999..9999999, 1)
    private val pearlSpotZbringBack = setting("pearlSpotZbringBack", 60, -9999999..9999999, 1)
    private val afkX = setting("afkX", 50, -9999999..9999999, 1)
    private val afkZ = setting("afkZ", 50, -9999999..9999999, 1)
    var moving = false
    var returning = false
    var tempDummySlave = "Hoodlands"
    init {

        safeListener<TickEvent.ClientTickEvent> {
            if ( it.phase == TickEvent.Phase.START) {
                if (ToSpotYet(pearlSpotXbringBack.toString(), pearlSpotZbringBack.toString()) && moving && mc.player.ticksExisted % 50 == 0) {
                    moving = false
                    returning = true
                    mc.player.sendChatMessage("/msg "+ tempDummySlave+" pearled "+"stashMoverBot")
                 //   mc.renderGlobal.loadRenderers()

                    MessageSendHelper.sendBaritoneCommand("#goto " + afkX.value.toString() + " " + afkZ.value.toString())

                }
                if (ToSpotYet(afkX.toString(), afkZ.toString()) && returning && it.phase == TickEvent.Phase.START) {
                    moving = false
                    returning = false
                    MessageSendHelper.sendBaritoneCommand("#stop")
                    MessageSendHelper.sendChatMessage("awaiting to be moved again1")
                    mc.renderGlobal.loadRenderers()

                }
                if (returning && mc.player.ticksExisted % 50 == 0) {
                    mc.renderGlobal.loadRenderers()

                    MessageSendHelper.sendBaritoneCommand("#goto " + afkX.value.toString() + " " + afkZ.value.toString())

                }
                if (moving && mc.player.ticksExisted % 50 == 0) {
                  //  mc.renderGlobal.loadRenderers()

                    MessageSendHelper.sendBaritoneCommand("#goto " + pearlSpotXbringBack.value.toString() + " " + pearlSpotZbringBack.value.toString())


                }
            }
        }

        listener<PacketEvent.Receive> {
            if (it.packet !is SPacketChat) return@listener
            val message = it.packet.chatComponent.unformattedText
            if (MessageDetection.Direct.RECEIVE detect message) {
                if (message.contains("stashMoverBot")) {
                    if (message.contains("bringBack")) {
                        MessageSendHelper.sendChatMessage("run sum now)")
                        //  if (message.contains("x") &&message.contains("z")){
                     //   var frontOfX = message.indexOf("x")
                     //   var frontOfZ = message.indexOf("z")
                       // var frontOfMoveTo = message.indexOf("moveto")

                       // var xCoord = message.substring(frontOfX + 2, frontOfZ)
                      //  var zCoord = message.substring(frontOfZ + 2, message.length)
//

                    //    var coordString = "x" + xCoord + "z" + zCoord
                        //MessageSendHelper.sendBaritoneCommand("#goto " + xCoord.toString() + " " + zCoord.toString())// this line has to be off when enabling/ when clients turning on
                       // print(coordString)

                            moving = true



                        }
                        //   }
                    }
                    if(message.contains("bringMeBack")){
                        print("move to bring abck pealer")//probably actually wont need this cuz of the moveto method
                    }
                    if(message.contains("stopall")){
                        MessageSendHelper.sendBaritoneCommand("#stop")
                    }
                }

            }
        }


    fun ToSpotYet(x:String,z:String):Boolean{
        var isInX = false
        var isInZ = false

        if (mc.player.posX-1<x.toInt()&&x.toInt()<mc.player.posX+1){
            isInX= true
        }
        if (mc.player.posZ-1<z.toInt()&&z.toInt()<mc.player.posZ+1){
            isInZ= true
        }
        if (isInX&& isInZ){
            return true
        }else{
            return false
        }


            }
}
