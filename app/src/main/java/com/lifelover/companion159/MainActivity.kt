package com.lifelover.companion159

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.lifelover.companion159.ui.screen.PostScreen
import com.lifelover.companion159.ui.theme.Companion159Theme
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.lifelover.companion159.ui.screen.MainScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            Companion159Theme {
                MyApp()
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MyApp() {
    var selectedItem by remember { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),

        topBar = { MyToolbar() }
    ) { innerPadding ->
        AppNavHost(
            modifier = Modifier.padding(innerPadding)
        )
        //PostScreen(modifier = Modifier.padding(innerPadding))
    }
}

@Composable
fun AppNavHost(
    modifier: Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                navController = navController,
                modifier = modifier
            )
        }
        composable("post") {
            PostScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyToolbar() {
    TopAppBar(
        title = { Text("My Toolbar") },
        navigationIcon = { /* ... */ },
        actions = { /* ... */ }
    )
}

@Composable
fun PostScreenView(modifier: Modifier) {
    PostScreen(modifier = modifier)
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Companion159Theme {
        Greeting("Android")
    }
}