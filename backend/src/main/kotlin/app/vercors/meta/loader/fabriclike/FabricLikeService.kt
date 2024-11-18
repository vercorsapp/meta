/*
 * Copyright (c) 2024 skyecodes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package app.vercors.meta.loader.fabriclike

import app.vercors.meta.loader.VersionList
import app.vercors.meta.loader.LoaderServiceBase
import app.vercors.meta.project.ProjectInstaller
import app.vercors.meta.project.projectInstaller
import app.vercors.meta.loader.versionList
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

abstract class FabricLikeService(
    private val name: String,
    private val api: FabricLikeApi,
    private val externalScope: CoroutineScope
) : LoaderServiceBase {
    private var loadingJob: Job? = null
    private var minecraftVersions: List<String> = emptyList()
    private var loaderVersions: VersionList? = null
    private var installerVersion: ProjectInstaller? = null
    abstract val installerArgs: List<String>

    override suspend fun getLoaderVersionsForGameVersion(gameVersion: String): VersionList? {
        if (loaderVersions == null) {
            if (loadingJob == null) load()
            loadingJob!!.join()
        }
        return if (gameVersion in minecraftVersions) loaderVersions else null
    }

    override suspend fun getInstaller(): ProjectInstaller? = installerVersion

    override fun load() {
        loadingJob = externalScope.launch {
            logger.info { "Loading $name Loader data..." }
            val (game, loader, installer) = api.getAllVersions()
            minecraftVersions = game.map { it.version }
            val latestInstaller = installer.first()
            installerVersion = projectInstaller {
                url = latestInstaller.url
                maven = latestInstaller.maven
                version = latestInstaller.version
                minimumJavaVersion = 8
                arguments.addAll(installerArgs)
            }
            loaderVersions = versionList {
                loader.firstOrNull { it.stable ?: !it.version.contains('-') }?.let { this.recommended = it.version }
                loader.firstOrNull()?.let { this.latest = it.version }
                this.versions += loader.map { it.version }
            }
            logger.info { "Loaded $name Loader data" }
        }
    }
}
