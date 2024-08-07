package com.skillw.fightsystem.internal.core.fight

import com.skillw.asahi.api.member.context.AsahiContext
import com.skillw.fightsystem.FightSystem
import com.skillw.fightsystem.api.event.MechanicLoadEvent
import com.skillw.fightsystem.api.fight.DamageType
import com.skillw.fightsystem.api.fight.FightData
import com.skillw.fightsystem.api.fight.mechanic.Mechanic
import com.skillw.pouvoir.api.plugin.map.component.Keyable
import com.skillw.pouvoir.util.toMap
import org.bukkit.configuration.serialization.ConfigurationSerializable
import taboolib.common.platform.function.console
import taboolib.module.lang.sendLang
import kotlin.jvm.optionals.getOrNull

/**
 * Mechanic data
 *
 * @constructor Create empty Mechanic data
 * @property key 伤害类型
 * @property enable 是否启用(字符串类型，会被解析)
 */
class MechanicDataCompound private constructor(
    override val key: DamageType,
    val enable: String,
) : Keyable<DamageType>,
    ConfigurationSerializable {
    val process = ArrayList<MechanicData>()

    companion object {
        private fun loadMechanicEvent(key:String): Mechanic? {
            val event = MechanicLoadEvent(key)
            event.call()
            return event.mechanic.getOrNull()?.apply { register() }
        }

        @JvmStatic
        fun deserialize(section: org.bukkit.configuration.ConfigurationSection): MechanicDataCompound? {
            val damageType = FightSystem.damageTypeManager[section.name]
            damageType ?: kotlin.run {
                console().sendLang("invalid-damage-type", section.currentPath.toString())
                return null
            }
            val compound = MechanicDataCompound(damageType, section.getString("enable") ?: "true")
            if (section.contains("mechanics")) {
                val mechanics = section.getList("mechanics") ?: return compound
                for (context in mechanics) {
                    context as? MutableMap<String, Any>? ?: continue
                    val key = context["mechanic"].toString()
                    val machine = FightSystem.mechanicManager[key] ?: loadMechanicEvent(key)
                    if (machine == null) {
                        console().sendLang("invalid-mechanic", "${section.currentPath}.$key")
                        continue
                    }
                    compound.process += MechanicData(machine, damageType, AsahiContext.create(context))
                }
                return compound
            }
            for (key in section.getKeys(false)) {
                if (key == "enable") continue
                val machine = FightSystem.mechanicManager[key]
                if (machine == null) {
                    console().sendLang("invalid-mechanic", "${section.currentPath}.$key")
                    continue
                }
                compound.process += MechanicData(
                    machine,
                    damageType,
                    AsahiContext.create().apply {
                        putAll(section.getConfigurationSection(key)!!.toMap())
                    }
                )
            }
            return compound
        }
    }

    /**
     * Run
     *
     * @param fightData
     */
    fun run(fightData: FightData): Boolean {
        if (!fightData.handleStr(enable).toBoolean().also {
                fightData["enable"] = it
            }) return false
        for (data in process) if (!data.run(fightData)) break
        return true
    }

    override fun serialize(): MutableMap<String, Any> {
        val map = LinkedHashMap<String, Any>()
        map["enable"] = enable
        map["mechanics"] = process.map { it.serialize() }
        return map
    }


}
