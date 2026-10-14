# VoltBody — Sistema de diseño v2 ("rompedor", 120 Hz)

El usuario odia el diseño anterior: genérico, lento, verde lima. Quiere algo **rompedor, muy intuitivo, con animaciones fluidas a 120 Hz**.
Referencias (3 mockups): (1) app oscura estilo bento con donut grande, barras y tarjetas muy redondeadas; (2) portada blanca con tipografía gigante bicolor ("Stronger Every Day" en negro + "Healthier Every Step" en gris claro) y botón pill grande; (3) tarjetas con gradiente suave azul/aguamarina, titulares enormes en negrita ("The Power Start"), temporizador circular con órbita, curva de kcal con tooltips, botón pill negro de pausa.
**Nada de verde lima/neón verde.** Tipografía: **Plus Jakarta Sans** (variable, `R.font.jakarta`), titulares peso 800 con tracking negativo, cuerpo 500.

## Tokens
- Temas (los ids vienen del servidor y NO se pueden cambiar; solo cambian colores y etiquetas): `ocaso-negro` = **Coral** (por defecto en la app: accent #FF5A36, accent2 #FF9A3D), `aguamarina-negro` = **Hielo** (accent #3DD6F5, accent2 #5B7CFF), `verde-negro` = **Violeta** (accent #9B6BFF, accent2 #FF6BC1). Fondo #0B0B0F, surface #15151B, surface2 #1F1F27, texto #F6F3EF, dim #8D8A96. Gradientes de marca = accent→accent2. Estado "danger" #FF4D6A, "water" #4DA3FF, kcal = accent.
- Formas: radios 28dp tarjetas, 36dp hero, pill para botones. Sin bordes duros: usa 1dp blanco 6% y brillos de gradiente.
- Tipo: Display 52/800 (-2%), H1 32/800, H2 22/700, Title 17/700, Body 14/500, Label 11/700 MAYÚSCULAS +8% tracking. Números grandes siempre en 800.
- Movimiento: `spring(dampingRatio=0.82f, stiffness=380f)` para entradas, `tween(450, EaseOutExpo)` para contadores/dibujos; entrada escalonada (60ms) en toda lista; transición de pestañas = fade + escala 0.96→1 + desplazamiento 24dp; pulsar = escala 0.94 con spring rebotón; háptica en cada selección importante. Pantallas con fondos vivos (malla de degradados animada, lenta).

## Reglas de RENDIMIENTO (obligatorias, el usuario dice que va lenta)
1. Los valores animados se leen en fase de **draw/layout** (`graphicsLayer {}`, `drawBehind`, `Canvas`, `Modifier.offset{}`), NUNCA en composición (no `val x by animate...` usado como parámetro de composables hijos).
2. No recolectar `AppState` entero en composables hijos: usa `repo.state.map{...}.distinctUntilChanged()` con `collectAsStateWithLifecycle` o `derivedStateOf`. Listas con `key=` y `contentType=`. Datos derivados pesados con `remember(keys)` o fuera de composición.
3. Nada de `blur`, sombras grandes (`shadow` > 8dp), ni `Dialog` con animaciones pesadas innecesarias. Prefiere `Canvas`/`drawWithCache` a muchos composables. Pocas capas de alfa.
4. Imágenes/GIF con Coil con `size` acotado y `crossfade(false)` en listas; GIF solo el visible. Decodificar base64/fotos fuera del hilo principal (`Dispatchers.Default`) y cachear el `ImageBitmap` en `remember`.
5. Cálculos (fatiga, rachas, agregados) en `remember(logs)`; nunca dentro de `LazyColumn` items sin remember.
6. Compila en release (R8) para pruebas: el APK debug de Compose es 3–5x más lento.
7. 120 Hz: `MainActivity` fija `preferredDisplayModeId` al modo de mayor refresco (ya lo hace el agente de shell).

## Componentes (ui/Components.kt, ui/Anim.kt, ui/theme/*) — API estable
Se mantienen los nombres existentes (`VoltCard`, `GradientBanner`, `SectionTitle`, `StatPill`, `PrimaryButton`, `VoltTextField`, `VoltChip`, `EmptyState`, `staggeredEntry`, `pressScale`, `CountUpText`, `DonutProgress`, `AnimatedBars`, `Sparkline`, `rememberPulse`, `Volt.*`) para que todo compile, pero **restilizados**. Nuevos (los crea el agente de design system): `Volt.accent2`, `Volt.gradient`(Brush), `VoltType` (estilos), `MeshBackground(modifier)`, `KineticTitle(lines)`, `BentoCard`, `BigNumberPicker`, `SegmentedProgress`, `SwipeToStart`, `GlowRing`, `Modifier.gradientBorder()`.

## Acabado PREMIUM (obligatorio en todas las pantallas)
- Jerarquía clara: un único elemento héroe por pantalla (titular o número gigante); el resto en segundo plano. Espaciado en rejilla de 8dp, márgenes 20dp, mucho aire. Nada de pantallas apretadas.
- Detalles de lujo: bordes degradados finos (1dp, accent al 35% → transparente) en tarjetas destacadas; brillo radial sutil detrás del número héroe; gradiente de marca solo en CTA, anillos y datos clave (nunca como relleno de todo); texto en gradiente para cifras héroe; iconos redondeados de trazo uniforme; divisores casi invisibles.
- Microinteracciones: botones con spring y háptica ligera; chips que "saltan" al seleccionarse; contadores que ruedan dígito a dígito; checks que se dibujan; confeti/partículas contenidas en hitos; pull-to-refresh con anillo propio; skeletons con shimmer suave en cargas (no spinners genéricos); estados vacíos ilustrados con trazos animados y copy con personalidad.
- Transiciones: elementos compartidos entre lista y detalle (expansión de tarjeta a pantalla), entradas escalonadas, salida rápida (150ms) y entrada expresiva (400ms); nunca saltos bruscos de layout (animateContentSize / AnimatedContent).
- Copy en español cercano y motivador, corto, con verbos de acción. Números con unidades en peso menor (kcal, kg) y alineados a la línea base.
- Accesibilidad: contraste ≥ 4.5:1 en texto, áreas táctiles ≥ 48dp, respeta "reducir animaciones" (ANIMATOR_DURATION_SCALE=0 → sin movimiento decorativo).
