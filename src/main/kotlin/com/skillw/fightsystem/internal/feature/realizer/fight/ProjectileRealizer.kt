package com.skillw.fightsystem.internal.feature.realizer.fight

import com.skillw.attsystem.AttributeSystem
import com.skillw.attsystem.api.realizer.BaseRealizer
import com.skillw.attsystem.api.realizer.component.Awakeable
import com.skillw.attsystem.api.realizer.component.Switchable
import com.skillw.fightsystem.FightSystem
import com.skillw.fightsystem.api.fight.FightData
import com.skillw.fightsystem.internal.feature.realizer.fight.AttackCooldownRealizer.CHARGE_KEY
import com.skillw.pouvoir.api.plugin.annotation.AutoRegister
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.metadata.FixedMetadataValue
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent

@AutoRegister
internal object ProjectileRealizer : BaseRealizer("projectile"), Awakeable, Switchable {

    override val file by lazy {
        FightSystem.options.file!!
    }

    override val defaultEnable: Boolean = true

    const val CACHE_KEY = "ATTRIBUTE_SYSTEM_DATA"

    @Ghost
    @SubscribeEvent
    fun projectileLaunch(event: ProjectileLaunchEvent) {
        val projectile = event.entity
        val shooter = (projectile.shooter as? LivingEntity?) ?: return
        val cacheData = FightData(shooter, null)
        projectile.setMetadata(CACHE_KEY, FixedMetadataValue(AttributeSystem.plugin, cacheData))
    }


    @Ghost
    @SubscribeEvent
    fun projectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity
        val hitEntity = (event.hitEntity as? LivingEntity?) ?: return
        val velocity = projectile.velocity.clone().subtract(hitEntity.velocity).length() / 2.0
        projectile.setMetadata(CHARGE_KEY, FixedMetadataValue(AttributeSystem.plugin, velocity))
    }

    fun Entity.projectileCache(): FightData? =
        if (hasMetadata(CACHE_KEY)) getMetadata(CACHE_KEY)[0] as? FightData? else null

    fun Entity.projectileCharged(): Double? =
        if (hasMetadata(CHARGE_KEY)) getMetadata(CHARGE_KEY)[0].asDouble() else null
}