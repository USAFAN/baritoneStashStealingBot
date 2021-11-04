package com.lambda.client.module.modules.misc

import com.lambda.client.event.SafeClientEvent
import net.minecraft.inventory.ClickType

import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.util.EnumHand
import com.lambda.client.event.events.PacketEvent
import com.lambda.client.event.events.RenderWorldEvent
import com.lambda.client.module.Category
import com.lambda.client.module.Module
import com.lambda.client.module.modules.combat.AutoCombat
import com.lambda.client.module.modules.movement.AutoWalk
import com.lambda.client.module.modules.player.ChestStealer
import com.lambda.client.module.modules.player.ViewLock
import com.lambda.client.util.BaritoneUtils
import com.lambda.client.util.color.ColorHolder
import com.lambda.client.util.graphics.ESPRenderer
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
import net.minecraft.util.math.BlockPos
import com.lambda.client.util.TickTimer
import com.lambda.client.util.TimeUnit
import com.lambda.client.util.items.*
import com.lambda.client.util.threads.runSafe
import com.sun.org.apache.xpath.internal.operations.Bool
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.util.math.AxisAlignedBB



//thx 4 the help ionar




object StashMoverBot : Module(
    name = "StashMoverBot",
    category = Category.MISC,
    description = "auto moves stash"
) {
    private var renderer = ESPRenderer()
    private val debugMessages by setting("debugMessages", true)

    private val isThePearler by setting("isThePearler", true)
    private val setKillSpot by setting("setKillSpot", false)
    private val killSpotX = setting("killSpotX", 50, -9999999..9999999, 1)
    private val killSpotY = setting("killSpotY", 50, -9999999..9999999, 1)

    private val killSpotZ = setting("killSpotZ", 50, -9999999..9999999, 1)
    private val pearlSpotXThrow = setting("pearlSpotXThrow", 50, -9999999..9999999, 1)
    private val pearlSpotYThrow = setting("pearlSpotYThrow", 50, 1..256, 1)

    private val pearlSpotZThrow = setting("pearlSpotZThrow", 50, -9999999..9999999, 1)


    private val baseX= setting("baseX", 0, -9999999..9999999, 1)
    private val baseY= setting("baseY", 100, -9999999..9999999, 1)

    private val baseZ = setting("baseZ", 0, -9999999..9999999, 1)
    private val baseXlengthFromWall = setting("baseXZlengthFromWall", 50, 1..9999, 1)
    private val baseZlengthFromWall = setting("baseZlengthFromWall", 50, 1..9999, 1)


    private val pearlThrowPitch = setting("pearlThrowPitch", 50, -50..50, 1)
    private val pearlThrowYaw = setting("pearlThrowYaw", 50, -179..179, 1)
    private val pearlThrowTickDelay = setting("pearlThrowTickDelay", 1000, 1..1000, 50)
    private val walkForLengthSeconds = setting("walkForLengthSeconds", 3, 1..100, 50)
    private var sinceMovedHotbar = TickTimer(TimeUnit.SECONDS)
    private var sinceThrownPearl = TickTimer(TimeUnit.SECONDS)


    private var sinceSentPearlMsg = TickTimer(TimeUnit.SECONDS)
    private var sinceKilledSelf = TickTimer(TimeUnit.SECONDS)
    private var sinceInRenderDistance = TickTimer(TimeUnit.SECONDS)
    private var sinceMovedOutEchest = TickTimer(TimeUnit.SECONDS)

    private var sinceStoleEchest = TickTimer(TimeUnit.SECONDS)
    var hasMovedEchest= false
    var gotoTeleport = false
    var switchTooGather = false
  
    var tempDummyAcctName = "Etbes"
  
var hasBeenPearled = false
   var gatheringPhase = true
    var hasThrowPearl = false
var readyToThrow = false
    var awaitingTeleport = false
    var startStoringChain = false
    var shouldBeAtPearlSpot= false
    var bedAtStash=true
    var hasDied = false
    var dontMineAnymore = false
    var hasDiedFirstTime = false
    var hasDiedSecondTime = false
    var deathCount = 0
    var shouldClearHotbar = false

    init {

        onEnable {
          //  MessageSendHelper.sendBaritoneCommand("#blocksToAvoidBreaking obsidian")

            if (isThePearler) {

               // MessageSendHelper.sendChatMessage("You are the pearler!")
              //  MessageSendHelper.sendChatMessage("You are the pearler!")
                BaritoneUtils.cancelEverything()
                //getPearlAndThrow()
                //  MessageSendHelper.sendBaritoneCommand("#mine minecraft:trapped_chest chest")// this line has to be off when enabling/ when clients turning on


            } else {
                MessageSendHelper.sendChatMessage("You are not the pearler!")
            }


        }
        onDisable {
            switchTooGather= false
            gatheringPhase= true
            hasBeenPearled = false
            hasThrowPearl = false
            readyToThrow = false
            awaitingTeleport = false
            shouldBeAtPearlSpot= false
            startStoringChain = false



             hasBeenPearled = false
             gatheringPhase = true
             hasThrowPearl = false
            readyToThrow = false
            awaitingTeleport = false
        gatheringPhase =true//not crucial 2 logic
            MessageSendHelper.sendBaritoneCommand("#stop")
        }

        safeListener<TickEvent.ClientTickEvent> {
/*

            never finished this other mode where the bed could be set at your own stash.
            if (!bedAtStash) {
                if (!mc.player.isEntityAlive){
                    deathCount +=1
                }
            if (!gatheringPhase&& !hasBeenPearled && awaitingTeleport&& hasDiedFirstTime){      //
                sendCoordsForBringBackPearlSpot(tempDummyAcctName)
                hasDied = true
            }

            if (!gatheringPhase&& mc.player.ticksExisted %250 == 0 && hasDiedFirstTime){
                MessageSendHelper.sendLambdaCommand(";set chestStealer MovingMode Quick_Move")

                ChestStealer.stealing = true
            }
            if (it.phase == TickEvent.Phase.START) {
                if (hasDiedFirstTime&& !gatheringPhase&& mc.player.ticksExisted % 10 == 0&&!hasDied){
                    MessageSendHelper.sendBaritoneCommand("#goto ender_chest")
                    hasDiedSecondTime = true



                }
                if (hasDied && !isInRenderDistance()&& hasDiedSecondTime) {
                    sendCoordsForBringBackPearlSpot(tempDummyAcctName)

                }

                    if (sinceKilledSelf.tick(3) && !hasBeenPearled && awaitingTeleport) {

                        sendCoordsForBringBackPearlSpot(tempDummyAcctName)
                        if (isInRenderDistance()){
                            hasBeenPearled = true
                        }
                    }

                    if (hasBeenPearled) {
                        gatheringPhase = true
                    }
                    if (gatheringPhase && mc.player.ticksExisted % 50 == 0) {


                        //  if (debugMessages) MessageSendHelper.sendChatMessage("gathering fase ")

                        if (isInBaseCoords()) {
                            // if (debugMessages) MessageSendHelper.sendChatMessage("in base cords")

                            if (!isFullEnoughToMove() && sinceMovedOutEchest.tick(30)) {
                                if (areShulkersOnGround()) {

                                    gatheringPhase = true
                                    gotoShulkersonGround()
                                    MessageSendHelper.sendChatMessage("goto shulkes")
                                } else {

                                    if (debugMessages) MessageSendHelper.sendChatMessage("Wasnt full gonna mine chest")

                                    gatheringPhase = true
                                    if (!dontMineAnymore) MessageSendHelper.sendBaritoneCommand("#mine minecraft:trapped_chest chest")// this line has to be off when enabling/ when clients turning on
                                }
                            } else {
                                if (!awaitingTeleport) {
                                    MessageSendHelper.sendChatMessage("should b goto enderchest")
                                    MessageSendHelper.sendLambdaCommand(";set chestStealer MovingMode Quick_Move")
                                    ChestStealer.stealing = false
                                    ChestStealer.storing = true
                                    MessageSendHelper.sendBaritoneCommand("#goto ender_chest")
                                }
                                if (mc.player.openContainer != null && !dontMineAnymore) {
                                    MessageSendHelper.sendChatMessage("in container")

                                    sinceStoleEchest.reset()

                                    dontMineAnymore = true

                                }
                                if (sinceStoleEchest.tick(5)) {
                                    MessageSendHelper.sendChatMessage("echest full enough 2 pearl")
                                    sinceMovedOutEchest.reset()

                                    hasMovedEchest = true
                                    dontMineAnymore = true

                                }
                                if (sinceMovedOutEchest.tick(3) && hasMovedEchest) {
                                    hasMovedEchest = false
                                    awaitingTeleport = true
                                    gatheringPhase = false
                                    startStoringChain = true
                                    MessageSendHelper.sendChatMessage("full enough 2 pearl")

                                    MessageSendHelper.sendBaritoneCommand("#stop")
                                }


                            }//tempDummyAcctName


                        } else {
                            // pearl away

                            MessageSendHelper.sendBaritoneCommand("#stop")

                            if (debugMessages) MessageSendHelper.sendChatMessage("wasnt in base cords")
                            sendToCenterOfBase()
                        }
                    }
                    if (!gatheringPhase) {
                        if (startStoringChain) {
                            startStoringChain = false
                           // MessageSendHelper.sendBaritoneCommand("#goto " + pearlSpotXThrow.value.toString() + " " + pearlSpotYThrow.value.toString() + " " + pearlSpotZThrow.value.toString())
                            shouldBeAtPearlSpot = true
                            clearSpotForPearl()
                        }
                        if (shouldBeAtPearlSpot && ToSpotYet(pearlSpotXThrow.toString(), pearlSpotZThrow.toString())) {
                            shouldBeAtPearlSpot = false
                            MessageSendHelper.sendBaritoneCommand("#stop")
                            readyToThrow = true
                            fixPitchForPearl()
                        } else {
                            if (shouldBeAtPearlSpot)  MessageSendHelper.sendBaritoneCommand("#goto " + pearlSpotXThrow.value.toString() + " " + pearlSpotYThrow.value.toString() + " " + pearlSpotZThrow.value.toString())


                        }
                        if (!isInRenderDistance() && readyToThrow && mc.player.ticksExisted % 1000 == 0) {//longer time based on how far ur other acct gona have 2 move too load pearl
                            getPearlAndThrow()
                            readyToThrow = false
                            hasThrowPearl = true

                        }
                        if (hasThrowPearl && mc.player.ticksExisted % 100 == 0) {
                            hasThrowPearl = false
                            if (!AutoWalk.isEnabled) AutoWalk.enable()

                            //MessageSendHelper.sendBaritoneCommand("#goto "+ killSpotX.value.toString()+" "+ killSpotZ.value.toString())
                            sinceKilledSelf.reset()
                            switchTooGather = true
                        }
                        if (sinceKilledSelf.tick(1)) {
                            if (AutoWalk.isEnabled) {
                                AutoWalk.disable()
                                hasDiedFirstTime = true

                            }
                        }
                    }

                }
            }
*/
            if (bedAtStash) {
                if (!mc.player.isEntityAlive) {
                    if (!gatheringPhase)
                        gatheringPhase = true
                }
                if (it.phase == TickEvent.Phase.START) {
                //    if (shouldClearHotbar) firstSlotToOffHand()
                    if (shouldClearHotbar&& mc.player.offhandSlot.stack.item == Items.AIR){//mc.player.offhandSlot.stack.item == Items.AIR    mc.player.inventory.getCurrentItem().item == Items.AIR || mc.player.inventory.getCurrentItem().item == Items.ENDER_PEARL
                        firstSlotToOffHand()
                        shouldClearHotbar = false
                        sinceMovedHotbar.reset()
                        hasBeenPearled = true
                    }
                    //isInRenderDistance(tempDummyAcctName)
                    //isInBaseCoords()
                    areShulkersOnGround()
                    // if (mc.player.ticksExisted % 50 == 0) MessageSendHelper.sendChatMessage(gatheringPhase.toString() + " gathering fase")
                    if (isInRenderDistance() && sinceInRenderDistance.tick(5)) {
                        sinceInRenderDistance.reset()
                        awaitingTeleport = false
                        startStoringChain = true
                    }
                    if (awaitingTeleport && sinceSentPearlMsg.tick(25)) {

                        if (!isInRenderDistance()) {
                            sendCoordsForBringBackPearlSpot(tempDummyAcctName)
                            mc.player.inventory.currentItem = mc.player.hotbarSlots[0].hotbarSlot
                        }
                        sinceSentPearlMsg.reset()

                    }
                    if (gatheringPhase && mc.player.ticksExisted % 50 == 0) {
                        hasBeenPearled= false
                        shouldClearHotbar = false
                        if (debugMessages) MessageSendHelper.sendChatMessage("gathering fase ")

                        if (isInBaseCoords()) {
                            if (debugMessages) MessageSendHelper.sendChatMessage("in base cords")

                            if (!isFullEnoughToMove()) {
                                if (areShulkersOnGround()) {

                                    gatheringPhase = true
                                    gotoShulkersonGround()
                                    MessageSendHelper.sendChatMessage("goto shulkes")
                                } else {

                                    if (debugMessages) MessageSendHelper.sendChatMessage("Wasnt full gonna mine chest")

                                    gatheringPhase = true
                                    MessageSendHelper.sendBaritoneCommand("#mine minecraft:trapped_chest chest ")// this line has to be off when enabling/ when clients turning on
                                }
                            } else {

                                awaitingTeleport = true
                                gatheringPhase = false
                                MessageSendHelper.sendChatMessage("full enough 2 pearl")

                                MessageSendHelper.sendBaritoneCommand("#stop")
                            }//tempDummyAcctName


                        } else {
                            // pearl away

                            MessageSendHelper.sendBaritoneCommand("#stop")

                            if (debugMessages) MessageSendHelper.sendChatMessage("wasnt in base cords")
                            sendToCenterOfBase()
                        }
                    }//firstSlotToOffHand
                    if (!gatheringPhase) {
                        if (hasBeenPearled && sinceMovedHotbar.tick(3)) {
                            shouldClearHotbar = false
                            hasBeenPearled= false
                            startStoringChain = false
                           // mc.player.inventory.currentItem = mc.player.hotbarSlots[1].hotbarSlot

                            MessageSendHelper.sendBaritoneCommand("#goto " + pearlSpotXThrow.value.toString() + " " + pearlSpotYThrow.value.toString() + " " + pearlSpotZThrow.value.toString())
                            shouldBeAtPearlSpot = true
                           // clearSpotForPearl()
                        }
                        if (shouldBeAtPearlSpot && ToSpotYet(pearlSpotXThrow.toString(), pearlSpotZThrow.toString())&& ToSpotYetY(pearlSpotYThrow.toString())) {
                            shouldBeAtPearlSpot = false
                            MessageSendHelper.sendBaritoneCommand("#stop")
                            readyToThrow = true
                            fixPitchForPearl()
                            mc.renderGlobal.loadRenderers()


                        }else{
                            //MessageSendHelper.sendChatMessage("elsed at pearl spot" + shouldBeAtPearlSpot.toString()+" should b at pearl spot")
                            if (shouldBeAtPearlSpot){
                            //  if (mc.player.ticksExisted%100==0)  firstSlotToOffHand()
                                MessageSendHelper.sendBaritoneCommand("#goto " + pearlSpotXThrow.value.toString() + " " + pearlSpotYThrow.value.toString() + " " + pearlSpotZThrow.value.toString())
                            }

                        }
                        if (isInRenderDistance() && readyToThrow && mc.player.ticksExisted % pearlThrowTickDelay.value == 0) {//longer time based on how far ur other acct gona have 2 move too load pearl
                     //       mc.renderGlobal.loadRenderers()

                            getPearlAndThrow()
                            readyToThrow = false
                            hasThrowPearl = true
                            sinceThrownPearl.reset()

                        }
                        if (hasThrowPearl && sinceThrownPearl.tick(5)) {
                            hasThrowPearl = false
                            if (!AutoWalk.isEnabled) AutoWalk.enable()

                            //MessageSendHelper.sendBaritoneCommand("#goto "+ killSpotX.value.toString()+" "+ killSpotZ.value.toString())
                            sinceKilledSelf.reset()
                            switchTooGather = true
                        }
                        if (sinceKilledSelf.tick(walkForLengthSeconds.value)) {
                            if (AutoWalk.isEnabled) {
                                AutoWalk.disable()
                                if (ViewLock.isEnabled)ViewLock.disable()


                            }
                        }
                    }

                    if (switchTooGather && sinceKilledSelf.tick(10)) {
                       // shou
                        MessageSendHelper.sendChatMessage("should switch 2 gathering now")
                        switchTooGather = false

                        hasBeenPearled = false
                        gatheringPhase = true
                        hasThrowPearl = false
                        readyToThrow = false
                        awaitingTeleport = false
                        startStoringChain = false
                        shouldBeAtPearlSpot = false

                        switchTooGather = false
                        gatheringPhase = true
                        hasBeenPearled = false
                        hasThrowPearl = false
                        readyToThrow = false
                        shouldClearHotbar = false
                    }
                    //if (gatheringPhase && !isInBaseCoords()) {
                    // continue
                    //MessageSendHelper.sendBaritoneCommand("#stop")

                    // }
                }
            }
        }
        listener<PacketEvent.Receive> {


            if (it.packet !is SPacketChat) return@listener
            val message = it.packet.chatComponent.unformattedText
            if (MessageDetection.Direct.RECEIVE detect message) {
                if (message.contains("stashMoverBot")) {
                    if (debugMessages) MessageSendHelper.sendChatMessage("run sum now)")
                    if (message.contains("pearled")) {
                        MessageSendHelper.sendChatMessage("got pearled msg")
                        gatheringPhase=false
                        hasBeenPearled = false
                        awaitingTeleport= false
                        hasThrowPearl = false
                        shouldClearHotbar = true
                        mc.player.inventory.currentItem = mc.player.hotbarSlots[0].hotbarSlot
                       // firstSlotToOffHand()
                    }
                }

            }
        }

        safeListener<RenderWorldEvent> {
            //highlight base corner
            renderer.aFilled = 25
            renderer.aOutline = 255

            renderer.through = true
            renderer.thickness = 1f
            renderer.fullOutline = true

            for (i in 1..255) {


                var baseCorner = BlockPos(baseX.value, i, baseZ.value)
                //var newb = AxisAlignedBB(airBlockOne)

                renderer.add(AxisAlignedBB(baseCorner), ColorHolder(205, 0, 209, 255))

                var baseCorner2 = BlockPos(baseX.value+ baseXlengthFromWall.value, i, baseZ.value)
                //var newb = AxisAlignedBB(airBlockOne)

                renderer.add(AxisAlignedBB(baseCorner2), ColorHolder(205, 208, 209, 255))


                var baseCorner3 = BlockPos(baseX.value, i, baseZ.value+ baseZlengthFromWall.value)
                //var newb = AxisAlignedBB(airBlockOne)

                renderer.add(AxisAlignedBB(baseCorner3), ColorHolder(205, 208, 209, 255))


                var baseCorner4 = BlockPos(baseX.value+ baseXlengthFromWall.value, i, baseZ.value+ baseZlengthFromWall.value)
                //var newb = AxisAlignedBB(airBlockOne)

                renderer.add(AxisAlignedBB(baseCorner4), ColorHolder(205, 208, 209, 255))

                var centerBase = BlockPos(baseX.value + (baseXlengthFromWall.value / 2), i, baseZ.value + (baseZlengthFromWall.value / 2))
                //var newb = AxisAlignedBB(airBlockOne)

                renderer.add(AxisAlignedBB(centerBase), ColorHolder(255, 22, 22, 255))

            }

            renderer.render(true)



        }
    }
    fun firstSlotToOffHand() {
        mc.player.inventory.currentItem = mc.player.hotbarSlots[0].hotbarSlot

        if (mc.player.offhandSlot.stack.item == Items.AIR && mc.player.inventory.getCurrentItem().item != Items.ENDER_PEARL){

            mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.SWAP_HELD_ITEMS, mc.player.position, mc.player.adjustedHorizontalFacing))
        }
    }

    fun enderChestFull():Boolean{
        MessageSendHelper.sendBaritoneCommand("#goto ender_chest")
        ChestStealer.stealing=true

        return false
    }
    fun clearSpotForPearl(){
        var droppedItem = false
        runSafe {

            if (mc.player.inventory.getCurrentItem().item != Items.ENDER_PEARL){
                ;
            }else{
                var slotWithEnderPearl = mc.player.hotbarSlots.firstItem(Items.ENDER_PEARL)

                if(slotWithEnderPearl!=null){
                    mc.player.inventory.currentItem = slotWithEnderPearl.slotIndex
                }else{
                    mc.player.dropItem(true)


                    }
                }
            }


    }
    fun ToSpotYetY(y:String):Boolean {
        var isInY = false

        return mc.player.posY - 1 < y.toInt() && y.toInt() < mc.player.posY + 1

    }
    fun ToSpotYet(x:String,z:String):Boolean {
        var isInX = false
        var isInZ = false

        if (mc.player.posX - 1 < x.toInt() && x.toInt() < mc.player.posX + 1) {
            isInX = true
        }
        if (mc.player.posZ - 1 < z.toInt() && z.toInt() < mc.player.posZ + 1) {
            isInZ = true
        }
        if (isInX && isInZ) {
            return true
        } else {
            return false
        }
    }
    fun isInRenderDistance(): Boolean {

        runSafe {
            var list = LinkedHashSet(world.loadedEntityList)
            for (potentialShulker in list) {
                //if (potentialShulker == player)
                //MessageSendHelper.sendChatMessage(z.displayName.unformattedText)

                var entName = potentialShulker.displayName.unformattedText.toString()
                if (tempDummyAcctName in entName || tempDummyAcctName in entName) {
                   // if (debugMessages)   MessageSendHelper.sendChatMessage("other player in render distance")
                        return true
                        ;
                    break
                }
            }

        }
        return false


    }
    fun gotoShulkersonGround(){
        runSafe {
            var list = LinkedHashSet(world.loadedEntityList)
            for (potentialShulker in list) {
                //MessageSendHelper.sendChatMessage(z.displayName.unformattedText)

                var entName = potentialShulker.displayName.unformattedText.toString()
                if ("shulker" in entName || "Shulker" in entName) {
                    if (debugMessages)   MessageSendHelper.sendChatMessage("Shulkerks on ground we going 2 them")
                    var shulkerX = potentialShulker.posX.toString()
                    var shulkerY = potentialShulker.posY.toString()
                    var shulkerZ = potentialShulker.posZ.toString()

                    MessageSendHelper.sendBaritoneCommand("#goto "+shulkerX+' '+shulkerY+' '+shulkerZ)

                    break
                }
            }

        }

    }
    fun areShulkersOnGround():Boolean {
        runSafe {
            var l = LinkedHashSet(world.loadedEntityList)
            for (z in l) {
                //MessageSendHelper.sendChatMessage(z.displayName.unformattedText)

                var entName = z.displayName.unformattedText.toString()
                if ("shulker" in entName || "Shulker" in entName) {
                   // if (debugMessages)   MessageSendHelper.sendChatMessage("Shulkerks on ground")
                    return true
                }
            }

        }
        return false
    }
        fun sendToCenterOfBase() {
            var xToGoto = (baseX.value + (baseXlengthFromWall.value / 2)).toString()
            var zToGoto = (baseZ.value + (baseZlengthFromWall.value / 2)).toString()

            MessageSendHelper.sendBaritoneCommand("#goto " + xToGoto +" "+ zToGoto)
        }

        fun isInBaseCoords(): Boolean {
            var isInBaseX = false
            var isInBaseZ = false
            if (baseX.value < mc.player.posX && mc.player.posX < baseX.value + baseXlengthFromWall.value) {
                isInBaseX = true
             //   MessageSendHelper.sendChatMessage("is in x")
            }

            if (baseZ.value < mc.player.posZ && mc.player.posZ < baseZ.value + baseZlengthFromWall.value) {
                isInBaseZ = true
                //MessageSendHelper.sendChatMessage("is in z")
            }


            if (isInBaseX && isInBaseZ) {
                return true
            } else {
                return false
            }
            // return false
        }
        fun fixPitchForPearl(){
           if (!ViewLock.isEnabled)ViewLock.enable()

        }
        fun getPearlAndThrow() {



            val slotWithPearlHotbar = Companion.mc.player.hotbarSlots.firstItem(Items.ENDER_PEARL)
            if (slotWithPearlHotbar != null) {
                mc.player.inventory.currentItem = slotWithPearlHotbar.slotIndex

            }
            mc.playerController.processRightClick(mc.player, mc.world, EnumHand.MAIN_HAND)
            //connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
        }

    fun isFullEnoughToMoveEchest(): Boolean {
        val container = mc.player.openContainer.inventory
        var containerSize = mc.player.openContainer.inventorySlots.size// - 36
        MessageSendHelper.sendChatMessage(containerSize.toString()+" container size")
        for  (i in 0 ..27) {
            print(i)
            var slots = mc.player.openContainer.inventorySlots.get(i).stack.item
         //  var slots = mc.player.inventory.mainInventory.get(i).item
            if (slots == Items.AIR) {
                if (debugMessages)    MessageSendHelper.sendChatMessage("got air")
                return false

            }
        }

        return true
    }

        fun isFullEnoughToMove(): Boolean {

            for (i in 9..35) {
                print(i)
                var slots = mc.player.inventory.mainInventory.get(i).item
                if (slots == Items.AIR) {
                    if (debugMessages)    MessageSendHelper.sendChatMessage("got air")
                    return false
                }
            }

            return true
        }

        fun getRandomString(length: Int): String {
            val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
            return (1..length)
                .map { allowedChars.random() }
                .joinToString("")
        }

        fun sendCoordsForBringBackPearlSpot(moverName: String) {
            val array = ByteArray(7) // length is bounded by 7

            Random.nextBytes(array)
            val generatedString = String(array, Charset.forName("UTF-8"))

            mc.player.sendChatMessage("/msg " + moverName +" bringBack stashMoverBot")

        }
    }



