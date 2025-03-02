package app.revanced.patches.reddit.customclients.infinityforreddit.api

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.fingerprint.MethodFingerprintResult
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.reddit.customclients.AbstractSpoofClientPatch
import app.revanced.patches.reddit.customclients.Constants.OAUTH_USER_AGENT
import app.revanced.patches.reddit.customclients.infinityforreddit.api.fingerprints.GetHttpBasicAuthHeaderFingerprint
import app.revanced.patches.reddit.customclients.infinityforreddit.api.fingerprints.LoginActivityOnCreateFingerprint
import app.revanced.patches.reddit.customclients.infinityforreddit.api.fingerprints.SetWebViewSettingsFingerprint
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Spoof client",
    description = "Restores functionality of the app by using custom client ID's.",
    compatiblePackages = [
        CompatiblePackage(
            "ml.docilealligator.infinityforreddit", [
                "5.4.0",
                "5.4.1",
                "5.4.2",
                "6.0.1",
                "6.0.2",
                "6.0.4",
                "6.0.6",
                "6.1.1"
            ]
        )
    ]
)
@Suppress("unused")
object SpoofClientPatch : AbstractSpoofClientPatch(
    "infinity://localhost",
    clientIdFingerprints = listOf(GetHttpBasicAuthHeaderFingerprint, LoginActivityOnCreateFingerprint),
    userAgentFingerprints = listOf(SetWebViewSettingsFingerprint)
) {
    override fun List<MethodFingerprintResult>.patchClientId(context: BytecodeContext) {
        forEach {
            // First is index of the clientId string.
            val clientIdIndex = it.scanResult.stringsScanResult!!.matches.first().index
            it.mutableMethod.apply {
                val oAuthClientIdRegister = getInstruction<OneRegisterInstruction>(clientIdIndex).registerA

                replaceInstruction(
                    clientIdIndex,
                    "const-string v$oAuthClientIdRegister, \"$clientId\""
                )
            }
        }
    }

    override fun List<MethodFingerprintResult>.patchUserAgent(context: BytecodeContext) {
        first().let { result ->
            val insertIndex = result.scanResult.stringsScanResult!!.matches.first().index

            result.mutableMethod.addInstructions(
                insertIndex,
                """
                    const-string v0, "$OAUTH_USER_AGENT"
                    invoke-virtual {p1, v0}, Landroid/webkit/WebSettings;->setUserAgentString(Ljava/lang/String;)V
                """
            )
        }
    }
}
