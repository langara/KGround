package pl.mareklangiewicz.kommand

import kotlinx.coroutines.flow.*

// temporary hack
@Deprecated("Use suspend fun Kommand.exec(...)")
actual fun Kommand.execb(
    platform: CliPlatform,
    vararg useNamedArgs: Unit,
    dir: String?,
    inContent: String?,
    inLineS: Flow<String>?,
    inFile: String?,
    outFile: String?,
): List<String> = TODO("Remove this functionality")

// also temporary hack
@Deprecated("Use suspend fun ReducedKommand.exec(...)")
actual fun <K: Kommand, In, Out, Err, TK: TypedKommand<K, In, Out, Err>, ReducedOut> ReducedKommand<K, In, Out, Err, TK, ReducedOut>
        .execb(platform: CliPlatform, dir: String?): ReducedOut = TODO("Remove this functionality")
