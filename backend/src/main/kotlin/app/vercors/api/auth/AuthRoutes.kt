package app.vercors.api.auth

import app.vercors.api.queryParam
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.authRoutes() {
    val authService by inject<AuthService>()

    route("/auth") {
        get("") {
            val port = queryParam("port")
            val state = queryParam("state")
            val codeChallenge = queryParam("codeChallenge")
            call.respondRedirect(authService.getAuthUrl(port, state, codeChallenge))
        }
    }
}