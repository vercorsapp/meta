package app.vercors.api.plugins

import app.vercors.api.AppModule
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module
import org.koin.fileProperties
import org.koin.ksp.generated.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDI(externalScope: CoroutineScope) {
    install(Koin) {
        modules(module { single { externalScope } }, AppModule().module)
        slf4jLogger()
        fileProperties()
    }
}