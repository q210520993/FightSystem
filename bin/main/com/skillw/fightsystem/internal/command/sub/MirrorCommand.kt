package com.skillw.fightsystem.internal.command.sub

import com.skillw.fightsystem.internal.command.FSCommand.soundSuccess
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.subCommand
import taboolib.common5.Mirror
import taboolib.module.lang.sendLang

object MirrorCommand {
    val report = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.soundSuccess()
            Mirror.report(sender)
        }
    }

    val clear = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.soundSuccess()
            sender.sendLang("command-clear")
            Mirror.mirrorData.clear()
        }
    }
}