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

package app.vercors.meta.game

import app.vercors.meta.game.mojang.MojangReleaseType
import app.vercors.meta.game.mojang.MojangVersionManifest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.minutes

@Single
class GameServiceImpl(
    private val http: HttpClient,
    private val externalScope: CoroutineScope
) : GameService {
    private var loadingJob: Job? = null
    private var data: GameVersionList? = null

    init {
        externalScope.launch {
            while (isActive) {
                load()
                delay(5.minutes)
            }
        }
    }

    override suspend fun getGameVersions(): GameVersionList {
        if (data == null) {
            if (loadingJob == null) load()
            loadingJob!!.join()
        }
        return data!!
    }

    private fun load() {
        loadingJob?.cancel()
        loadingJob = externalScope.launch {
            val res = http.get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
                .body<MojangVersionManifest>()
            data = gameVersionList {
                recommended = res.latest.release
                latest = res.latest.snapshot
                versions += res.versions.map {
                    gameVersion {
                        id = it.id
                        type = it.type.convert()
                        time = it.time.toEpochMilli()
                        url = it.url
                        sha1 = it.sha1
                    }
                }
            }
        }
    }

    private fun MojangReleaseType.convert(): GameVersionType = when (this) {
        MojangReleaseType.Release -> GameVersionType.release
        MojangReleaseType.Snapshot -> GameVersionType.snapshot
        MojangReleaseType.Beta -> GameVersionType.beta
        MojangReleaseType.Alpha -> GameVersionType.alpha
    }
}
