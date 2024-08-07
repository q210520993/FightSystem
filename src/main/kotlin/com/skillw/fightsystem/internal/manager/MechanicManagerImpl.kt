package com.skillw.fightsystem.internal.manager

import com.skillw.fightsystem.api.fight.mechanic.Mechanic
import com.skillw.fightsystem.api.manager.MechanicManager
import com.skillw.fightsystem.internal.manager.FSConfig.debug
import taboolib.common.platform.function.info

object MechanicManagerImpl : MechanicManager() {
    override val key = "MechanicManager"
    override val priority: Int = 11
    override val subPouvoir = com.skillw.fightsystem.FightSystem

    override fun onEnable() {
        onReload()
    }

    override fun onReload() {
        this.entries.filter { it.value.release }.forEach { this.remove(it.key) }
    }

    override fun register(key: String, value: Mechanic): Mechanic? {
        val event = com.skillw.fightsystem.api.event.MechanicRegisterEvent(value)
        event.call()
        debug {
            info("机制注册 key: $key, value: $value")
        }
        if (event.isCancelled) return null
        return put(key, value)
    }
}
