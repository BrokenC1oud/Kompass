package me.brokencloud.kompass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import me.brokencloud.kompass.ui.theme.KompassTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            KompassTheme {
                KompassAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KompassAppScreen(modifier: Modifier = Modifier) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.app_name))
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More..."
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                Toast.makeText(context, "Settings Clicked", Toast.LENGTH_SHORT).show()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = "Settings",
                                )
                            }
                        )
                    }
                },
            )
        },
        content = { innerPadding ->
            KompassAppContent(modifier = Modifier.padding(innerPadding))
        }
    )
}

@Composable
fun KompassAppContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val compassAngle = compassSensor()

        Canvas(Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .aspectRatio(1f)
            .rotate(-compassAngle)
        ) {
            val roundedPolygon = RoundedPolygon.star(
                numVerticesPerRadius = 36,
                radius = size.minDimension * .4f,
                innerRadius = size.minDimension * .35f,
                centerX = size.width / 2,
                centerY = size.height / 2,
                rounding = CornerRounding(size.minDimension),
            )
            drawPath(roundedPolygon.toPath().asComposePath(), Color.Blue)
            drawCircle(Color.White, radius = size.minDimension * .03f, center = center.plus(Offset(0f, -size.height * .25f)))
        }
        Text(
            text = "Kompass",
            style = MaterialTheme.typography.headlineLarge,
        )
    }
}

@Composable
fun compassSensor(): Float {
    val context = LocalContext.current
    var azimuth by remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var gravity = FloatArray(3)
        var geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasGeomagnetic = false

        val sensorListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

            }

            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        gravity = event.values.clone()
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        geomagnetic = event.values.clone()
                        hasGeomagnetic = true
                    }
                }

                if (hasGeomagnetic && hasGravity) {
                    val rotationMatrix = FloatArray(9)
                    val orientationValues = FloatArray(3)

                    if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                        SensorManager.getOrientation(rotationMatrix, orientationValues)
                        azimuth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()

                        if (azimuth < 0) {
                            azimuth += 360f
                        }
                    }
                }
            }
        }

        sensorManager?.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager?.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager?.unregisterListener(sensorListener)
        }
    }

    return azimuth
}

@Preview
@Composable
fun KompassPreview() {
    KompassTheme {
        KompassAppScreen()
    }
}
