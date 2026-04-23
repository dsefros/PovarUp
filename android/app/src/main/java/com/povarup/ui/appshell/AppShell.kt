package com.povarup.ui.appshell

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.povarup.core.environment.AppMode
import com.povarup.core.environment.AppRole
import com.povarup.core.environment.LocalAppEnvironment
import com.povarup.data.repository.demo.ScenarioMarketplaceRepository
import com.povarup.ui.business.applicants.ApplicantsScreen
import com.povarup.ui.business.home.BusinessHomeScreen
import com.povarup.ui.business.operations.OperationsScreen
import com.povarup.ui.business.operations.PayoutReleaseScreen
import com.povarup.ui.business.shifts.BusinessShiftCreateScreen
import com.povarup.ui.business.shifts.BusinessShiftDetailScreen
import com.povarup.ui.business.shifts.BusinessShiftsScreen
import com.povarup.ui.shared.components.ErrorState
import com.povarup.ui.shared.components.LoadingState
import com.povarup.ui.shared.components.ScreenState
import com.povarup.ui.shared.navigation.Screen
import com.povarup.ui.worker.applications.ApplicationsScreen
import com.povarup.ui.worker.assignments.AssignmentDetailScreen
import com.povarup.ui.worker.assignments.AssignmentsScreen
import com.povarup.ui.worker.chat.ChatScreen
import com.povarup.ui.worker.chat.ProfileScreen
import com.povarup.ui.worker.home.WorkerHomeScreen
import com.povarup.ui.worker.payouts.PayoutsScreen
import com.povarup.ui.worker.shifts.ShiftDetailScreen
import com.povarup.ui.worker.shifts.ShiftFeedScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(viewModel: AppStateViewModel) {
    val navController = rememberNavController()
    val env by viewModel.environment.collectAsStateWithLifecycle()
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    val envKey = "${env.mode}-${env.role.label}-${env.scenarioId ?: "none"}"

    val workerBottom = listOf(Screen.WorkerHome, Screen.ShiftFeed, Screen.Applications, Screen.Assignments, Screen.Payouts, Screen.Profile)
    val businessBottom = listOf(Screen.BusinessHome, Screen.BusinessShifts, Screen.Operations, Screen.Profile)
    val activeBottom = if (env.role is AppRole.Worker) workerBottom else businessBottom

    CompositionLocalProvider(LocalAppEnvironment provides env) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("PovarUp • ${env.role.label} • ${env.mode.name}") }) },
            bottomBar = {
                NavigationBar {
                    activeBottom.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            },
                            label = { Text(screen.route.substringBefore("/")) },
                            icon = {}
                        )
                    }
                }
            }
        ) { padding ->
            key(env.mode, env.role) {
                NavHost(
                    navController = navController,
                    startDestination = if (env.role is AppRole.Worker) Screen.WorkerHome.route else Screen.BusinessHome.route,
                    modifier = Modifier.padding(padding)
                ) {
                    composable(Screen.WorkerHome.route) {
                        val shiftsVm: ShiftsViewModel = viewModel(key = "worker-home-shifts-$envKey", factory = ShiftsViewModel.Factory(viewModel.dataSource()))
                        val assignmentsVm: AssignmentsViewModel = viewModel(key = "worker-home-assignments-$envKey", factory = AssignmentsViewModel.Factory(viewModel.dataSource()))
                        val appsVm: ApplicationsViewModel = viewModel(key = "worker-home-apps-$envKey", factory = ApplicationsViewModel.Factory(viewModel.dataSource()))
                        val shifts by shiftsVm.state.collectAsStateWithLifecycle()
                        val assignments by assignmentsVm.state.collectAsStateWithLifecycle()
                        val apps by appsVm.state.collectAsStateWithLifecycle()
                        WorkerHomeScreen(
                            activeAssignment = (assignments as? ScreenState.Success)?.data?.firstOrNull(),
                            upcomingShifts = (shifts as? ScreenState.Success)?.data.orEmpty(),
                            applicationUpdates = (apps as? ScreenState.Success)?.data.orEmpty(),
                            onGoToFeed = { navController.navigate(Screen.ShiftFeed.route) },
                            onOpenApplications = { navController.navigate(Screen.Applications.route) }
                        )
                    }
                    composable(Screen.ShiftFeed.route) {
                        val vm: ShiftsViewModel = viewModel(key = "shift-feed-$envKey", factory = ShiftsViewModel.Factory(viewModel.dataSource()))
                        when (val state = vm.state.collectAsStateWithLifecycle().value) {
                            is ScreenState.Loading -> LoadingState()
                            is ScreenState.Error -> ErrorState(state.message) { vm.refresh() }
                            is ScreenState.Success -> ShiftFeedScreen(
                                shifts = state.data,
                                capabilities = env.capabilities,
                                onOpenDetail = { navController.navigate(Screen.ShiftDetail.withId(it)) },
                                onApply = vm::apply
                            )
                        }
                    }
                    composable(Screen.ShiftDetail.route, arguments = listOf(navArgument("shiftId") { type = NavType.StringType })) { backStack ->
                        val shiftId = backStack.arguments?.getString("shiftId") ?: return@composable
                        val vm: ShiftDetailViewModel = viewModel(
                            key = "shift-detail-$shiftId-$envKey",
                            factory = ShiftDetailViewModel.Factory(viewModel.dataSource(), shiftId)
                        )
                        val detailMessage by vm.message.collectAsStateWithLifecycle()
                        when (val state = vm.state.collectAsStateWithLifecycle().value) {
                            is ScreenState.Loading -> LoadingState()
                            is ScreenState.Error -> ErrorState(state.message, vm::refresh)
                            is ScreenState.Success -> ShiftDetailScreen(
                                shift = state.data,
                                capabilities = env.capabilities,
                                onApply = vm::applyToShift,
                                infoMessage = detailMessage
                            )
                        }
                    }
                    composable(Screen.Applications.route) {
                        val vm: ApplicationsViewModel = viewModel(key = "applications-$envKey", factory = ApplicationsViewModel.Factory(viewModel.dataSource()))
                        when (val state = vm.state.collectAsStateWithLifecycle().value) {
                            is ScreenState.Success -> ApplicationsScreen(state.data)
                            is ScreenState.Loading -> LoadingState()
                            is ScreenState.Error -> ErrorState(state.message)
                        }
                    }
                    composable(Screen.Assignments.route) {
                        val vm: AssignmentsViewModel = viewModel(key = "assignments-$envKey", factory = AssignmentsViewModel.Factory(viewModel.dataSource()))
                        when (val state = vm.state.collectAsStateWithLifecycle().value) {
                            is ScreenState.Success -> AssignmentsScreen(state.data) { navController.navigate(Screen.AssignmentDetail.withId(it)) }
                            is ScreenState.Loading -> LoadingState()
                            is ScreenState.Error -> ErrorState(state.message)
                        }
                    }
                    composable(Screen.AssignmentDetail.route, arguments = listOf(navArgument("assignmentId") { type = NavType.StringType })) { backStack ->
                        val id = backStack.arguments?.getString("assignmentId") ?: return@composable
                        val vm: AssignmentDetailViewModel = viewModel(
                            key = "assignment-detail-$id-$envKey",
                            factory = AssignmentDetailViewModel.Factory(viewModel.dataSource(), id)
                        )
                        val message by vm.message.collectAsStateWithLifecycle()
                        when (val state = vm.state.collectAsStateWithLifecycle().value) {
                            is ScreenState.Success -> AssignmentDetailScreen(
                                assignment = state.data,
                                capabilities = env.capabilities,
                                canCheckIn = vm.canCheckIn(state.data),
                                canCheckOut = vm.canCheckOut(state.data),
                                message = message,
                                onCheckIn = vm::checkIn,
                                onCheckOut = vm::checkOut,
                                onChat = { navController.navigate(Screen.Chat.withId(id)) }
                            )
                            is ScreenState.Loading -> LoadingState()
                            is ScreenState.Error -> ErrorState(state.message)
                        }
                    }
                    composable(Screen.Payouts.route) {
                        val vm: PayoutsViewModel = viewModel(key = "payouts-$envKey", factory = PayoutsViewModel.Factory(viewModel.dataSource()))
                        when (val state = vm.state.collectAsStateWithLifecycle().value) {
                            is ScreenState.Success -> PayoutsScreen(state.data)
                            is ScreenState.Loading -> LoadingState()
                            is ScreenState.Error -> ErrorState(state.message)
                        }
                    }
                    composable(Screen.BusinessHome.route) {
                        val shiftsVm: ShiftsViewModel = viewModel(key = "business-home-shifts-$envKey", factory = ShiftsViewModel.Factory(viewModel.dataSource()))
                        val shiftCount = ((shiftsVm.state.collectAsStateWithLifecycle().value as? ScreenState.Success)?.data?.size ?: 0)
                        val waiting = 0 // Demo-safe summary: avoid fake-ID lookups in production paths
                        BusinessHomeScreen(shiftCount, waiting) { navController.navigate(Screen.BusinessShiftCreate.route) }
                    }
                    composable(Screen.BusinessShifts.route) {
                        val vm: ShiftsViewModel = viewModel(key = "business-shifts-$envKey", factory = ShiftsViewModel.Factory(viewModel.dataSource()))
                        when (val state = vm.state.collectAsStateWithLifecycle().value) {
                            is ScreenState.Success -> BusinessShiftsScreen(state.data) { navController.navigate(Screen.BusinessShiftDetail.withId(it)) }
                            is ScreenState.Loading -> LoadingState()
                            is ScreenState.Error -> ErrorState(state.message)
                        }
                    }
                    composable(Screen.BusinessShiftCreate.route) {
                        val vm: BusinessShiftCreateViewModel = viewModel(key = "business-shift-create-$envKey", factory = BusinessShiftCreateViewModel.Factory(viewModel.dataSource()))
                        val form by vm.form.collectAsStateWithLifecycle()
                        val message by vm.message.collectAsStateWithLifecycle()
                        BusinessShiftCreateScreen(
                            form = form,
                            message = message,
                            publishEnabled = vm.publishEnabled,
                            publishDisabledReason = vm.publishDisabledReason,
                            onUpdate = vm::update,
                            onSaveDraft = vm::createDraft,
                            onPublish = {}
                        )
                    }
                    composable(Screen.BusinessShiftDetail.route, arguments = listOf(navArgument("shiftId") { type = NavType.StringType })) { backStack ->
                        val id = backStack.arguments?.getString("shiftId") ?: return@composable
                        val vm: ShiftDetailViewModel = viewModel(
                            key = "business-shift-detail-$id-$envKey",
                            factory = ShiftDetailViewModel.Factory(viewModel.dataSource(), id)
                        )
                        when (val state = vm.state.collectAsStateWithLifecycle().value) {
                            is ScreenState.Success -> BusinessShiftDetailScreen(
                                shift = state.data,
                                onEdit = { navController.navigate(Screen.BusinessShiftCreate.route) },
                                onApplicants = { navController.navigate(Screen.Applicants.withId(id)) }
                            )
                            is ScreenState.Loading -> LoadingState()
                            is ScreenState.Error -> ErrorState(state.message)
                        }
                    }
                    composable(Screen.Applicants.route, arguments = listOf(navArgument("shiftId") { type = NavType.StringType })) { backStack ->
                        val id = backStack.arguments?.getString("shiftId") ?: return@composable
                        val vm: ShiftApplicantsViewModel = viewModel(
                            key = "applicants-$id-$envKey",
                            factory = ShiftApplicantsViewModel.Factory(viewModel.dataSource(), id)
                        )
                        when (val state = vm.state.collectAsStateWithLifecycle().value) {
                            is ScreenState.Success -> ApplicantsScreen(state.data, vm::assign, vm::reject)
                            is ScreenState.Loading -> LoadingState()
                            is ScreenState.Error -> ErrorState(state.message)
                        }
                    }
                    composable(Screen.Operations.route) {
                        val vm: AssignmentsViewModel = viewModel(key = "operations-$envKey", factory = AssignmentsViewModel.Factory(viewModel.dataSource()))
                        when (val state = vm.state.collectAsStateWithLifecycle().value) {
                            is ScreenState.Success -> OperationsScreen(state.data, { navController.navigate(Screen.AssignmentDetail.withId(it)) }, { navController.navigate(Screen.PayoutRelease.route) })
                            is ScreenState.Loading -> LoadingState()
                            is ScreenState.Error -> ErrorState(state.message)
                        }
                    }
                    composable(Screen.PayoutRelease.route) {
                        val vm: PayoutReleaseViewModel = viewModel(key = "payout-release-$envKey", factory = PayoutReleaseViewModel.Factory(viewModel.dataSource()))
                        when (val state = vm.state.collectAsStateWithLifecycle().value) {
                            is ScreenState.Success -> PayoutReleaseScreen(state.data, vm::canRelease, vm::release)
                            is ScreenState.Loading -> LoadingState()
                            is ScreenState.Error -> ErrorState(state.message)
                        }
                    }
                    composable(Screen.Chat.route, arguments = listOf(navArgument("threadId") { type = NavType.StringType })) { backStack ->
                        ChatScreen(backStack.arguments?.getString("threadId") ?: "general")
                    }
                    composable(Screen.Profile.route) {
                        ProfileScreen(
                            mode = env.mode,
                            role = env.role,
                            scenarioId = env.scenarioId,
                            scenarios = ScenarioMarketplaceRepository.scenarioIds,
                            canSelectProduction = viewModel.canEnterProduction(),
                            onRoleSwitch = {
                                viewModel.setRole(it)
                                navController.navigate(if (it is AppRole.Worker) Screen.WorkerHome.route else Screen.BusinessHome.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            },
                            onScenarioSwitch = viewModel::setScenario,
                            onModeSwitch = viewModel::setMode,
                            onLogout = {
                                viewModel.setMode(AppMode.DEMO)
                                viewModel.setRole(AppRole.Worker)
                                navController.navigate(Screen.WorkerHome.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
