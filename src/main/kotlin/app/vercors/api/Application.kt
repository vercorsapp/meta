/*
 * Copyright (c) 2024 skyecodes
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package app.vercors.api

import app.vercors.api.plugins.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import java.util.*

private class VercorsApi

private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "Hello world!" }
    val externalScope = CoroutineScope(Dispatchers.Default) + SupervisorJob()
    val appProperties = Properties().apply { load(VercorsApi::class.java.getResourceAsStream("/api.properties")) }
    logger.info { "Loaded API properties" }
    val appComponent = DaggerAppComponent.builder()
        .externalScope(externalScope)
        .modrinthApiKey(appProperties.getProperty("modrinthApiKey"))
        .curseforgeApiKey(appProperties.getProperty("curseforgeApiKey"))
        .build()
    logger.info { "Starting server" }
    val app = embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = { module(appComponent) })
    app.addShutdownHook {
        externalScope.cancel()
        logger.info { "Goodbye!" }
    }
    app.start(wait = true)
}

fun Application.module(appComponent: AppComponent) {
    configureSecurity()
    configureSerialization()
    configureMonitoring()
    configureCache()
    configureRouting(appComponent)
}
