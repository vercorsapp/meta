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

package app.vercors.api.loader

import app.vercors.api.loader.fabric.FabricService
import app.vercors.api.loader.forge.ForgeService
import app.vercors.api.loader.neoforge.NeoforgeService
import app.vercors.api.loader.quilt.QuiltService
import app.vercors.proto.Loader.LoaderType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

@Singleton
class LoaderServiceImpl @Inject constructor(
    externalScope: CoroutineScope,
    private val fabricService: FabricService,
    private val forgeService: ForgeService,
    private val neoforgeService: NeoforgeService,
    private val quiltService: QuiltService
) : LoaderService {
    init {
        externalScope.launch {
            while (isActive) {
                logger.info { "Loading all loader data" }
                listOf(fabricService, forgeService, neoforgeService, quiltService).forEach { it.load() }
                delay(5.minutes)
            }
        }
    }

    override suspend fun getLoaderVersionsForGameVersion(loaderType: LoaderType, gameVersion: String) =
        getLoader(loaderType)?.getLoaderVersionsForGameVersion(gameVersion)

    /*suspend fun getLoaderVersion(loaderType: LoaderType, gameVersion: String, loaderVersion: String) =
        getLoader(loaderType)?.getLoaderVersion(gameVersion, loaderVersion)*/

    private fun getLoader(loaderType: LoaderType): LoaderServiceBase? = when (loaderType) {
        LoaderType.fabric -> fabricService
        LoaderType.forge -> forgeService
        LoaderType.neoforge -> neoforgeService
        LoaderType.quilt -> quiltService
        else -> null
    }
}
