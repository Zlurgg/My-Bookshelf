package uk.co.zlurgg.mybookshelf.app.presentation

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import uk.co.zlurgg.mybookshelf.app.NavigationRoute
import uk.co.zlurgg.mybookshelf.auth.presentation.PostSignInDestination
import uk.co.zlurgg.mybookshelf.auth.presentation.SignInScreenRoot
import uk.co.zlurgg.mybookshelf.app.presentation.theme.ThemeViewModel
import uk.co.zlurgg.mybookshelf.core.domain.model.ThemeMode
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.InitializeWelcomeUseCase
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.BookDetailViewModel
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.BookDetailsScreenRoot
import uk.co.zlurgg.mybookshelf.bookcase.presentation.BookcaseAction
import uk.co.zlurgg.mybookshelf.account.presentation.AccountScreenRoot
import uk.co.zlurgg.mybookshelf.bookcase.presentation.BookcaseScreenRoot
import uk.co.zlurgg.mybookshelf.bookcase.presentation.BookcaseViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.BookshelfAction
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.BookshelfScreenRoot
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.BookshelfViewModel
import uk.co.zlurgg.mybookshelf.welcome.presentation.WelcomeScreenRoot
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme

@Composable
fun MyBookShelfApp() {
    val activity = LocalActivity.current as ComponentActivity

    val themeViewModel = koinViewModel<ThemeViewModel>(
        viewModelStoreOwner = activity
    )
    val themeState by themeViewModel.state.collectAsStateWithLifecycle()
    val darkTheme = when (themeState.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    LaunchedEffect(darkTheme) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ) { darkTheme },
            navigationBarStyle = if (darkTheme) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                )
            },
        )
    }

    MyBookshelfTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val initializeWelcome = koinInject<InitializeWelcomeUseCase>()

        // Initialize welcome on first app launch
        LaunchedEffect(Unit) {
            initializeWelcome()
        }

        // Always start at SignIn - it will auto-navigate if already signed in
        NavHost(
            navController = navController,
            startDestination = NavigationRoute.MyBookshelfGraph.ROUTE
        ) {
            navigation(
                route = NavigationRoute.MyBookshelfGraph.ROUTE,
                startDestination = NavigationRoute.SignIn.ROUTE
            ) {
                composable(
                    route = NavigationRoute.SignIn.ROUTE
                ) {
                    SignInScreenRoot(
                        onNavigate = { destination ->
                            val route = when (destination) {
                                PostSignInDestination.Welcome -> NavigationRoute.Welcome.createRoute()
                                PostSignInDestination.Bookcase -> NavigationRoute.Bookcase.createRoute()
                            }
                            navController.navigate(route) {
                                popUpTo(NavigationRoute.SignIn.ROUTE) { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    route = NavigationRoute.Welcome.ROUTE
                ) {
                    WelcomeScreenRoot(
                        onNavigateToBookcase = {
                            navController.navigate(NavigationRoute.Bookcase.createRoute()) {
                                popUpTo(NavigationRoute.Welcome.ROUTE) { inclusive = true }
                            }
                        }
                    )
                }
                composable(
                    route = NavigationRoute.Bookcase.ROUTE,
                    arguments = listOf(
                        navArgument(NavigationRoute.Bookcase.ARG_NEW_SHELF) {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                        navArgument(NavigationRoute.Bookcase.ARG_SWITCH_TO_BOOK_CLUBS) {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { backStackEntry ->
                    val viewModel = koinViewModel<BookcaseViewModel>()
                    val isNewShelf = backStackEntry.arguments?.getBoolean(
                        NavigationRoute.Bookcase.ARG_NEW_SHELF
                    ) ?: false
                    val switchToBookClubs = backStackEntry.arguments?.getBoolean(
                        NavigationRoute.Bookcase.ARG_SWITCH_TO_BOOK_CLUBS
                    ) ?: false

                    // Observe result from BookshelfScreen's "Create Book Club" FAB
                    val createClubForShelfId by backStackEntry.savedStateHandle
                        .getStateFlow<String?>(
                            NavigationRoute.Bookcase.ARG_CREATE_CLUB_FOR_SHELF,
                            null
                        )
                        .collectAsStateWithLifecycle()

                    BookcaseScreenRoot(
                        viewModel = viewModel,
                        switchToBookClubs = switchToBookClubs,
                        createClubForShelfId = createClubForShelfId,
                        onCreateClubConsumed = {
                            backStackEntry.savedStateHandle[NavigationRoute.Bookcase.ARG_CREATE_CLUB_FOR_SHELF] = null
                        },
                        onBookshelfClick = { shelf ->
                            navController.navigate(NavigationRoute.Bookshelf.createRoute(shelf.id))
                        },
                        onBookDetailClick = { bookId, shelfId ->
                            navController.navigate(NavigationRoute.BookDetail.createRoute(bookId, shelfId)) {
                                launchSingleTop = true
                            }
                        },
                        onAddBookshelfClick = { name, style ->
                            viewModel.onAction(BookcaseAction.OnAddBookshelfClick(name, style))
                        },
                        onSignIn = { navigateToSignIn(navController) },
                        onAccountClick = { isSignedIn ->
                            if (isSignedIn) {
                                navController.navigate(NavigationRoute.Account.createRoute())
                            } else {
                                navigateToSignIn(navController)
                            }
                        },
                    )

                    // Show add dialog if we're coming back from creating a new shelf
                    if (isNewShelf) {
                        LaunchedEffect(Unit) {
                            viewModel.onAction(BookcaseAction.ShowAddDialog(true))
                        }
                    }
                }

                composable(
                    route = NavigationRoute.Account.ROUTE,
                ) {
                    AccountScreenRoot(
                        onNavigateToSignIn = { navigateToSignIn(navController) },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(
                    route = NavigationRoute.Bookshelf.ROUTE,
                    arguments = listOf(
                        navArgument(NavigationRoute.Bookshelf.KEY_ID) {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val shelfId = backStackEntry.arguments?.getString(
                        NavigationRoute.Bookshelf.KEY_ID
                    ) ?: ""

                    val viewModel = koinViewModel<BookshelfViewModel>(
                        parameters = { parametersOf(shelfId) }
                    )
                    val state = viewModel.state.collectAsStateWithLifecycle().value

                    BookshelfScreenRoot(
                        viewModel = viewModel,
                        onAddBookClick = { book ->
                            viewModel.onAction(BookshelfAction.OnAddBookClick(book = book))
                        },
                        onBookClick = { book ->
                            viewModel.onAction(BookshelfAction.OnBookClick(book))
                            viewModel.onAction(BookshelfAction.OnDismissSearchDialog)
                            navController.navigate(NavigationRoute.BookDetail.createRoute(book.id, shelfId)) {
                                launchSingleTop = true
                            }
                        },
                        onBackClick = { navController.popBackStack() },
                        onCreateBookClub = {
                            navController.previousBackStackEntry?.savedStateHandle?.set(
                                NavigationRoute.Bookcase.ARG_CREATE_CLUB_FOR_SHELF,
                                shelfId
                            )
                            navController.popBackStack()
                        },
                        onSignIn = { navigateToSignIn(navController) },
                        shelfName = state.shelfName,
                        shelfMaterial = state.shelfMaterial,
                    )
                }

                composable(
                    route = NavigationRoute.BookDetail.ROUTE,
                    arguments = listOf(
                        navArgument(NavigationRoute.BookDetail.KEY_ID) {
                            type = NavType.StringType
                        },
                        navArgument(NavigationRoute.BookDetail.KEY_SHELF_ID) {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getString(
                        NavigationRoute.BookDetail.KEY_ID
                    ) ?: ""
                    val shelfIdArg = backStackEntry.arguments?.getString(
                        NavigationRoute.BookDetail.KEY_SHELF_ID
                    ).takeIf { !it.isNullOrBlank() }

                    val viewModel = koinViewModel<BookDetailViewModel>(
                        parameters = { parametersOf(bookId, shelfIdArg) }
                    )

                    BookDetailsScreenRoot(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

private fun navigateToSignIn(navController: NavHostController) {
    navController.navigate(NavigationRoute.SignIn.createRoute()) {
        popUpTo(NavigationRoute.MyBookshelfGraph.ROUTE) { inclusive = true }
    }
}
