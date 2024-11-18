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

package app.vercors.api.loader.forge

import app.vercors.api.loader.VersionList
import app.vercors.api.project.ProjectInstaller
import app.vercors.api.loader.versionList
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class ForgeServiceImpl(
    private val forgeApi: ForgeApi,
    private val externalScope: CoroutineScope
) : ForgeService {
    private var data: VersionMap? = null
    private var loadingJob: Job? = null

    override suspend fun getLoaderVersionsForGameVersion(gameVersion: String): VersionList? {
        if (data == null) {
            if (loadingJob == null) load()
            loadingJob!!.join()
        }
        return data!![gameVersion]
    }

    override suspend fun getInstaller(): ProjectInstaller? {
        TODO("Not yet implemented")
    }

    override fun load() {
        loadingJob?.cancel()
        loadingJob = externalScope.launch {
            logger.info { "Loading Forge Loader data..." }
            val metadata = async { forgeApi.getMavenMetadata() }
            val promotions = async { forgeApi.getPromotions().promos }
            data = metadata.await().versioning.versions
                .map { it.split('-') }
                .filter { it.size == 2 }
                .groupBy({ it[0] }) { it[1] }
                .mapValues { (mcVersion, forgeVersions) ->
                    versionList {
                        latest = "$mcVersion-${forgeVersions.first()}"
                        promotions.await()["$mcVersion-recommended"]?.let { recommended = "$mcVersion-$it" }
                        versions.addAll(forgeVersions.map { "$mcVersion-$it" })
                    }
                }
            logger.info { "Loaded Forge Loader data" }
        }
    }
}

private typealias VersionMap = Map<String, VersionList>