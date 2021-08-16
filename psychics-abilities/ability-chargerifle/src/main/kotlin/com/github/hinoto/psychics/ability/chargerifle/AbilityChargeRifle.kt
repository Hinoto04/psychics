package com.github.hinoto.psychics.ability.chargerifle

import io.github.monun.psychics.AbilityConcept
import io.github.monun.psychics.AbilityType
import io.github.monun.psychics.ActiveAbility
import io.github.monun.psychics.attribute.EsperAttribute
import io.github.monun.psychics.damage.Damage
import io.github.monun.psychics.damage.DamageType
import io.github.monun.psychics.util.TargetFilter
import io.github.monun.tap.config.Config
import io.github.monun.tap.config.Name
import io.github.monun.tap.trail.TrailSupport
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.LivingEntity
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

@Name("charge-rifle")
class ChargerifleConcept : AbilityConcept() {
    @Config
    var hitRange = 1.0

    init {
        type = AbilityType.ACTIVE
        range = 200.0
        cost = 80.0
        knockback = 1.0
        damage = Damage.of(DamageType.RANGED, EsperAttribute.ATTACK_DAMAGE to 15.0)
        wand = ItemStack(Material.END_ROD)
        displayName = "차지 라이플"
        description = listOf(
            Component.text("능력 사용 시 레이저 차지를 시작합니다."),
            Component.text("차지가 끝날 시 즉시 발사되며 맞은 적에게 큰 피해를 입힙니다."),
            Component.text("레이저가 보이는 것보다 멀리 날아갑니다.")
        )
        cooldownTime = 15000L
    }
}

class AbilityChargerifle : ActiveAbility<ChargerifleConcept>() {
    override fun onCast(event: PlayerEvent, action: WandAction, target: Any?) {
        cooldownTime = concept.cooldownTime
        psychic.consumeMana(concept.cost)

        val world = event.player.world
        val player = event.player

        player.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 80, 10,
            false, false, false))
        player.addPotionEffect(PotionEffect(PotionEffectType.JUMP, 80, 200,
            false, false, false))

        world.playSound(player.location, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 1.0f)
        val task = psychic.runTaskTimer(Runnable {
            val loc = player.eyeLocation
            val hitLocation = loc.clone().add(loc.direction.clone().multiply(concept.range))

            TrailSupport.trail(loc, hitLocation, 0.4) { w, x, y, z ->
                w.spawnParticle(Particle.REDSTONE, x, y, z, 1,
                    0.0, 0.0, 0.0, 0.0, Particle.DustOptions(Color.RED, 0.2f))
            }
        }, 0L, 1L)
        psychic.runTask(Runnable {
            if(!event.player.isDead) {
                val loc = player.eyeLocation
                var hitLocation = loc.clone().add(loc.direction.clone().multiply(concept.range))

                world.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.0f)
                world.playSound(loc, Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f)
                val result = world.rayTrace(
                    loc,
                    loc.direction,
                    concept.range,
                    FluidCollisionMode.NEVER,
                    true,
                    concept.hitRange/2,
                    TargetFilter(esper.player)
                )
                if(result != null) {
                    hitLocation = result.hitPosition.toLocation(world)

                    result.hitEntity?.let { entity ->
                        if(entity is LivingEntity) {
                            entity.psychicDamage()
                        }
                    }
                }
                TrailSupport.trail(loc, hitLocation, 0.4) { w, x, y, z ->
                    w.spawnParticle(Particle.END_ROD, x, y, z, 10,
                        0.2, 0.2, 0.2, 0.0)
                }
                task.cancel()
            }
        }, 80L)
    }
}