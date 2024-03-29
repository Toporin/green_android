package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.settings.WalletSettingsSection
import com.blockstream.common.models.settings.WalletSettingsViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.settings.WalletSettingsScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class WalletSettingsFragment : AppFragment<ComposeViewBinding>(R.layout.compose_view, 0) {
    val args: WalletSettingsFragmentArgs by navArgs()

    val viewModel: WalletSettingsViewModel by viewModel {
        parametersOf(
            args.wallet,
            if (args.showRecoveryTransactions) WalletSettingsSection.RecoveryTransactions else WalletSettingsSection.General,
            args.network
        )
    }

    override fun getGreenViewModel() = viewModel

    override val useCompose: Boolean = true

    override val sideEffectsHandledByAppFragment: Boolean = false

    override val title: String?
        get() = if (args.showRecoveryTransactions) getString(R.string.id_recovery_transactions) else if(args.network != null) getString(R.string.id_settings) else null

    override val subtitle: String?
        get() = if(args.network != null) getString(R.string.id_multisig) else null

    override val toolbarIcon: Int?
        get() = args.network?.getNetworkIcon()

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        when (sideEffect) {
            is SideEffects.NavigateTo -> {
                (sideEffect.destination as? NavigateDestinations.RecoveryIntro)?.also {
                    navigate(
                        WalletSettingsFragmentDirections.actionWalletSettingsFragmentToRecoveryIntroFragment(
                            setupArgs = it.args
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.TwoFactorAuthentication)?.also {
                    navigate(
                        WalletSettingsFragmentDirections.actionWalletSettingsFragmentToTwoFractorAuthenticationFragment(
                            wallet = it.greenWallet
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.TwoFactorSetup)?.also {
                    navigate(
                        WalletSettingsFragmentDirections.actionWalletSettingsFragmentToTwoFactorSetupFragment(
                            wallet = it.greenWallet,
                            method = it.method,
                            action = it.action,
                            network = it.network
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.ArchivedAccounts)?.also {
                    navigate(
                        WalletSettingsFragmentDirections.actionGlobalArchivedAccountsFragment(
                            wallet = it.greenWallet
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.ChangePin)?.also {
                    navigate(
                        WalletSettingsFragmentDirections.actionWalletSettingsFragmentToChangePinFragment(
                            wallet = it.greenWallet
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.WatchOnly)?.also {
                    navigate(
                        WalletSettingsFragmentDirections.actionWalletSettingsFragmentToWatchOnlyFragment(
                            wallet = it.greenWallet
                        )
                    )
                }

            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    WalletSettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
