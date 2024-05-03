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

package app.vercors.api.loader.neoforge

import app.vercors.proto.Common.VersionList
import app.vercors.proto.ProjectOuterClass.ProjectInstaller
import app.vercors.proto.versionList
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val logger = KotlinLogging.logger {}

@Singleton
class NeoforgeServiceImpl @Inject constructor(
    private val externalScope: CoroutineScope,
    private val neoforgeApi: NeoforgeApi
) : NeoforgeService {
    private var loadingJob: Job? = null
    private var data: VersionMap? = null

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
        loadingJob = externalScope.launch {
            logger.info { "Loading NeoForge Loader data..." }
            data = neoforgeApi.getAllVersions().versions
                .groupBy { it.split('.').dropLast(1).joinToString(".") }
                .mapKeys { (key, _) -> "1.$key" }
                .mapValues { (mcVersion, neoforgeVersions) ->
                    versionList {
                        latest = "$mcVersion-${neoforgeVersions.last()}"
                        neoforgeVersions.lastOrNull { !it.endsWith("-beta") }
                            ?.let { recommended = "$mcVersion-$it" }
                        versions.addAll(neoforgeVersions.reversed().map { "$mcVersion-$it" })
                    }
                }
            logger.info { "Loaded NeoForge Loader data" }
        }
    }
}

private typealias VersionMap = Map<String, VersionList>