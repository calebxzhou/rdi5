package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.RAccount
import calebxzhou.rdi.ihq.net.uid
import io.ktor.server.application.ApplicationCall

suspend fun ApplicationCall.player(): RAccount = PlayerService.getById(uid)?: throw RequestError("用户不存在")