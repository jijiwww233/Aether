Warning: truncated output (original token count: 69197)
Total output lines: 6498

package com.zhousl.aether.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.FileOutputStream
import com.zhousl.aether.BuildConfig
import com.zhousl.aether.R
import com.zhousl.aether.aetherRuntime
import com.zhousl.aether.data.ActiveSkillContext
import com.zhousl.aether.data.AetherAnalytics
import com.zhousl.aether.data.AetherModOperationDecision
import com.zhousl.aether.data.AetherModServiceMethod
import com.zhousl.aether.data.AppUpdateManager
import com.zhousl.aether.data.AutomaticModelPurpose
import com.zhousl.aether.data.AgentModeAuthorizationMethod
import com.zhousl.aether.data.AgentWorkspaceMode
import com.zhousl.aether.data.AlpineEnvironmentVariable
import com.zhousl.aether.data.AppLanguage
import com.zhousl.aether.data.AppSettings
import com.zhousl.aether.data.normalizeReasoningEffort
import com.zhousl.aether.data.AppThemeMode
import com.zhousl.aether.data.CurrentOnboardingVersion
import com.zhousl.aether.data.DiagnosticRedactor
import com.zhousl.aether.data.InstalledSkill
import com.zhousl.aether.data.InstalledPiExtension
import com.zhousl.aether.data.PiExtensionInstallKind
import com.zhousl.aether.data.PiExtensionCatalogEntry
import com.zhousl.aether.data.PiDiscoveredSkillSource
import com.zhousl.aether.data.ProviderModelCatalogClient
import com.zhousl.aether.data.thinkingCatalogKey
import com.zhousl.aether.data.LlmProviderConfig
import com.zhousl.aether.data.LlmTokenUsage
import com.zhousl.aether.data.ModelCatalogClient
import com.zhousl.aether.data.LocalRuntimeId
import com.zhousl.aether.data.ProviderModelOption
import com.zhousl.aether.data.PersistedChatState
import com.zhousl.aether.data.PersistedChatWriteIntent
import com.zhousl.aether.data.availableModelOptions
import com.zhousl.aether.data.McpServerConfig
import com.zhousl.aether.data.normalizeLlmInactivityReconnectTimeoutSeconds
import com.zhousl.aether.data.normalizeLlmUserAgent
import com.zhousl.aether.data.normalizeOldCommandHistoryRetentionHours
import com.zhousl.aether.data.OnboardingStarterPrompt
import com.zhousl.aether.data.PackageProfileState
import com.zhousl.aether.data.RootSetupIssue
import com.zhousl.aether.data.RootSetupState
import com.zhousl.aether.data.ScheduledTask
import com.zhousl.aether.data.ScheduledTaskCreator
import com.zhousl.aether.data.ScheduledTaskSchedule
import com.zhousl.aether.data.TermuxEnvironmentVariable
import com.zhousl.aether.data.normalizeTermuxEnvironmentVariables
import com.zhousl.aether.data.normalizeAlpineEnvironmentVariables
import com.zhousl.aether.data.SessionFollowUpMode
import com.zhousl.aether.data.SessionExecutionState
import com.zhousl.aether.data.SessionTurnEvent
import com.zhousl.aether.data.SessionTurnOutcome
import com.zhousl.aether.data.SessionTurnRequest
import com.zhousl.aether.data.parseChatSessions
import com.zhousl.aether.data.parseCustomHeaders
import com.zhousl.aether.data.parseProviderConfigs
import com.zhousl.aether.data.serializeChatSessions
import com.zhousl.aether.data.serializeProviderConfigs
import com.zhousl.aether.data.toJson
import com.zhousl.aether.data.toJsonArray
import com.zhousl.aether.data.withExplicitDefaultChatModel
import com.zhousl.aether.data.LlmMessage
import com.zhousl.aether.data.LlmTextPart
import com.zhousl.aether.data.ProviderAuthMethod
import com.zhousl.aether.data.pi.PiCompletionClient
import com.zhousl.aether.data.pi.PiKernelBridge
import com.zhousl.aether.data.pi.PiCoreSetupActivity
import com.zhousl.aether.data.pi.PiCoreSetupPhase
import com.zhousl.aether.data.pi.PiCoreSetupState
import com.zhousl.aether.data.pi.PiCoreSetupUpdate
import com.zhousl.aether.data.pi.PiProviderAuthState
import com.zhousl.aether.data.pi.toProviderPayloadJson
import com.zhousl.aether.data.pi.toPiOAuthPrompt
import com.zhousl.aether.data.pi.toPiProviderEnvironmentVariables
import com.zhousl.aether.data.pi.toPiModelConfig
import com.zhousl.aether.data.isProviderSetupValid
import com.zhousl.aether.data.isNightlyUpdateNewer
import com.zhousl.aether.data.isVersionNewer
import com.zhousl.aether.data.isOnboardingComplete
import com.zhousl.aether.data.shouldMarkOnboardingCompleted
import com.zhousl.aether.data.shouldLaunchOnboarding
import com.zhousl.aether.data.shouldRevealFollowUpTourCard
import com.zhousl.aether.data.resolveDefaultChatModelKey
import com.zhousl.aether.data.resolveDefaultCompactingModelKey
import com.zhousl.aether.data.resolveDefaultTitleModelKey
import com.zhousl.aether.data.resolveAutomaticModelKey
import com.zhousl.aether.data.resolveModelSettings
import com.zhousl.aether.data.resolveStoredOrAutomaticModelKey
import com.zhousl.aether.termux.TermuxSetupIssue
import com.zhousl.aether.termux.TermuxSetupState
import com.zhousl.aether.runtime.AlpineSetupProgress
import com.zhousl.aether.runtime.AlpineTerminalLaunchSpec
import com.zhousl.aether.runtime.AlpineSetupActivity
import com.zhousl.aether.runtime.LocalRuntimeIssue
import com.zhousl.aether.runtime.LocalRuntimeSetupState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.Base64
import java.util.Locale
import java.util.UUID

private const val FollowUpTourAutoOpenDelayMillis = 2_500L
private const val AppUpdateCheckIntervalMillis = 3L * 24L * 60L * 60L * 1000L
private const val UpdateChannelNightly = "nightly"
private const val LogcatReadTimeoutSeconds = 4L
private const val MaxSetupOutputChars = 48_000
private const val SessionTitleSystemPrompt =
    "Generate a concise chat title for this conversation. Return only the title, in the user's language when possible, with no quotes, no emoji, and at most 6 words."
private const val CompactCommand = "/compact"
// Pi Coding Agent defaults: compact before the model loses the 16K response reserve.
private const val ContextWindowTokens = 128_000L
private const val AutoCompactionReserveTokens = 16_384L
private const val MaxInlineImageAttachmentBytes = 5 * 1024 * 1024

internal fun shouldAutoCompactContext(
    usage: LlmTokenUsage?,
    tokenUsageSource: String,
    assistantText: String,
    contextWindow: Long = ContextWindowTokens,
    reserveTokens: Long = AutoCompactionReserveTokens,
): Boolean {
    val totalTokens = usage?.withMissingTotalResolved()?.totalTokens ?: return false
    if (contextWindow <= reserveTokens) return false
    val trailingEstimate = if (tokenUsageSource == "estimated") {
        (assistantText.length + 3L) / 4L
    } else {
        0L
    }
    return totalTokens + trailingEstimate > contextWindow - reserveTokens
}

internal fun mergeImportedPiExtensions(
    current: List<InstalledPiExtension>,
    imported: List<InstalledPiExtension>,
): List<InstalledPiExtension> =
    (current.filter { it.kind != PiExtensionInstallKind.Imported } + imported)
        .distinctBy(InstalledPiExtension::id)
        .sortedBy { it.name.lowercase(Locale.US) }

class AetherViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val runtime = application.aetherRuntime
    private val diagnosticLogger = runtime.diagnosticLogger
    private val settingsRepository = runtime.settingsRepository
    private val modKernel = runtime.modKernel
    private val chatStateStore = runtime.chatStateStore
    private val extensionsRepository = runtime.extensionsRepository
    private val sessionExecutionManager = runtime.sessionExecutionManager
    private val piCompletionClient: PiCompletionClient = runtime.piCompletionClient
    private val piKernelBridge: PiKernelBridge = runtime.piKernelBridge
    private val bashTool = runtime.bashTool
    private val rootSetupController = runtime.rootSetupController
    private val workspaceFileBridge = runtime.workspaceFileBridge
    private val runtimeWorkspaceFileBridge = runtime.runtimeWorkspaceFileBridge
    private val agentModeController = runtime.agentModeController
    private val skillManager = runtime.skillManager
    private val piExtensionManager = runtime.piExtensionManager
    private val aetherAppExtensionManager = runtime.aetherAppExtensionManager
    private val scheduledTaskManager = runtime.scheduledTaskManager
    private val appUpdateManager = AppUpdateManager(application.applicationContext)
    private var didEvaluateStartupUpdateCheck = false
    private var lastTrackedTermuxDetectedIssue: TermuxSetupIssue? = null
    private var pendingTermuxSetupSource: String? = null
    private var lastModelCatalogRequestKey: String = ""
    private var didInitializeStartupDraftModel = false
    private var didInitializeStartupDraftSkills = false
    private var didReceiveInitialChatState = false
    private val _uiState = MutableStateFlow(AetherUiState())
    private val _transientMessages = MutableSharedFlow<UiText>(extraBufferCapacity = 4)
    private var didEvaluateWorkspaceMode = false
    private var selectSessionJob: Job? = null
    private var providerAuthJob: Job? = null
    private var extensionSendHookJob: Job? = null
    private var settingsSaveJob: Job? = null
    private var developerAlpineSetupPreviewJob: Job? = null
    private var piExtensionRefreshGeneration: Long = 0
    private var didRefreshAlpineAfterSettingsLoad = false

    val uiState: StateFlow<AetherUiState> = _uiState.asStateFlow()
    val transientMessages = _transientMessages.asSharedFlow()

    init {
        registerCoreModServices()
        refreshTermuxSetup()
        refreshRootSetup()
        refreshImportedPiExtensions()

        viewModelScope.launch {
            settingsRepository.initializeLanguageIfNeeded()
            settingsRepository.settings.collect { settings ->
                if (settings.privacyPolicyAccepted) {
                    runtime.initializePostHog()
                }
                _uiState.update { current ->
                    if (!current.isStartupRouteResolved) {
                        current.copy(
                            settings = settings,
                            currentScreen = if (settings.shouldLaunchOnboarding()) {
                                AppScreen.Onboarding
                            } else {
                                AppScreen.Chat
                            },
                            isStartupRouteResolved = true,
                            isOnboardingReplay = false,
                            onboardingStep = OnboardingStep.Landing,
                            onboardingReturnScreen = AppScreen.Chat,
                        )
                    } else {
                        current.copy(settings = settings)
                    }
                }
                initializeStartupDraftModelIfReady()
                initializeStartupDraftSkillsIfReady()
                syncTermuxSettings()
                bashTool.setEnvironmentVariables(settings.termuxEnvironmentVariables)
                bashTool.setManagedBashRunCleanupPolicy(
                    enabled = settings.autoCleanOldCommandHistory,
                    retentionHours = settings.oldCommandHistoryRetentionHours,
                )
                runtime.alpineRuntime.setEnvironmentVariables(settings.alpineEnvironmentVariables)
                if (!didRefreshAlpineAfterSettingsLoad) {
                    didRefreshAlpineAfterSettingsLoad = true
                    refreshAlpineSetup(startPiIfReady = false)
                }
                if (!didEvaluateStartupUpdateCheck && settings.privacyPolicyAccepted) {
                    didEvaluateStartupUpdateCheck = true
                    maybeCheckForUpdates(settings)
                }
                agentModeController.refreshAuthorization(settings)
                maybeInitializeWorkspaceMode(settings)
            }
        }

        viewModelScope.launch {
            var didReceiveChatState = false
            chatStateStore.state.collect { persisted ->
                if (!didReceiveChatState && persisted.sessions.isEmpty() && persisted.currentSessionId == DraftSessionId) {
                    didReceiveChatState = true
                    didReceiveInitialChatState = true
                    initializeStartupDraftModelIfReady()
                    return@collect
                }
                didReceiveChatState = true
                val hydratedPersisted = hydrateCurrentSessionMessages(persisted)
                _uiState.update { current ->
                    val currentSessionId = hydratedPersisted.currentSessionId.ifBlank { DraftSessionId }
                    val currentExecution = current.sessionExecutionStates[currentSessionId]
                    current.copy(
                        sessions = hydratedPersisted.sessions,
                        currentSessionId = currentSessionId,
                        isSending = currentExecution?.isRunning == true,
                        pendingResponseSessionId = currentExecution?.sessionId,
                        pendingToolInvocations = currentExecution?.pendingToolInvocations.orEmpty(),
                        pendingResponseBlocks = currentExecution?.pendingResponseBlocks.orEmpty(),
                        pendingAssistantText = currentExecution?.pendingAssistantText.orEmpty(),
                        pendingStatusText = currentExecution?.pendingStatusText.orEmpty(),
                        pendingStatusDetail = currentExecution?.pendingStatusDetail.orEmpty(),
                    )
                }
                didReceiveInitialChatState = true
                initializeStartupDraftModelIfReady()
            }
        }

        viewModelScope.launch {
            sessionExecutionManager.executionStates.collect { executionStates ->
                _uiState.update { current ->
                    val currentExecution = executionStates[current.currentSessionId]
                    current.copy(
                        sessionExecutionStates = executionStates,
                        isSending = currentExecution?.isRunning == true,
                        pendingResponseSessionId = currentExecution?.sessionId,
                        pendingToolInvocations = currentExecution?.pendingToolInvocations.orEmpty(),
                        pendingResponseBlocks = currentExecution?.pendingResponseBlocks.orEmpty(),
                        pendingAssistantText = currentExecution?.pendingAssistantText.orEmpty(),
                        pendingStatusText = currentExecution?.pendingStatusText.orEmpty(),
                        pendingStatusDetail = currentExecution?.pendingStatusDetail.orEmpty(),
                    )
                }
            }
        }

        viewModelScope.launch {
            sessionExecutionManager.turnEvents.collect { event ->
                handleTurnEvent(event)
            }
        }

        viewModelScope.launch {
            extensionsRepository.extensionState.collect { extensionState ->
                var didPruneSelections = false
                val enabledSkillIds = extensionState.installedSkills
                    .filter { it.isEnabled }
                    .map { it.id }
                    .toSet()
                val enabledMcpServerIds = emptySet<String>()
                _uiState.update { current ->
                    val updatedSessions = current.sessions.map { session ->
                        val updatedSelectedSkillIds = session.selectedSkillIds.filter(enabledSkillIds::contains)
                        val updatedActiveSkills = session.activeSkills.filter { activeSkill ->
                            updatedSelectedSkillIds.contains(activeSkill.skillId)
                        }
                        val updatedActiveMcpServerIds = session.activeMcpServerIds.filter(enabledMcpServerIds::contains)
                        if (
                            updatedSelectedSkillIds != session.selectedSkillIds ||
                            updatedActiveSkills != session.activeSkills ||
                            updatedActiveMcpServerIds != session.activeMcpServerIds
                        ) {
                            didPruneSelections = true
                            session.copy(
                                selectedSkillIds = updatedSelectedSkillIds,
                                activeSkills = updatedActiveSkills,
                                activeMcpServerIds = updatedActiveMcpServerIds,
                            )
                        } else {
                            session
                        }
                    }
                    val updatedDraftSelectedSkillIds = current.draftSelectedSkillIds.filter(enabledSkillIds::contains)
                    val updatedDraftSelectedMcpServerIds = current.draftSelectedMcpServerIds.filter(enabledMcpServerIds::contains)
                    if (
                        updatedDraftSelectedSkillIds != current.draftSelectedSkillIds ||
                        updatedDraftSelectedMcpServerIds != current.draftSelectedMcpServerIds
                    ) {
                        didPruneSelections = true
                    }
                    current.copy(
                        sessions = updatedSessions,
                        draftSelectedSkillIds = updatedDraftSelectedSkillIds,
                        draftSelectedMcpServerIds = updatedDraftSelectedMcpServerIds,
                        installedSkills = extensionState.installedSkills,
                        mcpServers = emptyList(),
                    )
                }
                if (didPruneSelections) {
                    persistPrunedSessionSelections(
                        enabledSkillIds = enabledSkillIds,
                        enabledMcpServerIds = enabledMcpServerIds,
                    )
                }
                initializeStartupDraftSkillsIfReady()
            }
        }

        viewModelScope.launch {
            settingsRepository.providerConfigs.collect { configs ->
                _uiState.update { current -> current.copy(providerConfigs = configs) }
                initializeStartupDraftModelIfReady()
                refreshModelCatalogInfo(configs)
            }
        }
        viewModelScope.launch {
            scheduledTaskManager.scheduledTasks.collect { tasks ->
                _uiState.update { current -> current.copy(scheduledTasks = tasks) }
            }
        }
        viewModelScope.launch {
            agentModeController.displayState.collect { displayState ->
                _uiState.update { current -> current.copy(agentModeDisplayState = displayState) }
            }
        }
        viewModelScope.launch {
            runtime.alpineChromeController.displayState.collect { displayState ->
                _uiState.update { current -> current.copy(chromeDisplayState = displayState) }
            }
        }
        viewModelScope.launch {
            agentModeController.authorizationState.collect { authorizationState ->
                _uiState.update { current -> current.copy(agentModeAuthorizationState = authorizationState) }
            }
        }
    }

    private fun refreshModelCatalogInfo(configs: List<LlmProviderConfig>) {
        val options = configs.availableModelOptions()
        val requestKey = options.joinToString("|") { "${it.key}:${it.fullLabel}" }
        if (requestKey == lastModelCatalogRequestKey) return
        lastModelCatalogRequestKey = requestKey
        if (options.isEmpty()) {
            _uiState.update { current -> current.copy(modelCatalogInfo = emptyMap()) }
            return
        }
        viewModelScope.launch {
            if (_uiState.value.modelCatalogInfo.isEmpty()) {
                val cached = settingsRepository.loadModelCatalogCache()
                    .filterKeys(options.mapTo(mutableSetOf(), ProviderModelOption::key)::contains)
                if (cached.isNotEmpty() && requestKey == lastModelCatalogRequestKey) {
                    _uiState.update { current -> current.copy(modelCatalogInfo = cached) }
                }
            }
            val thinkingCacheKeys = options.mapTo(mutableSetOf()) { option ->
                thinkingCatalogKey(option.piProviderId, option.modelId)
            }
            val cachedThinkingLevels = settingsRepository.loadThinkingCatalogCache()
                .filterKeys(thinkingCacheKeys::contains)
            val cachedThinkingLevelMaps = settingsRepository.loadThinkingLevelMapsCache()
                .filterKeys(thinkingCacheKeys::contains)
            val cachedReasoningModels = settingsRepository.loadReasoningModelsCache()
                .filterTo(mutableSetOf(), thinkingCacheKeys::contains)
            if (cachedThinkingLevels.isNotEmpty() && requestKey == lastModelCatalogRequestKey) {
                _uiState.update { current ->
                    current.copy(
                        thinkingLevelsByProviderModel = current.thinkingLevelsByProviderModel + cachedThinkingLevels,
                        thinkingLevelClampsByProviderModel = current.thinkingLevelClampsByProviderModel + cachedThinkingLevelMaps,
                        reasoningModels = current.reasoningModels + cachedReasoningModels,
                    )
                }
            }
            val modelInfo = ModelCatalogClient.fetchModelInfo(options)
            if (modelInfo.isNotEmpty()) {
                settingsRepository.saveModelCatalogCache(modelInfo)
            }
            if (modelInfo.isNotEmpty() && requestKey == lastModelCatalogRequestKey) {
                _uiState.update { current -> current.copy(modelCatalogInfo = modelInfo) }
            }

            // Populate the effort cache during startup as well as when the model
            // picker is opened. Cached values are already applied above, so an
            // unavailable network never delays the initial picker state.
            val catalogResult = ProviderModelCatalogClient.fetchPublicThinkingCatalog(options)
            if (catalogResult.levelsByProviderModel.isNotEmpty()) {
                settingsRepository.saveThinkingCatalogCache(
                    catalogResult.levelsByProviderModel,
                    catalogResult.levelMapsByProviderModel,
                    catalogResult.reasoningModels,
                )
                if (requestKey == lastModelCatalogRequestKey) {
                    _uiState.update { state ->
                        state.copy(
                            thinkingLevelsByProviderModel =
                                state.thinkingLevelsByProviderModel + catalogResult.levelsByProviderModel,
                            thinkingLevelClampsByProviderModel =
                                (state.thinkingLevelClampsByProviderModel -
                                    catalogResult.levelsByProviderModel.keys) +
                                    catalogResult.levelMapsByProviderModel,
                            reasoningModels =
                                (state.reasoningModels - catalogResult.levelsByProviderModel.keys) +
                                    catalogResult.reasoningModels,
                        )
                    }
                }
            }
        }
    }

    fun acceptPrivacyPolicy() {
        viewModelScope.launch {
            settingsRepository.updatePrivacyPolicyAccepted(true)
            runtime.initializePostHog()
        }
    }

    fun refreshTermuxSetup() {
        viewModelScope.launch {
            val inspectedSetupState = withContext(Dispatchers.IO) {
                inspectTermuxSetupWithRootRepair()
            }
            val setupState = rememberTermuxSetupCompleted(inspectedSetupState)
            trackTermuxSetupState(setupState, source = "refresh")
            _uiState.update { current -> current.copy(termuxSetupState = setupState) }
        }
    }

    fun refreshRootSetup() {
        viewModelScope.launch {
            val inspectedRootState = rootSetupController.inspect()
            _uiState.update { current ->
                val rootState = if (current.rootSetupState.isReady && inspectedRootState.rootAvailable) {
                    current.rootSetupState.copy(
                        rootAvailable = true,
                        suPath = inspectedRootState.suPath,
                        lastUpdatedMillis = inspectedRootState.lastUpdatedMillis,
                    )
                } else {
                    inspectedRootState
                }
                current.copy(rootSetupState = rootState)
            }
            syncTermuxSettings()
        }
    }

    fun configureLocalAccessWithRoot() {
        val currentRootState = _uiState.value.rootSetupState
        if (currentRootState.isRunning) return
        trackTermuxSetupStarted(source = "root_setup")
        trackPermissionRequested(
            permission = "root_su",
            source = "root_setup",
        )
        _uiState.update { current ->
            current.copy(
                rootSetupState = RootSetupState(
                    issue = RootSetupIssue.Running,
                    detail = "Root access is available. Preparing Termux command access and Root Agent Mode...",
                    rootAvailable = currentRootState.rootAvailable,
                    suPath = currentRootState.suPath,
                    lastUpdatedMillis = System.currentTimeMillis(),
                )
            )
        }
        viewModelScope.launch {
            val rootState = rootSetupController.configureLocalAccess()
            trackPermissionResult(
                permission = "root_su",
                granted = rootState.isReady,
                source = "root_setup",
                result = rootState.issue.name.lowercase(),
            )
            if (rootState.isReady) {
                val settings = _uiState.value.settings
                val updatedSettings = settings.withRuntimeEnabled(LocalRuntimeId.Termux).copy(
                    agentModeAuthorizationEnabled = true,
                    agentModeAuthorizationMethod = AgentModeAuthorizationMethod.Root,
                    termuxSetupCompleted = true,
                )
                settingsRepository.markRootSetupComplete()
                agentModeController.refreshAuthorization(updatedSettings)
            }
            val inspectedSetupState = withContext(Dispatchers.IO) { bashTool.inspectSetup() }
            val setupState = rememberTermuxSetupCompleted(inspectedSetupState)
            trackTermuxSetupState(setupState, source = "root_setup")
            _uiState.update { current ->
                current.copy(
                    rootSetupState = rootState,
                    termuxSetupState = setupState,
                )
            }
            emitTransientMessage(
                if (rootState.isReady) {
                    uiString(R.string.message_root_setup_completed)
                } else {
                    uiString(R.string.message_root_setup_failed, rootState.detail.ifBlank { rootState.issue.name })
                }
            )
        }
    }

    fun startRootSetupFromSettings(returnPage: RootSetupProgressReturnPage) {
        _uiState.update { current ->
            current.copy(
                currentScreen = AppScreen.Settings,
                rootSetupProgressReturnPage = returnPage,
            )
        }
        configureLocalAccessWithRoot()
    }

    fun dismissRootSetupProgress() {
        _uiState.update { current ->
            current.copy(rootSetupProgressReturnPage = null)
        }
    }

    private suspend fun inspectTermuxSetupWithRootRepair(): TermuxSetupState {
        syncTermuxSettings()
        val setupState = bashTool.inspectSetup()
        if (setupState.isReady) return rememberTermuxSetupCompleted(setupState)
        return setupState.withRememberedTermuxConfiguration()
    }

    private suspend fun rememberTermuxSetupCompleted(setupState: TermuxSetupState): TermuxSetupState {
        val settings = _uiState.value.settings
        if (setupState.isReady && !settings.termuxSetupCompleted) {
            settingsRepository.updateSettings(settings.withRuntimeEnabled(LocalRuntimeId.Termux))
        }
        return setupState.copy(
            previouslyConfigured = setupState.previouslyConfigured ||
                settings.termuxSetupCompleted ||
                setupState.isReady,
        )
    }

    private fun TermuxSetupState.withRememberedTermuxConfiguration(): TermuxSetupState =
        if (_uiState.value.settings.termuxSetupCompleted &&
            issue == TermuxSetupIssue.ExternalAppsDisabled
        ) {
            copy(
                issue = TermuxSetupIssue.DispatchFailed,
                detail = "",
                previouslyConfigured = true,
            )
        } else {
            copy(previouslyConfigured = previouslyConfigured || _uiState.value.settings.termuxSetupCompleted)
        }

    private fun syncTermuxSettings() {
        val snapshot = _uiState.value
        bashTool.setRootBackgroundLaunchEnabled(
            snapshot.rootSetupState.isReady ||
                (
                    snapshot.settings.agentModeAuthorizationEnabled &&
                        snapshot.settings.agentModeAuthorizationMethod == AgentModeAuthorizationMethod.Root
                    ),
        )
        bashTool.setEnvironmentVariables(snapshot.settings.termuxEnvironmentVariables)
    }

    fun refreshAlpineSetup(startPiIfReady: Boolean = false) {
        viewModelScope.launch {
            val setupState = withContext(Dispatchers.IO) {
                runtime.alpineRuntime.inspectSetup()
            }
            _uiState.update { current ->
                current.copy(
                    alpineSetupState = setupState,
                    piCoreSetupState = if (setupState.isReady) {
                        current.piCoreSetupState.copy(
                            isChecking = false,
                            activity = PiCoreSetupActivity.None,
                            bytesPerSecond = 0L,
                        )
                    } else {
                        current.piCoreSetupState
                    },
                )
            }
            if (setupState.isReady) {
                val storedSettings = _uiState.value.settings
                val settings = storedSettings.copy(
                    alpineSetupCompleted = true,
                    enabledRuntimeIds = storedSettings.enabledRuntimeIds + LocalRuntimeId.Alpine,
                )
                if (settings != storedSettings) settingsRepository.updateSettings(settings)
                val verifiedProfiles = withContext(Dispatchers.IO) {
                    settings.alpinePackageProfiles.mapValues { (profileId, profileState) ->
                        if (
                            profileState.installed &&
                            !runtime.alpineRuntime.isPackageProfileInstalled(profileId)
                        ) {
                            profileState.copy(
                                installed = false,
                                installedAtMillis = 0L,
                                lastError = "",
                            )
                        } else {
                            profileState
                        }
                    }
                }
                if (verifiedProfiles != settings.alpinePackageProfiles) {
                    settingsRepository.updateSettings(
                        settings.copy(alpinePackageProfiles = verifiedProfiles)
                    )
                    if (verifiedProfiles["chrome"]?.installed != true) {
                        _uiState.update { current ->
                            current.copy(
                                draftChromeEnabled = false,
                                sessions = current.sessions.map { it.copy(chromeEnabled = false) },
                            )
                        }
                        chatStateStore.updateAndFlush { persisted ->
                            persisted.copy(
                                sessions = persisted.sessions.map { it.copy(chromeEnabled = false) },
                            )
                        }
                    }
                }
                if (startPiIfReady) {
                    refreshPiCoreSetup()
                } else {
                    viewModelScope.launch { syncPiDiscoveredSkills() }
                }
            } else {
                _uiState.update {
                    it.copy(
                        piCoreSetupState = PiCoreSetupState(
                            detail = "Initialize Alpine before starting the agent runtime.",
                        )
                    )
                }
            }
        }
    }

    fun initializeAlpineRuntime(makeDefault: Boolean = true) {
        viewModelScope.launch {
            initializeAlpineRuntimeAfterReset(makeDefault)
        }
    }

    fun retryAlpineRuntimeSetup(makeDefault: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    alpineSetupState = LocalRuntimeSetupState(
                        runtimeId = LocalRuntimeId.Alpine,
                        issue = LocalRuntimeIssue.NotInstalled,
                    ),
                    piCoreSetupState = PiCoreSetupState(
                        isChecking = true,
                        phase = PiCoreSetupPhase.CheckingAlpine,
                        output = "Starting Alpine setup...\n",
                    ),
                )
            }
            val resetFailure = try {
                withContext(Dispatchers.IO) {
                    runtime.alpineChromeController.stop()
                    runtime.piKernelBridge.stop()
                    runtime.alpineRuntime.reset()
                }
                null
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                error
            }
            if (resetFailure != null) {
                val detail = resetFailure.userFacingMessage()
                _uiState.update { current ->
                    current.copy(
                        alpineSetupState = LocalRuntimeSetupState(
                            runtimeId = LocalRuntimeId.Alpine,
                            issue = LocalRuntimeIssue.Failed,
                            detail = detail,
                        ),
                        piCoreSetupState = current.piCoreSetupState.copy(
                            isChecking = false,
                            phase = PiCoreSetupPhase.Failed,
                            failedAtPhase = PiCoreSetupPhase.CheckingAlpine,
                            detail = detail,
                            output = appendSetupOutput(
                                current.piCoreSetupState.output,
                                "Setup failed: $detail\n",
                            ),
                        ),
                    )
                }
                emitTransientMessage(UiText.Raw(detail))
                return@launch
            }
            val settings = _uiState.value.settings
            val remainingRuntimeIds = settings.enabledRuntimeIds - LocalRuntimeId.Alpine
            settingsRepository.updateSettings(
                settings.copy(
                    alpineSetupCompleted = false,
                    alpinePackageProfiles = emptyMap(),
                    enabledRuntimeIds = remainingRuntimeIds,
                    defaultRuntimeId = if (settings.defaultRuntimeId == LocalRuntimeId.Alpine) {
                        remainingRuntimeIds.firstOrNull()
                    } else {
                        settings.defaultRuntimeId
                    },
                )
            )
            _uiState.update { current ->
                current.copy(
                    alpinePackageInstallProgress = emptyMap(),
                    draftChromeEnabled = false,
                    sessions = current.sessions.map { it.copy(chromeEnabled = false) },
                )
            }
            chatStateStore.updateAndFlush { persisted ->
                persisted.copy(
                    sessions = persisted.sessions.map { it.copy(chromeEnabled = false) },
                )
            }
            initializeAlpineRuntimeAfterReset(makeDefault)
        }
    }

    private suspend fun initializeAlpineRuntimeAfterReset(makeDefault: Boolean) {
        _uiState.update { current ->
            current.copy(
                piCoreSetupState = PiCoreSetupState(
                    isChecking = true,
                    phase = PiCoreSetupPhase.CheckingAlpine,
                    output = "Starting Alpine setup...\n",
                )
            )
        }
        val setupState = withContext(Dispatchers.IO) {
            runtime.alpineRuntime.initialize { progress ->
                applyPiCoreSetupUpdate(
                    PiCoreSetupUpdate(
                        phase = PiCoreSetupPhase.CheckingAlpine,
                        activity = when (progress.activity) {
                            AlpineSetupActivity.Extracting -> PiCoreSetupActivity.Extracting
                            AlpineSetupActivity.Downloading -> PiCoreSetupActivity.Downloading
                            AlpineSetupActivity.Installing -> PiCoreSetupActivity.None
                            AlpineSetupActivity.None -> PiCoreSetupActivity.None
                        },
                        bytesPerSecond = progress.bytesPerSecond,
                        output = progress.output,
                    )
                )
            }
        }
        if (setupState.isReady) {
            settingsRepository.updateSettings(
                _uiState.value.settings.withRuntimeEnabled(
                    runtimeId = LocalRuntimeId.Alpine,
                    makeDefault = makeDefault,
                )
            )
        }
        _uiState.update { current -> current.copy(alpineSetupState = setupState) }
        if (setupState.isReady) {
            _uiState.update { current ->
                current.copy(
                    piCoreSetupState = current.piCoreSetupState.copy(
                        isChecking = false,
                        activity = PiCoreSetupActivity.None,
                        bytesPerSecond = 0L,
                    ),
                )
            }
            refreshPiCoreSetup()
        } else {
            _uiState.update { current ->
                current.copy(
                    piCoreSetupState = current.piCoreSetupState.copy(
                        isChecking = false,
                        phase = PiCoreSetupPhase.Failed,
                        failedAtPhase = PiCoreSetupPhase.CheckingAlpine,
                        detail = setupState.detail,
                        activity = PiCoreSetupActivity.None,
                        bytesPerSecond = 0L,
                        output = appendSetupOutput(
                            current.piCoreSetupState.output,
                            "Setup failed: ${setupState.detail}\n",
                        ),
                    )
                )
            }
        }
        emitTransientMessage(UiText.Raw(setupState.detail.ifBlank { "Alpine runtime status refreshed." }))
    }

    private suspend fun refreshPiCoreSetup() {
        if (_uiState.value.piCoreSetupState.isChecking) return
        _uiState.update {
            it.copy(
                piCoreSetupState = PiCoreSetupState(
                    isChecking = true,
                    phase = PiCoreSetupPhase.CheckingAlpine,
                    output = it.piCoreSetupState.output.ifBlank { "Starting agent runtime setup...\n" },
                )
            )
        }
        runCatching {
            withContext(Dispatchers.IO) {
                runtime.piKernelBridge.ping(::applyPiCoreSetupUpdate)
            }
        }.fold(
            onSuccess = { payload ->
                _uiState.update {
                    it.copy(
                        piCoreSetupState = PiCoreSetupState(
                            isReady = true,
                            phase = PiCoreSetupPhase.Ready,
                            nodeVersion = payload.optString("node_version"),
                            bridgeVersion = payload.optString("bridge_version"),
                            output = appendSetupOutput(
                                it.piCoreSetupState.output,
                                "AI engine setup complete.\n",
                            ),
                        )
                    )
                }
                syncPiDiscoveredSkills()
            },
            onFailure = { throwable ->
                if (throwable is CancellationException) throw throwable
                _uiState.update { current ->
                    current.copy(
                        piCoreSetupState = PiCoreSetupState(
                            phase = PiCoreSetupPhase.Failed,
                            failedAtPhase = current.piCoreSetupState.phase,
                            detail = throwable.userFacingMessage(),
                            output = appendSetupOutput(
                                current.piCoreSetupState.output,
                                "Setup failed: ${throwable.userFacingMessage()}\n",
                            ),
                        )
                    )
                }
            },
        )
    }

    private suspend fun syncPiDiscoveredSkills() {
        runCatching {
            val response = piKernelBridge.listDiscoveredSkills()
            val skills = response.optJSONArray("skills") ?: return@runCatching
            val discovered = buildList {
                for (index in 0 until skills.length()) {
                    val item = skills.optJSONObject(index) ?: continue
                    val guestFilePath = item.optString("file_path").trim()
                    val guestBaseDir = item.optString("base_dir").trim()
                    if (guestFilePath.isBlank() || guestBaseDir.isBlank()) continue
                    val hostFile = runtime.alpineRuntime.resolveWorkspaceHostPath(guestFilePath)?.hostFile
                        ?: runtime.alpineRuntime.resolveGuestPath(guestFilePath)
                    val hostRoot = runtime.alpineRuntime.resolveWorkspaceHostPath(guestBaseDir)?.hostFile
                        ?: runtime.alpineRuntime.resolveGuestPath(guestBaseDir)
                    if (!hostFile.isFile || !hostRoot.isDirectory) continue
                    add(
                        PiDiscoveredSkillSource(
                            guestFilePath = guestFilePath,
                            guestBaseDir = guestBaseDir,
                            hostFile = hostFile,
                            hostRoot = hostRoot,
                        )
                    )
                }
            }
            skillManager.syncPiDiscoveredSkills(discovered).getOrThrow()
        }
    }

    fun resetAlpineRuntime() {
        viewModelScope.launch {
            val setupState = withContext(Dispatchers.IO) {
                runtime.alpineChromeController.stop()
                runtime.alpineRuntime.reset()
            }
            val settings = _uiState.value.settings
            settingsRepository.updateSettings(
                settings.copy(
                    enabledRuntimeIds = settings.enabledRuntimeIds - LocalRuntimeId.Alpine,
                    defaultRuntimeId = if (settings.defaultRuntimeId == LocalRuntimeId.Alpine) {
                        (settings.enabledRuntimeIds - LocalRuntimeId.Alpine).firstOrNull()
                    } else {
                        settings.defaultRuntimeId
                    },
                    alpineSetupCompleted = false,
                    alpinePackageProfiles = emptyMap(),
                )
            )
            _uiState.update { current ->
                current.copy(
                    alpineSetupState = setupState,
                    alpinePackageInstallProgress = emptyMap(),
                    draftChromeEnabled = false,
                    sessions = current.sessions.map { it.copy(chromeEnabled = false) },
                )
            }
            chatStateStore.updateAndFlush { persisted ->
                persisted.copy(
                    sessions = persisted.sessions.map { it.copy(chromeEnabled = false) },
                )
            }
        }
    }

    fun installAlpinePackageProfile(profileId: String) {
        if (_uiState.value.alpinePackageInstallProgress.containsKey(profileId)) return
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    alpinePackageInstallProgress = current.alpinePackageInstallProgress +
                        (
                            profileId to AlpineSetupProgress(
                                activity = AlpineSetupActivity.Downloading,
                            )
                            ),
                )
            }
            val installState = try {
                withContext(Dispatchers.IO) {
                    runtime.alpineRuntime.installPackageProfile(profileId) { progress ->
                        _uiState.update { current ->
                            val previous = current.alpinePackageInstallProgress[profileId]
                            val merged = progress.copy(
                                bytesPerSecond = progress.bytesPerSecond.takeIf { it > 0L }
                                    ?: previous?.bytesPerSecond
                                    ?: 0L,
                                progressPercent = progress.progressPercent
                                    ?: previous?.progressPercent,
                            )
                            current.copy(
                                alpinePackageInstallProgress =
                                    current.alpinePackageInstallProgress + (profileId to merged),
                            )
                        }
                    }
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                LocalRuntimeSetupState(
                    runtimeId = LocalRuntimeId.Alpine,
                    issue = LocalRuntimeIssue.Failed,
                    detail = throwable.userFacingMessage(),
                )
            } finally {
                _uiState.update { current ->
                    current.copy(
                        alpinePackageInstallProgress =
                            current.alpinePackageInstallProgress - profileId,
                    )
                }
            }
            val profileState = if (installState.isReady) {
                PackageProfileState(
                    profileId = profileId,
                    installed = true,
                    installedAtMillis = System.currentTimeMillis(),
                    lastError = "",
                )
            } else {
                PackageProfileState(
                    profileId = profileId,
                    installed = false,
                    installedAtMillis = 0L,
                    lastError = installState.detail.ifBlank { "Install failed." },
                )
            }
            val settings = _uiState.value.settings
            settingsRepository.updateSettings(
                settings.copy(
                    alpinePackageProfiles = settings.alpinePackageProfiles + (profileId to profileState),
                )
            )
            val runtimeState = withContext(Dispatchers.IO) {
                runtime.alpineRuntime.inspectSetup()
            }
            _uiState.update { current -> current.copy(alpineSetupState = runtimeState) }
            emitTransientMessage(UiText.Raw(installState.detail.ifBlank { "Alpine package profile updated." }))
        }
    }

    suspend fun createAlpineTerminalLaunchSpec(): Result<AlpineTerminalLaunchSpec> =
        withContext(Dispatchers.IO) {
            runCatching { runtime.alpineRuntime.createTerminalLaunchSpec() }
        }

    suspend fun startAlpineChrome(): Result<Unit> =
        runtime.alpineChromeController.startBrowser()

    suspend fun shouldShowAlpineChromeKeyboard(x: Int, y: Int): Result<Boolean> =
        runtime.alpineChromeController.shouldShowKeyboardAfterClick(x, y)

    fun setDefaultRuntime(runtimeId: LocalRuntimeId) {
        viewModelScope.launch {
            val settings = _uiState.value.settings
            settingsRepository.updateSettings(
                settings.copy(
                    enabledRuntimeIds = settings.enabledRuntimeIds + runtimeId,
                    defaultRuntimeId = runtimeId,
                )
            )
        }
    }

    fun refreshAgentModeAuthorization() {
        val settings = _uiState.value.settings
        refreshAgentModeAuthorization(
            enabled = settings.agentModeAuthorizationEnabled,
            method = settings.agentModeAuthorizationMethod,
        )
    }

    fun refreshAgentModeAuthorization(
        enabled: Boolean,
        method: AgentModeAuthorizationMethod,
    ) {
        viewModelScope.launch {
            agentModeController.refreshAuthorization(
                _uiState.value.settings.copy(
                    agentModeAuthorizationEnabled = enabled,
                    agentModeAuthorizationMethod = method,
                )
            )
        }
    }

    fun requestShizukuPermission() {
        _uiState.update { current ->
            current.copy(agentModeAuthorizationState = agentModeController.requestShizukuPermission())
        }
    }

    fun trackTermuxSetupStarted(source: String) {
        pendingTermuxSetupSource = source
        captureAnalyticsEvent(
            event = "termux setup started",
            properties = mapOf(
                "source" to source,
                "current_issue" to _uiState.value.termuxSetupState.issue.name.lowercase(),
                "is_ready" to _uiState.value.termuxSetupState.isReady,
            ),
        )
    }

    fun trackPermissionRequested(
        permission: String,
        source: String,
    ) {
        captureAnalyticsEvent(
            event = "permission requested",
            properties = mapOf(
                "permission" to permission,
                "source" to source,
            ),
        )
    }

    fun trackPermissionResult(
        permission: String,
        granted: Boolean,
        source: String,
        result: String = if (granted) "granted" else "denied",
    ) {
        captureAnalyticsEvent(
            event = "permission result",
            properties = mapOf(
                "permission" to permission,
                "source" to source,
                "granted" to granted,
                "result" to result,
            ),
        )
    }

    fun checkForUpdates() {
        checkForUpdates(manual = true, forceAvailable = false)
    }

    fun forceUpdateCheckForTesting() {
        checkForUpdates(manual = true, forceAvailable = true)
    }

    fun dismissUpdateAvailableDialog() {
        _uiState.update { current ->
            current.copy(
                appUpdate = current.appUpdate.copy(showAvailableDialog = false)
            )
        }
    }

    fun downloadAndInstallUpdate() {
        val release = _uiState.value.appUpdate.availableRelease ?: return
        if (_uiState.value.appUpdate.isDownloading) return

        _uiState.update { current ->
            current.copy(
                appUpdate = current.appUpdate.copy(
                    isDownloading = true,
                    downloadProgress = null,
                )
            )
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    appUpdateManager.downloadApk(release) { progress ->
                        _uiState.update { current ->
                            current.copy(
                                appUpdate = current.appUpdate.copy(downloadProgress = progress)
                            )
                        }
                    }
                }
            }
            result
                .onSuccess { installUri ->
                    _uiState.update { current ->
                        current.copy(
                            appUpdate = current.appUpdate.copy(
                                isDownloading = false,
                                downloadProgress = null,
                                pendingInstallUri = installUri.toString(),
                                showAvailableDialog = false,
                            )
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { current ->
                        current.copy(
                            appUpdate = current.appUpdate.copy(
                                isDownloading = false,
                                downloadProgress = null,
                            )
                        )
                    }
                    emitTransientMessage(uiString(R.string.message_update_download_failed, throwable.userFacingMessage()))
                }
        }
    }

    fun consumePendingUpdateInstallUri() {
        _uiState.update { current ->
            current.copy(
                appUpdate = current.appUpdate.copy(pendingInstallUri = "")
            )
        }
    }

    fun stopAgentModeDisplay() {
        agentModeController.stopDisplay()
    }

    fun refreshAgentModeDisplays() {
        refreshAgentModeDisplays(_uiState.value.settings.agentModeAuthorizationMethod)
    }

    fun refreshAgentModeDisplays(method: AgentModeAuthorizationMethod) {
        viewModelScope.launch {
            agentModeController.refreshDisplays(
                _uiState.value.settings.copy(agentModeAuthorizationMethod = method)
            )
        }
    }

    fun attachAgentModePreviewSurface(surface: Surface) {
        viewModelScope.launch {
            agentModeController.attachPreviewSurface(_uiState.value.settings, surface)
        }
    }

    fun detachAgentModePreviewSurface(surface: Surface) {
        viewModelScope.launch {
            agentModeController.detachPreviewSurface(_uiState.value.settings, surface)
        }
    }

    fun updateDraftInput(value: String) {
        _uiState.update { current ->
            current.copy(
                draftInput = value,
                showStarterPromptHint = if (
                    current.showStarterPromptHint && value != current.draftInput
                ) {
                    false
                } else {
                    current.showStarterPromptHint
                },
            )
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            settingsRepository.updateOnboardingSeenVersion(CurrentOnboardingVersion)
            _uiState.update { current ->
                current.copy(
                    currentScreen = AppScreen.Chat,
                    isStartupRouteResolved = true,
                    isOnboardingReplay = false,
                    onboardingStep = OnboardingStep.Landing,
                    onboardingReturnScreen = AppScreen.Chat,
                )
            }
        }
    }

    fun openOnboardingFromSettings() {
        _uiState.update { current ->
            current.copy(
                currentScreen = AppScreen.Onboarding,
                isOnboardingReplay = true,
                onboardingStep = OnboardingStep.Landing,
                onboardingReturnScreen = AppScreen.Settings,
            )
        }
    }

    fun openFollowUpOnboardingFromSettings() {
        _uiState.update { current ->
            current.copy(
                currentScreen = AppScreen.Onboarding,
                isOnboardingReplay = true,
                onboardingStep = OnboardingStep.LocalRuntimeChoice,
                onboardingReturnScreen = AppScreen.Settings,
                awaitingFollowUpTour = false,
                showFollowUpTourCard = false,
            )
        }
    }

    fun openDeveloperAlpineSetupPreview() {
        developerAlpineSetupPreviewJob?.cancel()
        _uiState.update { current ->
            current.copy(
                currentScreen = AppScreen.Onboarding,
                isOnboardingReplay = true,
                onboardingStep = OnboardingStep.AlpineSetup,
                onboardingReturnScreen = AppScreen.Settings,
                developerAlpineSetupPreviewState = PiCoreSetupState(),
            )
        }
        restartDeveloperAlpineSetupPreview()
    }

    fun restartDeveloperAlpineSetupPreview() {
        developerAlpineSetupPreviewJob?.cancel()
        _uiState.update { current ->
            if (current.developerAlpineSetupPreviewState == null) {
                current
            } else {
                current.copy(developerAlpineSetupPreviewState = PiCoreSetupState())
            }
        }
        developerAlpineSetupPreviewJob = viewModelScope.launch {
            fun update(
                phase: PiCoreSetupPhase,
                activity: PiCoreSetupActivity = PiCoreSetupActivity.None,
                bytesPerSecond: Long = 0L,
                output: String = "",
            ) {
                _uiState.update { current ->
                    val preview = current.developerAlpineSetupPreviewState ?: return@update current
                    current.copy(
                        developerAlpineSetupPreviewState = preview.copy(
                            isChecking = phase != PiCoreSetupPhase.Ready && phase != PiCoreSetupPhase.Failed,
                            isReady = phase == PiCoreSetupPhase.Ready,
                            phase = phase,
                            activity = activity,
                            bytesPerSecond = bytesPerSecond,
                            output = appendSetupOutput(preview.output, output),
                            nodeVersion = if (phase == PiCoreSetupPhase.Ready) "22.21.1" else preview.nodeVersion,
                            bridgeVersion = if (phase == PiCoreSetupPhase.Ready) "2.0.0-alpha.0" else preview.bridgeVersion,
                        )
                    )
                }
            }

            update(PiCoreSetupPhase.CheckingAlpine, output = "Starting Alpine setup preview...\n")
            delay(700L)
            update(
                phase = PiCoreSetupPhase.CheckingAlpine,
                activity = PiCoreSetupActivity.Extracting,
                bytesPerSecond = 18L * 1024L * 1024L,
                output = "Preparing Alpine runtime files...\n",
            )
            delay(900L)
            val extractionEntries = listOf(
                "bin/busybox",
                "etc/alpine-release",
                "usr/lib/libcrypto.so.3",
                "usr/bin/env",
                "var/lib/apk/world",
            )
            extractionEntries.forEachIndexed { index, path ->
                update(
                    phase = PiCoreSetupPhase.CheckingAlpine,
                    activity = PiCoreSetupActivity.Extracting,
                    bytesPerSecond = (18L + index * 3L) * 1024L * 1024L,
                    output = "Extracting $path\n",
                )
                delay(1_600L)
            }
            update(
                phase = PiCoreSetupPhase.CheckingNode,
                output = "Alpine root filesystem ready.\nChecking node --version...\n",
            )
            delay(1_000L)
            val apkLines = listOf(
                "\$ apk add --no-cache --no-chown nodejs npm",
                "fetch https://dl-cdn.alpinelinux.org/alpine/v3.23/main/aarch64/APKINDEX.tar.gz",
                "(1/8) Installing ca-certificates",
                "(2/8) Installing libuv",
                "(7/8) Installing nodejs",
                "(8/8) Installing npm",
                "OK: 94 MiB in 32 packages",
            )
            apkLines.forEachIndexed { index, line ->
                update(
                    phase = PiCoreSetupPhase.InstallingNode,
                    activity = PiCoreSetupActivity.Downloading,
                    bytesPerSecond = (2_400L + index * 370L) * 1024L,
                    output = "$line\n",
                )
                delay(1_400L)
            }
            update(PiCoreSetupPhase.PreparingBridge, output = "Preparing the AI engine bridge...\n")
            delay(900L)
            update(PiCoreSetupPhase.StartingBridge, output = "Starting node bridge.mjs...\n")
            delay(900L)
            update(PiCoreSetupPhase.VerifyingBridge, output = "Verifying bridge response...\n")
            delay(900L)
            update(PiCoreSetupPhase.Ready, output = "AI engine setup complete.\n")
        }
    }

    fun resumeOnboarding() {
        _uiState.update { current ->
            current.copy(
                currentScreen = AppScreen.Onboarding,
                isOnboardingReplay = true,
                onboardingStep = OnboardingStep.ProviderSetup,
                onboardingReturnScreen = AppScreen.Chat,
            )
        }
    }

    fun closeOnboarding() {
        developerAlpineSetupPreviewJob?.cancel()
        developerAlpineSetupPreviewJob = null
        _uiState.update { current ->
            current.copy(
                currentScreen = current.onboardingReturnScreen,
                isOnboardingReplay = false,
                onboardingStep = OnboardingStep.Landing,
                onboardingReturnScreen = AppScreen.Chat,
                developerAlpineSetupPreviewState = null,
            )
        }
    }

    private fun applyPiCoreSetupUpdate(update: PiCoreSetupUpdate) {
        _uiState.update { current ->
            current.copy(
                piCoreSetupState = current.piCoreSetupState.copy(
                    isChecking = true,
                    phase = update.phase,
                    activity = update.activity,
                    bytesPerSecond = update.bytesPerSecond,
                    output = appendSetupOutput(current.piCoreSetupState.output, update.output),
                )
            )
        }
    }

    private fun appendSetupOutput(
        current: String,
        addition: String,
    ): String {
        if (addition.isEmpty()) return current
        val combined = current + addition
        return if (combined.length <= MaxSetupOutputChars) {
            combined
        } else {
            combined.takeLast(MaxSetupOutputChars)
        }
    }

    fun completeFollowUpOnboarding() {
        captureAnalyticsEvent(
            event = "onboarding follow up completed",
            properties = mapOf(
                "section" to "follow_up",
                "source" to if (_uiState.value.isOnboardingReplay) "replay" else "auto",
                "termux_ready" to _uiState.value.termuxSetupState.isReady,
                "agent_mode_authorized" to _uiState.value.agentModeAuthorizationState.isReady,
                "skill_count" to _uiState.value.installedSkills.size,
                "mcp_server_count" to _uiState.value.mcpServers.size,
            ),
        )
        closeOnboarding()
    }

    fun completeOnboardingProviderSetup(config: LlmProviderConfig) {
        viewModelScope.launch {
            val enabledConfig = config.copy(isEnabled = true)
            settingsRepository.upsertProviderConfig(enabledConfig)
            settingsRepository.setProviderEnabled(enabledConfig.id, true)
            val updatedSettings = _uiState.value.settings.withExplicitDefaultChatModel(enabledConfig)
            val defaultModelKey = updatedSettings.defaultChatModelKey
            settingsRepository.updateSettings(updatedSettings)
            settingsRepository.updateDefaultModelKeys(
                updatedSettings.defaultChatModelKey,
                updatedSettings.defaultTitleModelKey,
                updatedSettings.defaultNamingModelKey,
                updatedSettings.defaultCompactingModelKey,
            )
            settingsRepository.updateOnboardingSeenVersion(CurrentOnboardingVersion)
            _uiState.update { current ->
                current.copy(
                    currentScreen = AppScreen.Chat,
                    isStartupRouteResolved = true,
                    isOnboardingReplay = false,
                    onboardingStep = OnboardingStep.Landing,
                    onboardingReturnScreen = AppScreen.Chat,
                    currentSessionId = DraftSessionId,
                    draftInput = OnboardingStarterPrompt,
                    draftAttachments = emptyList(),
                    draftSelectedModelKey = defaultModelKey,
                    draftSelectedSkillIds = emptyList(),
                    draftSelectedMcpServerIds = emptyList(),
                    draftConversationMode = ConversationMode.Chat,
                    draftAgentModeEnabled = false,
                    draftChromeEnabled = false,
                    draftWorkspaceId = null,
                    editingSessionId = null,
                    editingMessageId = null,
                    showStarterPromptHint = true,
                    awaitingFollowUpTour = true,
                    showFollowUpTourCard = false,
                )
            }
            persistCurrentSessionId(DraftSessionId)
            captureAnalyticsEvent(
                event = "onboarding completed",
                properties = mapOf(
                    "section" to "initial",
                    "provider" to com.zhousl.aether.data.PiProviderCatalog
                        .resolve(enabledConfig.piProviderId).displayName,
                    "provider_id" to enabledConfig.id,
                ),
            )
            captureAnalyticsEvent(
                event = "onboarding initial completed",
                properties = mapOf(
                    "section" to "initial",
                    "provider" to com.zhousl.aether.data.PiProviderCatalog
                        .resolve(enabledConfig.piProviderId).displayName,
                    "provider_id" to enabledConfig.id,
                ),
            )
        }
    }

    fun dismissStarterPromptHint() {
        _uiState.update { current -> current.copy(showStarterPromptHint = false) }
    }

    fun dismissTermuxSetupNotice() {
        viewModelScope.launch {
            settingsRepository.updateSettings(
                _uiState.value.settings.copy(termuxSetupNoticeDismissed = true)
            )
        }
    }

    fun openFollowUpTour() {
        _uiState.update { current ->
            current.copy(
                currentScreen = AppScreen.Onboarding,
                isOnboardingReplay = true,
                onboardingStep = OnboardingStep.LocalRuntimeChoice,
                onboardingReturnScreen = AppScreen.Chat,
                awaitingFollowUpTour = false,
                showFollowUpTourCard = false,
            )
        }
    }

    fun saveOnboardingAgentModeAuthorization(
        enabled: Boolean,
        method: AgentModeAuthorizationMethod,
    ) {
        viewModelScope.launch {
            settingsRepository.updateAgentModeAuthorization(enabled, method)
        }
    }

    fun appendDraftAttachments(uris: List<Uri>) {
        if (uris.isEmpty()) return

        val targetSessionId = ensureDraftWorkspaceId()

        viewModelScope.launch {
            val pendingAttachments = withContext(Dispatchers.IO) {
                uris
                    .mapNotNull { uri -> buildPendingDraftAttachment(uri) }
                    .distinctBy { it.uri }
                    .toList()
            }
            if (pendingAttachments.isEmpty()) return@launch

            var attachmentsToImport = emptyList<ChatAttachment>()
            _uiState.update { current ->
                val newAttachments = pendingAttachments.filterNot { candidate ->
                    current.draftAttachments.any { existing -> existing.uri == candidate.uri }
                }
                attachmentsToImport = newAttachments
                if (newAttachments.isEmpty()) {
                    current
                } else {
                    current.copy(
                        draftAttachments = current.draftAttachments + newAttachments
                    )
                }
            }

            attachmentsToImport.forEach { attachment ->
                launch(Dispatchers.IO) {
                    importDraftAttachmentToWorkspace(
                        attachment = attachment,
                        sessionId = targetSessionId,
                    )
                }
            }
        }
    }

    fun removeDraftAttachment(attachmentId: String) {
        _uiState.update { current ->
            current.copy(
                draftAttachments = current.draftAttachments.filterNot { it.id == attachmentId }
            )
        }
    }

    fun pauseGeneration() {
        val snapshot = _uiState.value
        val sessionId = sequenceOf(
            snapshot.currentSessionId,
            snapshot.pendingResponseSessionId,
        )
            .filterNotNull()
            .firstOrNull(sessionExecutionManager::isSessionRunning)
            ?: return
        val finalizedSession = sessionExecutionManager.pauseSession(sessionId) ?: return
        val executionStates = sessionExecutionManager.executionStates.value
        _uiState.update { current -> current.withFinalizedPausedSession(finalizedSession, executionStates) }
    }

    fun openSettings() {
        _uiState.update { it.copy(currentScreen = AppScreen.Settings) }
    }

    fun refreshUsageStatisticsSnapshots() {
        viewModelScope.launch {
            val snapshots = withContext(Dispatchers.IO) {
                runtime.chatRepository.getUsageStatisticsSnapshot()
            }
            _uiState.update { it.copy(usageStatisticsSnapshots = snapshots) }
        }
    }

    fun closeSettings() {
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.Chat,
                rootSetupProgressReturnPage = null,
            )
        }
    }

    fun startNewChat() {
        val snapshot = _uiState.value
        val defaultSkillIds = enabledDefaultSkillIds(snapshot)
        val operation = "chat.new"
        if (
            !modKernel.operations.hasInterceptors(operation) &&
            "operation:$operation" !in aetherAppExtensionManager.state.value.snapshot.eventNames
        ) {
            startNewChatNow(defaultSkillIds)
            return
        }
        viewModelScope.launch {
            val result = dispatchAetherOperation(
                operation = operation,
                payload = JSONObject().put(
                    "selected_skill_ids",
                    JSONArray(defaultSkillIds),
                ),
            )
            if (result.cancelled) return@launch
            val selectedSkillIds = if (result.payload.has("selected_skill_ids")) {
                result.payload.optJSONArray("selected_skill_ids").toStringList()
            } else {
                defaultSkillIds
            }
            withContext(Dispatchers.Main.immediate) {
                startNewChatNow(selectedSkillIds)
            }
        }
    }

    private fun startNewChatNow(
        selectedSkillIds: List<String>,
    ) {
        _uiState.update {
            val inheritedModelKey = currentConversationModelKey(it)
            val enabledSkillIds = it.installedSkills
                .filter(InstalledSkill::isEnabled)
                .map(InstalledSkill::id)
                .toSet()
            it.copy(
                currentScreen = AppScreen.Chat,
                currentSessionId = DraftSessionId,
                draftInput = "",
                draftAttachments = emptyList(),
                draftSelectedModelKey = inheritedModelKey,
                draftSelectedSkillIds = selectedSkillIds.filter(enabledSkillIds::contains),
                draftSelectedMcpServerIds = emptyList(),
                draftConversationMode = ConversationMode.Chat,
                draftAgentModeEnabled = false,
                draftChromeEnabled = false,
                draftWorkspaceId = null,
                editingSessionId = null,
                editingMessageId = null,
                unviewedCompletedSessionIds = it.unviewedCompletedSessionIds - DraftSessionId,
                showStarterPromptHint = false,
            )
        }
        persistCurrentSessionId(DraftSessionId)
        captureAnalyticsEvent(event = "conversation started")
    }

    private fun initializeStartupDraftModelIfReady() {
        if (didInitializeStartupDraftModel) return
        var sessionToPersist: Pair<String, String>? = null
        _uiState.update { current ->
            val options = current.providerConfigs.availableModelOptions()
            if (
                !current.isStartupRouteResolved ||
                !didReceiveInitialChatState ||
                options.isEmpty()
            ) return@update current
            didInitializeStartupDraftModel = true
            val defaultModelKey = resolveDefaultChatModelKey(current.settings, current.providerConfigs)
            if (current.currentSessionId == DraftSessionId) {
                current.copy(draftSelectedModelKey = defaultModelKey)
            } else {
                sessionToPersist = current.currentSessionId to defaultModelKey
                current.copy(
                    sessions = current.sessions.map { session ->
                        if (session.id == current.currentSessionId) {
                            session.copy(selectedModelKey = defaultModelKey)
                        } else {
                            session
                        }
                    },
                )
            }
        }
        sessionToPersist?.let { (sessionId, defaultModelKey) ->
            persistSessionMutation(sessionId) { session ->
                if (session.selectedModelKey == defaultModelKey) null
                else session.copy(selectedModelKey = defaultModelKey)
            }
        }
    }

    private fun initializeStartupDraftSkillsIfReady() {
        if (didInitializeStartupDraftSkills) return
        _uiState.update { current ->
            if (!current.isStartupRouteResolved) return@update current
            if (current.currentSessionId != DraftSessionId) {
                didInitializeStartupDraftSkills = true
                return@update current
            }
            if (current.draftSelectedSkillIds.isNotEmpty()) {
                didInitializeStartupDraftSkills = true
                return@update current
            }
            val enabledIds = current.installedSkills
                .filter(InstalledSkill::isEnabled)
                .map(InstalledSkill::id)
                .toSet()
            val selectedDefaults = current.settings.defaultSelectedSkillIds
                .filter(enabledIds::contains)
            if (
                current.settings.defaultSelectedSkillIds.isNotEmpty() &&
                selectedDefaults.isEmpty()
            ) {
                return@update current
            }
            didInitializeStartupDraftSkills = true
            current.copy(draftSelectedSkillIds = selectedDefaults)
        }
    }

    private fun currentConversationModelKey(state: AetherUiState): String {
        val options = state.providerConfigs.availableModelOptions()
        val selectedKey = state.sessions
            .firstOrNull { it.id == state.currentSessionId }
            ?.selectedModelKey
            ?: state.draftSelectedModelKey
        return selectedKey.takeIf { key -> options.any { it.key == key } }
            ?: resolveDefaultChatModelKey(state.settings, state.providerConfigs)
    }

    fun selectSession(sessionId: String) {
        selectSessionJob?.cancel()
        _uiState.update { current ->
            current.copy(
                currentScreen = AppScreen.Chat,
                sessions = current.sessions.withMessagesOnlyForSession(sessionId),
                currentSessionId = sessionId,
                draftInput = "",
                draftAttachments = emptyList(),
                draftSelectedModelKey = "",
                draftSelectedSkillIds = emptyList(),
                draftSelectedMcpServerIds = emptyList(),
                draftConversationMode = ConversationMode.Chat,
                draftAgentModeEnabled = false,
                draftChromeEnabled = false,
                draftWorkspaceId = null,
                editingSessionId = null,
                editingMessageId = null,
                unviewedCompletedSessionIds = current.unviewedCompletedSessionIds - sessionId,
                showStarterPromptHint = false,
            )
        }
        selectSessionJob = viewModelScope.launch {
            val loadedSession = if (sessionId == DraftSessionId) {
                null
            } else {
                runCatching {
                    withContext(Dispatchers.IO) {
                        runtime.chatRepository.getSessionWithMessages(sessionId)
                    }
                }.getOrElse { throwable ->
                    if (throwable is CancellationException) throw throwable
                    diagnosticLogger.exception(
                        category = "storage",
                        event = "session_selection_hydration_failed",
                        throwable = throwable,
                        level = "warn",
                        sessionId = sessionId,
                    )
                    null
                }
            }
            loadedSession?.let { session ->
                _uiState.update { current ->
                    if (current.currentSessionId != sessionId) {
                        current
                    } else {
                        current.copy(
                            sessions = replaceOrPrependSession(current.sessions, session)
                                .withMessagesOnlyForSession(sessionId),
                        )
                    }
                }
            }
            persistSessionSelection(sessionId, loadedSession)
        }
    }

    fun renameSession(
        sessionId: String,
        title: String,
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) return
        updateSession(sessionId) { session ->
            if (session.title == trimmedTitle && session.hasCustomTitle) {
                null
            } else {
                session.copy(
                    title = trimmedTitle.take(80),
                    hasCustomTitle = true,
                )
            }
        }
    }

    fun deleteSession(sessionId: String) {
        if (sessionExecutionManager.isSessionRunning(sessionId)) {
            emitTransientMessage(uiString(R.string.message_pause_before_deleting_session))
            return
        }

        var didUpdate = false
        _uiState.update { current ->
            val session = current.sessions.firstOrNull { it.id == sessionId } ?: return@update current
            val updatedSessions = current.sessions.filterNot { it.id == sessionId }
            if (updatedSessions.size == current.sessions.size) return@update current
            didUpdate = true
            current.copy(
                sessions = updatedSessions,
                currentSessionId = if (current.currentSessionId == sessionId) DraftSessionId else current.currentSessionId,
                draftInput = if (current.editingSessionId == sessionId) "" else current.draftInput,
                draftAttachments = if (current.editingSessionId == sessionId) emptyList() else current.draftAttachments,
                draftWorkspaceId = if (current.editingSessionId == sessionId) null else current.draftWorkspaceId,
                editingSessionId = if (current.editingSessionId == sessionId) null else current.editingSessionId,
                editingMessageId = if (current.editingSessionId == sessionId) null else current.editingMessageId,
                unviewedCompletedSessionIds = current.unviewedCompletedSessionIds - sessionId,
                showStarterPromptHint = false,
            )
        }
        if (didUpdate) {
            viewModelScope.launch {
                chatStateStore.flush()
                val sharedWorkspaceFilePaths = withContext(Dispatchers.IO) {
                    runCatching {
                        runtime.chatRepository.getUnreferencedWorkspaceFilePathsForDeletedSession(sessionId)
                    }.getOrElse { throwable ->
                        diagnosticLogger.exception(
                            category = "storage",
                            event = "unreferenced_workspace_lookup_failed",
                            throwable = throwable,
                            level = "warn",
                            sessionId = sessionId,
                            details = mapOf("lookup_scope" to "session"),
                        )
                        emptyList()
                    }
                }
                persistDeleteSession(
                    sessionId = sessionId,
                    sharedWorkspaceFilePaths = sharedWorkspaceFilePaths,
                )
                captureAnalyticsEvent(event = "conversation deleted")
            }
        }
    }

    fun exportSessionToUri(
        sessionId: String,
        destinationUri: Uri,
    ) {
        viewModelScope.launch {
            val didExport = withContext(Dispatchers.IO) {
                val session = runtime.chatRepository.getSessionWithMessages(sessionId) ?: return@withContext false
                val piSession = runCatching { piKernelBridge.exportSessionJsonl(sessionId) }.getOrNull()
                val piPath = piSession?.optString("exported_path").orEmpty()
                val piJsonl = piPath.takeIf(String::isNotBlank)
                    ?.let { path -> runCatching { java.io.File(path).readText(Charsets.UTF_8) }.getOrNull() }
                writeTextToUri(
                    uri = destinationUri,
                    text = JSONObject().apply {
                        put("schemaVersion", 1)
                        put("exportType", "session")
                        put("exportedAtMillis", System.currentTimeMillis())
                        put("session", session.copy(messages = syncActiveBranches(session.messages)).toJson())
                        put("piSession", JSONObject().apply {
                            put("sessionId", sessionId)
                            put("jsonlPath", piPath)
                            put("jsonl", piJsonl ?: "")
                        })
                    }.toString(2),
                )
            }
            emitTransientMessage(uiString(if (didExport) R.string.message_session_exported else R.string.message_session_export_failed))
        }
    }

    fun exportAllDataToUri(destinationUri: Uri) {
        val snapshot = _uiState.value
        viewModelScope.launch {
            val didExport = withContext(Dispatchers.IO) {
                val sessions = runtime.chatRepository.getSessionsWithMessages()
                writeTextToUri(
                    uri = destinationUri,
                    text = buildFullAppExportJson(snapshot, sessions).toString(2),
                )
            }
            emitTransientMessage(uiString(if (didExport) R.string.message_app_data_exported else R.string.message_app_data_export_failed))
        }
    }

    fun exportLogsToUri(destinationUri: Uri) {
        val snapshot = _uiState.value
        viewModelScope.launch {
            diagnosticLogger.event(
                category = "export",
                event = "diagnostic_export_start",
                details = mapOf(
                    "screen" to snapshot.currentScreen.name,
                    "session_count" to snapshot.sessions.size,
                ),
            )
            val didExport = withContext(Dispatchers.IO) {
                writeTextToUri(
                    uri = destinationUri,
                    text = buildDiagnosticLogText(snapshot),
                )
            }
            diagnosticLogger.event(
                category = "export",
                event = if (didExport) "diagnostic_export_end" else "diagnostic_export_failed",
                level = if (didExport) "info" else "warn",
            )
            emitTransientMessage(uiString(if (didExport) R.string.message_logs_exported else R.string.message_logs_export_failed))
        }
    }

    fun importAllDataFromUri(sourceUri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val rawValue = readTextFromUri(sourceUri)
                    val json = JSONObject(rawValue)
                    val importedSkills = skillManager.importSkillBundles(json.optJSONArray("skillBundles"))
                    json.optJSONObject("extensionArchive")?.let { archive ->
                        piExtensionManager.restoreArchive(archive)
                    }
                    parseFullAppImport(json, importedSkills)
                }
            }
            result
                .onSuccess { imported ->
                    imported.piSessions.forEach { (sessionId, jsonl) ->
                        runCatching {
                            val importedPi = piKernelBridge.importSessionJsonl(sessionId, jsonl)
                            val path = importedPi.optString("session_file")
                            runtime.chatRepository.upsertAgentSessionMetadata(
                                chatSessionId = sessionId,
                                piSessionId = sessionId,
                                jsonlPath = path,
                                runtime = _uiState.value.settings.defaultRuntimeId?.storageValue.orEmpty(),
                                migrationVersion = 2,
                            )
                        }.onFailure { throwable ->
                            diagnosticLogger.exception(
                                category = "pi_bridge",
                                event = "import_session_jsonl_failed",
                                throwable = throwable,
                                level = "warn",
                                sessionId = sessionId,
                            )
                        }
                    }
                    settingsRepository.replaceImportedSettings(
                        settings = imported.settings,
                        providerConfigs = imported.providerConfigs,
                    )
                    extensionsRepository.updateInstalledSkills(imported.installedSkills)
                    chatStateStore.updateAndFlush(
                        writeIntent = PersistedChatWriteIntent.ReplaceFromImport,
                    ) {
                        it.copy(
                            sessions = imported.sessions.map { session ->
                                session.copy(activeMcpServerIds = emptyList())
                            },
                            currentSessionId = imported.currentSessionId,
                        )
                    }
                    _uiState.update { current ->
                        current.copy(
                            sessions = imported.sessions.map { session ->
                                session.copy(activeMcpServerIds = emptyList())
                            },
                            currentSessionId = imported.currentSessionId,
                            draftInput = "",
                            draftAttachments = emptyList(),
                            draftSelectedModelKey = "",
                            draftSelectedSkillIds = emptyList(),
                            draftSelectedMcpServerIds = emptyList(),
                            draftConversationMode = ConversationMode.Chat,
                            draftAgentModeEnabled = false,
                            draftChromeEnabled = false,
                            draftWorkspaceId = null,
                            editingSessionId = null,
                            editingMessageId = null,
                            unviewedCompletedSessionIds = emptySet(),
                            mcpServers = emptyList(),
                        )
                    }
                    refreshPiExtensionState(loadCatalog = false)
                    emitTransientMessage(uiString(R.string.message_app_data_imported))
                }
                .onFailure { throwable ->
                    emitTransientMessage(uiString(R.string.message_app_data_import_failed, throwable.userFacingMessage()))
                }
        }
    }

    fun startEditingUserMessage(
        sessionId: String,
        messageId: String,
    ) {
        if (sessionExecutionManager.isSessionRunning(sessionId)) return

        val session = _uiState.value.sessions.firstOrNull { it.id == sessionId } ?: return
        val message = session.messages.firstOrNull {
            it.id == messageId && it.author == MessageAuthor.User
        } ?: return

        _uiState.update {
            it.copy(
                currentScreen = AppScreen.Chat,
                currentSessionId = sessionId,
                draftInput = message.text,
                draftAttachments = message.attachments.map(::normalizeDraftAttachmentForEditing),
                draftSelectedModelKey = if (sessionId == DraftSessionId) {
                    it.draftSelectedModelKey.ifBlank {
                        resolveDefaultChatModelKey(it.settings, it.providerConfigs)
                    }
                } else {
                    it.draftSelectedModelKey
                },
                draftWorkspaceId = sessionId,
                editingSessionId = sessionId,
                editingMessageId = messageId,
                showStarterPromptHint = false,
            )
        }
        persistCurrentSessionId(sessionId)
    }

    fun cancelMessageEdit() {
        _uiState.update {
            it.copy(
                draftInput = "",
                draftAttachments = emptyList(),
                draftSelectedModelKey = if (it.currentSessionId == DraftSessionId) {
                    it.draftSelectedModelKey.ifBlank {
                        resolveDefaultChatModelKey(it.settings, it.providerConfigs)
                    }
                } else {
                    it.draftSelectedModelKey
                },
                draftSelectedSkillIds = emptyList(),
                draftSelectedMcpServerIds = emptyList(),
                draftConversationMode = ConversationMode.Chat,
                draftAgentModeEnabled = false,
                draftChromeEnabled = false,
                draftWorkspaceId = null,
                editingSessionId = null,
                editingMessageId = null,
                showStarterPromptHint = false,
            )
        }
    }

    fun deleteMessage(
        sessionId: String,
        messageId: String,
    ) {
        if (sessionExecutionManager.isSessionRunning(sessionId)) return

        var didUpdate = false
        var updatedSessionForPersistence: ChatSession? = null
        var removedSessionForPersistence = false
        var removedMessageIds = emptyList<String>()

        _uiState.update { current ->
            val sessionIndex = current.sessions.indexOfFirst { it.id == sessionId }
            if (sessionIndex < 0) return@update current

            val session = current.sessions[sessionIndex]
            val messageIndex = session.messages.indexOfFirst { it.id == messageId }
            if (messageIndex < 0) return@update current

            val trimFromIndex = session.messages.resolveConversationTrimIndex(messageIndex)
            val trimmedMessages = session.messages.take(trimFromIndex)
            val removedMessages = session.messages.drop(trimFromIndex)
            removedMessageIds = removedMessages.map { it.id }
            val updatedSessions = current.sessions.toMutableList().apply {
                removeAt(sessionIndex)
                if (trimmedMessages.isNotEmpty()) {
                    val updatedSession = session.withMessages(trimmedMessages)
                    updatedSessionForPersistence = updatedSession
                    add(sessionIndex.coerceAtMost(size), updatedSession)
                } else {
                    removedSessionForPersistence = true
                }
            }

            didUpdate = true
            current.copy(
                sessions = updatedSessions,
                currentSessionId = when {
                    trimmedMessages.isEmpty() && current.currentSessionId == sessionId -> DraftSessionId
                    else -> current.currentSessionId
                },
                draftSelectedSkillIds = if (
                    trimmedMessages.isEmpty() && current.currentSessionId == sessionId
                ) {
                    emptyList()
                } else {
                    current.draftSelectedSkillIds
                },
                draftSelectedMcpServerIds = if (
                    trimmedMessages.isEmpty() && current.currentSessionId == sessionId
                ) {
                    emptyList()
                } else {
                    current.draftSelectedMcpServerIds
                },
                draftInput = if (current.editingSessionId == sessionId) "" else current.draftInput,
                draftAttachments = if (current.editingSessionId == sessionId) {
                    emptyList()
                } else {
                    current.draftAttachments
                },
                draftWorkspaceId = if (current.editingSessionId == sessionId) null else current.draftWorkspaceId,
                editingSessionId = if (current.editingSessionId == sessionId) null else current.editingSessionId,
                editingMessageId = if (current.editingSessionId == sessionId) null else current.editingMessageId,
                pendingResponseSessionId = if (current.pendingResponseSessionId == sessionId) null else current.pendingResponseSessionId,
                pendingToolInvocations = if (current.pendingResponseSessionId == sessionId) {
                    emptyList()
                } else {
                    current.pendingToolInvocations
                },
            )
        }

        if (didUpdate) {
            viewModelScope.launch {
                chatStateStore.flush()
                val sharedWorkspaceFilePaths = withContext(Dispatchers.IO) {
                    runCatching {
                        runtime.chatRepository.getUnreferencedWorkspaceFilePathsForDeletedMessages(
                            sessionId = sessionId,
                            messageIds = removedMessageIds,
                        )
                    }.getOrElse { throwable ->
                        diagnosticLogger.exception(
                            category = "storage",
                            event = "unreferenced_workspace_lookup_failed",
                            throwable = throwable,
                            level = "warn",
                            sessionId = sessionId,
                            details = mapOf(
                                "lookup_scope" to "messages",
                                "message_count" to removedMessageIds.size,
                            ),
                        )
                        emptyList()
                    }
                }
                if (removedSessionForPersistence) {
                    persistDeleteSession(
                        sessionId = sessionId,
                        sharedWorkspaceFilePaths = sharedWorkspaceFilePaths,
                    )
                } else {
                    updatedSessionForPersistence?.let { persistSessionSnapshotAndFlush(it) }
                    if (sharedWorkspaceFilePaths.isNotEmpty()) {
                        scheduleSessionRuntimeDataCleanup(
                            sessionIds = emptyList(),
                            sharedWorkspaceFilePaths = sharedWorkspaceFilePaths,
                            mode = _uiState.value.settings.agentWorkspaceMode,
                        )
                    }
                }
            }
        }
    }

    fun redoAgentMessage(
        sessionId: String,
        messageId: String,
    ) {
        if (sessionExecutionManager.isSessionRunning(sessionId)) return

        val snapshot = _uiState.value
        var request: SessionTurnRequest? = null
        var updatedSessionForPersistence: ChatSession? = null
        var piBranchMessageId: String? = null

        _uiState.update { current ->
            val sessionIndex = current.sessions.indexOfFirst { it.id == sessionId }
            if (sessionIndex < 0) return@update current

            val session = current.sessions[sessionIndex]
            val messageIndex = session.messages.indexOfFirst {
                it.id == messageId && it.author == MessageAuthor.Agent
            }
            if (messageIndex < 0) return@update current

            val trimFromIndex = session.messages.resolveConversationTrimIndex(messageIndex)
            val trimmedMessages = session.messages.take(trimFromIndex)
            if (trimmedMessages.lastOrNull()?.author != MessageAuthor.User) {
                return@update current
            }
            piBranchMessageId = trimmedMessages.piBranchMessageIdBeforeLastUser()

            request = SessionTurnRequest(
                sessionId = sessionId,
                settings = resolveModelSettings(
                    baseSettings = snapshot.settings,
                    providerConfigs = snapshot.providerConfigs,
                    preferredModelKey = session.selectedModelKey,
                    fallbackModelKey = resolveDefaultChatModelKey(snapshot.settings, snapshot.providerConfigs),
                ),
                requestMessages = trimmedMessages,
                selectedSkillIds = session.selectedSkillIds,
                activeSkills = session.activeSkills,
                activeMcpServerIds = session.activeMcpServerIds,
                conversationMode = session.conversationMode,
                agentModeEnabled = session.agentModeEnabled,
                chromeEnabled = session.chromeEnabled,
                providerConfigs = snapshot.providerConfigs,
            )
            val updatedSessions = current.sessions.toMutableList().apply {
                removeAt(sessionIndex)
                val updatedSession = session.withMessages(trimmedMessages)
                updatedSessionForPersistence = updatedSession
                add(0, updatedSession)
            }

            current.copy(
                sessions = updatedSessions,
                currentSessionId = sessionId,
                currentScreen = AppScreen.Chat,
                draftInput = "",
                draftAttachments = emptyList(),
                draftWorkspaceId = null,
                editingSessionId = null,
                editingMessageId = null,
            )
        }

        val turnRequest = request ?: return
        updatedSessionForPersistence?.let { session ->
            persistSessionSnapshot(
                session = session,
                currentSessionId = sessionId,
                moveToFront = true,
            )
        }
        viewModelScope.launch {
            navigatePiBranch(sessionId, piBranchMessage…19197 tokens truncated…&& attachments.isEmpty()) return
        if (attachments.any { it.workspaceState != AttachmentWorkspaceState.Ready }) return
        if (isCompactCommand(content)) {
            compactCurrentSession(snapshot)
            return
        }

        val targetSessionId = snapshot.editingSessionId ?: when {
            snapshot.currentSessionId != DraftSessionId -> snapshot.currentSessionId
            !snapshot.draftWorkspaceId.isNullOrBlank() -> snapshot.draftWorkspaceId.orEmpty()
            else -> "session-${System.currentTimeMillis()}"
        }

        val now = System.currentTimeMillis()
        val userMessage = ChatMessage(
            id = "user-$now",
            author = MessageAuthor.User,
            text = content,
            createdAtMillis = now,
            attachments = attachments,
        )

        if (snapshot.editingSessionId != null && sessionExecutionManager.isSessionRunning(targetSessionId)) {
            emitTransientMessage(uiString(R.string.message_pause_before_editing_message))
            return
        }

        if (sessionExecutionManager.isSessionRunning(targetSessionId)) {
            if (!sessionExecutionManager.submitFollowUp(targetSessionId, userMessage, runningFollowUpMode)) {
                emitTransientMessage(uiString(R.string.message_session_no_longer_running))
                return
            }
            buildAnalyticsTurnRequest(
                snapshot = snapshot,
                sessionId = targetSessionId,
                userMessage = userMessage,
            )?.let { turnRequest ->
                captureMessageSent(
                    request = turnRequest,
                    attachments = attachments,
                    isEdit = false,
                    submissionType = runningFollowUpMode.name.lowercase(),
                )
            }
            _uiState.update { current ->
                current.copy(
                    currentScreen = AppScreen.Chat,
                    draftInput = "",
                    draftAttachments = emptyList(),
                    draftWorkspaceId = null,
                    editingSessionId = null,
                    editingMessageId = null,
                    showStarterPromptHint = false,
                )
            }
            if ("message_sent" in aetherAppExtensionManager.state.value.snapshot.eventNames) {
                aetherAppExtensionManager.emitEvent(
                    event = "message_sent",
                    data = JSONObject()
                        .put("session_id", targetSessionId)
                        .put("message_id", userMessage.id)
                        .put("text", userMessage.text)
                        .put("mode", runningFollowUpMode.name.lowercase()),
                    context = buildAetherExtensionHostState(_uiState.value),
                )
            }
            return
        }

        var request: SessionTurnRequest? = null
        var requestMessages: List<ChatMessage> = emptyList()
        var requestSelectedSkillIds: List<String> = emptyList()
        var requestActiveSkills: List<ActiveSkillContext> = emptyList()
        var requestActiveMcpServerIds: List<String> = emptyList()
        var requestConversationMode = ConversationMode.Chat
        var requestAgentModeEnabled = false
        var requestChromeEnabled = false
        var requestModelKey = ""
        var shouldGenerateSessionTitle = false
        var sessionForPersistence: ChatSession? = null
        var editedMessagePredecessorId: String? = null
        var isEditingExistingMessage = false

        _uiState.update { current ->
            val updatedSessions = current.sessions.toMutableList()
            if (
                current.editingSessionId != null &&
                current.editingMessageId != null
            ) {
                val editingSessionIndex = updatedSessions.indexOfFirst {
                    it.id == current.editingSessionId
                }
                if (editingSessionIndex >= 0) {
                    val editingSession = updatedSessions.removeAt(editingSessionIndex)
                    val editingMessageIndex = editingSession.messages.indexOfFirst {
                        it.id == current.editingMessageId && it.author == MessageAuthor.User
                    }
                    if (editingMessageIndex >= 0) {
                        isEditingExistingMessage = true
                        editedMessagePredecessorId = editingSession.messages
                            .take(editingMessageIndex)
                            .lastOrNull()
                            ?.id
                        val branchedMessages = createEditedMessageBranch(
                            messages = editingSession.messages,
                            messageId = current.editingMessageId,
                            replacement = userMessage,
                        ) ?: (editingSession.messages.take(editingMessageIndex) + userMessage)
                        val updated = editingSession.withMessages(branchedMessages)
                        sessionForPersistence = updated
                        updatedSessions.add(0, updated)
                        requestMessages = updated.messages
                        requestSelectedSkillIds = updated.selectedSkillIds
                        requestActiveSkills = updated.activeSkills
                        requestActiveMcpServerIds = updated.activeMcpServerIds
                        requestConversationMode = updated.conversationMode
                        requestAgentModeEnabled = updated.agentModeEnabled
                        requestChromeEnabled = updated.chromeEnabled
                        requestModelKey = updated.selectedModelKey
                    } else {
                        updatedSessions.add(editingSessionIndex, editingSession)
                    }
                }
            }

            if (requestMessages.isEmpty()) {
                val existingIndex = updatedSessions.indexOfFirst { it.id == targetSessionId }
                if (existingIndex >= 0) {
                    val existing = updatedSessions.removeAt(existingIndex)
                    val updated = existing.withMessages(existing.messages + userMessage)
                    sessionForPersistence = updated
                    updatedSessions.add(0, updated)
                    requestMessages = updated.messages
                    requestSelectedSkillIds = updated.selectedSkillIds
                    requestActiveSkills = updated.activeSkills
                    requestActiveMcpServerIds = updated.activeMcpServerIds
                    requestConversationMode = updated.conversationMode
                    requestAgentModeEnabled = updated.agentModeEnabled
                    requestChromeEnabled = updated.chromeEnabled
                    requestModelKey = updated.selectedModelKey
                } else {
                    val newSession = createSession(
                        id = targetSessionId,
                        messages = listOf(userMessage),
                        title = "New chat",
                        hasCustomTitle = true,
                        selectedModelKey = current.draftSelectedModelKey.ifBlank {
                            resolveDefaultChatModelKey(current.settings, current.providerConfigs)
                        },
                        selectedSkillIds = current.draftSelectedSkillIds,
                        activeMcpServerIds = current.draftSelectedMcpServerIds,
                        conversationMode = current.draftConversationMode,
                        agentModeEnabled = current.draftAgentModeEnabled,
                        chromeEnabled = current.draftChromeEnabled,
                    )
                    shouldGenerateSessionTitle = true
                    sessionForPersistence = newSession
                    updatedSessions.add(0, newSession)
                    requestMessages = newSession.messages
                    requestSelectedSkillIds = newSession.selectedSkillIds
                    requestActiveSkills = newSession.activeSkills
                    requestActiveMcpServerIds = newSession.activeMcpServerIds
                    requestConversationMode = newSession.conversationMode
                    requestAgentModeEnabled = newSession.agentModeEnabled
                    requestChromeEnabled = newSession.chromeEnabled
                    requestModelKey = newSession.selectedModelKey
                }
            }

            request = SessionTurnRequest(
                sessionId = targetSessionId,
                settings = resolveModelSettings(
                    baseSettings = current.settings,
                    providerConfigs = current.providerConfigs,
                    preferredModelKey = requestModelKey,
                    fallbackModelKey = resolveDefaultChatModelKey(current.settings, current.providerConfigs),
                ),
                requestMessages = requestMessages,
                selectedSkillIds = requestSelectedSkillIds,
                activeSkills = requestActiveSkills,
                activeMcpServerIds = requestActiveMcpServerIds,
                conversationMode = requestConversationMode,
                agentModeEnabled = requestAgentModeEnabled,
                chromeEnabled = requestChromeEnabled,
                providerConfigs = current.providerConfigs,
            )

            current.copy(
                sessions = updatedSessions,
                currentSessionId = targetSessionId,
                draftInput = "",
                draftAttachments = emptyList(),
                draftSelectedModelKey = "",
                draftSelectedSkillIds = emptyList(),
                draftSelectedMcpServerIds = emptyList(),
                draftConversationMode = ConversationMode.Chat,
                draftAgentModeEnabled = false,
                draftChromeEnabled = false,
                draftWorkspaceId = null,
                editingSessionId = null,
                editingMessageId = null,
                currentScreen = AppScreen.Chat,
                showStarterPromptHint = false,
            )
        }

        val turnRequest = request ?: return
        sessionForPersistence?.let { session ->
            persistSessionSnapshot(
                session = session,
                currentSessionId = targetSessionId,
                moveToFront = true,
            )
        }
        captureMessageSent(
            request = turnRequest,
            attachments = attachments,
            isEdit = snapshot.editingSessionId != null,
            submissionType = "new_turn",
        )
        if (shouldGenerateSessionTitle) {
            generateSessionTitle(
                sessionId = targetSessionId,
                seedMessage = userMessage,
                settings = turnRequest.settings,
            )
        }
        if (isEditingExistingMessage) {
            viewModelScope.launch {
                navigatePiBranch(
                    sessionId = targetSessionId,
                    aetherMessageId = editedMessagePredecessorId,
                    resetWhenMissing = true,
                )
                sessionExecutionManager.startTurn(turnRequest)
            }
        } else {
            sessionExecutionManager.startTurn(turnRequest)
        }
        if ("message_sent" in aetherAppExtensionManager.state.value.snapshot.eventNames) {
            aetherAppExtensionManager.emitEvent(
                event = "message_sent",
                data = JSONObject()
                    .put("session_id", targetSessionId)
                    .put("message_id", userMessage.id)
                    .put("text", userMessage.text)
                    .put("mode", "new_turn"),
                context = buildAetherExtensionHostState(_uiState.value),
            )
        }
    }

    private fun handleTurnEvent(
        event: SessionTurnEvent,
    ) {
        captureTurnCompleted(event)
        val isSuccessfulAssistantReply = event.outcome == SessionTurnOutcome.Success
        if (
            shouldMarkOnboardingCompleted(
                settings = _uiState.value.settings,
                isSuccessfulAssistantReply = isSuccessfulAssistantReply,
            )
        ) {
            viewModelScope.launch {
                settingsRepository.updateOnboardingCompletedVersion(CurrentOnboardingVersion)
            }
        }
        if (
            shouldRevealFollowUpTourCard(
                isAwaitingFollowUpTour = _uiState.value.awaitingFollowUpTour,
                isSuccessfulAssistantReply = isSuccessfulAssistantReply,
            )
        ) {
            scheduleFollowUpTourAfterFirstReply()
        }
        _uiState.update { current ->
            val unviewedCompletedSessionIds = when {
                event.sessionId == current.currentSessionId -> current.unviewedCompletedSessionIds - event.sessionId
                event.outcome != SessionTurnOutcome.Neutral -> current.unviewedCompletedSessionIds + event.sessionId
                else -> current.unviewedCompletedSessionIds
            }
            current.copy(unviewedCompletedSessionIds = unviewedCompletedSessionIds)
        }
        if ("turn_complete" in aetherAppExtensionManager.state.value.snapshot.eventNames) {
            aetherAppExtensionManager.emitEvent(
                event = "turn_complete",
                data = JSONObject()
                    .put("session_id", event.sessionId)
                    .put("outcome", event.outcome.name.lowercase()),
                context = buildAetherExtensionHostState(_uiState.value),
            )
        }
    }

    private suspend fun buildPendingDraftAttachment(
        uri: Uri,
    ): ChatAttachment? {
        val metadata = readAttachmentMetadata(uri) ?: return null
        return ChatAttachment(
            id = "attachment-${System.currentTimeMillis()}-${uri.hashCode()}",
            uri = uri.toString(),
            name = metadata.displayName,
            mimeType = metadata.mimeType,
            sizeBytes = metadata.sizeBytes,
            kind = metadata.kind,
            workspaceState = AttachmentWorkspaceState.Pending,
        )
    }

    private suspend fun importDraftAttachmentToWorkspace(
        attachment: ChatAttachment,
        sessionId: String,
    ) {
        val importResult = runtimeWorkspaceFileBridge.importAttachmentToWorkspace(
            settings = _uiState.value.settings,
            sourceUri = Uri.parse(attachment.uri),
            sessionId = sessionId,
            attachmentId = attachment.id,
            displayName = attachment.name,
            mode = _uiState.value.settings.agentWorkspaceMode,
            onProgress = { progress ->
                _uiState.update { current ->
                    val attachmentIndex = current.draftAttachments.indexOfFirst { it.id == attachment.id }
                    if (attachmentIndex < 0) return@update current
                    val existingAttachment = current.draftAttachments[attachmentIndex]
                    current.copy(
                        draftAttachments = current.draftAttachments.toMutableList().apply {
                            set(
                                attachmentIndex,
                                existingAttachment.copy(
                                    workspaceBytesCopied = progress.bytesCopied,
                                    workspaceBytesPerSecond = progress.bytesPerSecond,
                                )
                            )
                        }
                    )
                }
            },
        )

        _uiState.update { current ->
            val attachmentIndex = current.draftAttachments.indexOfFirst { it.id == attachment.id }
            if (attachmentIndex < 0) return@update current

            val existingAttachment = current.draftAttachments[attachmentIndex]
            val updatedAttachment = importResult.fold(
                onSuccess = { importedFile ->
                    val resolvedMimeType = existingAttachment.mimeType.ifBlank {
                        workspaceFileBridge.guessMimeType(importedFile.absolutePath)
                    }
                    existingAttachment.copy(
                        mimeType = resolvedMimeType,
                        sizeBytes = existingAttachment.sizeBytes ?: importedFile.bytesCopied,
                        kind = if (resolvedMimeType.startsWith("image/")) {
                            AttachmentKind.Image
                        } else {
                            AttachmentKind.File
                        },
                        workspacePath = importedFile.absolutePath,
                        workspaceState = AttachmentWorkspaceState.Ready,
                        workspaceError = "",
                        workspaceBytesCopied = importedFile.bytesCopied,
                        workspaceBytesPerSecond = 0L,
                        inlineBase64 = if (
                            resolvedMimeType.startsWith("image/") &&
                            importedFile.inlineBytes.isNotEmpty() &&
                            importedFile.inlineBytes.size <= MaxInlineImageAttachmentBytes
                        ) {
                            Base64.getEncoder().encodeToString(importedFile.inlineBytes)
                        } else {
                            existingAttachment.inlineBase64
                        },
                    )
                },
                onFailure = { throwable ->
                    val inlineImageBase64 = readInlineImageAttachment(existingAttachment)
                    if (inlineImageBase64.isNotBlank()) {
                        existingAttachment.copy(
                            workspacePath = "",
                            workspaceState = AttachmentWorkspaceState.Ready,
                            workspaceError = "",
                            workspaceBytesPerSecond = 0L,
                            inlineBase64 = inlineImageBase64,
                        )
                    } else {
                        existingAttachment.copy(
                            workspaceState = AttachmentWorkspaceState.Failed,
                            workspaceError = throwable.message
                                .orEmpty()
                                .ifBlank { "Couldn't copy this attachment into the workspace." },
                            workspaceBytesPerSecond = 0L,
                        )
                    }
                },
            )

            current.copy(
                draftAttachments = current.draftAttachments.toMutableList().apply {
                    set(attachmentIndex, updatedAttachment)
                }
            )
        }
    }

    private fun readInlineImageAttachment(attachment: ChatAttachment): String {
        if (attachment.kind != AttachmentKind.Image || !attachment.mimeType.startsWith("image/")) return ""
        if (attachment.sizeBytes?.let { it > MaxInlineImageAttachmentBytes } == true) return ""
        return runCatching {
            val resolver = getApplication<Application>().contentResolver
            val bytes = resolver.openInputStream(Uri.parse(attachment.uri))?.use { input ->
                val buffer = ByteArray(MaxInlineImageAttachmentBytes + 1)
                var total = 0
                while (total < buffer.size) {
                    val read = input.read(buffer, total, buffer.size - total)
                    if (read <= 0) break
                    total += read
                }
                require(total <= MaxInlineImageAttachmentBytes) { "Image is too large to inline." }
                buffer.copyOf(total)
            } ?: return ""
            Base64.getEncoder().encodeToString(bytes)
        }.getOrDefault("")
    }

    private fun normalizeDraftAttachmentForEditing(
        attachment: ChatAttachment,
    ): ChatAttachment = if (
        attachment.workspacePath.isNotBlank() ||
        (attachment.kind == AttachmentKind.Image && attachment.inlineBase64.isNotBlank())
    ) {
        attachment.copy(
            workspaceState = AttachmentWorkspaceState.Ready,
            workspaceError = "",
            workspaceBytesPerSecond = 0L,
        )
    } else {
        attachment.copy(
            workspaceState = AttachmentWorkspaceState.Failed,
            workspaceError = "This attachment is missing its workspace copy. Re-upload it before sending.",
        )
    }

    private fun readAttachmentMetadata(
        uri: Uri,
    ): AttachmentMetadata? {
        val resolver = getApplication<Application>().contentResolver
        var mimeType = resolver.getType(uri).orEmpty()
        var displayName = uri.lastPathSegment ?: "Attachment"
        var sizeBytes: Long? = null

        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }

                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }

        if (mimeType.isBlank()) {
            mimeType = workspaceFileBridge.guessMimeType(displayName)
        }

        return AttachmentMetadata(
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            kind = if (mimeType.startsWith("image/")) AttachmentKind.Image else AttachmentKind.File,
        )
    }

    private fun scheduleFollowUpTourAfterFirstReply() {
        _uiState.update { current ->
            current.copy(
                awaitingFollowUpTour = false,
                showFollowUpTourCard = true,
            )
        }
        viewModelScope.launch {
            delay(FollowUpTourAutoOpenDelayMillis)
            _uiState.update { current ->
                if (current.currentScreen != AppScreen.Chat) {
                    current
                } else {
                    current.copy(
                        currentScreen = AppScreen.Onboarding,
                        isOnboardingReplay = true,
                        onboardingStep = OnboardingStep.LocalRuntimeChoice,
                        onboardingReturnScreen = AppScreen.Chat,
                        awaitingFollowUpTour = false,
                        showFollowUpTourCard = false,
                    )
                }
            }
        }
    }

    private fun persistCurrentSessionId(sessionId: String) {
        chatStateStore.update { persisted ->
            persisted.copy(
                sessions = persisted.sessions.withMessagesOnlyForSession(sessionId),
                currentSessionId = sessionId,
            )
        }
    }

    private suspend fun hydrateCurrentSessionMessages(persisted: PersistedChatState): PersistedChatState {
        val currentSessionId = persisted.currentSessionId.ifBlank { DraftSessionId }
        if (currentSessionId == DraftSessionId) return persisted
        val currentSession = persisted.sessions.firstOrNull { it.id == currentSessionId } ?: return persisted
        if (currentSession.messages.isNotEmpty() || currentSession.messageCount <= 0) return persisted
        val loadedSession = runCatching {
            withContext(Dispatchers.IO) {
                runtime.chatRepository.getSessionWithMessages(currentSessionId)
            }
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
            diagnosticLogger.exception(
                category = "storage",
                event = "session_hydration_failed",
                throwable = throwable,
                level = "warn",
                sessionId = currentSessionId,
            )
            null
        } ?: return persisted
        return persisted.copy(
            sessions = replaceOrPrependSession(persisted.sessions, loadedSession),
        )
    }

    private fun persistSessionSelection(
        sessionId: String,
        loadedSession: ChatSession?,
    ) {
        if (loadedSession == null) {
            persistCurrentSessionId(sessionId)
            return
        }
        chatStateStore.update { persisted ->
            persisted.copy(
                sessions = replaceOrPrependSession(persisted.sessions, loadedSession)
                    .withMessagesOnlyForSession(sessionId),
                currentSessionId = sessionId,
            )
        }
    }

    private fun replaceOrPrependSession(
        sessions: List<ChatSession>,
        session: ChatSession,
    ): List<ChatSession> = if (sessions.any { it.id == session.id }) {
        sessions.map { existing ->
            if (existing.id == session.id) session else existing
        }
    } else {
        listOf(session) + sessions
    }

    private fun List<ChatSession>.withMessagesOnlyForSession(sessionId: String): List<ChatSession> =
        map { session ->
            if (session.id == sessionId) {
                session
            } else {
                session.copy(messages = emptyList())
            }
        }

    private fun replacePersistedChats(
        sessions: List<ChatSession>,
        currentSessionId: String,
    ) {
        chatStateStore.update { persisted ->
            persisted.copy(
                sessions = sessions,
                currentSessionId = currentSessionId,
            )
        }
    }

    private fun persistSessionSnapshot(
        session: ChatSession,
        currentSessionId: String? = null,
        moveToFront: Boolean = false,
    ) {
        chatStateStore.update { persisted ->
            val currentIndex = persisted.sessions.indexOfFirst { it.id == session.id }
            val updatedSessions = persisted.sessions.toMutableList().apply {
                if (currentIndex >= 0) {
                    removeAt(currentIndex)
                }
                val insertIndex = when {
                    moveToFront -> 0
                    currentIndex >= 0 -> currentIndex.coerceAtMost(size)
                    else -> 0
                }
                add(insertIndex, session)
            }
            persisted.copy(
                sessions = updatedSessions,
                currentSessionId = currentSessionId ?: persisted.currentSessionId,
            )
        }
    }

    private suspend fun persistSessionSnapshotAndFlush(
        session: ChatSession,
        moveToFront: Boolean = false,
        currentSessionId: String? = null,
    ) {
        chatStateStore.updateAndFlush { persisted ->
            val currentIndex = persisted.sessions.indexOfFirst { it.id == session.id }
            val updatedSessions = persisted.sessions.toMutableList().apply {
                if (currentIndex >= 0) {
                    removeAt(currentIndex)
                }
                val insertIndex = when {
                    moveToFront -> 0
                    currentIndex >= 0 -> currentIndex.coerceAtMost(size)
                    else -> 0
                }
                add(insertIndex, session)
            }
            persisted.copy(
                sessions = updatedSessions,
                currentSessionId = currentSessionId ?: persisted.currentSessionId,
            )
        }
    }

    private fun persistSessionMutation(
        sessionId: String,
        transform: (ChatSession) -> ChatSession?,
    ) {
        chatStateStore.update { persisted ->
            val sessionIndex = persisted.sessions.indexOfFirst { it.id == sessionId }
            if (sessionIndex < 0) return@update persisted
            val updatedSession = transform(persisted.sessions[sessionIndex]) ?: return@update persisted
            val updatedSessions = persisted.sessions.toMutableList().apply {
                set(sessionIndex, updatedSession)
            }
            persisted.copy(sessions = updatedSessions)
        }
    }

    private suspend fun persistDeleteSession(
        sessionId: String,
        sharedWorkspaceFilePaths: Collection<String> = emptyList(),
    ) {
        val agentSession = runCatching {
            runtime.chatRepository.getAgentSessionMetadata(sessionId)
        }.getOrNull()
        runCatching {
            runtime.piKernelBridge.closeSession(
                sessionId = sessionId,
                sessionFile = agentSession?.jsonlPath.orEmpty(),
                deleteFile = true,
            )
        }.onFailure { throwable ->
            diagnosticLogger.exception(
                category = "pi_bridge",
                event = "close_deleted_session_failed",
                throwable = throwable,
                level = "warn",
                sessionId = sessionId,
            )
        }
        val unreferencedSharedWorkspaceFilePaths = sharedWorkspaceFilePaths
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
        chatStateStore.updateAndFlush(
            writeIntent = PersistedChatWriteIntent.DeleteSession,
        ) { persisted ->
            val updatedSessions = persisted.sessions.filterNot { it.id == sessionId }
            persisted.copy(
                sessions = updatedSessions,
                currentSessionId = if (persisted.currentSessionId == sessionId) {
                    DraftSessionId
                } else {
                    persisted.currentSessionId
                },
            )
        }
        scheduleSessionRuntimeDataCleanup(
            sessionIds = listOf(sessionId),
            sharedWorkspaceFilePaths = unreferencedSharedWorkspaceFilePaths,
            mode = _uiState.value.settings.agentWorkspaceMode,
        )
    }

    private fun scheduleSessionRuntimeDataCleanup(
        sessionIds: Collection<String>,
        sharedWorkspaceFilePaths: Collection<String>,
        mode: AgentWorkspaceMode,
    ) {
        val cleanupSessionIds = sessionIds
            .filter { it.isNotBlank() && it != DraftSessionId }
            .distinct()
        val sharedWorkspaceFileCount = sharedWorkspaceFilePaths.distinct().size
        if (cleanupSessionIds.isEmpty() && sharedWorkspaceFileCount == 0) return

        viewModelScope.launch {
            workspaceFileBridge.deleteSessionsRuntimeData(
                sessionIds = cleanupSessionIds,
                sharedWorkspaceFilePaths = sharedWorkspaceFilePaths,
            )
                .onSuccess {
                    diagnosticLogger.event(
                        category = "storage",
                        event = "deleted_session_runtime_cleanup_completed",
                        sessionId = cleanupSessionIds.singleOrNull(),
                        details = mapOf(
                            "session_count" to cleanupSessionIds.size,
                            "shared_workspace_file_count" to sharedWorkspaceFileCount,
                            "settings_workspace_mode" to mode.storageValue,
                        ),
                    )
                }
                .onFailure { throwable ->
                    diagnosticLogger.exception(
                        category = "storage",
                        event = "deleted_session_runtime_cleanup_failed",
                        throwable = throwable,
                        level = "warn",
                        sessionId = cleanupSessionIds.singleOrNull(),
                        details = mapOf(
                            "session_count" to cleanupSessionIds.size,
                            "shared_workspace_file_count" to sharedWorkspaceFileCount,
                            "settings_workspace_mode" to mode.storageValue,
                        ),
                    )
                }
        }
    }

    private fun persistPrunedSessionSelections(
        enabledSkillIds: Set<String>,
        enabledMcpServerIds: Set<String>,
    ) {
        chatStateStore.update { persisted ->
            persisted.copy(
                sessions = persisted.sessions.map { session ->
                    val selectedSkillIds = session.selectedSkillIds.filter(enabledSkillIds::contains)
                    val activeSkills = session.activeSkills.filter { activeSkill ->
                        selectedSkillIds.contains(activeSkill.skillId)
                    }
                    val activeMcpServerIds = session.activeMcpServerIds.filter(enabledMcpServerIds::contains)
                    if (
                        selectedSkillIds == session.selectedSkillIds &&
                        activeSkills == session.activeSkills &&
                        activeMcpServerIds == session.activeMcpServerIds
                    ) {
                        session
                    } else {
                        session.copy(
                            selectedSkillIds = selectedSkillIds,
                            activeSkills = activeSkills,
                            activeMcpServerIds = activeMcpServerIds,
                        )
                    }
                }
            )
        }
    }

    private fun buildAnalyticsTurnRequest(
        snapshot: AetherUiState,
        sessionId: String,
        userMessage: ChatMessage,
    ): SessionTurnRequest? {
        val session = snapshot.sessions.firstOrNull { it.id == sessionId } ?: return null
        return SessionTurnRequest(
            sessionId = sessionId,
            settings = resolveModelSettings(
                baseSettings = snapshot.settings,
                providerConfigs = snapshot.providerConfigs,
                preferredModelKey = session.selectedModelKey,
                fallbackModelKey = resolveDefaultChatModelKey(snapshot.settings, snapshot.providerConfigs),
            ),
            requestMessages = session.messages + userMessage,
            selectedSkillIds = session.selectedSkillIds,
            activeSkills = session.activeSkills,
            activeMcpServerIds = session.activeMcpServerIds,
            conversationMode = session.conversationMode,
            agentModeEnabled = session.agentModeEnabled,
            chromeEnabled = session.chromeEnabled,
            providerConfigs = snapshot.providerConfigs,
        )
    }

    private fun captureMessageSent(
        request: SessionTurnRequest,
        attachments: List<ChatAttachment>,
        isEdit: Boolean,
        submissionType: String,
    ) {
        val modelProperties = modelUsageProperties(request, source = "message")
        captureAnalyticsEvent(
            event = "message sent",
            properties = mapOf(
                "has_attachments" to attachments.isNotEmpty(),
                "attachment_count" to attachments.size,
                "agent_mode_enabled" to request.agentModeEnabled,
                "skill_count" to request.selectedSkillIds.size,
                "mcp_server_count" to request.activeMcpServerIds.size,
                "is_edit" to isEdit,
                "submission_type" to submissionType,
            ) + modelProperties,
        )
        captureAnalyticsEvent(
            event = "model used",
            properties = modelProperties + mapOf(
                "has_attachments" to attachments.isNotEmpty(),
                "attachment_count" to attachments.size,
                "is_edit" to isEdit,
                "submission_type" to submissionType,
            ),
        )
        captureAgentModeStarted(
            request = request,
            isEdit = isEdit,
            submissionType = submissionType,
        )
    }

    private fun captureTurnCompleted(event: SessionTurnEvent) {
        val tokenProperties = tokenUsageAnalyticsProperties(event)
        captureAnalyticsEvent(
            event = "conversation turn completed",
            properties = mapOf(
                "outcome" to event.outcome.name.lowercase(),
                "tool_call_count" to event.toolCallCount,
                "distinct_tool_count" to event.distinctToolCount,
                "tool_names" to event.toolNames,
                "has_tool_calls" to (event.toolCallCount > 0),
                "duration_millis" to (event.durationMillis ?: 0L),
            ) + tokenProperties,
        )
        if (event.tokenUsage != null) {
            captureAnalyticsEvent(
                event = "tokens used",
                properties = tokenProperties + mapOf(
                    "outcome" to event.outcome.name.lowercase(),
                    "tool_call_count" to event.toolCallCount,
                    "has_tool_calls" to (event.toolCallCount > 0),
                    "duration_millis" to (event.durationMillis ?: 0L),
                ),
            )
        }
    }

    private fun trackTermuxSetupState(
        setupState: TermuxSetupState,
        source: String,
    ) {
        if (!_uiState.value.settings.privacyPolicyAccepted) return
        if (setupState.issue != TermuxSetupIssue.NotInstalled &&
            lastTrackedTermuxDetectedIssue != setupState.issue
        ) {
            lastTrackedTermuxDetectedIssue = setupState.issue
            captureAnalyticsEvent(
                event = "termux detected",
                properties = mapOf(
                    "source" to source,
                    "issue" to setupState.issue.name.lowercase(),
                    "is_ready" to setupState.isReady,
                ),
            )
        }

        val setupSource = pendingTermuxSetupSource ?: return
        if (!setupState.isReady) return
        pendingTermuxSetupSource = null
        captureAnalyticsEvent(
            event = "termux setup completed",
            properties = mapOf(
                "source" to setupSource,
                "detected_source" to source,
                "issue" to setupState.issue.name.lowercase(),
            ),
        )
    }

    private fun captureAgentModeStarted(
        request: SessionTurnRequest,
        isEdit: Boolean,
        submissionType: String,
    ) {
        if (!request.agentModeEnabled) return
        val properties = modelUsageProperties(request, source = "agent_mode") + mapOf(
            "authorization_enabled" to request.settings.agentModeAuthorizationEnabled,
            "authorization_method" to request.settings.agentModeAuthorizationMethod.storageValue,
            "is_edit" to isEdit,
            "submission_type" to submissionType,
        )
        if (request.settings.agentModeAuthorizationEnabled) {
            captureAnalyticsEvent(
                event = "agent mode started",
                properties = properties,
            )
        } else {
            captureAnalyticsEvent(
                event = "agent mode failed",
                properties = properties + mapOf("reason" to "authorization_disabled"),
            )
        }
    }

    private fun modelUsageProperties(
        request: SessionTurnRequest,
        source: String,
    ): Map<String, Any> = mapOf(
        "model" to request.settings.modelId.trim(),
        "provider" to com.zhousl.aether.data.PiProviderCatalog
            .resolve(request.settings.piProviderId).displayName,
        "provider_type" to request.settings.piProviderId,
        "source" to source,
        "agent_mode_enabled" to request.agentModeEnabled,
        "skill_count" to request.selectedSkillIds.size,
        "mcp_server_count" to request.activeMcpServerIds.size,
    )

    private fun tokenUsageAnalyticsProperties(event: SessionTurnEvent): Map<String, Any> {
        val usage = event.tokenUsage
        val totalTokens = usage?.totalTokens ?: 0L
        val inputTokens = usage?.inputTokens ?: 0L
        val outputTokens = usage?.outputTokens ?: 0L
        val inputMessageCount = event.inputMessageCount.coerceAtLeast(0)
        val userMessageCount = event.userMessageCount.coerceAtLeast(0)
        return buildMap {
            put("token_usage_source", event.tokenUsageSource)
            put("has_token_usage", usage != null)
            put("input_message_count", inputMessageCount)
            put("user_message_count", userMessageCount)
            put("llm_request_count", usage?.requestCount ?: 0)
            put("input_tokens", inputTokens)
            put("output_tokens", outputTokens)
            put("total_tokens", totalTokens)
            usage?.reasoningTokens?.let { put("reasoning_tokens", it) }
            usage?.cachedInputTokens?.let { put("cached_input_tokens", it) }
            put(
                "average_tokens_per_input_message",
                if (inputMessageCount > 0) totalTokens.toDouble() / inputMessageCount else 0.0,
            )
            put(
                "average_input_tokens_per_input_message",
                if (inputMessageCount > 0) inputTokens.toDouble() / inputMessageCount else 0.0,
            )
            put(
                "average_tokens_per_user_message",
                if (userMessageCount > 0) totalTokens.toDouble() / userMessageCount else 0.0,
            )
        }
    }

    private fun captureAnalyticsEvent(
        event: String,
        properties: Map<String, Any> = emptyMap(),
    ) {
        AetherAnalytics.capture(event = event, properties = properties)
    }

    private fun normalizeProviderConfig(
        config: LlmProviderConfig,
    ): LlmProviderConfig {
        val definition = com.zhousl.aether.data.PiProviderCatalog.resolve(
            config.piProviderId,
        )
        val manualModels = (config.manualModelIds.ifEmpty { listOf(config.modelId) })
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
        val cachedModels = config.cachedModels
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
        val availableModels = (cachedModels + manualModels)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
        val normalizedModelId = config.modelId.trim()
            .takeIf { it.isNotBlank() && availableModels.contains(it) }
            ?: manualModels.firstOrNull()
            ?: cachedModels.firstOrNull()
            ?: definition.defaultModelId
        val normalizedEnabledModels = config.enabledModelIds
            .map(String::trim)
            .filter { it.isNotEmpty() && availableModels.contains(it) }
            .distinct()
        return config.copy(
            providerId = config.providerId.trim(),
            name = config.name.trim().ifBlank { definition.displayName },
            piProviderId = definition.id,
            baseUrl = config.baseUrl.trim(),
            modelId = normalizedModelId,
            manualModelIds = manualModels,
            userAgent = normalizeLlmUserAgent(config.userAgent),
            customHeaders = config.customHeaders
                .map { header -> header.copy(name = header.name.trim()) }
                .filter { header ->
                    header.name.isNotBlank() &&
                        !header.name.equals("User-Agent", ignoreCase = true)
                }
                .distinctBy { header -> header.name.lowercase() },
            providerEnvironmentVariables = config.providerEnvironmentVariables
                .map { variable -> variable.copy(name = variable.name.trim()) }
                .filter { variable -> variable.name.isNotBlank() }
                .distinctBy { variable -> variable.name.uppercase() },
            cachedModels = cachedModels,
            enabledModelIds = normalizedEnabledModels,
        )
    }

    private fun createSession(
        id: String,
        messages: List<ChatMessage>,
        title: String? = null,
        hasCustomTitle: Boolean = false,
        selectedModelKey: String = "",
        selectedSkillIds: List<String> = emptyList(),
        activeSkills: List<ActiveSkillContext> = emptyList(),
        activeMcpServerIds: List<String> = emptyList(),
        conversationMode: ConversationMode = ConversationMode.Chat,
        agentModeEnabled: Boolean = false,
        chromeEnabled: Boolean = false,
    ): ChatSession {
        val metadata = deriveSessionMetadata(messages)
        return ChatSession(
            id = id,
            title = title ?: metadata.title,
            preview = metadata.preview,
            hasCustomTitle = hasCustomTitle,
            messages = messages,
            selectedModelKey = selectedModelKey,
            selectedSkillIds = selectedSkillIds,
            activeSkills = activeSkills,
            activeMcpServerIds = activeMcpServerIds,
            conversationMode = conversationMode,
            agentModeEnabled = agentModeEnabled,
            chromeEnabled = chromeEnabled,
        )
    }

    private fun ChatSession.withMessages(messages: List<ChatMessage>): ChatSession {
        val syncedMessages = syncActiveBranches(messages)
        val metadata = deriveSessionMetadata(syncedMessages)
        return copy(
            title = if (hasCustomTitle) title else metadata.title,
            preview = metadata.preview,
            messages = syncedMessages,
            messageCount = syncedMessages.size,
            lastMessageAtMillis = syncedMessages.maxOfOrNull { it.createdAtMillis },
        )
    }

    private suspend fun resolveSelectedActiveSkills(
        selectedSkillIds: List<String>,
        existingActiveSkills: List<ActiveSkillContext>,
    ): List<ActiveSkillContext> {
        if (selectedSkillIds.isEmpty()) return emptyList()
        val installedSkillsById = _uiState.value.installedSkills
            .filter { it.isEnabled }
            .associateBy { it.id }
        return buildList {
            selectedSkillIds.distinct().forEach { skillId ->
                val installedSkill = installedSkillsById[skillId] ?: return@forEach
                val refreshedSkill = skillManager.buildActiveSkillContext(installedSkill)
                    .getOrElse { return@forEach }
                add(refreshedSkill)
            }
        }
    }

    private fun upsertActiveSkillContext(
        activeSkills: List<ActiveSkillContext>,
        activeSkill: ActiveSkillContext,
    ): List<ActiveSkillContext> {
        val existingIndex = activeSkills.indexOfFirst { it.skillId == activeSkill.skillId }
        if (existingIndex < 0) return activeSkills + activeSkill
        return activeSkills.toMutableList().apply {
            set(existingIndex, activeSkill)
        }
    }


    private fun setSessionSelectedSkillIds(
        sessionId: String,
        selectedSkillIds: List<String>,
    ) {
        updateSession(sessionId) { session ->
            val activeSkills = session.activeSkills.filter { activeSkill ->
                selectedSkillIds.contains(activeSkill.skillId)
            }
            if (
                session.selectedSkillIds == selectedSkillIds &&
                session.activeSkills == activeSkills
            ) {
                null
            } else {
                session.copy(
                    selectedSkillIds = selectedSkillIds,
                    activeSkills = activeSkills,
                )
            }
        }
    }

    private fun setSessionActiveSkills(
        sessionId: String,
        activeSkills: List<ActiveSkillContext>,
    ) {
        updateSession(sessionId) { session ->
            if (session.activeSkills == activeSkills) {
                null
            } else {
                session.copy(activeSkills = activeSkills)
            }
        }
    }


    private fun updateSession(
        sessionId: String,
        transform: (ChatSession) -> ChatSession?,
    ) {
        var didUpdate = false
        _uiState.update { current ->
            val sessionIndex = current.sessions.indexOfFirst { it.id == sessionId }
            if (sessionIndex < 0) return@update current
            val updatedSessions = current.sessions.toMutableList()
            val session = updatedSessions.removeAt(sessionIndex)
            val updatedSession = transform(session)
            if (updatedSession == null) {
                updatedSessions.add(sessionIndex, session)
                current
            } else {
                didUpdate = true
                updatedSessions.add(
                    sessionIndex.coerceAtMost(updatedSessions.size),
                    updatedSession,
                )
                current.copy(sessions = updatedSessions)
            }
        }
        if (didUpdate) {
            persistSessionMutation(sessionId, transform)
        }
    }


    private fun updateOrderedSelection(
        currentSelection: List<String>,
        id: String,
        selected: Boolean,
    ): List<String> = when {
        selected && currentSelection.contains(id) -> currentSelection
        selected -> currentSelection + id
        else -> currentSelection.filterNot { it == id }
    }

    private fun deriveSessionMetadata(messages: List<ChatMessage>): SessionMetadata {
        val visibleMessages = messages.filter { it.displayKind != MessageDisplayKind.HiddenContext }
        val title = messages
            .firstOrNull { it.author == MessageAuthor.User && it.displayKind == MessageDisplayKind.Standard }
            ?.summaryText()
            .orEmpty()
            .ifBlank { "New chat" }
            .take(36)

        val preview = visibleMessages
            .lastOrNull()
            ?.summaryText()
            .orEmpty()
            .ifBlank { "No messages yet." }
            .take(96)

        return SessionMetadata(title = title, preview = preview)
    }

    private fun generateSessionTitle(
        sessionId: String,
        seedMessage: ChatMessage,
        settings: AppSettings,
    ) {
                if (!settings.isProviderSetupValid()) {
            return
        }

        val titleInput = buildTitleGenerationInput(seedMessage)
        if (titleInput.isBlank()) return

        viewModelScope.launch {
            val providerConfigs = _uiState.value.providerConfigs
            val titleSettings = resolveModelSettings(
                baseSettings = settings,
                providerConfigs = providerConfigs,
                preferredModelKey = resolveDefaultTitleModelKey(settings, providerConfigs),
                fallbackModelKey = resolveDefaultChatModelKey(settings, providerConfigs),
            )
            val modelKey = thinkingCatalogKey(titleSettings.piProviderId, titleSettings.modelId)
            val thinkingLevelMap = _uiState.value.thinkingLevelClampsByProviderModel[modelKey].orEmpty()
            val isReasoningModel = modelKey in _uiState.value.reasoningModels
            val title = piCompletionClient.completeOnce(
                settings = titleSettings,
                systemPrompt = SessionTitleSystemPrompt,
                messages = listOf(
                    LlmMessage(
                        role = "user",
                        contentParts = listOf(LlmTextPart(titleInput)),
                    )
                ),
                disableReasoning = true,
                thinkingLevelMap = thinkingLevelMap,
                isReasoningModel = isReasoningModel,
            ).getOrNull()
                ?.assistantText
                ?.sanitizeGeneratedSessionTitle()
                .orEmpty()

            if (title.isBlank()) return@launch

            updateSession(sessionId) { session ->
                val firstUserMessage = session.messages.firstOrNull { it.author == MessageAuthor.User }
                if (firstUserMessage?.id != seedMessage.id) {
                    null
                } else {
                    session.copy(
                        title = title,
                        hasCustomTitle = true,
                    )
                }
            }
        }
    }

    private fun buildTitleGenerationInput(
        message: ChatMessage,
    ): String = buildString {
        val text = message.text.trim()
        if (text.isNotBlank()) {
            appendLine("First user message:")
            appendLine(text)
        }
        if (message.attachments.isNotEmpty()) {
            if (isNotEmpty()) appendLine()
            appendLine("Attachments:")
            message.attachments.forEach { attachment ->
                appendLine("- ${attachment.name}")
            }
        }
    }.trim()

    private fun isCompactCommand(content: String): Boolean =
        content.trim().equals(CompactCommand, ignoreCase = true)

    private fun compactCurrentSession(snapshot: AetherUiState) {
        compactSession(snapshot.currentSessionId, manual = true, snapshot = snapshot)
    }

    private fun compactSession(
        sessionId: String,
        manual: Boolean,
        snapshot: AetherUiState = _uiState.value,
    ) {
        if ((manual && snapshot.editingSessionId != null) || snapshot.editingSessionId == sessionId) {
            if (!manual) return
            emitTransientMessage(uiString(R.string.message_finish_editing_before_compacting))
            return
        }
        if (sessionId == DraftSessionId) {
            if (!manual) return
            emitTransientMessage(uiString(R.string.message_no_conversation_to_compact))
            return
        }
        if (sessionExecutionManager.isSessionRunning(sessionId)) {
            if (!manual) return
            emitTransientMessage(uiString(R.string.message_pause_before_compacting))
            return
        }
        if (snapshot.compactingSessionId != null) return
        val session = snapshot.sessions.firstOrNull { it.id == sessionId }
        if (session == null || session.messages.size < 2) {
            if (!manual) return
            emitTransientMessage(uiString(R.string.message_not_enough_conversation_to_compact))
            return
        }
        _uiState.update { current ->
            if (manual) {
                current.copy(
                    draftInput = "",
                    draftAttachments = emptyList(),
                    draftWorkspaceId = null,
                    editingSessionId = null,
                    editingMessageId = null,
                    showStarterPromptHint = false,
                    compactingSessionId = sessionId,
                )
            } else {
                current.copy(compactingSessionId = sessionId)
            }
        }

        viewModelScope.launch {
            try {
                val metadata = runtime.chatRepository.getAgentSessionMetadata(sessionId)
                val settings = snapshot.settings
                val termuxWorkspaceDirectory = workspaceFileBridge.workspaceDirectory(
                    sessionId,
                    settings.agentWorkspaceMode,
                )
                piKernelBridge.compactSession(
                    sessionId = sessionId,
                    sessionPayload = JSONObject().apply {
                        put("session_file", metadata?.jsonlPath.orEmpty())
                        put("workspace_directory", runtime.runtimeRouter.alpineWorkspaceDirectory())
                        put("termux_workspace_directory", termuxWorkspaceDirectory)
                        put("runtime", metadata?.runtime ?: settings.defaultRuntimeId?.storageValue.orEmpty())
                        put("platform", "android")
                        val modelKey = thinkingCatalogKey(settings.piProviderId, settings.modelId)
                        val thinkingLevelMap = _uiState.value.thinkingLevelClampsByProviderModel[modelKey].orEmpty()
                        val isReasoningModel = modelKey in _uiState.value.reasoningModels
                        put("model_config", settings.toPiModelConfig(thinkingLevelMap, isReasoningModel).toJson())
                        put("system_prompt", "")
                        put("host_tools", JSONArray())
                    },
                )
                val now = System.currentTimeMillis()
                val compactedMessages = session.messages + ChatMessage(
                    id = "compact-status-$now",
                    author = MessageAuthor.Agent,
                    text = "Context compacted",
                    createdAtMillis = now,
                    assistantActionsHidden = true,
                    displayKind = MessageDisplayKind.CompactStatus,
                )
                val updatedSession = session.withMessages(compactedMessages)
                _uiState.update { current ->
                    val sessionIndex = current.sessions.indexOfFirst { it.id == sessionId }
                    if (sessionIndex < 0) return@update current
                    val updatedSessions = current.sessions.toMutableList().apply {
                        set(sessionIndex, updatedSession)
                    }
                    current.copy(sessions = updatedSessions)
                }
                persistSessionSnapshot(updatedSession, currentSessionId = sessionId)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                emitTransientMessage(uiString(R.string.message_compaction_failed, throwable.userFacingMessage()))
            } finally {
                _uiState.update { current ->
                    if (current.compactingSessionId == sessionId) {
                        current.copy(compactingSessionId = null)
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun String.sanitizeGeneratedSessionTitle(): String =
        lineSequence()
            .map { line ->
                line.trim()
                    .removePrefix("Title:")
                    .removePrefix("title:")
                    .trim()
                    .trim('"', '\'', '`')
            }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
            .trimEnd('.', '!', '?')
            .take(36)

    private fun ensureDraftWorkspaceId(): String {
        val snapshot = _uiState.value
        return snapshot.editingSessionId ?: when {
            snapshot.currentSessionId != DraftSessionId -> snapshot.currentSessionId
            !snapshot.draftWorkspaceId.isNullOrBlank() -> snapshot.draftWorkspaceId.orEmpty()
            else -> {
                val generatedId = "session-${System.currentTimeMillis()}"
                _uiState.update { current ->
                    if (current.currentSessionId == DraftSessionId && current.draftWorkspaceId.isNullOrBlank()) {
                        current.copy(draftWorkspaceId = generatedId)
                    } else {
                        current
                    }
                }
                _uiState.value.draftWorkspaceId ?: generatedId
            }
        }
    }

    private fun ChatMessage.summaryText(): String {
        if (displayKind == MessageDisplayKind.CompactStatus) return text.ifBlank { "Context compacted" }
        val textSummary = text.trim()
        if (textSummary.isNotBlank()) return textSummary
        reasoningTrace?.let { trace ->
            trace.chunks.lastOrNull { it.detail.isNotBlank() || it.title.isNotBlank() }?.let { chunk ->
                return chunk.detail.ifBlank { chunk.title }
            }
            return if (trace.toolInvocations.isNotEmpty()) {
                "Thought and used ${trace.toolInvocations.size} tools"
            } else {
                "Thought"
            }
        }
        if (toolInvocations.isNotEmpty()) {
            return if (toolInvocations.size == 1) {
                when (toolInvocations.first().toolName.lowercase()) {
                    "bash" -> "Ran bash command"
                    else -> "Used ${toolInvocations.first().toolName}"
                }
            } else {
                "Used ${toolInvocations.size} tools"
            }
        }
        if (attachments.isEmpty()) return "Empty message"
        if (attachments.size == 1) return attachments.first().name
        return "${attachments.size} attachments"
    }

    private fun List<ChatMessage>.resolveConversationTrimIndex(
        targetIndex: Int,
    ): Int {
        val targetMessage = getOrNull(targetIndex) ?: return targetIndex
        val responseGroupId = targetMessage.responseGroupId
        if (
            targetMessage.author != MessageAuthor.Agent ||
            responseGroupId.isNullOrBlank()
        ) {
            return targetIndex
        }
        if (responseGroupId.isNullOrBlank()) {
            return resolveLegacyAssistantGroupStartIndex(targetIndex)
        }
        val groupStartIndex = indexOfFirst { message ->
            message.author == MessageAuthor.Agent && message.responseGroupId == responseGroupId
        }
        return if (groupStartIndex >= 0) groupStartIndex else targetIndex
    }

    private fun List<ChatMessage>.resolveLegacyAssistantGroupStartIndex(
        targetIndex: Int,
    ): Int {
        val targetMessage = getOrNull(targetIndex) ?: return targetIndex
        if (targetMessage.author != MessageAuthor.Agent) return targetIndex
        var groupStartIndex = targetIndex
        var expectedCreatedAtMillis = targetMessage.createdAtMillis
        while (groupStartIndex > 0) {
            val previous = this[groupStartIndex - 1]
            if (
                previous.author != MessageAuthor.Agent ||
                !previous.responseGroupId.isNullOrBlank() ||
                previous.createdAtMillis != expectedCreatedAtMillis - 1
            ) {
                break
            }
            groupStartIndex -= 1
            expectedCreatedAtMillis = previous.createdAtMillis
        }
        return groupStartIndex
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024f * 1024f))
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024f)
        else -> "$bytes B"
    }


    private fun performSkillInstall(
        onComplete: (Boolean) -> Unit = {},
        installBlock: suspend () -> Result<InstalledSkill>,
    ) {
        viewModelScope.launch {
            val result = installBlock()
            if (result.isSuccess) {
                piKernelBridge.reloadAllExtensions(runtime.piExtensionStateRepository.loadOptions())
            }
            result
                .onSuccess { installedSkill ->
                    emitTransientMessage(uiString(R.string.message_installed_skill, installedSkill.name))
                    captureAnalyticsEvent(
                        event = "skill installed",
                        properties = mapOf(
                            "skill_id" to installedSkill.id,
                            "skill_name" to installedSkill.name,
                        ),
                    )
                }
                .onFailure { throwable ->
                    emitTransientMessage(
                        uiString(R.string.message_install_skill_failed, throwable.userFacingMessage())
                    )
                }
            onComplete(result.isSuccess)
        }
    }

    private fun maybeCheckForUpdates(settings: AppSettings) {
        val now = System.currentTimeMillis()
        if (now - settings.lastUpdateCheckAtMillis < AppUpdateCheckIntervalMillis) {
            return
        }
        checkForUpdates(manual = false, forceAvailable = false)
    }

    private fun checkForUpdates(
        manual: Boolean,
        forceAvailable: Boolean,
    ) {
        if (_uiState.value.appUpdate.isChecking) return

        _uiState.update { current ->
            current.copy(
                appUpdate = current.appUpdate.copy(
                    isChecking = true,
                    showAvailableDialog = if (manual) false else current.appUpdate.showAvailableDialog,
                )
            )
        }
        viewModelScope.launch {
            val checkedAtMillis = System.currentTimeMillis()
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (BuildConfig.UPDATE_CHANNEL == UpdateChannelNightly) {
                        appUpdateManager.fetchLatestNightly()
                    } else {
                        appUpdateManager.fetchLatestRelease()
                    }
                }
            }
            settingsRepository.updateLastUpdateCheckAtMillis(checkedAtMillis)

            result
                .onSuccess { release ->
                    val hasUpdate = forceAvailable || if (BuildConfig.UPDATE_CHANNEL == UpdateChannelNightly) {
                        isNightlyUpdateNewer(
                            remoteVersion = release.versionName,
                            currentVersion = BuildConfig.VERSION_NAME,
                        )
                    } else {
                        isVersionNewer(
                            remoteVersion = release.versionName,
                            currentVersion = BuildConfig.VERSION_NAME,
                        )
                    }
                    _uiState.update { current ->
                        current.copy(
                            appUpdate = current.appUpdate.copy(
                                isChecking = false,
                                availableRelease = if (hasUpdate) release else current.appUpdate.availableRelease,
                                showAvailableDialog = hasUpdate,
                            )
                        )
                    }
                    if (!hasUpdate && manual) {
                        emitTransientMessage(uiString(R.string.message_aether_up_to_date))
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { current ->
                        current.copy(
                            appUpdate = current.appUpdate.copy(isChecking = false)
                        )
                    }
                    if (manual) {
                        emitTransientMessage(uiString(R.string.message_update_check_failed, throwable.userFacingMessage()))
                    }
                }
        }
    }

    private fun writeTextToUri(
        uri: Uri,
        text: String,
    ): Boolean = runCatching {
        val resolver = getApplication<Application>().contentResolver
        // Open with "rwt" so the destination is truncated before writing. The default "w"
        // mode only truncates when the document provider declares truncation support, and
        // providers that ignore mode entirely (or stream via FUSE) can otherwise leave the
        // exported file at 0 bytes.
        resolver.openOutputStream(uri, "rwt")?.use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
            output.flush()
            (output as? FileOutputStream)?.fd?.sync()
        } ?: return false
        true
    }.getOrDefault(false)

    private fun readTextFromUri(uri: Uri): String =
        getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: error("Unable to read the selected file.")

    private fun buildDiagnosticLogText(snapshot: AetherUiState): String = buildString {
        val logcat = readLogcatDump()
        val diagnosticEvents = diagnosticLogger.readEventsText()
        val lastCrash = diagnosticLogger.readLastCrashText()
        appendLine("Aether diagnostic log")
        appendLine("generatedAtMillis=${System.currentTimeMillis()}")
        appendLine("versionName=${BuildConfig.VERSION_NAME}")
        appendLine("versionCode=${BuildConfig.VERSION_CODE}")
        appendLine("debug=${BuildConfig.DEBUG}")
        appendLine("screen=${snapshot.currentScreen}")
        appendLine("currentSessionId=${snapshot.currentSessionId}")
        appendLine("sessionCount=${snapshot.sessions.size}")
        appendLine("runningSessionCount=${snapshot.sessionExecutionStates.values.count { it.isRunning }}")
        appendLine("piProviderId=${snapshot.settings.piProviderId}")
        appendLine("providerConfigCount=${snapshot.providerConfigs.size}")
        appendLine("skillCount=${snapshot.installedSkills.size}")
        appendLine("mcpServerCount=${snapshot.mcpServers.size}")
        appendLine("termuxReady=${snapshot.termuxSetupState.isReady}")
        appendLine("rootReady=${snapshot.rootSetupState.isReady}")
        appendLine("agentModeAuthorized=${snapshot.agentModeAuthorizationState.isReady}")
        appendLine()
        appendLine("settingsSummary:")
        appendLine(buildSettingsDiagnosticSummary(snapshot).toString(2))
        appendLine()
        appendLine("providerConfigsSummary:")
        appendLine(buildProviderConfigsDiagnosticSummary(snapshot).toString(2))
        appendLine()
        appendLine("mcpServersSummary:")
        appendLine(buildMcpServersDiagnosticSummary(snapshot).toString(2))
        appendLine()
        appendLine("sessionsSummary:")
        appendLine(buildSessionsDiagnosticSummary(snapshot).toString(2))
        appendLine()
        appendLine("lastCrash:")
        appendLine(lastCrash.ifBlank { "No crash breadcrumb recorded." })
        appendLine()
        appendLine("diagnosticEventsJsonl:")
        appendLine(diagnosticEvents.ifBlank { "No diagnostic events recorded." })
        appendLine()
        appendLine("logcatReadStatus:")
        appendLine(logcat.toJson().toString(2))
        appendLine()
        appendLine("logcat:")
        append(logcat.output.ifBlank { logcat.message.ifBlank { "No logcat output." } })
    }

    private fun buildSettingsDiagnosticSummary(snapshot: AetherUiState): JSONObject =
        JSONObject().apply {
            put("piProviderId", snapshot.settings.piProviderId)
            put("modelId", snapshot.settings.modelId)
            put("baseUrl", DiagnosticRedactor.sanitizedBaseUrl(snapshot.settings.baseUrl))
            put("defaultChatModelKey", snapshot.settings.defaultChatModelKey)
            put("defaultTitleModelKey", snapshot.settings.defaultTitleModelKey)
            put("defaultNamingModelKey", snapshot.settings.defaultNamingModelKey)
            put("llmInactivityReconnectTimeoutSeconds", snapshot.settings.llmInactivityReconnectTimeoutSeconds)
            put("keepTasksRunningInBackground", snapshot.settings.keepTasksRunningInBackground)
            put("notifyOnTaskCompletion", snapshot.settings.notifyOnTaskCompletion)
            put("termuxSetupCompleted", snapshot.settings.termuxSetupCompleted)
            put("termuxSetupNoticeDismissed", snapshot.settings.termuxSetupNoticeDismissed)
            put("privacyPolicyAccepted", snapshot.settings.privacyPolicyAccepted)
            put("termux", JSONObject().apply {
                put("issue", snapshot.termuxSetupState.issue.name)
                put("isReady", snapshot.termuxSetupState.isReady)
                put("detail", snapshot.termuxSetupState.detail)
                put("previouslyConfigured", snapshot.termuxSetupState.previouslyConfigured)
            })
            put("root", JSONObject().apply {
                put("issue", snapshot.rootSetupState.issue.name)
                put("isReady", snapshot.rootSetupState.isReady)
                put("detail", snapshot.rootSetupState.detail)
                put("rootAvailable", snapshot.rootSetupState.rootAvailable)
                put("lastUpdatedMillis", snapshot.rootSetupState.lastUpdatedMillis)
            })
            put("agentMode", JSONObject().apply {
                put("authorizationEnabled", snapshot.settings.agentModeAuthorizationEnabled)
                put("authorizationMethod", snapshot.settings.agentModeAuthorizationMethod.storageValue)
                put("authorizationIssue", snapshot.agentModeAuthorizationState.issue.name)
                put("authorizationReady", snapshot.agentModeAuthorizationState.isReady)
                put("authorizationDetail", snapshot.agentModeAuthorizationState.detail)
                put("displayActive", snapshot.agentModeDisplayState.isActive)
                put("displayId", snapshot.agentModeDisplayState.displayId ?: JSONObject.NULL)
                put("displayStatus", snapshot.agentModeDisplayState.status)
                put("livePreviewActive", snapshot.agentModeDisplayState.isLivePreviewActive)
                put("lastUpdatedMillis", snapshot.agentModeDisplayState.lastUpdatedMillis)
            })
        }

    private fun buildProviderConfigsDiagnosticSummary(snapshot: AetherUiState): JSONArray =
        JSONArray().apply {
            snapshot.providerConfigs.forEach { config ->
                put(
                    JSONObject().apply {
                        put("id", config.id)
                        put("name", config.name)
                        put("piProviderId", config.piProviderId)
                        put("baseUrl", DiagnosticRedactor.sanitizedBaseUrl(config.baseUrl))
                        put("modelId", config.modelId)
                        put("cachedModelCount", config.cachedModels.size)
                        put("enabledModelCount", config.enabledModelIds.size)
                        put("isEnabled", config.isEnabled)
                    }
                )
            }
        }

    private fun buildMcpServersDiagnosticSummary(snapshot: AetherUiState): JSONArray =
        JSONArray().apply {
            snapshot.mcpServers.forEach { server ->
                put(
                    JSONObject().apply {
                        put("id", server.id)
                        put("displayName", server.displayName)
                        put("isEnabled", server.isEnabled)
                        put("transportType", server.transport.transportType.storageValue)
                        put("connectTimeoutMillis", server.connectTimeoutMillis)
                        put("requestTimeoutMillis", server.requestTimeoutMillis)
                        when (val transport = server.transport) {
                            is com.zhousl.aether.data.McpTransportConfig.StdIo -> {
                                put("commandSummary", transport.command.lineSequence().firstOrNull().orEmpty().take(160))
                                put("argumentCount", transport.arguments.size)
                                put("workingDirectory", transport.workingDirectory)
                                put("environmentKeyCount", transport.environment.size)
                            }

                            is com.zhousl.aether.data.McpTransportConfig.StreamableHttp -> {
                                put("url", DiagnosticRedactor.sanitizedBaseUrl(transport.url))
                                put("headerKeyCount", transport.headers.size)
                            }
                        }
                    }
                )
            }
        }

    private fun buildSessionsDiagnosticSummary(snapshot: AetherUiState): JSONObject =
        JSONObject().apply {
            put("currentSessionId", snapshot.currentSessionId)
            put("sessionCount", snapshot.sessions.size)
            put(
                "runningSessions",
                JSONArray().apply {
                    snapshot.sessionExecutionStates.values
                        .filter { it.isRunning }
                        .forEach { state ->
                            put(
                                JSONObject().apply {
                                    put("sessionId", state.sessionId)
                                    put("pendingToolCount", state.pendingToolInvocations.size)
                                    put("pendingInputCount", state.pendingInputs.size)
                                    put("activeTurnStartedAtMillis", state.activeTurnStartedAtMillis ?: JSONObject.NULL)
                                    put("pendingStatusText", state.pendingStatusText)
                                    put("pendingStatusDetail", state.pendingStatusDetail)
                                }
                            )
                        }
                },
            )
            put(
                "recentSessions",
                JSONArray().apply {
                    snapshot.sessions
                        .sortedByDescending { session -> session.lastMessageAtMillis ?: 0L }
                        .take(12)
                        .forEach { session ->
                            put(
                                JSONObject().apply {
                                    put("id", session.id)
                                    put("title", session.title)
                                    put("messageCount", session.messageCount)
                                    put("lastMessageAtMillis", session.lastMessageAtMillis ?: 0L)
                                    put("selectedModelKey", session.selectedModelKey)
                                    put("selectedSkillCount", session.selectedSkillIds.size)
                                    put("activeMcpServerCount", session.activeMcpServerIds.size)
                                    put("agentModeEnabled", session.agentModeEnabled)
                                }
                            )
                        }
                },
            )
        }

    private fun readLogcatDump(): LogcatDump {
        val pid = android.os.Process.myPid().toString()
        val commands = listOf(
            listOf("logcat", "-d", "-v", "threadtime", "-t", "4000", "--pid", pid),
            listOf("logcat", "-d", "-v", "threadtime", "-t", "8000"),
        )

        commands.forEach { command ->
            val dump = runCatching { runLogcatCommand(command) }.getOrElse { throwable ->
                LogcatDump(
                    command = command,
                    success = false,
                    exitCode = null,
                    output = "",
                    message = throwable.message ?: "Unable to run logcat command.",
                )
            }
            if (dump.success && dump.output.isNotBlank()) {
                return dump
            }
        }

        return LogcatDump(
            command = emptyList(),
            success = false,
            exitCode = null,
            output = "",
            message = "Unable to read logcat output.",
        )
    }

    private fun runLogcatCommand(command: List<String>): LogcatDump {
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val executor = Executors.newSingleThreadExecutor()
        val outputFuture = executor.submit<String> {
            process.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                reader.readText()
            }
        }
        return try {
            if (!process.waitFor(LogcatReadTimeoutSeconds, TimeUnit.SECONDS)) {
                process.destroy()
                LogcatDump(
                    command = command,
                    success = false,
                    exitCode = null,
                    output = runCatching { outputFuture.get(500, TimeUnit.MILLISECONDS) }.getOrDefault(""),
                    message = "Logcat command timed out.",
                )
            } else {
                val output = outputFuture.get(1, TimeUnit.SECONDS)
                val exitCode = process.exitValue()
                LogcatDump(
                    command = command,
                    success = exitCode == 0 && output.isNotBlank(),
                    exitCode = exitCode,
                    output = output,
                    message = when {
                        exitCode != 0 -> "Logcat command exited with code $exitCode."
                        output.isBlank() -> "Logcat command returned no output."
                        else -> ""
                    },
                )
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private suspend fun buildFullAppExportJson(
        snapshot: AetherUiState,
        sessions: List<ChatSession>,
    ): JSONObject {
        val piSessions = JSONArray()
        sessions.forEach { session ->
            runCatching { piKernelBridge.exportSessionJsonl(session.id) }
                .getOrNull()
                ?.let { exported ->
                    val path = exported.optString("exported_path")
                    val jsonl = path.takeIf(String::isNotBlank)
                        ?.let { filePath -> runCatching { java.io.File(filePath).readText(Charsets.UTF_8) }.getOrNull() }
                    piSessions.put(JSONObject().apply {
                        put("sessionId", session.id)
                        put("jsonlPath", path)
                        put("jsonl", jsonl ?: "")
                    })
                }
        }
        return JSONObject().apply {
            put("schemaVersion", 3)
            put("exportType", "app")
            put("exportedAtMillis", System.currentTimeMillis())
            put("settings", snapshot.settings.toJson())
            put("providerConfigs", JSONArray(serializeProviderConfigs(snapshot.providerConfigs)))
            put("sessions", JSONArray(serializeChatSessions(sessions.map { it.copy(activeSkills = emptyList()) })))
            put("currentSessionId", snapshot.currentSessionId)
            put("skillBundles", skillManager.exportSkillBundles(snapshot.installedSkills))
            put("piSessions", piSessions)
            put("extensionArchive", piExtensionManager.exportArchive())
        }
    }

    private fun parseFullAppImport(
        json: JSONObject,
        installedSkills: List<InstalledSkill>,
    ): ImportedAppData {
        val sessions = sanitizeImportedSessions(
            sessions = parseChatSessions(json.optJSONArray("sessions")?.toString().orEmpty()),
            installedSkillIds = installedSkills.map { it.id }.toSet(),
        )
        val piSessions = buildMap {
            val entries = json.optJSONArray("piSessions") ?: JSONArray()
            for (index in 0 until entries.length()) {
                val item = entries.optJSONObject(index) ?: continue
                val id = item.optString("sessionId").trim()
                val jsonl = item.optString("jsonl")
                if (id.isNotBlank() && jsonl.isNotBlank()) put(id, jsonl)
            }
        }
        return ImportedAppData(
            settings = parseImportedSettings(json.optJSONObject("settings")),
            providerConfigs = parseProviderConfigs(json.optJSONArray("providerConfigs")?.toString().orEmpty()),
            sessions = sessions,
            currentSessionId = json.optString("currentSessionId")
                .takeIf { id -> id == DraftSessionId || sessions.any { it.id == id } }
                ?: DraftSessionId,
            installedSkills = installedSkills,
            mcpServers = emptyList(),
            piSessions = piSessions,
        )
    }

    private fun sanitizeImportedSessions(
        sessions: List<ChatSession>,
        installedSkillIds: Set<String>,
    ): List<ChatSession> =
        sessions.map { session ->
            session.copy(
                selectedSkillIds = session.selectedSkillIds.filter(installedSkillIds::contains),
                activeSkills = emptyList(),
                activeMcpServerIds = emptyList(),
            )
        }

    private fun AppSettings.toJson(): JSONObject = JSONObject().apply {
        put("piProviderId", piProviderId)
        put("providerConfigId", providerConfigId)
        put("providerAuthMethod", providerAuthMethod.storageValue)
        put("apiKey", apiKey)
        put("oauthCredentialJson", oauthCredentialJson)
        put(
            "providerEnvironmentVariables",
            JSONArray().apply {
                providerEnvironmentVariables.forEach { variable ->
                    put(
                        JSONObject()
                            .put("name", variable.name)
                            .put("value", variable.value)
                    )
                }
            },
        )
        put("baseUrl", baseUrl)
        put("modelId", modelId)
        put("userAgent", normalizeLlmUserAgent(userAgent))
        put("customHeaders", customHeaders.toJsonArray())
        put("reasoningEffort", reasoningEffort)
        put("systemPrompt", systemPrompt)
        put("llmInactivityReconnectTimeoutSeconds", llmInactivityReconnectTimeoutSeconds)
        put("keepTasksRunningInBackground", keepTasksRunningInBackground)
        put("notifyOnTaskCompletion", notifyOnTaskCompletion)
        put("agentWorkspaceMode", agentWorkspaceMode.storageValue)
        put("termuxSetupCompleted", termuxSetupCompleted)
        put("termuxSetupNoticeDismissed", termuxSetupNoticeDismissed)
        put(
            "termuxEnvironmentVariables",
            JSONArray().apply {
                termuxEnvironmentVariables.forEach { variable ->
                    put(
                        JSONObject().apply {
                            put("name", variable.name)
                            put("value", variable.value)
                        }
                    )
                }
            },
        )
        put("enabledRuntimeIds", JSONArray().apply { enabledRuntimeIds.forEach { put(it.storageValue) } })
        put("defaultRuntimeId", defaultRuntimeId?.storageValue ?: JSONObject.NULL)
        put("alpineSetupCompleted", alpineSetupCompleted)
        put(
            "alpinePackageProfiles",
            JSONArray().apply {
                alpinePackageProfiles.values.forEach { profile ->
                    put(
                        JSONObject().apply {
                            put("profileId", profile.profileId)
                            put("installed", profile.installed)
                            put("installedAtMillis", profile.installedAtMillis)
                            put("lastError", profile.lastError)
                        }
                    )
                }
            },
        )
        put(
            "alpineEnvironmentVariables",
            JSONArray().apply {
                alpineEnvironmentVariables.forEach { variable ->
                    put(JSONObject().put("name", variable.name).put("value", variable.value))
                }
            },
        )
        put("autoCleanOldCommandHistory", autoCleanOldCommandHistory)
        put("oldCommandHistoryRetentionHours", oldCommandHistoryRetentionHours)
        put("agentModeAuthorizationEnabled", agentModeAuthorizationEnabled)
        put("agentModeAuthorizationMethod", agentModeAuthorizationMethod.storageValue)
        put("language", language.storageValue)
        put("themeMode", themeMode.storageValue)
        put("defaultChatModelKey", defaultChatModelKey)
        put("defaultTitleModelKey", defaultTitleModelKey)
        put("defaultNamingModelKey", defaultNamingModelKey)
        put("defaultCompactingModelKey", defaultCompactingModelKey)
        put("defaultSelectedSkillIds", JSONArray(defaultSelectedSkillIds))
        put("onboardingSeenVersion", onboardingSeenVersion)
        put("onboardingCompletedVersion", onboardingCompletedVersion)
        put("privacyPolicyAccepted", privacyPolicyAccepted)
        put("lastUpdateCheckAtMillis", lastUpdateCheckAtMillis)
    }

    private fun parseImportedSettings(json: JSONObject?): AppSettings {
        if (json == null) return AppSettings()
        val defaults = AppSettings()
        val importedBaseUrl = json.optString("baseUrl", defaults.baseUrl)
        val importedPiProviderId = json.optString("piProviderId").trim().ifBlank {
            com.zhousl.aether.data.inferLegacyPiProviderId(
                json.optString("provider"),
                importedBaseUrl,
            )
        }
        return AppSettings(
            piProviderId = importedPiProviderId,
            providerConfigId = json.optString("providerConfigId"),
            providerAuthMethod = com.zhousl.aether.data.ProviderAuthMethod.fromStorage(
                json.optString("providerAuthMethod"),
            ),
            apiKey = json.optString("apiKey", defaults.apiKey),
            oauthCredentialJson = json.optString(
                "oauthCredentialJson",
                defaults.oauthCredentialJson,
            ),
            providerEnvironmentVariables = com.zhousl.aether.data
                .parseProviderEnvironmentVariables(
                    json.optJSONArray("providerEnvironmentVariables"),
                ),
            baseUrl = importedBaseUrl,
            modelId = json.optString("modelId", defaults.modelId),
            userAgent = normalizeLlmUserAgent(json.optString("userAgent", defaults.userAgent)),
            customHeaders = parseCustomHeaders(json.optJSONArray("customHeaders")),
            reasoningEffort = normalizeReasoningEffort(
                json.optString("reasoningEffort", defaults.reasoningEffort),
            ),
            systemPrompt = json.optString("systemPrompt", defaults.systemPrompt),
            llmInactivityReconnectTimeoutSeconds = normalizeLlmInactivityReconnectTimeoutSeconds(
                json.optInt(
                    "llmInactivityReconnectTimeoutSeconds",
                    defaults.llmInactivityReconnectTimeoutSeconds,
                )
            ),
            keepTasksRunningInBackground = json.optBoolean(
                "keepTasksRunningInBackground",
                defaults.keepTasksRunningInBackground,
            ),
            notifyOnTaskCompletion = json.optBoolean(
                "notifyOnTaskCompletion",
                defaults.notifyOnTaskCompletion,
            ),
            agentWorkspaceMode = AgentWorkspaceMode.fromStorage(
                json.optString("agentWorkspaceMode", defaults.agentWorkspaceMode.storageValue),
            ),
            termuxSetupCompleted = json.optBoolean(
                "termuxSetupCompleted",
                defaults.termuxSetupCompleted,
            ),
            termuxSetupNoticeDismissed = json.optBoolean(
                "termuxSetupNoticeDismissed",
                defaults.termuxSetupNoticeDismissed,
            ),
            termuxEnvironmentVariables = parseImportedTermuxEnvironmentVariables(
                json.optJSONArray("termuxEnvironmentVariables")
            ),
            autoCleanOldCommandHistory = json.optBoolean(
                "autoCleanOldCommandHistory",
                defaults.autoCleanOldCommandHistory,
            ),
            oldCommandHistoryRetentionHours = normalizeOldCommandHistoryRetentionHours(
                json.optInt(
                    "oldCommandHistoryRetentionHours",
                    defaults.oldCommandHistoryRetentionHours,
                )
            ),
            enabledRuntimeIds = parseImportedRuntimeIds(json.optJSONArray("enabledRuntimeIds")),
            defaultRuntimeId = LocalRuntimeId.fromStorage(json.optString("defaultRuntimeId")),
            alpineSetupCompleted = json.optBoolean(
                "alpineSetupCompleted",
                defaults.alpineSetupCompleted,
            ),
            alpinePackageProfiles = parseImportedPackageProfileStates(
                json.optJSONArray("alpinePackageProfiles")
            ),
            alpineEnvironmentVariables = parseImportedAlpineEnvironmentVariables(
                json.optJSONArray("alpineEnvironmentVariables")
            ),
            agentModeAuthorizationEnabled = json.optBoolean(
                "agentModeAuthorizationEnabled",
                defaults.agentModeAuthorizationEnabled,
            ),
            agentModeAuthorizationMethod = AgentModeAuthorizationMethod.fromStorage(
                json.optString("agentModeAuthorizationMethod"),
                defaults.agentModeAuthorizationMethod,
            ),
            language = AppLanguage.fromStorage(
                json.optString("language"),
                defaults.language,
            ),
            themeMode = AppThemeMode.fromStorage(json.optString("themeMode")),
            defaultChatModelKey = json.optString("defaultChatModelKey", defaults.defaultChatModelKey),
            defaultTitleModelKey = json.optString("defaultTitleModelKey", defaults.defaultTitleModelKey),
            defaultNamingModelKey = json.optString("defaultNamingModelKey", defaults.defaultNamingModelKey),
            defaultCompactingModelKey = json.optString(
                "defaultCompactingModelKey",
                defaults.defaultCompactingModelKey,
            ),
            defaultSelectedSkillIds = json.optJSONArray("defaultSelectedSkillIds").toStringList(),
            onboardingSeenVersion = json.optInt("onboardingSeenVersion", defaults.onboardingSeenVersion),
            onboardingCompletedVersion = json.optInt(
                "onboardingCompletedVersion",
                defaults.onboardingCompletedVersion,
            ),
            privacyPolicyAccepted = json.optBoolean(
                "privacyPolicyAccepted",
                defaults.privacyPolicyAccepted,
            ),
            lastUpdateCheckAtMillis = json.optLong(
                "lastUpdateCheckAtMillis",
                defaults.lastUpdateCheckAtMillis,
            ),
        )
    }

    private fun parseImportedStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.optString(index).trim()
                if (value.isNotEmpty()) {
                    add(value)
                }
            }
        }.distinct()
    }

    private fun parseImportedTermuxEnvironmentVariables(
        array: JSONArray?,
    ): List<TermuxEnvironmentVariable> {
        if (array == null) return emptyList()
        return normalizeTermuxEnvironmentVariables(
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        TermuxEnvironmentVariable(
                            name = item.optString("name"),
                            value = item.optString("value"),
                        )
                    )
                }
            }
        )
    }

    private fun parseImportedAlpineEnvironmentVariables(
        array: JSONArray?,
    ): List<AlpineEnvironmentVariable> {
        if (array == null) return emptyList()
        return normalizeAlpineEnvironmentVariables(
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        AlpineEnvironmentVariable(
                            name = item.optString("name"),
                            value = item.optString("value"),
                        )
                    )
                }
            }
        )
    }

    private fun parseImportedRuntimeIds(array: JSONArray?): Set<LocalRuntimeId> {
        if (array == null) return emptySet()
        return buildSet {
            for (index in 0 until array.length()) {
                LocalRuntimeId.fromStorage(array.optString(index))?.let(::add)
            }
        }
    }

    private fun parseImportedPackageProfileStates(
        array: JSONArray?,
    ): Map<String, PackageProfileState> {
        if (array == null) return emptyMap()
        return buildMap {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val profileId = item.optString("profileId").trim()
                if (profileId.isBlank()) continue
                put(
                    profileId,
                    PackageProfileState(
                        profileId = profileId,
                        installed = item.optBoolean("installed", false),
                        installedAtMillis = item.optLong("installedAtMillis", 0L),
                        lastError = item.optString("lastError"),
                    )
                )
            }
        }
    }

    private fun emitTransientMessage(message: UiText) {
        _transientMessages.tryEmit(message)
    }

    private fun uiString(resId: Int, vararg formatArgs: Any): UiText =
        UiText.Resource(resId, formatArgs.toList())

    private fun Throwable.userFacingMessage(): String =
        message?.trim().takeUnless { it.isNullOrBlank() } ?: javaClass.simpleName

    private data class SessionMetadata(
        val title: String,
        val preview: String,
    )

    private data class AttachmentMetadata(
        val displayName: String,
        val mimeType: String,
        val sizeBytes: Long?,
        val kind: AttachmentKind,
    )

    private data class LogcatDump(
        val command: List<String>,
        val success: Boolean,
        val exitCode: Int?,
        val output: String,
        val message: String,
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("command", command.joinToString(" "))
            put("success", success)
            put("exitCode", exitCode ?: JSONObject.NULL)
            put("lineCount", output.lineSequence().count())
            put("message", message)
        }
    }

    private data class ImportedAppData(
        val settings: AppSettings,
        val providerConfigs: List<LlmProviderConfig>,
        val sessions: List<ChatSession>,
        val currentSessionId: String,
        val installedSkills: List<InstalledSkill>,
        val mcpServers: List<McpServerConfig>,
        val piSessions: Map<String, String> = emptyMap(),
    )
}

internal fun AetherUiState.withFinalizedPausedSession(
    finalizedSession: ChatSession,
    executionStates: Map<String, SessionExecutionState>,
): AetherUiState {
    val currentExecution = executionStates[currentSessionId]
    val updatedSessions = if (sessions.any { it.id == finalizedSession.id }) {
        sessions.map { if (it.id == finalizedSession.id) finalizedSession else it }
    } else {
        listOf(finalizedSession) + sessions
    }
    return copy(
        sessions = updatedSessions,
        sessionExecutionStates = executionStates,
        isSending = currentExecution?.isRunning == true,
        pendingResponseSessionId = currentExecution?.sessionId,
        pendingToolInvocations = currentExecution?.pendingToolInvocations.orEmpty(),
        pendingResponseBlocks = currentExecution?.pendingResponseBlocks.orEmpty(),
        pendingAssistantText = currentExecution?.pendingAssistantText.orEmpty(),
        pendingStatusText = currentExecution?.pendingStatusText.orEmpty(),
        pendingStatusDetail = currentExecution?.pendingStatusDetail.orEmpty(),
    )
}

private fun AetherUiState.isTermuxReadyForAgentMode(): Boolean =
    developerTermuxReadyOverride ?: (
        termuxSetupState.isReady ||
            rootSetupState.isReady ||
            (
                settings.agentModeAuthorizationEnabled &&
                    settings.agentModeAuthorizationMethod == AgentModeAuthorizationMethod.Root &&
                    agentModeAuthorizationState.isReady
                )
        )

private fun AetherUiState.isAgentModeReady(): Boolean =
    settings.agentModeAuthorizationEnabled &&
        agentModeAuthorizationState.isReady &&
        isTermuxReadyForAgentMode()

private fun AppSettings.withRuntimeEnabled(
    runtimeId: LocalRuntimeId,
    makeDefault: Boolean = defaultRuntimeId == null,
): AppSettings {
    val enabled = enabledRuntimeIds + runtimeId
    return copy(
        termuxSetupCompleted = termuxSetupCompleted || runtimeId == LocalRuntimeId.Termux,
        alpineSetupCompleted = alpineSetupCompleted || runtimeId == LocalRuntimeId.Alpine,
        enabledRuntimeIds = enabled,
        defaultRuntimeId = if (makeDefault) runtimeId else defaultRuntimeId,
    )
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
        }
    }.distinct()
}
