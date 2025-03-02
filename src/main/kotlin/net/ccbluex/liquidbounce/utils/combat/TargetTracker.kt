package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.eyesPos
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity

/**
 * A target tracker to choose the best enemy to attack
 */
class TargetTracker(defaultPriority: PriorityEnum = PriorityEnum.HEALTH) : Configurable("target"), Iterable<Entity> {

    var possibleTargets: Array<Entity> = emptyArray()
    var lockedOnTarget: Entity? = null

    val priority by enumChoice("Priority", PriorityEnum.HEALTH, PriorityEnum.values())
    val lockOnTarget by boolean("LockOnTarget", false)
    val sortOut by boolean("SortOut", true)
    val delayableSwitch by intRange("DelayableSwitch", 10..20, 0..40)

    /**
     * Update should be called to always pick the best target out of the current world context
     */
    fun update(enemyConf: EnemyConfigurable = globalEnemyConfigurable) {
        possibleTargets = emptyArray()

        val entities = (mc.world ?: return).entities
            .filter { it.shouldBeAttacked(enemyConf) }
            .sortedBy { mc.player!!.squaredBoxedDistanceTo(it) } // Sort by distance

        val eyePos = mc.player!!.eyesPos

        // default
        when (priority) {
            PriorityEnum.HEALTH -> entities.sortedBy { if (it is LivingEntity) it.health else 0f } // Sort by health
            PriorityEnum.DIRECTION -> entities.sortedBy {
                RotationManager.rotationDifference(
                    RotationManager.makeRotation(
                        it.boundingBox.center,
                        eyePos,
                    )
                )
            } // Sort by FOV
            PriorityEnum.AGE -> entities.sortedBy { -it.age } // Sort by existence
        }

        possibleTargets = entities.toTypedArray()
    }

    fun cleanup() {
        possibleTargets = emptyArray()
        lockedOnTarget = null
    }

    fun lock(entity: Entity) {
        lockedOnTarget = entity
    }

    override fun iterator() = possibleTargets.iterator()

}

enum class PriorityEnum(override val choiceName: String) : NamedChoice {
    HEALTH("Health"),
    DISTANCE("Distance"),
    DIRECTION("Direction"),
    HURT_TIME("HurtTime"),
    AGE("Age")
}
