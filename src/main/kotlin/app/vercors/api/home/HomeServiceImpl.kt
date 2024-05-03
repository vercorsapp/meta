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

package app.vercors.api.home

import app.vercors.api.mapAsync
import app.vercors.api.project.ProjectService
import app.vercors.proto.Home.HomeResponse
import app.vercors.proto.Home.HomeSection
import app.vercors.proto.ProjectOuterClass.ProjectProvider
import app.vercors.proto.ProjectOuterClass.ProjectType
import app.vercors.proto.homeResponse
import app.vercors.proto.homeSection
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

private val logger = KotlinLogging.logger {}

@Singleton
class HomeServiceImpl @Inject constructor(
    private val externalScope: CoroutineScope,
    private val projectService: ProjectService
) : HomeService {
    private var loadingJob: Job? = null
    private var cache: HomeData? = null

    init {
        externalScope.launch {
            while (isActive) {
                load()
                delay(1.hours)
            }
        }
    }

    override suspend fun getHomeProjects(provider: ProjectProvider, types: List<ProjectType>): HomeResponse {
        if (cache == null) {
            if (loadingJob == null) load()
            loadingJob!!.join()
        }
        val data = cache!!
            .filterKeys { (keyProvider, keyType) -> keyProvider == provider && keyType in types }
            .map { it.value }
        return homeResponse { sections.addAll(data) }
    }

    private fun load() {
        loadingJob = externalScope.launch {
            logger.info { "Loading home data" }
            cache = ProjectProvider.entries
                .filter { it != ProjectProvider.UNRECOGNIZED }
                .flatMap { provider ->
                    ProjectType.entries
                        .filter { it != ProjectType.UNRECOGNIZED }
                        .map { type -> HomeKey(provider, type) }
                }
                .mapAsync {
                    it to homeSection {
                        type = it.type
                        projects.addAll(
                            projectService.searchProject(
                                provider = it.provider,
                                type = it.type,
                                limit = 10
                            )
                        )
                    }
                }
                .toMap()
            logger.info { "Loaded home data" }
        }
    }
}

private typealias HomeData = Map<HomeKey, HomeSection>

private data class HomeKey(val provider: ProjectProvider, val type: ProjectType)