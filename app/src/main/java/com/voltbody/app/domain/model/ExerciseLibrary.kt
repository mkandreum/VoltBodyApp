package com.voltbody.app.domain.model

val defaultExerciseLibrary = listOf(
    ExerciseLibraryEntry(
        id = "chest-1", name = "Press de banca", nameEn = "bench press", muscleGroup = "Pecho", defaultSets = 4, defaultReps = "8-12",
        technique = "1. Túmbate en el banco con los pies planos en el suelo.\n2. Agarra la barra a la anchura de los hombros.\n3. Baja la barra hasta rozar el pecho controladamente.\n4. Empuja explosivo hasta extender los codos.\n5. Mantén las escápulas retraídas durante todo el movimiento.",
        exerciseType = ExerciseType.WEIGHTED
    ),
    ExerciseLibraryEntry(
        id = "chest-2", name = "Press inclinado con mancuernas", nameEn = "incline dumbbell press", muscleGroup = "Pecho", defaultSets = 4, defaultReps = "10-12",
        technique = "1. Ajusta el banco a 30-45°.\n2. Sujeta una mancuerna en cada mano a la altura del pecho.\n3. Empuja las mancuernas hacia arriba y al centro.\n4. Baja despacio hasta que los codos queden a 90°.\n5. Mantén los glúteos apoyados en el banco.",
        exerciseType = ExerciseType.WEIGHTED
    ),
    ExerciseLibraryEntry(
        id = "chest-3", name = "Aperturas en polea", nameEn = "cable fly", muscleGroup = "Pecho", defaultSets = 3, defaultReps = "12-15",
        technique = "1. Coloca las poleas en posición alta y da un paso adelante.\n2. Agarra un extremo en cada mano con los brazos extendidos a los lados.\n3. Lleva las manos al frente con un arco, como si abrazaras.\n4. Contrae el pecho en la posición final 1 segundo.\n5. Vuelve despacio a la posición inicial.",
        exerciseType = ExerciseType.WEIGHTED
    ),
    ExerciseLibraryEntry(
        id = "chest-4", name = "Fondos en paralelas", nameEn = "chest dips", muscleGroup = "Pecho", defaultSets = 3, defaultReps = "8-12",
        technique = "1. Apóyate en las barras con los brazos extendidos.\n2. Inclínate ligeramente hacia delante para activar el pecho.\n3. Baja doblando los codos hasta 90°.\n4. Empuja hacia arriba controlando el movimiento.\n5. No hiperextiendas los codos arriba.",
        exerciseType = ExerciseType.BODYWEIGHT
    ),
    ExerciseLibraryEntry(
        id = "back-1", name = "Dominadas asistidas", nameEn = "assisted pull up", muscleGroup = "Espalda", defaultSets = 4, defaultReps = "6-10",
        technique = "1. Agarra la barra con agarre prono a la anchura de los hombros.\n2. Cuelga con los brazos extendidos y activa las escápulas hacia abajo y atrás.\n3. Tira del cuerpo hacia arriba hasta que la barbilla supere la barra.\n4. Baja lentamente hasta la extensión completa.\n5. Evita balancear el cuerpo.",
        exerciseType = ExerciseType.BODYWEIGHT
    ),
    ExerciseLibraryEntry(
        id = "back-2", name = "Remo con barra", nameEn = "barbell row", muscleGroup = "Espalda", defaultSets = 4, defaultReps = "8-12",
        technique = "1. Inclínate hacia delante con la espalda recta y rodillas ligeramente flexionadas.\n2. Agarra la barra a la anchura de los hombros con agarre prono.\n3. Tira de la barra hacia el abdomen.\n4. Aprieta las escápulas en la posición alta.\n5. Baja despacio hasta la extensión completa.",
        exerciseType = ExerciseType.WEIGHTED
    ),
    ExerciseLibraryEntry(
        id = "back-3", name = "Jalón al pecho", nameEn = "lat pulldown", muscleGroup = "Espalda", defaultSets = 3, defaultReps = "10-12",
        technique = "1. Siéntate en la máquina y sujeta la barra con agarre amplio.\n2. Inclínate ligeramente hacia atrás.\n3. Baja la barra hasta el nivel del mentón o parte alta del pecho.\n4. Aprieta los dorsales en la posición baja.\n5. Sube controlando el peso, sin dejar que los hombros suban.",
        exerciseType = ExerciseType.WEIGHTED
    ),
    ExerciseLibraryEntry(
        id = "back-4", name = "Remo en cable sentado", nameEn = "seated cable row", muscleGroup = "Espalda", defaultSets = 3, defaultReps = "10-12",
        technique = "1. Siéntate con la espalda recta y pies apoyados en la plataforma.\n2. Sujeta el accesorio con ambas manos y brazos extendidos.\n3. Tira hacia el abdomen juntando los codos.\n4. Aprieta las escápulas al final del recorrido.\n5. Vuelve controlando la resistencia del cable.",
        exerciseType = ExerciseType.WEIGHTED
    ),
    ExerciseLibraryEntry(
        id = "legs-1", name = "Sentadilla goblet", nameEn = "goblet squat", muscleGroup = "Piernas", defaultSets = 4, defaultReps = "10-12",
        technique = "1. Sujeta una mancuerna o kettlebell con ambas manos a la altura del pecho.\n2. Separa los pies a la anchura de los hombros con las puntas hacia afuera.\n3. Baja manteniendo el torso erguido hasta que los muslos queden paralelos al suelo.\n4. Empuja a través de los talones para subir.\n5. Mantén las rodillas alineadas con los dedos de los pies.",
        exerciseType = ExerciseType.WEIGHTED
    ),
    ExerciseLibraryEntry(
        id = "legs-2", name = "Prensa de piernas", nameEn = "leg press", muscleGroup = "Piernas", defaultSets = 4, defaultReps = "10-15",
        technique = "1. Siéntate con la espalda completamente apoyada.\n2. Coloca los pies a la anchura de los hombros en la plataforma.\n3. Libera el seguro y baja la plataforma doblando las rodillas hasta 90°.\n4. Empuja de vuelta a la posición inicial sin bloquear las rodillas.\n5. No dejes que la zona lumbar se despegue del respaldo.",
        exerciseType = ExerciseType.WEIGHTED
    ),
    ExerciseLibraryEntry(
        id = "legs-3", name = "Peso muerto rumano", nameEn = "romanian deadlift", muscleGroup = "Piernas", defaultSets = 4, defaultReps = "8-10",
        technique = "1. Agarra la barra a la anchura de los hombros con la espalda recta.\n2. Con las rodillas ligeramente flexionadas, desliza la barra por las piernas mientras bajas.\n3. Siente el estiramiento en los isquiotibiales al descender hasta la mitad de la tibia.\n4. Lleva las caderas hacia adelante para subir.\n5. Aprieta los glúteos en la posición alta.",
        exerciseType = ExerciseType.WEIGHTED
    ),
    ExerciseLibraryEntry(
        id = "legs-4", name = "Zancadas caminando", nameEn = "walking lunge", muscleGroup = "Piernas", defaultSets = 3, defaultReps = "12 por pierna",
        technique = "1. De pie con los pies juntos y mancuernas en manos.\n2. Da un paso largo hacia adelante y baja la rodilla trasera hacia el suelo.\n3. La rodilla delantera no debe pasar la punta del pie.\n4. Empuja con el pie delantero y lleva la pierna trasera al frente.\n5. Alterna lados caminando.",
        exerciseType = ExerciseType.WEIGHTED
    ),
    ExerciseLibraryEntry(
        id = "core-1", name = "Plancha", nameEn = "plank", muscleGroup = "Core", defaultSets = 3, defaultReps = "30-60s",
        technique = "1. Apoya los antebrazos y las puntas de los pies en el suelo.\n2. Mantén el cuerpo alineado de cabeza a talones.\n3. Contrae el abdomen, glúteos y cuádriceps durante toda la serie.\n4. No dejes caer ni elevar las caderas.\n5. Respira de forma continua y controlada.",
        exerciseType = ExerciseType.ISOMETRIC
    )
)
