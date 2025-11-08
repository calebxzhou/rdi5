package calebxzhou.rdi.service

import calebxzhou.rdi.model.Host
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.server

suspend fun RAccount.myTeamHosts() = server.makeRequest<List<Host>>("host/").data