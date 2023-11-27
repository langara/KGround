package pl.mareklangiewicz.kground.io

import kotlinx.coroutines.*
import okio.*
import pl.mareklangiewicz.kground.*
import pl.mareklangiewicz.kommand.*
import pl.mareklangiewicz.ulog.*
import kotlin.coroutines.*

interface WithCLI {
    val cli: CliPlatform
}

interface KGroundXIO: KGroundIO, WithCLI

/**
 * Sth like this be used as a context receiver when Kotlin supports it.
 *
 * For now just pass it inside [coroutineContext] when in coroutine.
 * or as "kg" parameter - when in blocking world,
 *
 * Warning: I use experimental polymorphic keys as in kdoc of [AbstractCoroutineContextKey]
 * Warning: Make sure all derived classes also follow this experimental API rules correctly.
 */
@OptIn(ExperimentalStdlibApi::class)
open class KGroundXIOCtx(override val cli: CliPlatform, fs: FileSystem, ulog: ULog): KGroundIOCtx(fs, ulog), KGroundXIO {
    companion object Key : AbstractCoroutineContextKey<KGroundIOCtx, KGroundXIOCtx>(KGroundIOCtx, { it as? KGroundXIOCtx })
}

/**
 * @param name set to non-null to add new [CoroutineName] to context
 * @param cli set to non-null to set specific [CliPlatform] in context,
 * when null it tries to use one from current(parent) [KGroundXIOCtx],
 * and defaults to [SysPlatform] if no [CliPlatform] found.
 * @param fs set to non-null to set specific [FileSystem] in context,
 * when null it tries to use one from current (parent) [KGroundIOCtx],
 * and defaults to [DefaultOkioFileSystemOrErr] if no [FileSystem] found.
 * @param ulog set to non-null to change the [ULog],
 * when null it tries to use one from current (parent) [KGroundCtx],
 * and defaults to [ULogPrintLn] if no [ULog] found.
 */
suspend fun <T> withKGroundXIOCtx(
    name: String? = null,
    cli: CliPlatform? = null,
    fs: FileSystem? = null,
    ulog: ULog? = null,
    block: suspend CoroutineScope.() -> T,
): T = withContext(
    KGroundXIOCtx(
        cli ?: coroutineContext[KGroundXIOCtx]?.cli ?: SysPlatform(),
        fs ?: coroutineContext[KGroundIOCtx]?.fs ?: DefaultOkioFileSystemOrErr,
        ulog ?: coroutineContext[KGroundCtx]?.ulog ?: ULogPrintLn()
    ) plusIfNN name?.let(::CoroutineName),
    block
)
