package com.example.olahragakuy.pages.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.olahragakuy.ui.components.HomeNavTab
import com.example.olahragakuy.ui.components.KuyBackButton
import com.example.olahragakuy.ui.components.KuyBottomNavBar
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.KuyButton
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme

private val CustomCardTopBg = Color(0xFF1D3C61)
private val CustomCardBottomBg = Color(0xFF0E2137)
private val CustomCardBorder = Color(0xFF284C75)
private val PickerPanelBg = Color(0xFF1A3759)
private val CategoryGroupBg = Color(0xFF23456E)
private val CategoryGroupBorder = Color(0xFF5A7CA5)
private val ExerciseTileBg = Color(0xFF122842)
private val InputBg = Color(0xFF0A192A)
private val CreatedDialogBg = Color(0xFF1D3C61)

@Composable
fun CustomWorkoutListScreen(
    modifier: Modifier = Modifier,
    workouts: List<CustomWorkoutPlan> = CustomWorkoutStore.workouts,
    isLoading: Boolean = CustomWorkoutStore.isLoading,
    errorMessage: String? = CustomWorkoutStore.errorMessage,
    onBack: () -> Unit = {},
    onTabSelected: (HomeNavTab) -> Unit = {},
    onWorkoutClick: (String) -> Unit = {},
    onAddWorkoutClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        containerColor = HomeNavy,
        contentColor = HomeText,
        bottomBar = {
            KuyBottomNavBar(
                selectedTab = HomeNavTab.LATIHAN,
                onTabSelected = onTabSelected,
            )
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            KuyBackButton(onClick = onBack)

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Kostumisasi Latihan",
                color = HomeText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(18.dp))

            when {
                isLoading && workouts.isEmpty() -> {
                    FirestoreStatusCard(
                        title = "Memuat Kostumisasi Latihan...",
                        description = "Mengambil daftar latihan pribadimu dari Cloud Firestore.",
                        showProgress = true,
                        actionLabel = null,
                        onActionClick = {},
                    )
                }

                errorMessage != null && workouts.isEmpty() -> {
                    FirestoreStatusCard(
                        title = "Gagal Memuat Latihan",
                        description = errorMessage,
                        showProgress = false,
                        actionLabel = null,
                        onActionClick = {},
                    )
                }

                workouts.isEmpty() -> {
                    FirestoreStatusCard(
                        title = "Belum Ada Latihan Kostumisasi",
                        description = "Buat rangkaian gerakan latihanmu sendiri dengan menekan tombol Tambah Latihan di bawah.",
                        showProgress = false,
                        actionLabel = null,
                        onActionClick = {},
                    )
                }

                else -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        workouts.forEach { workout ->
                            CustomWorkoutPlanCard(
                                workout = workout,
                                onClick = { onWorkoutClick(workout.id) },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onAddWorkoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KuyButton,
                    contentColor = HomeText,
                ),
            ) {
                Text(
                    text = "Tambah Latihan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CustomWorkoutPlanCard(
    workout: CustomWorkoutPlan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(CustomCardTopBg, CustomCardBottomBg),
                ),
            )
            .border(
                width = 1.dp,
                color = CustomCardBorder.copy(alpha = 0.35f),
                shape = shape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = workout.name,
                color = HomeText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${workout.exercises.size} gerakkan",
                color = HomeText.copy(alpha = 0.72f),
                fontSize = 11.sp,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "Buka ${workout.name}",
            tint = HomeText.copy(alpha = 0.9f),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun CustomWorkoutDetailScreen(
    workoutId: String,
    modifier: Modifier = Modifier,
    workout: CustomWorkoutPlan? = CustomWorkoutStore.workoutById(workoutId),
    exerciseLookup: (String) -> WorkoutExerciseDetail? = WorkoutFirestoreStore::exerciseOrNull,
    onBack: () -> Unit = {},
    onExerciseClick: (String) -> Unit = {},
    onStartWorkoutClick: () -> Unit = {},
    onDeleteWorkoutClick: ((String) -> Unit)? = null,
) {
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = HomeNavy,
        contentColor = HomeText,
        bottomBar = {
            if (workout != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HomeNavy)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Button(
                        onClick = onStartWorkoutClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KuyButton,
                            contentColor = HomeText,
                        ),
                    ) {
                        Text(
                            text = "Mulai",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KuyBackButton(onClick = onBack)

                if (workout != null && onDeleteWorkoutClick != null) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Hapus latihan",
                            tint = HomeText.copy(alpha = 0.78f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            if (workout == null) {
                FirestoreStatusCard(
                    title = "Latihan Tidak Ditemukan",
                    description = "Data latihan kostumisasi ini belum tersedia atau sudah dihapus dari Firestore.",
                    showProgress = CustomWorkoutStore.isLoading,
                    actionLabel = "Kembali",
                    onActionClick = onBack,
                )
                return@Column
            }

            Text(
                text = workout.name,
                color = HomeText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Estimasi Latihan",
                color = HomeText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.Top,
            ) {
                WorkoutMetricColumn(
                    value = workout.calories.toString(),
                    label = "KKAL",
                )
                WorkoutMetricColumn(
                    value = workout.durationLabel,
                    label = "Menit",
                )
                WorkoutMetricColumn(
                    value = "Kesulitan",
                    label = workout.difficultyLabel,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                workout.exercises.forEach { step ->
                    val detail = exerciseLookup(step.exerciseId)
                    GuidedExerciseCard(
                        exerciseName = detail?.name ?: step.exerciseId,
                        targetLabel = step.targetLabel,
                        exerciseType = detail?.type ?: step.exerciseType,
                        onClick = { onExerciseClick(step.exerciseId) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showDeleteConfirm && workout != null && onDeleteWorkoutClick != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Latihan?") },
            text = { Text("Latihan \"${workout.name}\" akan dihapus dari daftar kostumisasi kamu.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteWorkoutClick(workout.id)
                    },
                ) {
                    Text("Hapus", color = HomeOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            },
        )
    }
}

@Composable
fun CreateCustomWorkoutScreen(
    modifier: Modifier = Modifier,
    availableExercises: List<WorkoutExerciseDetail> = WorkoutFirestoreStore.allExercises(),
    isLoadingExercises: Boolean = WorkoutFirestoreStore.isLoadingExercises,
    exercisesErrorMessage: String? = WorkoutFirestoreStore.errorMessage,
    isSeeding: Boolean = WorkoutFirestoreStore.isSeeding,
    seedMessage: String? = WorkoutFirestoreStore.seedMessage,
    isSaving: Boolean = CustomWorkoutStore.isSaving,
    onBack: () -> Unit = {},
    onSeedClick: () -> Unit = { WorkoutCatalogSeeder.seedToFirestore(onlyIfEmpty = false) },
    onSaveWorkout: (name: String, selectedExercises: List<WorkoutExerciseDetail>, onResult: (Boolean, String?) -> Unit) -> Unit = { _, _, onResult ->
        onResult(true, null)
    },
    onCreatedContinue: () -> Unit = {},
) {
    var workoutName by rememberSaveable { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<String>() }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreatedDialog by rememberSaveable { mutableStateOf(false) }

    val groupedExercises = remember(availableExercises) {
        groupExercisesByCategory(availableExercises)
    }
    val exercisesById = remember(availableExercises) {
        availableExercises.associateBy { it.id }
    }
    val selectedExerciseObjects = selectedIds.mapNotNull { exercisesById[it] }
    val canSave = workoutName.trim().isNotEmpty() && selectedExerciseObjects.isNotEmpty() && !isSaving

    Scaffold(
        modifier = modifier,
        containerColor = HomeNavy,
        contentColor = HomeText,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HomeNavy)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Button(
                    onClick = {
                        formError = null
                        onSaveWorkout(workoutName.trim(), selectedExerciseObjects) { success, err ->
                            if (success) {
                                showCreatedDialog = true
                            } else {
                                formError = err ?: "Gagal menyimpan latihan ke Firestore."
                            }
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KuyButton,
                        contentColor = HomeText,
                        disabledContainerColor = KuyButton.copy(alpha = 0.4f),
                        disabledContentColor = HomeText.copy(alpha = 0.6f),
                    ),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = HomeText,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = if (isSaving) "Menyimpan..." else "Simpan Latihan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            KuyBackButton(onClick = onBack)

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Tambah Latihan",
                color = HomeText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Nama Latihan",
                color = HomeText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = workoutName,
                onValueChange = { input ->
                    workoutName = input.take(CUSTOM_WORKOUT_NAME_MAX_LENGTH)
                    formError = null
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Maksimal $CUSTOM_WORKOUT_NAME_MAX_LENGTH karakter",
                        color = HomeText.copy(alpha = 0.45f),
                        fontSize = 13.sp,
                    )
                },
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = HomeText,
                    unfocusedTextColor = HomeText,
                    focusedContainerColor = InputBg,
                    unfocusedContainerColor = InputBg,
                    focusedBorderColor = HomeOrange,
                    unfocusedBorderColor = HomeOrange.copy(alpha = 0.65f),
                    cursorColor = HomeOrange,
                ),
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Pilih Gerakkan :",
                    color = HomeText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (selectedExerciseObjects.isNotEmpty()) {
                    Text(
                        text = "${selectedExerciseObjects.size} dipilih",
                        color = HomeOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when {
                isLoadingExercises && availableExercises.isEmpty() -> {
                    FirestoreStatusCard(
                        title = "Memuat Gerakan dari Firestore...",
                        description = "Mengambil pilihan gerakan langsung dari koleksi `workout_exercises`.",
                        showProgress = true,
                        actionLabel = null,
                        onActionClick = {},
                    )
                }

                availableExercises.isEmpty() -> {
                    FirestoreStatusCard(
                        title = if (exercisesErrorMessage != null) {
                            "Gagal Memuat Gerakan Firestore"
                        } else {
                            "Pilihan Gerakan Masih Kosong"
                        },
                        description = buildString {
                            if (exercisesErrorMessage != null) {
                                append(exercisesErrorMessage)
                            } else {
                                append("Koleksi `workout_exercises` di Firestore belum berisi gerakan.")
                            }
                            if (!seedMessage.isNullOrBlank()) {
                                append("\n\n")
                                append(seedMessage)
                            }
                        },
                        showProgress = isSeeding,
                        actionLabel = if (isSeeding) "Mengunggah..." else "Seed Data Latihan ke Firestore",
                        actionEnabled = !isSeeding,
                        onActionClick = onSeedClick,
                    )
                }

                else -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = PickerPanelBg,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            groupedExercises.forEach { (categoryName, exercises) ->
                                ExerciseCategoryPickerSection(
                                    categoryName = categoryName,
                                    exercises = exercises,
                                    selectedIds = selectedIds,
                                    onToggleExercise = { exerciseId ->
                                        formError = null
                                        if (exerciseId in selectedIds) {
                                            selectedIds.remove(exerciseId)
                                        } else {
                                            selectedIds.add(exerciseId)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (!formError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = formError.orEmpty(),
                    color = Color(0xFFFF8A80),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showCreatedDialog) {
        CustomWorkoutCreatedDialog(
            onContinue = {
                showCreatedDialog = false
                onCreatedContinue()
            },
        )
    }
}

@Composable
private fun ExerciseCategoryPickerSection(
    categoryName: String,
    exercises: List<WorkoutExerciseDetail>,
    selectedIds: List<String>,
    onToggleExercise: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = categoryName,
            color = HomeText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )

        val groupShape = RoundedCornerShape(14.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(groupShape)
                .background(CategoryGroupBg)
                .border(
                    width = 1.dp,
                    color = CategoryGroupBorder.copy(alpha = 0.55f),
                    shape = groupShape,
                )
                .padding(vertical = 10.dp),
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items = exercises, key = { it.id }) { exercise ->
                    val isSelected = exercise.id in selectedIds
                    ExerciseSelectableTile(
                        exercise = exercise,
                        isSelected = isSelected,
                        onClick = { onToggleExercise(exercise.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseSelectableTile(
    exercise: WorkoutExerciseDetail,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .width(86.dp)
            .height(92.dp)
            .clip(shape)
            .background(ExerciseTileBg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) HomeOrange else CustomCardBorder.copy(alpha = 0.4f),
                shape = shape,
            )
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(6.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isSelected) HomeOrange else Color.White),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Dipilih",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            WorkoutExerciseIllustration(
                type = exercise.type,
                modifier = Modifier.size(width = 50.dp, height = 40.dp),
            )
            Text(
                text = exercise.name,
                color = HomeText,
                fontSize = 9.5.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun CustomWorkoutCreatedDialog(
    onContinue: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        CustomWorkoutCreatedCard(onContinue = onContinue)
    }
}

@Composable
private fun CustomWorkoutCreatedCard(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = CreatedDialogBg,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WorkoutCompletedIllustration(modifier = Modifier.size(width = 132.dp, height = 112.dp))

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Selamat",
                color = HomeText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "latihan pribadi kamu berhasil dibuat",
                color = HomeText.copy(alpha = 0.88f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KuyButton,
                    contentColor = HomeText,
                ),
            ) {
                Text(
                    text = "Lanjutkan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private val PreviewCustomExercises by lazy {
    WorkoutCatalogSeeder.buildSeedExercises()
}

private val PreviewCustomWorkouts by lazy {
    listOf(
        buildCustomWorkoutPlan(
            id = "preview_1",
            name = "Latihan Bg Zahran",
            selectedExercises = PreviewCustomExercises.take(5),
        ),
        buildCustomWorkoutPlan(
            id = "preview_2",
            name = "Latihan Raja Gym",
            selectedExercises = PreviewCustomExercises.take(5),
        ),
    )
}

@Preview(name = "Kostumisasi Latihan", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun CustomWorkoutListScreenPreview() {
    OlahragaKuyTheme {
        CustomWorkoutListScreen(
            workouts = PreviewCustomWorkouts,
            isLoading = false,
            errorMessage = null,
        )
    }
}

@Preview(name = "Detail Kostumisasi Latihan", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun CustomWorkoutDetailScreenPreview() {
    val map = PreviewCustomExercises.associateBy { it.id.uppercase() }
    OlahragaKuyTheme {
        CustomWorkoutDetailScreen(
            workoutId = "preview_1",
            workout = PreviewCustomWorkouts.first(),
            exerciseLookup = { map[it.uppercase()] },
        )
    }
}

@Preview(name = "Tambah Latihan Kostumisasi", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun CreateCustomWorkoutScreenPreview() {
    OlahragaKuyTheme {
        CreateCustomWorkoutScreen(
            availableExercises = PreviewCustomExercises,
            isLoadingExercises = false,
            exercisesErrorMessage = null,
            isSeeding = false,
            isSaving = false,
        )
    }
}

@Preview(name = "Dialog Berhasil Buat Latihan", showBackground = true, backgroundColor = 0xFF071627)
@Composable
private fun CustomWorkoutCreatedCardPreview() {
    OlahragaKuyTheme {
        Box(modifier = Modifier.padding(20.dp)) {
            CustomWorkoutCreatedCard(onContinue = {})
        }
    }
}
