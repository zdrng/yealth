@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.zdrng.yealth.ui.navigation

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.navigation
import dev.zdrng.yealth.ui.theme.YealthRounded
import dev.zdrng.yealth.ui.theme.LocalMockupColors
import dev.zdrng.yealth.ui.theme.Ink
import dev.zdrng.yealth.R
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.domain.service.HealthBrowserService
import dev.zdrng.yealth.ui.components.DateDialog
import dev.zdrng.yealth.ui.feature.*

private enum class Root(val route: String, val label: Int, val icon: ImageVector) {
    TODAY("today", R.string.today, Icons.Default.Home),
    BROWSE("browse", R.string.browse, Icons.AutoMirrored.Filled.List),
    ACCESS("access", R.string.access, Icons.Default.Person),
}

@Composable
fun YealthApp(content: BrowserContent, access: AccessViewModel? = null, service: HealthBrowserService? = null,
    requestPermissions: (Set<String>) -> Unit = {}, requestHistory: () -> Unit = {},
    openSettings: () -> Unit = {}, recover: () -> Unit = {}, routeState: RouteUiState = RouteUiState(), requestRoute: (String) -> Unit = {}) {
    val accessState = access?.uiState?.collectAsStateWithLifecycle()?.value ?: AccessUiState()
    var showHistory by rememberSaveable { mutableStateOf(false) }

    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val root = Root.entries.firstOrNull { item -> current?.destination?.hierarchy?.any { it.route == item.route } == true } ?: Root.TODAY
    val focused = current?.destination?.route?.contains("/record/") == true
    val metricTitle = current?.arguments?.getString("type")?.let { typeLabels[it] }
    val atRoot = current?.destination?.route?.endsWith("/home") != false
    var showAccessInfo by rememberSaveable { mutableStateOf(false) }
    var accessCategory by rememberSaveable { mutableStateOf<String?>(null) }
    fun openAccess(category: HealthCategory? = null) {
        accessCategory = category?.name
        showAccessInfo = true
    }
    fun selectRoot(destination: Root) {
        if (destination == root) return
        nav.navigate(destination.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    if (showAccessInfo) {
        val category = accessCategory?.let { HealthCategory.valueOf(it) }
        if (content.isPreview) AccessDialog(category, content) { showAccessInfo = false }
        else if (category == null) AlertDialog(onDismissRequest = { showAccessInfo = false },
            title = { Text(stringResource(R.string.privacy)) }, text = { Text(stringResource(R.string.privacy_live), Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton({ showAccessInfo = false; openSettings() }) { Text(stringResource(R.string.health_settings)) } },
            dismissButton = { TextButton({ showAccessInfo = false }) { Text(stringResource(R.string.close)) } })
        else {
            val permissions = access?.permissions(category) ?: PermissionState(emptySet(), emptySet())
            PermissionExplanation(category, accessTypes(content.catalog, category, permissions), false,
                permissions.missing.isNotEmpty(), { showAccessInfo = false }, {
                    showAccessInfo = false
                    requestPermissions(access?.permissions(category)?.missing.orEmpty())
                })
        }
    }
    if (showHistory) PermissionExplanation(null, emptyList(), true,
        accessState.environment?.let { it.provider == ProviderStatus.AVAILABLE && HealthFeature.HISTORY in it.features && !it.historyGranted } == true,
        { showHistory = false }, { showHistory = false; requestHistory() })
    if (accessState.actionFailed) AlertDialog(onDismissRequest = { access?.clearActionFailure() },
        text = { Text(stringResource(R.string.action_failed)) },
        confirmButton = { TextButton({ access?.clearActionFailure() }) { Text(stringResource(R.string.close)) } })
    BoxWithConstraints {
        val rail = maxWidth >= 600.dp
        Row(Modifier.fillMaxSize()) {
            if (rail && !focused) NavigationRail(Modifier.fillMaxHeight().testTag("navigation-rail")) {
                Spacer(Modifier.weight(1f))
                Root.entries.forEach { item ->
                    NavigationRailItem(selected = root == item, onClick = { selectRoot(item) },
                        icon = { Icon(item.icon, null) }, label = { Text(stringResource(item.label)) }, modifier = Modifier.testTag("nav-${item.route}"))
                }
                Spacer(Modifier.weight(1f))
            }
            Scaffold(modifier = Modifier.weight(1f),
                topBar = {
                    Column {
                        Row(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLargeEmphasized,
                                color = MaterialTheme.colorScheme.secondary, fontSize = 24.sp, fontFamily = YealthRounded, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
                            FilledIconButton({ openAccess() }, Modifier.size(42.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.secondary)) {
                                Icon(if (atRoot && root != Root.ACCESS) Icons.Default.Settings else Icons.Default.MoreVert, stringResource(R.string.privacy), Modifier.size(21.dp))
                            }
                        }
                        if (!atRoot) Row(Modifier.fillMaxWidth().padding(start = 16.dp)) {
                            FilledIconButton({ nav.popBackStack() }, Modifier.size(40.dp).testTag("back"),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.secondary)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                            }
                        }
                    }
                },
                bottomBar = {
                    if (!rail && !focused) Surface(Modifier.fillMaxWidth().testTag("navigation-bar"),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)) {
                        Row(Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 5.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Root.entries.forEach { item ->
                                val selected = root == item
                                Column(Modifier.weight(1f).clip(CircleShape)
                                    .background(if (selected) LocalMockupColors.current.lilac else androidx.compose.ui.graphics.Color.Transparent)
                                    .selectable(selected = selected, role = Role.Tab, onClick = { selectRoot(item) })
                                    .testTag("nav-${item.route}").padding(vertical = 9.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    val tint = if (selected) Ink else MaterialTheme.colorScheme.secondary
                                    if (item == Root.BROWSE) Canvas(Modifier.size(24.dp)) {
                                        val side = size.width * .38f
                                        for (x in 0..1) for (y in 0..1) drawRoundRect(tint,
                                            Offset(x * size.width * .55f, y * size.height * .55f), Size(side, side), CornerRadius(size.width * .1f), style = if (selected) Fill else Stroke(2.dp.toPx()))
                                    } else Icon(if (selected) item.icon else if (item == Root.TODAY) Icons.Outlined.Home else Icons.Outlined.Person, null, Modifier.size(24.dp), tint = tint)
                                    Text(stringResource(item.label), color = tint, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }) { padding ->
                val motion = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                NavHost(navController = nav, startDestination = Root.TODAY.route, modifier = Modifier.padding(padding).consumeWindowInsets(padding),
                    enterTransition = { fadeIn(motion) }, exitTransition = { fadeOut(motion) }, popEnterTransition = { fadeIn(motion) }, popExitTransition = { fadeOut(motion) }) {
                    Root.entries.forEach { destination ->
                        val prefix = destination.route
                        navigation(startDestination = "$prefix/home", route = prefix) {
                            composable("$prefix/home") { entry ->
                                ScreenEntry(entry, content) { vm, state, chooseDate ->
                                    fun metric(id: String) {
                                        val period = if (id in listOf("vo2_max", "resting_heart_rate")) Period.DAYS_30 else state.period
                                        nav.navigate("$prefix/metric/${Uri.encode(id)}?date=${state.date}&period=${period.name}")
                                    }
                                    val stepSummary = if (destination == Root.TODAY && !content.isPreview)
                                        observedStepsSummary(entry, service!!, access!!, state) else null
                                    val overviewSelection = OverviewSelection(listOf("sleep_session", "heart_rate"), state.date, state.period)
                                    val overview = if (destination == Root.TODAY && !content.isPreview)
                                        observedOverview(entry, service!!, access!!, overviewSelection) else null
                                    when (destination) {
                                        Root.TODAY -> TodayScreen(content, state, chooseDate, vm::moveDate, ::metric, { openAccess() },
                                            stepsSummary = if (content.isPreview) null else { {
                                                StepsSummaryCard(stepSummary!!, accessState, LiveSelection("steps", state.date, state.period), { metric("steps") }, { access!!.refresh() }, "summary-steps")
                                            } }, openActivity = { nav.navigate("$prefix/category/ACTIVITY?date=${state.date}") },
                                            otherSummary = if (overview == null) null else { { id ->
                                                LiveOverviewCard(id, overview, accessState, overviewSelection, { metric(id) }, "summary-$id")
                                            } })
                                        Root.BROWSE -> BrowseScreen(content, state, vm::search,
                                            { nav.navigate("$prefix/category/${it.name}?date=${state.date}") }, ::metric)
                                        Root.ACCESS -> if (content.isPreview) AccessScreen(content, { openAccess(it) }, { openAccess() })
                                        else LiveAccessScreen(content, accessState, { access!!.permissions(it) }, { openAccess(it) },
                                            { showHistory = true }, openSettings, recover, { access?.refresh() })
                                    }
                                }
                            }
                            composable("$prefix/category/{category}?date={date}", arguments = listOf(navArgument("date") { defaultValue = content.initialDate.toString() })) { entry ->
                                ScreenEntry(entry, content) { vm, state, chooseDate ->
                                    val category = HealthCategory.valueOf(requireNotNull(entry.arguments?.getString("category")))
                                    val openMetric: (String) -> Unit = { id ->
                                        val period = if (id in listOf("vo2_max", "resting_heart_rate")) Period.DAYS_30 else state.period
                                        nav.navigate("$prefix/metric/${Uri.encode(id)}?date=${state.date}&period=${period.name}")
                                    }
                                    val stepSummary = if (category == HealthCategory.ACTIVITY && !content.isPreview)
                                        observedStepsSummary(entry, service!!, access!!, state) else null
                                    val overviewSelection = OverviewSelection(content.catalog.filter { it.category == category && it.id != "steps" }.map { it.id }, state.date, state.period)
                                    val overview = if (!content.isPreview) observedOverview(entry, service!!, access!!, overviewSelection) else null
                                    CategoryScreen(category, content, state, chooseDate, vm::moveDate, vm::selectPeriod, openMetric,
                                        stepsSummary = if (content.isPreview || category != HealthCategory.ACTIVITY) null else { {
                                            StepsSummaryCard(stepSummary!!, accessState, LiveSelection("steps", state.date, state.period), { openMetric("steps") }, { access!!.refresh() }, "type-steps")
                                        } }, categoryEmpty = overview != null && overview.selection == overviewSelection && overview.accessRevision == accessState.revision &&
                                            accessState.foreground && overviewSelection.types.isNotEmpty() && overviewSelection.types.all { overview.records[it] is RecordsState.Empty },
                                        typeContent = if (overview == null) null else { { id ->
                                            LiveOverviewCard(id, overview, accessState, overviewSelection, { openMetric(id) }, "type-$id")
                                        } })
                                }
                            }
                            composable("$prefix/metric/{type}?date={date}&period={period}", arguments = listOf(
                                navArgument("date") { defaultValue = content.initialDate.toString() }, navArgument("period") { defaultValue = Period.DAY.name })) { entry ->
                                ScreenEntry(entry, content) { vm, state, chooseDate ->
                                    val type = requireNotNull(entry.arguments?.getString("type"))
                                    val live = if (!content.isPreview) liveViewModel(entry, service!!, access!!) else null
                                    val liveState = live?.uiState?.collectAsStateWithLifecycle()?.value
                                    LaunchedEffect(type, state.date, state.period) { live?.select(LiveSelection(type, state.date, state.period)) }
                                    val openRecord: (String) -> Unit = { id -> nav.navigate("$prefix/record/${Uri.encode(type)}/${Uri.encode(id)}") }
                                    val stepSummary = if (type == "steps" && !content.isPreview)
                                        observedStepsSummary(entry, service!!, access!!, state, includeDaily = true) else null
                                    val overviewSelection = OverviewSelection(listOf(type), state.date, state.period)
                                    val overview = if (type in OverviewViewModel.SUMMARY_TYPES && !content.isPreview)
                                        observedOverview(entry, service!!, access!!, overviewSelection) else null
                                    MetricScreen(type, content, state, chooseDate, vm::moveDate, vm::selectPeriod, openRecord, { openAccess() },
                                        if (live == null) null else { {
                                            if (type == "steps") item {
                                                StepsSummaryCard(stepSummary!!, accessState, LiveSelection("steps", state.date, state.period), null, { access!!.refresh() }, "metric-steps-total", detailed = true)
                                            }
                                            if (overview != null) item {
                                                LiveMetricSummary(type, overview, accessState, overviewSelection)
                                            }
                                            val currentSelection = LiveSelection(type, state.date, state.period)
                                            val currentLive = liveState!!.takeIf { it.selection == currentSelection }
                                                ?: LiveRecordsUiState(accessRevision = accessState.revision)
                                            liveRecordsItems(content.catalog.single { it.id == type }, accessState, currentLive,
                                                { openAccess(content.catalog.single { it.id == type }.category) }, { showHistory = true }, recover, openSettings,
                                                { access?.refresh() }, live::retry, live::loadMore, chooseDate, openRecord, showHero = overview == null && type != "steps")
                                        } })
                                }
                            }
                            composable("$prefix/record/{type}/{record}") { entry ->
                                ScreenEntry(entry, content) { vm, _, _ ->
                                    val type = requireNotNull(entry.arguments?.getString("type"))
                                    val id = requireNotNull(entry.arguments?.getString("record"))
                                    if (content.isPreview) RecordScreen(type, vm.record(id), { openAccess() })
                                    else {
                                        val metricEntry = remember(entry) { nav.getBackStackEntry("$prefix/metric/{type}?date={date}&period={period}") }
                                        val live = liveViewModel(metricEntry, service!!, access!!)
                                        val liveState by live.uiState.collectAsStateWithLifecycle()
                                        val parentVm: BrowserViewModel = viewModel(viewModelStoreOwner = metricEntry, factory = viewModelFactory {
                                            initializer { BrowserViewModel(content, createSavedStateHandle()) }
                                        })
                                        val selected by parentVm.uiState.collectAsStateWithLifecycle()
                                        LaunchedEffect(type, selected.date, selected.period) { live.select(LiveSelection(type, selected.date, selected.period)) }
                                        val record = liveState.records.singleOrNull { it.metadata.id == id }
                                        if (accessState.environment?.provider == ProviderStatus.AVAILABLE && liveState.accessRevision == accessState.revision && record != null) {
                                            val route = (record.fields.singleOrNull { it.key == "exerciseRouteResult" }?.value as? HealthValue.Fields)
                                            val needsConsent = (route?.fields?.singleOrNull { it.key == "status" }?.value as? HealthValue.Text)?.value == "consent_required"
                                            val supplied = routeState.locations.takeIf { routeState.recordId == id && accessState.foreground }
                                            val displayed = if (supplied == null) record else record.copy(fields = record.fields.map {
                                                if (it.key == "exerciseRouteResult") RecordField(it.key, HealthValue.Fields(listOf(
                                                    RecordField("status", HealthValue.Text("available")), RecordField("locations", supplied)))) else it
                                            })
                                            RecordScreen(type, displayed, openSettings, routeContent = if (!needsConsent || supplied != null) null else { {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(stringResource(R.string.route_consent_title), style = MaterialTheme.typography.titleMedium)
                                                    Text(stringResource(R.string.route_consent_body))
                                                    if (routeState.recordId == id && (routeState.cancelled || routeState.failed))
                                                        Text(stringResource(if (routeState.failed) R.string.route_failed else R.string.route_cancelled))
                                                    Button({ requestRoute(id) }, Modifier.testTag("request-route")) { Text(stringResource(R.string.request_route)) }
                                                }
                                            } })
                                        }
                                        else dev.zdrng.yealth.ui.components.ScreenList("screen-record") {
                                            item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                if (accessState.environment?.provider == ProviderStatus.AVAILABLE && liveState.accessRevision == accessState.revision &&
                                                    (liveState.state is RecordsState.Content || liveState.state is RecordsState.Empty)) {
                                                    Text(stringResource(R.string.record_unavailable))
                                                    if ((liveState.state as? RecordsState.Content)?.page?.next != null) Button(live::loadMore, enabled = !liveState.loadingMore) {
                                                        Text(stringResource(R.string.load_more))
                                                    }
                                                    TextButton(live::retry) { Text(stringResource(R.string.refresh_records)) }
                                                } else LiveRecordsPanel(content.catalog.single { it.id == type }, accessState, liveState,
                                                    { openAccess(content.catalog.single { it.id == type }.category) }, { showHistory = true }, recover, openSettings,
                                                    access::refresh, live::retry, live::loadMore, {}, {})
                                            } }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenEntry(entry: NavBackStackEntry, content: BrowserContent,
    screen: @Composable (BrowserViewModel, BrowserUiState, () -> Unit) -> Unit) {
    val factory = remember(content) { viewModelFactory { initializer { BrowserViewModel(content, createSavedStateHandle()) } } }
    val vm: BrowserViewModel = viewModel(viewModelStoreOwner = entry, factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    var chooseDate by rememberSaveable { mutableStateOf(false) }
    if (chooseDate) DateDialog(state.date, { chooseDate = false }, vm::selectDate)
    screen(vm, state) { chooseDate = true }
}

@Composable
private fun liveViewModel(entry: NavBackStackEntry, service: HealthBrowserService, access: AccessViewModel): LiveRecordsViewModel {
    val vm: LiveRecordsViewModel = viewModel(viewModelStoreOwner = entry, factory = remember(service, access) {
        viewModelFactory { initializer { LiveRecordsViewModel(service, access.uiState) } }
    })
    DisposableEffect(vm) { vm.attach(); onDispose { vm.detach() } }
    return vm
}

@Composable
private fun observedStepsSummary(entry: NavBackStackEntry, service: HealthBrowserService, access: AccessViewModel,
    state: BrowserUiState, includeDaily: Boolean = false): StepsSummaryUiState {
    val vm: StepsSummaryViewModel = viewModel(viewModelStoreOwner = entry, factory = remember(service, access) {
        viewModelFactory { initializer { StepsSummaryViewModel(service, access.uiState, includeDaily = includeDaily) } }
    })
    DisposableEffect(vm) { vm.attach(); onDispose { vm.detach() } }
    val selection = LiveSelection("steps", state.date, state.period)
    LaunchedEffect(selection) { vm.select(selection) }
    val summary by vm.uiState.collectAsStateWithLifecycle()
    return summary
}

@Composable
private fun observedOverview(entry: NavBackStackEntry, service: HealthBrowserService, access: AccessViewModel,
    selection: OverviewSelection): OverviewUiState {
    val vm: OverviewViewModel = viewModel(viewModelStoreOwner = entry, factory = remember(service, access) {
        viewModelFactory { initializer { OverviewViewModel(service, access.uiState) } }
    })
    DisposableEffect(vm) { vm.attach(); onDispose { vm.detach() } }
    LaunchedEffect(selection) { vm.select(selection) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    return state
}
