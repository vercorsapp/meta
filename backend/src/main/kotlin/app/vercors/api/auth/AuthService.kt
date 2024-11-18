package app.vercors.api.auth

interface AuthService {
    fun getAuthUrl(port: String, state: String, codeChallenge: String): String
}