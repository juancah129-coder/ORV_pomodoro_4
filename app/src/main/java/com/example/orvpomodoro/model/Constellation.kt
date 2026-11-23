package com.example.orvpomodoro.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class Constellation(
    val name: String,
    val color: Color,
    val points: List<Offset>,
    /** Objeto que la identifica (solo en algunas temáticas). */
    val detail: String? = null
)

class ConstellationTheme(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    /** De menos a más estrellas. */
    val tiers: List<Constellation>,
    /** Minutos máximos de cada tramo, salvo el último. Tiene un elemento menos que tiers. */
    val breaks: List<Int> = listOf(2, 5, 10, 20, 35, 50)
) {
    /** Índice del tramo que corresponde a esos minutos. */
    fun tierFor(minutes: Int): Int {
        val i = breaks.indexOfFirst { minutes <= it }
        return if (i == -1) breaks.size else i
    }
}


object Themes {
    const val STAR_STREAM = "theme_star_stream"
    const val KIM_DOKJA_COMPANY = "theme_kim_dokja_company"
    const val NEBULAS = "theme_nebulas"

    // ======================= Star Stream (gratis) =======================

    private val plotter = Constellation(
        "Maquinador Secreto", Color(0xFFB8C7E6),
        listOf(
            Offset(0.12f, 0.75f), Offset(0.30f, 0.30f), Offset(0.55f, 0.15f),
            Offset(0.80f, 0.35f), Offset(0.62f, 0.70f)
        )
    )

    private val ancientDream = Constellation(
        "Sueño Más Antiguo", Color(0xFFC3A6F2),
        List(7) { i ->
            val a = PI * (0.35 + 1.3 * i / 6)
            Offset((0.55 + 0.38 * cos(a)).toFloat(), (0.5 + 0.40 * sin(a)).toFloat())
        }
    )

    private val fireJudge = Constellation(
        "Juez Demoníaco del Fuego", Color(0xFFFF8A5B),
        listOf(
            Offset(0.10f, 0.85f), Offset(0.22f, 0.35f), Offset(0.34f, 0.65f),
            Offset(0.46f, 0.15f), Offset(0.58f, 0.60f), Offset(0.70f, 0.30f),
            Offset(0.82f, 0.70f), Offset(0.92f, 0.45f)
        )
    )

    private val warGod = Constellation(
        "Dios de la Guerra Marítima", Color(0xFF5CC8FF),
        listOf(
            Offset(0.05f, 0.65f), Offset(0.15f, 0.45f), Offset(0.25f, 0.65f),
            Offset(0.35f, 0.45f), Offset(0.45f, 0.65f), Offset(0.55f, 0.45f),
            Offset(0.65f, 0.65f), Offset(0.78f, 0.70f), Offset(0.80f, 0.12f),
            Offset(0.95f, 0.50f), Offset(0.80f, 0.50f)
        )
    )

    private val headband = Constellation(
        "Prisionero de la Diadema Dorada", Color(0xFFF2C94C),
        List(14) { i ->
            val a = 2 * PI * i / 14 - PI / 2
            Offset((0.5 + 0.44 * cos(a)).toFloat(), (0.5 + 0.40 * sin(a)).toFloat())
        }
    )

    private val dragon = Constellation(
        "Dragón Abisal de la Llama Negra", Color(0xFFFF5C8A),
        List(17) { i ->
            Offset(0.05f + 0.9f * i / 16, (0.5 + 0.28 * sin(i * 0.85)).toFloat())
        }
    )

    private val demonKing = Constellation(
        "Rey Demonio de la Salvación", Color(0xFF7CF0C9),
        List(21) { i ->
            val a = i * 0.62
            val r = 0.46 * (1 - i / 24.0)
            Offset((0.5 + r * cos(a)).toFloat(), (0.5 + r * sin(a)).toFloat())
        }
    )

    val starStream = ConstellationTheme(
        id = STAR_STREAM,
        name = "Star Stream",
        description = "Las constelaciones que observan los escenarios.",
        cost = 0,
        tiers = listOf(plotter, ancientDream, fireJudge, warGod, headband, dragon, demonKing)
    )

    // ================== Kim Dokja Company (objetos) ==================

    // 5 estrellas: pluma
    private val sooyoung = Constellation(
        "Han Sooyoung", Color(0xFFFF8FB3),
        listOf(
            Offset(0.14f, 0.88f), Offset(0.26f, 0.60f), Offset(0.76f, 0.12f),
            Offset(0.90f, 0.28f), Offset(0.42f, 0.76f)
        ),
        detail = "Pluma"
    )

    // 7 estrellas: escudo
    private val hyunsung = Constellation(
        "Lee Hyunsung", Color(0xFF8FB8D8),
        listOf(
            Offset(0.25f, 0.15f), Offset(0.50f, 0.08f), Offset(0.75f, 0.15f),
            Offset(0.75f, 0.50f), Offset(0.50f, 0.92f), Offset(0.25f, 0.50f),
            Offset(0.25f, 0.15f)
        ),
        detail = "Escudo"
    )

    // 8 estrellas: sable curvo
    private val heewon = Constellation(
        "Jung Heewon", Color(0xFFFF6B5C),
        List(8) { i ->
            val t = i / 7f
            Offset(0.10f + 0.80f * t, 0.88f - 0.74f * t - 0.20f * sin(PI * t).toFloat())
        },
        detail = "Sable"
    )

    // 11 estrellas: nave tortuga
    private val jihye = Constellation(
        "Lee Jihye", Color(0xFF4FD1C5),
        listOf(
            Offset(0.05f, 0.48f), Offset(0.18f, 0.58f), Offset(0.30f, 0.40f),
            Offset(0.50f, 0.24f), Offset(0.70f, 0.40f), Offset(0.84f, 0.56f),
            Offset(0.78f, 0.74f), Offset(0.60f, 0.82f), Offset(0.38f, 0.82f),
            Offset(0.20f, 0.74f), Offset(0.18f, 0.58f)
        ),
        detail = "Nave tortuga"
    )

    // 14 estrellas: frasco con líquido
    private val seolhwa = Constellation(
        "Lee Seolhwa", Color(0xFF9FE870),
        listOf(
            Offset(0.42f, 0.10f), Offset(0.58f, 0.10f), Offset(0.58f, 0.34f),
            Offset(0.80f, 0.80f), Offset(0.72f, 0.88f), Offset(0.28f, 0.88f),
            Offset(0.20f, 0.80f), Offset(0.42f, 0.34f), Offset(0.42f, 0.10f),
            Offset(0.30f, 0.62f), Offset(0.40f, 0.58f), Offset(0.50f, 0.64f),
            Offset(0.60f, 0.58f), Offset(0.70f, 0.62f)
        ),
        detail = "Frasco"
    )

    // 17 estrellas: espada del Demonio Negro
    private val joonghyuk = Constellation(
        "Yoo Joonghyuk", Color(0xFFB8B4FF),
        listOf(
            Offset(0.50f, 0.94f), Offset(0.56f, 0.90f), Offset(0.44f, 0.86f),
            Offset(0.56f, 0.82f), Offset(0.50f, 0.78f), Offset(0.30f, 0.76f),
            Offset(0.70f, 0.76f), Offset(0.58f, 0.74f), Offset(0.58f, 0.42f),
            Offset(0.50f, 0.05f), Offset(0.42f, 0.42f), Offset(0.42f, 0.74f),
            Offset(0.50f, 0.72f), Offset(0.50f, 0.56f), Offset(0.50f, 0.40f),
            Offset(0.50f, 0.24f), Offset(0.50f, 0.12f)
        ),
        detail = "Espada del Demonio Negro"
    )

    // 21 estrellas: libro abierto con texto
    private val dokja = Constellation(
        "Kim Dokja", Color(0xFFFFD36B),
        listOf(
            Offset(0.50f, 0.80f), Offset(0.50f, 0.24f), Offset(0.30f, 0.16f),
            Offset(0.06f, 0.24f), Offset(0.06f, 0.78f), Offset(0.30f, 0.70f),
            Offset(0.50f, 0.80f), Offset(0.70f, 0.70f), Offset(0.94f, 0.78f),
            Offset(0.94f, 0.24f), Offset(0.70f, 0.16f), Offset(0.50f, 0.24f),
            Offset(0.38f, 0.32f), Offset(0.16f, 0.38f), Offset(0.38f, 0.46f),
            Offset(0.16f, 0.52f), Offset(0.38f, 0.60f), Offset(0.62f, 0.32f),
            Offset(0.84f, 0.38f), Offset(0.62f, 0.46f), Offset(0.84f, 0.52f)
        ),
        detail = "Libro"
    )

    // 6 estrellas: credencial de empleada con cordón (Mino Soft)
    private val sangah = Constellation(
        "Yoo Sangah", Color(0xFFE2D6FF),
        listOf(
            Offset(0.50f, 0.04f), Offset(0.50f, 0.20f), Offset(0.78f, 0.20f),
            Offset(0.78f, 0.92f), Offset(0.22f, 0.92f), Offset(0.22f, 0.20f)
        ),
        detail = "Credencial de Mino Soft"
    )

    // 9 estrellas: ola que rompe (Desastre de las Inundaciones)
    private val yoosung = Constellation(
        "Shin Yoosung", Color(0xFF6AA8FF),
        listOf(
            Offset(0.05f, 0.66f), Offset(0.18f, 0.52f), Offset(0.32f, 0.66f),
            Offset(0.46f, 0.46f), Offset(0.60f, 0.22f), Offset(0.74f, 0.20f),
            Offset(0.84f, 0.34f), Offset(0.78f, 0.46f), Offset(0.66f, 0.40f)
        ),
        detail = "Ola de la Inundación"
    )

    // 12 estrellas: escarabajo (antenas, caparazón y costura central)
    private val gilyoung = Constellation(
        "Lee Gilyoung", Color(0xFFC6E64A),
        listOf(
            Offset(0.34f, 0.06f), Offset(0.50f, 0.20f), Offset(0.66f, 0.06f),
            Offset(0.74f, 0.34f), Offset(0.74f, 0.66f), Offset(0.60f, 0.90f),
            Offset(0.50f, 0.94f), Offset(0.40f, 0.90f), Offset(0.26f, 0.66f),
            Offset(0.26f, 0.34f), Offset(0.50f, 0.20f), Offset(0.50f, 0.90f)
        ),
        detail = "Escarabajo"
    )
    val kimDokjaCompany = ConstellationTheme(
        id = KIM_DOKJA_COMPANY,
        name = "Kim Dokja Company",
        description = "Diez miembros de la compañía, cada uno dibujado como un objeto o símbolo que lo representa.",
        cost = 450,
        tiers = listOf(
            sooyoung, sangah, hyunsung, heewon, yoosung,
            jihye, gilyoung, seolhwa, joonghyuk, dokja
        ),
        breaks = listOf(2, 4, 6, 9, 12, 16, 22, 30, 45)
    )

    // ======================= Nebulosas =======================

    // 5 estrellas: anillo
    private val ring = Constellation(
        "Nebulosa del Anillo", Color(0xFF7FD6FF),
        List(5) { i ->
            val a = -PI / 2 + 2 * PI * (0.1 + 0.8 * i / 4.0)
            Offset((0.5 + 0.40 * cos(a)).toFloat(), (0.5 + 0.40 * sin(a)).toFloat())
        }
    )

    // 7 estrellas: espiral corta
    private val helix = Constellation(
        "Nebulosa de la Hélice", Color(0xFF6EE7E7),
        List(7) { i ->
            val s = i / 6.0
            val a = 2 * PI * 1.05 * s
            val r = 0.08 + 0.36 * s
            Offset((0.5 + r * cos(a)).toFloat(), (0.5 + r * sin(a)).toFloat())
        }
    )

    // 8 estrellas: lemniscata (ojo)
    private val catsEye = Constellation(
        "Nebulosa Ojo de Gato", Color(0xFF7CF0A8),
        List(8) { i ->
            val t = 2 * PI * i / 7
            val d = 1 + sin(t) * sin(t)
            Offset((0.5 + 0.44 * cos(t) / d).toFloat(), (0.5 + 0.8 * sin(t) * cos(t) / d).toFloat())
        }
    )

    // 11 estrellas: estallido (resto de supernova)
    private val crab = Constellation(
        "Nebulosa del Cangrejo", Color(0xFFFFA36B),
        List(11) { i ->
            val a = 2 * PI * i / 11 - PI / 2
            val r = if (i % 2 == 0) 0.44 else 0.20
            Offset((0.5 + r * cos(a)).toFloat(), (0.5 + r * sin(a)).toFloat())
        }
    )

    // 14 estrellas: nube ondulante
    private val orion = Constellation(
        "Nebulosa de Orión", Color(0xFFFF7AC8),
        List(14) { i ->
            val y = 0.5 + 0.22 * sin(i * 0.9) + 0.12 * sin(i * 2.3 + 1.0)
            Offset(0.05f + 0.90f * i / 13, y.toFloat().coerceIn(0.1f, 0.9f))
        }
    )

    // 17 estrellas: tres pilares
    private val pillars = Constellation(
        "Pilares de la Creación", Color(0xFFE0B070),
        listOf(
            Offset(0.06f, 0.92f), Offset(0.08f, 0.60f), Offset(0.12f, 0.34f),
            Offset(0.20f, 0.14f), Offset(0.28f, 0.36f), Offset(0.30f, 0.64f),
            Offset(0.34f, 0.92f), Offset(0.38f, 0.62f), Offset(0.42f, 0.40f),
            Offset(0.50f, 0.08f), Offset(0.58f, 0.40f), Offset(0.62f, 0.64f),
            Offset(0.66f, 0.92f), Offset(0.74f, 0.56f), Offset(0.82f, 0.28f),
            Offset(0.90f, 0.58f), Offset(0.94f, 0.92f)
        )
    )

    // 21 estrellas: galaxia de dos brazos
    private val whirlpool = Constellation(
        "Galaxia del Remolino", Color(0xFF9D8CFF),
        List(21) { i ->
            val k = i - 10
            val s = abs(k) / 10.0
            val a = 3.0 * s + (if (k < 0) 0.0 else PI)
            val r = 0.46 * s
            Offset((0.5 + r * cos(a)).toFloat(), (0.5 + r * sin(a)).toFloat())
        }
    )

    val nebulas = ConstellationTheme(
        id = NEBULAS,
        name = "Nebulosas",
        description = "Nubes de gas y polvo donde nacen las constelaciones.",
        cost = 300,
        tiers = listOf(ring, helix, catsEye, crab, orion, pillars, whirlpool)
    )

    val all = listOf(starStream, kimDokjaCompany, nebulas)
}