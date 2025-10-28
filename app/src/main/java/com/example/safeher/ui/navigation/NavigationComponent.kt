package com.example.safeher.ui.navigation

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.safeher.R
import com.example.safeher.data.model.ErrorMessage
import com.example.safeher.ui.explore.ExploreScreen
import com.example.safeher.ui.forgotpassword.ForgotPasswordScreen
import com.example.safeher.ui.friends.FriendsScreen
import com.example.safeher.ui.map.LiveMapScreen
import com.example.safeher.ui.me.MeScreen
import com.example.safeher.ui.resource.AIChat
import com.example.safeher.ui.resource.ResourceHubScreen
import com.example.safeher.ui.signin.SignInScreen
import com.example.safeher.ui.signup.SignUpScreen
import com.example.safeher.ui.splash.SplashScreenContent
import com.example.safeher.ui.splash.SplashViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Screen(@StringRes val title: Int) {
    Explore(R.string.explore_screen_title),
    SignUp(R.string.sign_up_screen_title),
    SignIn(R.string.sign_in_screen_title),
    ResourcesHub(R.string.resources_hub_screen_title),
    Profile(R.string.profile_screen_title),
    Map(R.string.map_screen_title),
    ForgotPassword(R.string.forgot_password_screen_title),
    Splash(R.string.splash_screen_title),
    Friends(R.string.friends_screen_title),
    FirebaseSearch(R.string.firebase_search_screen_title),
    CheckIn(R.string.check_in_screen_title)
}

data class MainNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

@Composable
fun App(
    windowSize: WindowWidthSizeClass,
    finishActivity: () -> Unit
){
    val splashViewModel: SplashViewModel = hiltViewModel()
    val userState by splashViewModel.userState.collectAsState()
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var navigationReady by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(userState) {
        delay(1000)
        navigationReady = true
        delay(300)
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (navigationReady) {
            MainAppLayout(
                windowSize = windowSize,
                isLoggedIn = userState != null,
                showSplash = showSplash
            )
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(animationSpec = tween(300))
        ) {
            SplashScreenContent()
        }
    }
}

@Composable
fun MainAppLayout(
    windowSize: WindowWidthSizeClass,
    isLoggedIn: Boolean,
    showSplash: Boolean = false
) {
    val navController = rememberNavController()
    val startDestination = if (isLoggedIn) Screen.Explore.name else Screen.SignIn.name

    Log.d("MainAppLayout", "isLoggedIn: $isLoggedIn")

    val isTablet = windowSize >= WindowWidthSizeClass.Medium
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentScreen = Screen.entries.find { it.name == currentRoute?.substringBefore("?")?.substringBefore("/") }
        ?: Screen.Splash

    val screensWithoutNavigationUi = listOf(Screen.Splash, Screen.SignIn, Screen.SignUp, Screen.ForgotPassword)
    val shouldShowNavigationUi = currentScreen !in screensWithoutNavigationUi

    val mainNavItems = listOf(
        MainNavItem(Screen.Explore, Icons.Default.Explore, "Explore"),
        MainNavItem(Screen.Friends, Icons.Default.People, "Friends"),
        MainNavItem(Screen.ResourcesHub, Icons.Default.Book, "Resources"),
        MainNavItem(Screen.Profile, Icons.Default.Person, "Profile"),
//        MainNavItem(Screen.Offline, Icons.Outlined.OfflineBolt, "Offline"), //Example only
//        MainNavItem(Screen.Community, Icons.Default.People, "Community"), ///////////////////////////////////
//        MainNavItem(Screen.Library, Icons.Default.Book, "Library"),
    )

    androidx.compose.animation.AnimatedVisibility(
        visible = !showSplash,
        enter = fadeIn(animationSpec = tween(300))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (isTablet && shouldShowNavigationUi) {
                NavigationRail(modifier = Modifier.weight(0.25f)) {
                    mainNavItems.forEach { item ->
                        NavigationRailItem(
                            icon = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = item.icon, contentDescription = null)
                                    Spacer(Modifier.width(16.dp))
                                    Text(text = item.label)
                                }
                            },
                            label = null,
                            selected = navController.currentDestination?.hierarchy?.any {
                                it.route?.substringBefore("?")?.substringBefore("/") == item.screen.name
                            } == true,
                            onClick = {
                                navController.navigate(item.screen.name) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
            AppScaffold(
                modifier = Modifier.weight(if (isTablet && shouldShowNavigationUi) 0.75f else 1f),
                navController = navController,
                mainNavItems = mainNavItems,
                showBottomBar = !isTablet,
                shouldShowNavigationUi = shouldShowNavigationUi,
                startDestination = startDestination,
                showSplash = showSplash
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    mainNavItems: List<MainNavItem>,
    showBottomBar: Boolean,
    shouldShowNavigationUi: Boolean,
    startDestination: String,
    showSplash: Boolean = false,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentScreen = Screen.entries.find {
        it.name == currentRoute?.substringBefore("?")?.substringBefore("/")
    } ?: Screen.Explore

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val showErrorSnackbar: (ErrorMessage) -> Unit = { errorMessage ->
        coroutineScope.launch {
            val message = when (errorMessage) {
                is ErrorMessage.StringError -> errorMessage.message
                is ErrorMessage.IdError -> context.getString(errorMessage.message)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    val mainNavigationScreens = listOf(Screen.Explore, Screen.CheckIn, Screen.ResourcesHub, Screen.Friends, Screen.Profile) // The Screens that should show in bottom tabs

    val showMainNavigationUi = currentScreen in mainNavigationScreens && !showSplash
    val canPop = navController.previousBackStackEntry != null
    val showBackButton = canPop && !showMainNavigationUi && !showSplash

//    val screensWithoutPlayerBar = listOf(Screen.Splash, Screen.SignIn, Screen.SignUp, Screen.ForgotPassword) // The states that don't show Navigation bar (tabs)
//    val isPlayerBarVisible = currentScreen !in screensWithoutPlayerBar && !showSplash
    val currentScreenTitle = stringResource(currentScreen.title)

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            androidx.compose.animation.AnimatedVisibility(
                visible = showBackButton,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                TopAppBar(
                    title = { Text(currentScreenTitle) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        },
        bottomBar = {
            androidx.compose.animation.AnimatedVisibility(

                visible = true, /////////////////////////////

                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Column {
                    if (showMainNavigationUi && showBottomBar && shouldShowNavigationUi) {
                        NavigationBar {
                            mainNavItems.forEach { item ->
                                NavigationBarItem(
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    selected = navController.currentDestination?.hierarchy?.any { it.route == item.screen.name } == true,
                                    onClick = {
                                        navController.navigate(item.screen.name) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(innerPadding)
                .navigationBarsPadding(),

//            enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() },
//            exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut() },
//            popEnterTransition = { slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn() },
//            popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut() }

        ) {
            composable(Screen.SignIn.name) {
                SignInScreen(
                    openHomeScreen = {
                        navController.navigate(Screen.Explore.name) {
                            popUpTo(
                                navController.graph.findStartDestination().id
                            ) { inclusive = true }
                        }
                    },
                    openSignUpScreen = { navController.navigate(Screen.SignUp.name) },
                    openForgotPasswordScreen = { navController.navigate(Screen.ForgotPassword.name) },
                    showError = showErrorSnackbar
                )
            }
            composable(Screen.SignUp.name) {
                SignUpScreen(
                    openHomeScreen = {
                        navController.navigate(Screen.Explore.name) {
                            popUpTo(
                                navController.graph.findStartDestination().id
                            ) { inclusive = true }
                        }
                    },
                    openSignInScreen = { navController.navigate(Screen.SignIn.name) },
                    showErrorSnackbar = showErrorSnackbar
                )
            }
            composable(Screen.ForgotPassword.name) {
                ForgotPasswordScreen(openSignInScreen = { navController.navigate(Screen.SignIn.name) })
            }
            composable(Screen.Explore.name) {
                ExploreScreen(
                    navController = navController
                )
            }
            composable(Screen.Friends.name) {
                FriendsScreen()
            }
            composable(Screen.ResourcesHub.name) {
                ResourceHubScreen(
                    onStartChat = { navController.navigate("AI Chat") }
                )
            }
            composable("AI Chat") {
                AIChat(
                    onBackClick = { navController.popBackStack() },
                    navController = navController
                )
            }
            composable(Screen.Profile.name) {
                val navigationViewModel: NavigationViewModel = hiltViewModel()
                MeScreen(
                    onAccountDeleted = {
                        navController.navigate(Screen.SignIn.name) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onSignOutClick = {
                            navigationViewModel.signOut()
                            navController.navigate(Screen.SignIn.name) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            }
                    }
                )
            }
            composable(Screen.Map.name) {
                // TODO: Create MapScreen
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Map Screen")
                }
            }
            composable(Screen.CheckIn.name) {
                // TODO: Create CheckInScreen
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Check-In Timer Screen")
                }
            }
            composable(
                route = "liveMap/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")
                if (userId != null) {
                    LiveMapScreen(
                        userId = userId,
                        onNavigateUp = { navController.navigateUp() }
                    )
                }
            }
        }
    }
}

