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

import com.google.protobuf.Message
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun ApplicationCall.respondProtobuf(message: Message?) =
    if (message == null) respond(HttpStatusCode.NotFound)
    else respondBytes(message.toByteArray(), ContentType.Application.ProtoBuf)

suspend fun <T, R> Iterable<T>.mapAsync(
    transform: suspend (T) -> R
): List<R> = coroutineScope { map { async { transform(it) } }.awaitAll() }

inline fun <reified E : Enum<E>> safeValueOf(value: String): E {
    if (value == "UNRECOGNIZED") throw BadRequestException("Invalid enum value $value")
    try {
        return enumValueOf<E>(value)
    } catch (e: IllegalArgumentException) {
        throw BadRequestException("Invalid enum value $value")
    }
}

fun PipelineContext<*, ApplicationCall>.queryParam(name: String) =
    call.request.queryParameters[name] ?: throw BadRequestException("Query parameter $name is required")

fun PipelineContext<*, ApplicationCall>.pathParam(name: String) =
    call.parameters[name] ?: throw BadRequestException("Path parameter $name is required")