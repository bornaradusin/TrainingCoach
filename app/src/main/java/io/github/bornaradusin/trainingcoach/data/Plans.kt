package io.github.bornaradusin.trainingcoach.data

/** The plans that come with the app, made for the IM-309's stations. */
object BuiltInPlans {
    private fun i(ex: String, sets: Int, min: Int, max: Int, rest: Int = 90) = PlanItem(ex, sets, min, max, rest)

    val all = listOf(
        Plan(
            "beginner", "Beginner full body",
            "One workout for the whole body, 3 times a week (for example Monday, Wednesday, Friday). Start light and learn every movement.",
            listOf(
                PlanDay("Full body", listOf(
                    i("chest_press", 3, 10, 12), i("lat_pulldown", 3, 10, 12), i("seated_row", 2, 10, 12),
                    i("leg_extension", 2, 12, 15, 75), i("leg_curl", 2, 12, 15, 60), i("triceps_pushdown", 2, 10, 15, 60),
                    i("preacher_curl", 2, 10, 12, 60), i("seated_crunch", 2, 15, 20, 45),
                )),
            ),
            builtIn = true,
        ),
        Plan(
            "fullbody_ab", "Full body A / B",
            "Two full-body workouts done in turn, 3 times a week: A, B, A, then B, A, B.",
            listOf(
                PlanDay("Workout A", listOf(
                    i("chest_press", 4, 8, 10, 120), i("lat_pulldown", 4, 8, 10, 120), i("leg_extension", 3, 10, 12),
                    i("leg_curl", 3, 10, 12, 60), i("upright_row", 3, 10, 12), i("triceps_pushdown", 3, 10, 12, 60),
                    i("kneeling_crunch", 3, 12, 15, 60),
                )),
                PlanDay("Workout B", listOf(
                    i("butterfly", 3, 10, 12), i("close_grip_pulldown", 4, 8, 10, 120), i("seated_row", 3, 8, 10, 120),
                    i("leg_extension", 3, 10, 12), i("front_raise", 3, 10, 12, 60), i("preacher_curl", 3, 10, 12, 60),
                    i("sit_up", 3, 12, 20, 60),
                )),
            ),
            builtIn = true,
        ),
        Plan(
            "upper_lower", "Upper / Lower",
            "4 days a week: upper body, lower body and core, a rest day, then again.",
            listOf(
                PlanDay("Upper 1", listOf(
                    i("chest_press", 4, 6, 10, 120), i("lat_pulldown", 4, 6, 10, 120), i("seated_row", 3, 8, 12),
                    i("upright_row", 3, 10, 12), i("triceps_pushdown", 3, 10, 12, 60), i("preacher_curl", 3, 10, 12, 60),
                )),
                PlanDay("Lower & core", listOf(
                    i("leg_extension", 4, 10, 15), i("leg_curl", 4, 10, 15, 60), i("kneeling_crunch", 3, 12, 15, 60),
                    i("seated_crunch", 3, 15, 20, 45), i("sit_up", 3, 12, 20, 60),
                )),
                PlanDay("Upper 2", listOf(
                    i("butterfly", 4, 10, 12), i("close_grip_pulldown", 4, 8, 12, 120), i("straight_arm_pulldown", 3, 12, 15, 60),
                    i("reverse_butterfly", 3, 12, 15, 60), i("rope_pushdown", 3, 10, 15, 60), i("standing_curl", 3, 10, 12, 60),
                    i("shrug", 3, 12, 15, 60),
                )),
                PlanDay("Lower & core 2", listOf(
                    i("leg_curl", 4, 10, 15, 60), i("leg_extension", 4, 12, 15), i("seated_crunch", 3, 15, 20, 45),
                    i("kneeling_crunch", 3, 12, 15, 60),
                )),
            ),
            builtIn = true,
        ),
        Plan(
            "ppl", "Push / Pull / Legs",
            "3 days in turn, done 3 to 6 times a week: pushing muscles, pulling muscles, then legs and core.",
            listOf(
                PlanDay("Push", listOf(
                    i("chest_press", 4, 8, 12, 120), i("butterfly", 3, 10, 15), i("upright_row", 3, 10, 12),
                    i("front_raise", 3, 10, 15, 60), i("triceps_pushdown", 3, 10, 12, 60), i("rope_pushdown", 2, 12, 15, 60),
                )),
                PlanDay("Pull", listOf(
                    i("lat_pulldown", 4, 8, 12, 120), i("seated_row", 3, 8, 12), i("close_grip_pulldown", 3, 10, 12),
                    i("reverse_butterfly", 3, 12, 15, 60), i("shrug", 3, 12, 15, 60), i("preacher_curl", 3, 10, 12, 60),
                    i("standing_curl", 2, 10, 12, 60),
                )),
                PlanDay("Legs & core", listOf(
                    i("leg_extension", 4, 10, 15), i("leg_curl", 4, 10, 15, 60), i("kneeling_crunch", 3, 12, 15, 60),
                    i("seated_crunch", 3, 15, 20, 45), i("sit_up", 2, 12, 20, 60),
                )),
            ),
            builtIn = true,
        ),
    )
}
