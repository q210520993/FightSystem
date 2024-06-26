package com.skillw.fightsystem.internal.feature.compat.mythicmobs.common

import com.skillw.fightsystem.api.FightAPI
import com.skillw.fightsystem.api.fight.DataCache
import com.skillw.fightsystem.api.fight.FightData
import com.skillw.fightsystem.util.syncTaskRun
import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.adapters.SkillAdapter
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.mobs.GenericCaster
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.api.skills.damage.DamageMetadata
import io.lumine.mythic.api.skills.placeholders.PlaceholderString
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.core.logging.MythicLogger
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageEvent
import taboolib.platform.util.getMetaFirstOrNull
import taboolib.platform.util.removeMeta
import taboolib.platform.util.setMeta

/**
 * @className AttributeDamageV
 *
 * @author Glom
 * @date 2022/7/11 17:14 Copyright 2022 user. All rights reserved.
 */
internal class AttributeDamageV(private val config: MythicLineConfig) :
    ITargetedEntitySkill {

    val key: PlaceholderString =
        PlaceholderString.of(config.getString(arrayOf("key", "k"), "null"))
    val cache: PlaceholderString =
        PlaceholderString.of(config.getString(arrayOf("cache", "c"), "null"))
    val attacker: PlaceholderString =
        PlaceholderString.of(config.getString(arrayOf("attacker", "a"), "null"))
    private var ignoresArmor = config.getBoolean(arrayOf("ignorearmor", "ia", "i"), false) // 无视互加
    private var preventImmunity = config.getBoolean(arrayOf("preventimmunity", "pi"), false) // 无视 无敌帧
    private var preventKnockback = config.getBoolean(arrayOf("preventknockback", "pkb", "pk"), false) // 不击退
    private var ignoresEnchantments = config.getBoolean(arrayOf("ignoreenchantments", "ignoreenchants", "ie"), false) // 无视保护附魔
    private var noAnger = config.getBoolean(arrayOf("noAnger", "na"), false) // 无仇恨
    private var ignoreInvulnerability = config.getBoolean(arrayOf("ignoreinvulnerability", "ignoreInvul", "ii"), false) // 无视无敌
    private var ignoreShield = config.getBoolean(arrayOf("ignoreshield", "is"), false) // 无视盾牌
    private var damagesHelmet = config.getBoolean(arrayOf("damageshelmet", "dh"), false) // 是否损害头盔耐久
    private var tags = config.getString(arrayOf("tags", "tag"), null)?.toString()?.split(",")?.map { it.replace(" ","").uppercase() }// 标签
    private var element: PlaceholderString? =
        PlaceholderString.of(config.getString(arrayOf("element", "e", "damagetype", "type"), null))
    private var cause: PlaceholderString =
        PlaceholderString.of(config.getString(arrayOf("damagecause", "dc", "cause"), "ENTITY_ATTACK"))

    override fun castAtEntity(data: SkillMetadata, targetAE: AbstractEntity): SkillResult {
        val caster = data.caster.entity.bukkitEntity
        val target = targetAE.bukkitEntity
        val cacheKey = cache.get(data, targetAE)
        val attackerName = attacker.get(data, targetAE)
        val params = HashMap<String, Any>().apply {
            config.entrySet().forEach { (key, value) ->
                put(key, PlaceholderString.of(value).get(data, targetAE))
            }
        }
        val attacker =
            if (attackerName == "null") caster else Bukkit.getPlayer(attackerName) ?: return SkillResult.INVALID_TARGET
        val originCaster = data.caster
        data.caster = GenericCaster(BukkitAdapter.adapt(attacker))
        val cache = if (cacheKey == "null") null else attacker.getMetaFirstOrNull(cacheKey)
            ?.value() as? DataCache?
        if (attacker is LivingEntity && target is LivingEntity && !target.isDead) {
            val fightData = FightData(attacker, target) {
                cache?.let { cacheData ->
                    it.cache.setData(cacheData)
                }
                it["power"] = data.power.toDouble()
                it.putAll(params)
            }
            val damage = doDamage(data, targetAE, fightData)
            MythicLogger.debug(
                MythicLogger.DebugLevel.MECHANIC,
                "+ AttributeDamageMechanic fired for {0} with {1} power",
                damage, data.power
            )
        }
        data.caster = originCaster
        return SkillResult.SUCCESS
    }

    private fun doDamage(data: SkillMetadata, target: AbstractEntity?, fightData: FightData): Double {
        val caster = data.caster
        target?.bukkitEntity?.setMeta("skill-damage", true)
        val damage = FightAPI.runFight(key.get(data, target), fightData, damage = false)
        target?.bukkitEntity?.removeMeta("skill-damage")
        val meta = DamageMetadata(
            caster, damage, HashMap(),
            element?.get(data, target), 1.0,ignoresArmor, preventImmunity, preventKnockback, ignoresEnchantments,
            EntityDamageEvent.DamageCause.valueOf(cause.get(data, target))
        )
        meta.putBoolean("no_anger", noAnger)
        meta.putBoolean("ignore_shield", ignoreShield)
        meta.putBoolean("ignore_invulnerability", ignoreInvulnerability)
        meta.putBoolean("damages_helmet", damagesHelmet)
        if (element != null) {
            meta.putTag(element?.get(data, target))
        }
        tags?.forEach(meta::putTag)
        syncTaskRun {
            SkillAdapter.get().doDamage(meta, target)
        }
        return damage
    }


}
