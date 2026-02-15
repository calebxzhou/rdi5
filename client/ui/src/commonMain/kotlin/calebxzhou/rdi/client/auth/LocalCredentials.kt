package calebxzhou.rdi.client.auth

import calebxzhou.rdi.client.model.LoginInfo

expect class LocalCredentials() {
    var loginInfos: MutableMap<String, LoginInfo>
    val lastLogged: LoginInfo?
    fun save()

    companion object {
        fun read(): LocalCredentials
    }
}
