package com.voltbody.app.ui.screens.workout

import com.voltbody.app.data.Exercise

/** Entrada de la biblioteca de ejercicios (equivale a ExerciseLibraryEntry de la web). exerciseType: weighted|isometric|bodyweight|cardio */
data class ExerciseLibraryEntry(
    val id: String, val name: String, val nameEn: String? = null, val technique: String? = null,
    val muscleGroup: String, val defaultSets: Int, val defaultReps: String, val exerciseType: String? = null,
)

/** Igual que addToCustomWorkout de la web. */
fun ExerciseLibraryEntry.toExercise() = Exercise(
    id = id, name = name, nameEn = nameEn, sets = defaultSets, reps = defaultReps, weight = 0.0, gifUrl = "",
    muscleGroup = muscleGroup, technique = technique, exerciseType = exerciseType,
)

fun libraryEntryById(id: String): ExerciseLibraryEntry? = defaultExerciseLibrary.firstOrNull { it.id == id }

val defaultExerciseLibrary: List<ExerciseLibraryEntry> = listOf(
    ExerciseLibraryEntry("chest-1", "Press de banca", "bench press", muscleGroup = "Pecho", defaultSets = 4, defaultReps = "8-12",
        technique = "1. Túmbate en el banco con los pies planos en el suelo.\n2. Agarra la barra a la anchura de los hombros.\n3. Baja la barra hasta rozar el pecho controladamente.\n4. Empuja explosivo hasta extender los codos.\n5. Mantén las escápulas retraídas durante todo el movimiento."),
    ExerciseLibraryEntry("chest-2", "Press inclinado con mancuernas", "incline dumbbell press", muscleGroup = "Pecho", defaultSets = 4, defaultReps = "10-12",
        technique = "1. Ajusta el banco a 30-45°.\n2. Sujeta una mancuerna en cada mano a la altura del pecho.\n3. Empuja las mancuernas hacia arriba y al centro.\n4. Baja despacio hasta que los codos queden a 90°.\n5. Mantén los glúteos apoyados en el banco."),
    ExerciseLibraryEntry("chest-3", "Aperturas en polea", "cable fly", muscleGroup = "Pecho", defaultSets = 3, defaultReps = "12-15",
        technique = "1. Coloca las poleas en posición alta y da un paso adelante.\n2. Agarra un extremo en cada mano con los brazos extendidos a los lados.\n3. Lleva las manos al frente con un arco, como si abrazaras.\n4. Contrae el pecho en la posición final 1 segundo.\n5. Vuelve despacio a la posición inicial."),
    ExerciseLibraryEntry("chest-4", "Fondos en paralelas", "chest dips", muscleGroup = "Pecho", defaultSets = 3, defaultReps = "8-12", exerciseType = "bodyweight",
        technique = "1. Apóyate en las barras con los brazos extendidos.\n2. Inclínate ligeramente hacia delante para activar el pecho.\n3. Baja doblando los codos hasta 90°.\n4. Empuja hacia arriba controlando el movimiento.\n5. No hiperextiendas los codos arriba."),
    ExerciseLibraryEntry("back-1", "Dominadas asistidas", "assisted pull up", muscleGroup = "Espalda", defaultSets = 4, defaultReps = "6-10", exerciseType = "bodyweight",
        technique = "1. Agarra la barra con agarre prono a la anchura de los hombros.\n2. Cuelga con los brazos extendidos y activa las escápulas hacia abajo y atrás.\n3. Tira del cuerpo hacia arriba hasta que la barbilla supere la barra.\n4. Baja lentamente hasta la extensión completa.\n5. Evita balancear el cuerpo."),
    ExerciseLibraryEntry("back-2", "Remo con barra", "barbell row", muscleGroup = "Espalda", defaultSets = 4, defaultReps = "8-12",
        technique = "1. Inclínate hacia delante con la espalda recta y rodillas ligeramente flexionadas.\n2. Agarra la barra a la anchura de los hombros con agarre prono.\n3. Tira de la barra hacia el abdomen.\n4. Aprieta las escápulas en la posición alta.\n5. Baja despacio hasta la extensión completa."),
    ExerciseLibraryEntry("back-3", "Jalón al pecho", "lat pulldown", muscleGroup = "Espalda", defaultSets = 3, defaultReps = "10-12",
        technique = "1. Siéntate en la máquina y sujeta la barra con agarre amplio.\n2. Inclínate ligeramente hacia atrás.\n3. Baja la barra hasta el nivel del mentón o parte alta del pecho.\n4. Aprieta los dorsales en la posición baja.\n5. Sube controlando el peso, sin dejar que los hombros suban."),
    ExerciseLibraryEntry("back-4", "Remo en cable sentado", "seated cable row", muscleGroup = "Espalda", defaultSets = 3, defaultReps = "10-12",
        technique = "1. Siéntate con la espalda recta y pies apoyados en la plataforma.\n2. Sujeta el accesorio con ambas manos y brazos extendidos.\n3. Tira hacia el abdomen juntando los codos.\n4. Aprieta las escápulas al final del recorrido.\n5. Vuelve controlando la resistencia del cable."),
    ExerciseLibraryEntry("legs-1", "Sentadilla goblet", "goblet squat", muscleGroup = "Piernas", defaultSets = 4, defaultReps = "10-12",
        technique = "1. Sujeta una mancuerna o kettlebell con ambas manos a la altura del pecho.\n2. Separa los pies a la anchura de los hombros con las puntas hacia afuera.\n3. Baja manteniendo el torso erguido hasta que los muslos queden paralelos al suelo.\n4. Empuja a través de los talones para subir.\n5. Mantén las rodillas alineadas con los dedos de los pies."),
    ExerciseLibraryEntry("legs-2", "Prensa de piernas", "leg press", muscleGroup = "Piernas", defaultSets = 4, defaultReps = "10-15",
        technique = "1. Siéntate con la espalda completamente apoyada.\n2. Coloca los pies a la anchura de los hombros en la plataforma.\n3. Libera el seguro y baja la plataforma doblando las rodillas hasta 90°.\n4. Empuja de vuelta a la posición inicial sin bloquear las rodillas.\n5. No dejes que la zona lumbar se despegue del respaldo."),
    ExerciseLibraryEntry("legs-3", "Peso muerto rumano", "romanian deadlift", muscleGroup = "Piernas", defaultSets = 4, defaultReps = "8-10",
        technique = "1. Agarra la barra a la anchura de los hombros con la espalda recta.\n2. Con las rodillas ligeramente flexionadas, desliza la barra por las piernas mientras bajas.\n3. Siente el estiramiento en los isquiotibiales al descender hasta la mitad de la tibia.\n4. Lleva las caderas hacia adelante para subir.\n5. Aprieta los glúteos en la posición alta."),
    ExerciseLibraryEntry("legs-4", "Zancadas caminando", "walking lunge", muscleGroup = "Piernas", defaultSets = 3, defaultReps = "12 por pierna",
        technique = "1. De pie con los pies juntos y mancuernas en manos.\n2. Da un paso largo hacia adelante y baja la rodilla trasera hacia el suelo.\n3. La rodilla delantera no debe pasar la punta del pie.\n4. Empuja con el pie delantero y lleva la pierna trasera al frente.\n5. Alterna lados caminando."),
    ExerciseLibraryEntry("shoulders-1", "Press militar con mancuernas", "dumbbell overhead press", muscleGroup = "Hombros", defaultSets = 4, defaultReps = "8-12",
        technique = "1. Siéntate en un banco con respaldo o de pie.\n2. Sujeta las mancuernas a la altura de los hombros con los codos a 90°.\n3. Empuja hacia arriba hasta casi extender los brazos.\n4. Baja controlando hasta la posición inicial.\n5. No arquees la espalda ni muevas la cabeza hacia adelante."),
    ExerciseLibraryEntry("shoulders-2", "Elevaciones laterales", "lateral raise", muscleGroup = "Hombros", defaultSets = 3, defaultReps = "12-15",
        technique = "1. De pie con las mancuernas a los lados.\n2. Con un ligero codo doblado, eleva los brazos lateralmente hasta la altura de los hombros.\n3. Gira levemente las muñecas como si vaciaras una jarra.\n4. Baja muy lentamente (3-4 segundos).\n5. Evita balancear el torso."),
    ExerciseLibraryEntry("shoulders-3", "Pájaros inclinado", "bent over lateral raise", muscleGroup = "Hombros", defaultSets = 3, defaultReps = "12-15",
        technique = "1. Inclínate hacia delante con la espalda recta.\n2. Sujeta las mancuernas con los brazos colgando.\n3. Eleva los brazos hacia los lados hasta la altura de los hombros.\n4. Focaliza el movimiento en el deltoides posterior y romboides.\n5. Baja controladamente sin tirar con la espalda."),
    ExerciseLibraryEntry("shoulders-4", "Face pulls", "face pull", muscleGroup = "Hombros", defaultSets = 3, defaultReps = "12-15",
        technique = "1. Coloca la polea alta con cuerda y agarra los extremos.\n2. Da un paso atrás hasta tener tensión en el cable.\n3. Tira hacia la cara separando los extremos de la cuerda.\n4. Lleva los codos hacia atrás a la altura de los hombros.\n5. Contrae el deltoides posterior y retractores escapulares."),
    ExerciseLibraryEntry("arms-1", "Curl de bíceps con barra", "barbell curl", muscleGroup = "Bíceps", defaultSets = 3, defaultReps = "10-12",
        technique = "1. De pie con la barra sujeta con agarre supino a la anchura de los hombros.\n2. Mantén los codos pegados a los costados durante todo el movimiento.\n3. Sube la barra doblando los codos hasta contraer el bíceps.\n4. Baja lentamente (2-3 segundos) sin perder la tensión.\n5. No balancees el torso para ganar inercia."),
    ExerciseLibraryEntry("arms-2", "Curl martillo", "hammer curl", muscleGroup = "Bíceps", defaultSets = 3, defaultReps = "10-12",
        technique = "1. De pie con mancuernas y las palmas mirando al torso (agarre neutro).\n2. Sube alternando brazos o de forma simultánea.\n3. Mantén los codos fijos en los costados.\n4. El pulgar siempre apunta hacia arriba.\n5. Baja controlado, sin dejar caer el peso."),
    ExerciseLibraryEntry("arms-3", "Extensión de tríceps en polea", "cable tricep extension", muscleGroup = "Tríceps", defaultSets = 3, defaultReps = "10-12",
        technique = "1. Coloca la polea alta con cuerda o barra recta.\n2. Agarra el accesorio con agarre prono y pega los codos a los costados.\n3. Extiende los brazos hacia abajo hasta la extensión completa.\n4. Contrae el tríceps 1 segundo en la posición baja.\n5. Vuelve despacio sin abrir los codos."),
    ExerciseLibraryEntry("arms-4", "Press francés", "skull crusher", muscleGroup = "Tríceps", defaultSets = 3, defaultReps = "10-12",
        technique = "1. Túmbate en el banco con una barra EZ o mancuernas sobre la cabeza.\n2. Con los codos apuntando al techo, baja el peso hacia la frente.\n3. Extiende los brazos sin bloquear los codos.\n4. Mantén los codos fijos, sin que se abran hacia los lados.\n5. Controla bien la bajada para proteger los codos."),
    // Isométricos
    ExerciseLibraryEntry("core-1", "Plancha", "plank", muscleGroup = "Core", defaultSets = 3, defaultReps = "30-60s", exerciseType = "isometric",
        technique = "1. Apoya los antebrazos y las puntas de los pies en el suelo.\n2. Mantén el cuerpo alineado de cabeza a talones.\n3. Contrae el abdomen, glúteos y cuádriceps durante toda la serie.\n4. No dejes caer ni elevar las caderas.\n5. Respira de forma continua y controlada."),
    ExerciseLibraryEntry("core-2", "Sentadilla isométrica", "wall sit", muscleGroup = "Piernas", defaultSets = 3, defaultReps = "30-60s", exerciseType = "isometric",
        technique = "1. Apoya la espalda en la pared.\n2. Baja hasta que los muslos queden paralelos al suelo y las rodillas a 90°.\n3. Los pies deben estar a la anchura de los hombros.\n4. Mantén la posición sin mover la espalda de la pared.\n5. Focaliza la tensión en cuádriceps y glúteos."),
    ExerciseLibraryEntry("cali-1", "Frog Stand", "frog stand", muscleGroup = "Calistenia", defaultSets = 3, defaultReps = "10-30s", exerciseType = "isometric",
        technique = "1. En cuclillas, coloca las manos en el suelo a la anchura de los hombros.\n2. Apoya las rodillas sobre los tríceps.\n3. Inclínate lentamente hacia delante transfiriendo el peso a las manos.\n4. Levanta los pies del suelo y mantén el equilibrio.\n5. Activa el core y mira al suelo a 30 cm enfrente."),
    ExerciseLibraryEntry("core-3", "Plancha lateral", "side plank", muscleGroup = "Core", defaultSets = 3, defaultReps = "20-45s", exerciseType = "isometric",
        technique = "1. Apoya el antebrazo derecho y el borde del pie derecho.\n2. Eleva las caderas hasta alinear el cuerpo lateralmente.\n3. Mantén el abdomen y glúteo activados.\n4. No dejes caer la cadera hacia el suelo.\n5. Repite por el lado izquierdo."),
)
