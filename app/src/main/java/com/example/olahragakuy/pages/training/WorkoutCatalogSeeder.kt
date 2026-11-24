package com.example.olahragakuy.pages.training

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Seeder khusus untuk memasukkan data awal macam-macam latihan (`workout_exercises`)
 * dan contoh jadwal 30 hari (`guided_workout_days`) langsung ke Cloud Firestore.
 *
 * File ini TIDAK dipakai sebagai fallback tampilan UI; layar hanya membaca hasil
 * sinkronisasi dari [WorkoutFirestoreStore].
 */
object WorkoutCatalogSeeder {

    fun buildSeedExercises(): List<WorkoutExerciseDetail> = listOf(
        WorkoutExerciseDetail(
            id = ExerciseType.SIT_UP.name,
            type = ExerciseType.SIT_UP,
            name = "Sit Up",
            description = "Melatih kekuatan otot perut dan membantu menjaga stabilitas inti tubuh untuk pemula.",
            category = "Otot Perut",
            defaultTarget = WorkoutTarget.Repetitions(12),
            order = 1,
            steps = listOf(
                "Posisikan tubuh terlentang di lantai dengan lutut ditekuk dan telapak kaki menempel pada lantai.",
                "Letakkan tangan di belakang kepala atau silang di dada.",
                "Angkat badan perlahan hingga mendekati lutut sambil mengencangkan otot perut.",
                "Turunkan badan kembali secara perlahan hingga menyentuh lantai.",
                "Ulangi sesuai jumlah repetisi.",
            ),
        ),
        WorkoutExerciseDetail(
            id = ExerciseType.PLANK_LURUS.name,
            type = ExerciseType.PLANK_LURUS,
            name = "Plank Lurus",
            description = "Latihan statis yang memperkuat otot inti, bahu, dan postur punggung tanpa perlu alat.",
            category = "Inti Tubuh",
            defaultTarget = WorkoutTarget.Duration(30),
            order = 2,
            steps = listOf(
                "Posisikan tubuh telungkup dengan kedua telapak tangan sejajar di bawah bahu.",
                "Luruskan kedua kaki ke belakang bertumpu pada ujung jari kaki.",
                "Jaga garis tubuh tetap lurus dari kepala hingga tumit tanpa menurunkan pinggul.",
                "Kencangkan otot perut dan atur napas dengan tenang selama durasi berlangsung.",
                "Turunkan lutut perlahan setelah waktu selesai.",
            ),
        ),
        WorkoutExerciseDetail(
            id = ExerciseType.SQUAT.name,
            type = ExerciseType.SQUAT,
            name = "Squat",
            description = "Menguatkan otot paha depan, paha belakang, dan pinggul sekaligus melatih keseimbangan.",
            category = "Kaki & Paha",
            defaultTarget = WorkoutTarget.Repetitions(12),
            order = 3,
            steps = listOf(
                "Berdiri tegak dengan kedua kaki dibuka selebar bahu dan jari kaki sedikit mengarah keluar.",
                "Luruskan kedua tangan ke depan dada untuk menjaga keseimbangan.",
                "Tekuk lutut dan dorong pinggul ke belakang seperti hendak duduk hingga paha sejajar lantai.",
                "Pastikan punggung tetap tegak dan lutut tidak melewati ujung jari kaki.",
                "Dorong tumit untuk kembali berdiri tegak, lalu ulangi sesuai repetisi.",
            ),
        ),
        WorkoutExerciseDetail(
            id = ExerciseType.PUSH_UP.name,
            type = ExerciseType.PUSH_UP,
            name = "Push Up",
            description = "Melatih kekuatan otot dada, bahu, dan lengan belakang secara efektif di rumah.",
            category = "Dada & Lengan",
            defaultTarget = WorkoutTarget.Repetitions(10),
            order = 4,
            steps = listOf(
                "Mulai dari posisi plank lurus dengan telapak tangan sedikit lebih lebar dari bahu.",
                "Kencangkan otot perut agar pinggul dan punggung tetap sejajar.",
                "Tekuk siku perlahan dan turunkan dada mendekati lantai.",
                "Dorong lantai dengan kedua tangan hingga lengan kembali lurus.",
                "Ulangi gerakan dengan ritme terkontrol (boleh bertumpu pada lutut untuk pemula).",
            ),
        ),
        WorkoutExerciseDetail(
            id = ExerciseType.JUMPING_JACK.name,
            type = ExerciseType.JUMPING_JACK,
            name = "Jumping Jack",
            description = "Gerakan kardio ringan untuk meningkatkan detak jantung, stamina, dan pemanasan seluruh tubuh.",
            category = "Kardio",
            defaultTarget = WorkoutTarget.Duration(30),
            order = 5,
            steps = listOf(
                "Berdiri tegak dengan kedua kaki rapat dan tangan di samping tubuh.",
                "Lompat ringan sambil membuka kedua kaki selebar bahu.",
                "Bersamaan dengan itu, angkat kedua lengan ke atas kepala.",
                "Lompat kembali untuk merapatkan kaki dan menurunkan tangan ke posisi awal.",
                "Lakukan secara berirama sesuai durasi atau repetisi.",
            ),
        ),
        WorkoutExerciseDetail(
            id = ExerciseType.LUNGES.name,
            type = ExerciseType.LUNGES,
            name = "Lunges",
            description = "Melatih kekuatan kaki secara bergantian untuk stabilitas lutut, paha, dan bokong.",
            category = "Kaki & Paha",
            defaultTarget = WorkoutTarget.Repetitions(12),
            order = 6,
            steps = listOf(
                "Berdiri tegak dengan kaki dibuka selebar pinggul dan tangan di pinggang.",
                "Langkahkan satu kaki ke depan dan turunkan tubuh hingga kedua lutut membentuk sudut 90 derajat.",
                "Pastikan lutut depan sejajar dengan pergelangan kaki dan dada tetap tegak.",
                "Dorong kaki depan untuk kembali ke posisi berdiri awal.",
                "Ulangi bergantian dengan kaki lainnya sesuai jumlah repetisi.",
            ),
        ),
        WorkoutExerciseDetail(
            id = ExerciseType.GLUTE_BRIDGE.name,
            type = ExerciseType.GLUTE_BRIDGE,
            name = "Glute Bridge",
            description = "Mengaktifkan otot pinggul, bokong, dan punggung bawah dengan tekanan minimal pada lutut.",
            category = "Pinggul & Inti",
            defaultTarget = WorkoutTarget.Repetitions(12),
            order = 7,
            steps = listOf(
                "Berbaring terlentang di lantai dengan lutut ditekuk dan telapak kaki menapak rata.",
                "Letakkan kedua lengan lurus di samping tubuh dengan telapak tangan menghadap ke bawah.",
                "Angkat pinggul perlahan hingga bahu, pinggul, dan lutut membentuk garis lurus.",
                "Tahan sejenak di posisi atas sambil mengencangkan otot bokong dan perut.",
                "Turunkan pinggul perlahan kembali ke lantai, lalu ulangi sesuai repetisi.",
            ),
        ),
        WorkoutExerciseDetail(
            id = ExerciseType.MOUNTAIN_CLIMBER.name,
            type = ExerciseType.MOUNTAIN_CLIMBER,
            name = "Mountain Climber",
            description = "Kombinasi kardio dan latihan perut untuk membakar kalori serta meningkatkan kelincahan.",
            category = "Kardio",
            defaultTarget = WorkoutTarget.Duration(30),
            order = 8,
            steps = listOf(
                "Mulai dari posisi plank lurus dengan kedua tangan menumpu tepat di bawah bahu.",
                "Tarik satu lutut ke arah dada dengan cepat namun tetap terkontrol.",
                "Kembalikan kaki ke belakang sambil langsung menarik lutut satunya ke arah dada.",
                "Jaga pinggul tetap stabil dan tidak terlalu naik selama gerakan bergantian.",
                "Lanjutkan ritme teratur hingga durasi berakhir.",
            ),
        ),
    )

    fun buildSeedGuidedDays(): List<GuidedWorkoutDay> =
        (1..GUIDED_WORKOUT_TOTAL_DAYS).map { buildDaySeed(it) }

    private fun buildDaySeed(dayNumber: Int): GuidedWorkoutDay {
        if (dayNumber == 1) {
            return GuidedWorkoutDay(
                dayNumber = 1,
                title = "Hari 1",
                description = "Pengenalan latihan dasar otot perut, inti tubuh, dan kaki untuk pemula.",
                calories = 43,
                durationLabel = "5-7",
                difficultyLabel = "Mudah",
                exercises = listOf(
                    GuidedWorkoutStep("d1_1", ExerciseType.SIT_UP, WorkoutTarget.Repetitions(12)),
                    GuidedWorkoutStep("d1_2", ExerciseType.PLANK_LURUS, WorkoutTarget.Duration(30)),
                    GuidedWorkoutStep("d1_3", ExerciseType.SQUAT, WorkoutTarget.Repetitions(12)),
                    GuidedWorkoutStep("d1_4", ExerciseType.SIT_UP, WorkoutTarget.Repetitions(12)),
                    GuidedWorkoutStep("d1_5", ExerciseType.PLANK_LURUS, WorkoutTarget.Duration(30)),
                ),
            )
        }

        val difficulty = when {
            dayNumber <= 10 -> "Mudah"
            dayNumber <= 20 -> "Sedang"
            else -> "Menantang"
        }
        val reps = 12 + (((dayNumber - 1) / 3) * 2)
        val pushUpReps = (reps - 2).coerceAtLeast(8)
        val plankSeconds = 30 + (((dayNumber - 1) / 5) * 10)
        val calories = 43 + ((dayNumber - 1) * 4)
        val minMinutes = 5 + ((dayNumber - 1) / 4)
        val maxMinutes = minMinutes + 2

        val (focusDescription, pattern) = when (dayNumber % 4) {
            0 -> "Kombinasi kardio ringan dan kekuatan kaki untuk meningkatkan daya tahan tubuh." to listOf(
                GuidedWorkoutStep("d${dayNumber}_1", ExerciseType.JUMPING_JACK, WorkoutTarget.Duration(plankSeconds)),
                GuidedWorkoutStep("d${dayNumber}_2", ExerciseType.SQUAT, WorkoutTarget.Repetitions(reps)),
                GuidedWorkoutStep("d${dayNumber}_3", ExerciseType.LUNGES, WorkoutTarget.Repetitions(reps)),
                GuidedWorkoutStep("d${dayNumber}_4", ExerciseType.GLUTE_BRIDGE, WorkoutTarget.Repetitions(reps)),
                GuidedWorkoutStep("d${dayNumber}_5", ExerciseType.PLANK_LURUS, WorkoutTarget.Duration(plankSeconds)),
            )
            2 -> "Latihan tubuh bagian atas dan otot perut untuk membangun kekuatan inti." to listOf(
                GuidedWorkoutStep("d${dayNumber}_1", ExerciseType.PUSH_UP, WorkoutTarget.Repetitions(pushUpReps)),
                GuidedWorkoutStep("d${dayNumber}_2", ExerciseType.SIT_UP, WorkoutTarget.Repetitions(reps)),
                GuidedWorkoutStep("d${dayNumber}_3", ExerciseType.PLANK_LURUS, WorkoutTarget.Duration(plankSeconds)),
                GuidedWorkoutStep("d${dayNumber}_4", ExerciseType.GLUTE_BRIDGE, WorkoutTarget.Repetitions(reps)),
                GuidedWorkoutStep("d${dayNumber}_5", ExerciseType.MOUNTAIN_CLIMBER, WorkoutTarget.Duration(plankSeconds)),
            )
            3 -> "Fokus mobilitas kaki, pinggul, serta pembakaran kalori menyeluruh." to listOf(
                GuidedWorkoutStep("d${dayNumber}_1", ExerciseType.JUMPING_JACK, WorkoutTarget.Duration(plankSeconds)),
                GuidedWorkoutStep("d${dayNumber}_2", ExerciseType.LUNGES, WorkoutTarget.Repetitions(reps)),
                GuidedWorkoutStep("d${dayNumber}_3", ExerciseType.SQUAT, WorkoutTarget.Repetitions(reps)),
                GuidedWorkoutStep("d${dayNumber}_4", ExerciseType.SIT_UP, WorkoutTarget.Repetitions(reps)),
                GuidedWorkoutStep("d${dayNumber}_5", ExerciseType.MOUNTAIN_CLIMBER, WorkoutTarget.Duration(plankSeconds)),
            )
            else -> "Latihan seluruh tubuh seimbang untuk menjaga konsistensi harian." to listOf(
                GuidedWorkoutStep("d${dayNumber}_1", ExerciseType.SIT_UP, WorkoutTarget.Repetitions(reps)),
                GuidedWorkoutStep("d${dayNumber}_2", ExerciseType.PLANK_LURUS, WorkoutTarget.Duration(plankSeconds)),
                GuidedWorkoutStep("d${dayNumber}_3", ExerciseType.SQUAT, WorkoutTarget.Repetitions(reps)),
                GuidedWorkoutStep("d${dayNumber}_4", ExerciseType.PUSH_UP, WorkoutTarget.Repetitions(pushUpReps)),
                GuidedWorkoutStep("d${dayNumber}_5", ExerciseType.PLANK_LURUS, WorkoutTarget.Duration(plankSeconds)),
            )
        }

        return GuidedWorkoutDay(
            dayNumber = dayNumber,
            title = "Hari $dayNumber",
            description = focusDescription,
            calories = calories,
            durationLabel = "$minMinutes-$maxMinutes",
            difficultyLabel = difficulty,
            exercises = pattern,
        )
    }

    /**
     * Memasukkan seluruh 8 macam latihan (`workout_exercises`) dan 30 hari contoh latihan
     * (`guided_workout_days`) ke Firestore menggunakan `WriteBatch`.
     */
    fun seedToFirestore(
        firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
        onlyIfEmpty: Boolean = false,
        onComplete: (Boolean, String) -> Unit = { _, _ -> },
    ) {
        if (WorkoutFirestoreStore.isSeeding) return
        WorkoutFirestoreStore.setSeedingState(
            seeding = true,
            message = "Mengunggah data latihan ke Firestore...",
        )

        try {
            val exercisesCol = firestore.collection(WORKOUT_EXERCISES_COLLECTION)
            val daysCol = firestore.collection(GUIDED_WORKOUT_DAYS_COLLECTION)

            fun commitSeed(writeExercises: Boolean, writeDays: Boolean) {
                if (!writeExercises && !writeDays) {
                    val msg = "Data latihan sudah tersedia di Firestore."
                    WorkoutFirestoreStore.setSeedingState(seeding = false, message = msg)
                    onComplete(true, msg)
                    return
                }

                val seedExercises = buildSeedExercises()
                val seedDays = buildSeedGuidedDays()
                val batch = firestore.batch()

                if (writeExercises) {
                    seedExercises.forEach { exercise ->
                        batch.set(
                            exercisesCol.document(exercise.id),
                            exercise.toFirestoreMap(includeServerTimestamp = true),
                            SetOptions.merge(),
                        )
                    }
                }
                if (writeDays) {
                    seedDays.forEach { day ->
                        batch.set(
                            daysCol.document(guidedWorkoutDayDocId(day.dayNumber)),
                            day.toFirestoreMap(includeServerTimestamp = true),
                            SetOptions.merge(),
                        )
                    }
                }

                batch.commit()
                    .addOnSuccessListener {
                        val msg = "Berhasil memasukkan ${seedExercises.size} latihan & ${seedDays.size} hari ke Firestore."
                        WorkoutFirestoreStore.setSeedingState(seeding = false, message = msg)
                        onComplete(true, msg)
                    }
                    .addOnFailureListener { err ->
                        val msg = "Gagal seeding ke Firestore: ${err.localizedMessage ?: "Akses ditolak"}"
                        WorkoutFirestoreStore.setSeedingState(seeding = false, message = msg)
                        onComplete(false, msg)
                    }
            }

            if (!onlyIfEmpty) {
                commitSeed(writeExercises = true, writeDays = true)
                return
            }

            exercisesCol.limit(1).get()
                .addOnSuccessListener { exerciseSnap ->
                    daysCol.limit(1).get()
                        .addOnSuccessListener { daySnap ->
                            commitSeed(
                                writeExercises = exerciseSnap.isEmpty,
                                writeDays = daySnap.isEmpty,
                            )
                        }
                        .addOnFailureListener { err ->
                            val msg = "Gagal mengecek koleksi hari latihan: ${err.localizedMessage ?: "Error"}"
                            WorkoutFirestoreStore.setSeedingState(seeding = false, message = msg)
                            onComplete(false, msg)
                        }
                }
                .addOnFailureListener { err ->
                    val msg = "Gagal mengecek koleksi latihan: ${err.localizedMessage ?: "Error"}"
                    WorkoutFirestoreStore.setSeedingState(seeding = false, message = msg)
                    onComplete(false, msg)
                }
        } catch (e: Exception) {
            val msg = "Gagal seeding ke Firestore: ${e.localizedMessage ?: "Error"}"
            WorkoutFirestoreStore.setSeedingState(seeding = false, message = msg)
            onComplete(false, msg)
        }
    }
}
