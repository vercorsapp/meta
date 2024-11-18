package app.vercors.api.auth

import org.koin.core.annotation.Property
import org.koin.core.annotation.Single

@Single
class AuthServiceImpl(@Property("microsoftClientId") private val microsoftClientId: String) : AuthService {
    override fun getAuthUrl(port: String, state: String, codeChallenge: String): String =
        "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?" +
            "client_id=$microsoftClientId" +
            "&response_type=code" +
            "&redirect_uri=http%3A%2F%2Flocalhost%3A$port" +
            "&response_mode=query" +
            "&scope=XboxLive.signin%20offline_access" +
            "&state=$state" +
            "&prompt=select_account" +
            "&code_challenge=$codeChallenge" +
            "&code_challenge_method=S256"

}