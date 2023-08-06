@file:Suppress("unused", "SpellCheckingInspection", "MemberVisibilityCanBePrivate")

package pl.mareklangiewicz.kommand.github

import pl.mareklangiewicz.kommand.samples.*

data object GhSamples {
    val help =
        ghHelp() s
                "gh help"

    val helpHelp =
        ghHelp { -Help } s
                "gh help --help"

    val helpSecret =
        ghHelp { +"secret" } s
                "gh help secret"

    val helpSecretList =
        ghHelp { +"secret"; +"list" } s
                "gh help secret list"

    val version =
        ghVersion() s
                "gh version"

    val versionHelp =
        ghVersion { -Help } s
                "gh version --help"

    val status =
        ghStatus() s
                "gh status"

    val statusOrgExclude =
        ghStatus { -Org("orgbla"); -Exclude("langara/bla", "langara/ble") } s
                "gh status --org orgbla --exclude langara/bla,langara/ble"

    val secretList =
        ghSecretList() s
                "gh secret list"

    val secretListHelp =
        ghSecretList { -Help } s
                "gh secret list --help"

    val secretListForAbcdK =
        ghSecretList("langara/AbcdK") s
                "gh secret list --repo langara/AbcdK"

    /** Not providing fake secret as input stream when starting this kommand will ask for it interactively */
    val secretSetFakeSecretInAbcdK =
        ghSecretSet("FAKE_SECRET", repoPath = "langara/AbcdK") s
                "gh secret set FAKE_SECRET --repo langara/AbcdK"

    val secretSetConcreteFakeSecret66InAbcdK =
        ghSecretSet(secretName = "FAKE_SECRET_66", secretValue = "concretevalue66", repoPath = "langara/AbcdK") rs
                "gh secret set FAKE_SECRET_66 --repo langara/AbcdK"

    val secretSetConcreteFakeSecret67InAbcdK =
        ghSecretSet(secretName = "FAKE_SECRET_67", secretValue = "concretevalue67", repoPath = "langara/AbcdK") rs
                "gh secret set FAKE_SECRET_67 --repo langara/AbcdK"

    val secretDeleteFakeSecretInAbcdK =
        ghSecretDelete(secretName = "FAKE_SECRET", repoPath = "langara/AbcdK") s
                "gh secret delete FAKE_SECRET --repo langara/AbcdK"

    val repoViewAbcdKWeb =
        ghRepoView("langara/AbcdK", web = true) s
                "gh repo view langara/AbcdK --web"

    val repoViewKotlinXCoroutinesDevelopWeb =
        ghRepoView("Kotlin/kotlinx.coroutines", branch = "develop", web = true) s
                "gh repo view Kotlin/kotlinx.coroutines --branch develop --web"

    val repoList=
        ghRepoList() s
                "gh repo list"

    val repoListRomanElizarov=
        ghRepoList("elizarov") s
                "gh repo list elizarov"

    val repoListAvailableJsonFields=
        ghRepoList { -Json() } s
                "gh repo list --json"

    val langaraPublicRepoListNamesAndUrls =
        ghLangaraRepoList().outputFields("name", "url") s
                "gh repo list langara --limit 1000 --language kotlin --no-archived --source --visibility public --json name,url --jq .[]|.name,.url"


    val langaraPublicRepoMarkdownList =
        ghLangaraRepoList().reducedToMarkdownList() rs
                langaraPublicRepoListNamesAndUrls.expectedLineRaw

}

fun ghLangaraRepoList(limit: Int = 1000, language: String? = "kotlin", onlyPublic: Boolean = true) = ghRepoList(
    "langara",
    limit = limit,
    onlyLanguage = language,
    onlyNotArchived = true,
    onlyNotForks = true,
    onlyPublic = onlyPublic,
)
