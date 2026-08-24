package com.sbby.aqzlgj.kotlin.ui.screen.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import com.sbby.aqzlgj.kotlin.BuildConfig
import com.sbby.aqzlgj.kotlin.R
import com.sbby.aqzlgj.kotlin.ui.LocalUiMode
import com.sbby.aqzlgj.kotlin.ui.UiMode
import com.sbby.aqzlgj.kotlin.ui.navigation3.LocalNavigator

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val githubLink = "<b><a href=\"https://github.com/scpao5/aqzlgj\">Github</a></b>"
    val qqLink = "<a href=\"mqqapi://card/show_pslcard?src_type=internal&version=1&uin=771217201&card_type=person&source=qrcode\">QQ: 771217201 (作者)</a>"
    val htmlString =
        stringResource(id = R.string.about_source_link, githubLink) + "<br/>" + qqLink
    val state = AboutUiState(
        title = stringResource(R.string.about),
        appName = stringResource(R.string.app_name),
        versionName = BuildConfig.VERSION_NAME,
        links = extractLinks(htmlString),
    )
    val actions = AboutScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onOpenLink = uriHandler::openUri,
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> AboutScreenMiuix(state, actions)
        UiMode.Material -> AboutScreenMaterial(state, actions)
    }
}
