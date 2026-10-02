package com.pasquale.notificationblocker.ui.theme

/**
 * Fixed palette of the scenes on Home (ui/zen: desk and lake pier), as opaque ARGB ints so the scene logic stays
 * pure Kotlin (JVM-testable). The illustration keeps its own colors in both themes: it is framed by a card.
 *
 * Colors are "daylight" values: the scene multiplies them by the ambient light of the time of day
 * ([Light]); only sky, water and light sources are defined per time of day.
 */
@Suppress("MagicNumber")
object ZenPalette {
    private fun rgb(value: Long): Int = (0xFF000000 or value).toInt()

    // Sky objects
    val Star = rgb(0xFFF1C1)
    val Sun = rgb(0xFFF6D5)
    val SunGlow = rgb(0xFFE08A)
    val Moon = rgb(0xF4EEDC)
    val MoonShade = rgb(0xD6CEB8)
    val Cloud = rgb(0xFFFFFF)
    val CloudShade = rgb(0xE3EAF2)
    val Bird = rgb(0x2B2D42)

    // Rainy days
    val Overcast = rgb(0x8C95A6)      // veil over the sky
    val RainCloud = rgb(0xAEB6C4)
    val Rain = rgb(0xDCE6F2)
    val RainAmbient = rgb(0xC4CAD6)   // multiplies the ambient light on rainy days

    // Shared by both scenes: warm lights, wood, snow
    val LanternLight = rgb(0xFFD27F)
    val LanternFlicker = rgb(0xFFB347)
    val Firefly = rgb(0xE8FF8A)
    val Trunk = rgb(0x5B3A2E)
    val Snow = rgb(0xF4F7FB)
    val SnowShade = rgb(0xCBD6E6)

    // Desk in a high-rise office (working hours): the "Chai" tones of the brand indoors
    val IndoorLight = rgb(0xFFEFD9)   // warm lamp light once it is dark outside
    val Plane = rgb(0xF2F4F8)
    val PlaneLight = rgb(0xFF5A5A)    // blinking light of the plane and of the spire
    val Steam = rgb(0xFFFFFF)
    val StudioWall = rgb(0xF1E4D3)
    val StudioWallShade = rgb(0xE0CDB6)
    val StudioDesk = rgb(0xB98A62)
    val StudioDeskTop = rgb(0xA47552)
    val StudioDeskEdge = rgb(0x8E6440)
    val Monitor = rgb(0x3B2F2A)
    val MonitorScreen = rgb(0x2A2320)
    val CodeAmber = rgb(0xE9B872)
    val CodeCream = rgb(0xF1E4D3)
    val CodeSage = rgb(0x9DC4A0)
    val CodeCoral = rgb(0xE08E6D)
    val CodeCursor = rgb(0xFBF5EC)
    val Keyboard = rgb(0x4A3C35)
    val Key = rgb(0x6B5A50)
    val KeyLit = rgb(0xE9B872)
    val Terracotta = rgb(0xD97757)
    val TerracottaShade = rgb(0xC4654A)
    val StudioLeaf = rgb(0x93BB8F)
    val StudioLeafDark = rgb(0x7FA77F)
    val ClockFace = rgb(0xFBF5EC)
    val ScreenWarmGlow = rgb(0xF3D9B0)   // light of the code screen on the desk after dark
    val WindowMetal = rgb(0x34302E)      // thin frames of the glass wall
    val TowerGlass = rgb(0x8FA9C4)       // glass facades, mixed with the sky they reflect
    val TowerGlassDark = rgb(0x6D86A3)
    val TowerStone = rgb(0xB9B2A8)       // older stepped tower
    val TowerStoneShade = rgb(0x948C82)
    val Spire = rgb(0xD8D4CC)

    // Lake pier (off work)
    val Fir = rgb(0x2E5444)
    val Pier = rgb(0xB58358)
    val PierLight = rgb(0xC99A6E)
    val PierShade = rgb(0x7E5537)
    val PierGap = rgb(0x6A4630)
    val LampIron = rgb(0x3E414C)
    val LampGlass = rgb(0xFFF1D6)
    val BoatHull = rgb(0xEDE6DA)
    val BoatStripe = rgb(0xD97757)
    val BoatInside = rgb(0x9C6B48)
    val Rope = rgb(0xC9B48A)
    val Reed = rgb(0x7FA06A)
    val ReedDark = rgb(0x5E7F4E)
    val ReedAutumn = rgb(0xC9A85C)
    val ReedWinter = rgb(0xC2BBA2)
    val Cattail = rgb(0x6B4A33)
    val Duck = rgb(0x8A6E52)
    val DuckHead = rgb(0x3F6B57)
    val Beak = rgb(0xE8B040)
    val LilyPad = rgb(0x5E9A5A)
    val Lily = rgb(0xF4B6C8)

    /** Sky, water and light for one time of day; the scene blends two of them for smooth changes. */
    data class Light(
        val skyTop: Int,
        val skyMid: Int,
        val skyLow: Int,
        val ambient: Int,      // multiplier applied to every daylight color
        val mist: Int,
        val water: Int,
        val waterDeep: Int,
        val waterLight: Int,
        val rays: Float,       // sun rays through the bamboo
        val lantern: Float,    // stone lantern glow
        val stars: Float,
    )

    val Daylight = Light(
        skyTop = rgb(0x3F9BEA), skyMid = rgb(0x74BFF7), skyLow = rgb(0xBFE6FF),
        ambient = rgb(0xFFFFFF), mist = rgb(0xE9F2F2),
        water = rgb(0x4FA3D1), waterDeep = rgb(0x3683B5), waterLight = rgb(0xD9F4FF),
        rays = 1f, lantern = 0f, stars = 0f,
    )
    val GoldenHour = Light(
        skyTop = rgb(0x5B4C8E), skyMid = rgb(0xE2786A), skyLow = rgb(0xFFC77A),
        ambient = rgb(0xFFD6A6), mist = rgb(0xF6D2B0),
        water = rgb(0xC87A62), waterDeep = rgb(0x8C5266), waterLight = rgb(0xFFE3A3),
        rays = 0.8f, lantern = 0.6f, stars = 0f,
    )
    val Night = Light(
        skyTop = rgb(0x0B1238), skyMid = rgb(0x1A2C78), skyLow = rgb(0x2E4AA0),
        ambient = rgb(0x5A6EB8), mist = rgb(0x3A4A80),
        water = rgb(0x1E3272), waterDeep = rgb(0x142352), waterLight = rgb(0xB8C8F0),
        rays = 0f, lantern = 1f, stars = 1f,
    )
    val Dawn = Light(
        skyTop = rgb(0x3E5A9E), skyMid = rgb(0xC99BC4), skyLow = rgb(0xFFD3A8),
        ambient = rgb(0xE8CCD4), mist = rgb(0xF2E0E4),
        water = rgb(0x9496C0), waterDeep = rgb(0x6C6E9E), waterLight = rgb(0xFFE6D6),
        rays = 0.4f, lantern = 0.3f, stars = 0.25f,
    )

    /** Ground, bamboo and trees for one season. */
    data class Flora(
        val ground: Int,
        val groundDark: Int,
        val groundLight: Int,
        val hillFar: Int,
        val forest: Int,
        val bambooStalk: Int,
        val bambooLight: Int,
        val bambooNode: Int,
        val bambooLeaf: Int,
        val bambooLeafDark: Int,
        val tree: Int,          // main canopy (left tree)
        val treeLight: Int,
        val treeDark: Int,
        val tree2: Int,         // second tree (right)
        val tree2Light: Int,
        val tree2Dark: Int,
        val particle: Int,
        val particle2: Int,
    )

    val Spring = Flora(
        ground = rgb(0x7DBF5E), groundDark = rgb(0x5A9A48), groundLight = rgb(0xA6D77A),
        hillFar = rgb(0x8FB5A8), forest = rgb(0x5E9A6E),
        bambooStalk = rgb(0x7FBF52), bambooLight = rgb(0xA8D96E), bambooNode = rgb(0x4E8A3A),
        bambooLeaf = rgb(0x74B85A), bambooLeafDark = rgb(0x4E8F42),
        tree = rgb(0xEFA3BC), treeLight = rgb(0xFFD0DE), treeDark = rgb(0xD5809E),
        tree2 = rgb(0xF5B8CB), tree2Light = rgb(0xFFE0EA), tree2Dark = rgb(0xDC8FA9),
        particle = rgb(0xFFC2D4), particle2 = rgb(0xFFE3EC),
    )
    val Summer = Flora(
        ground = rgb(0x4FAE4A), groundDark = rgb(0x388E3C), groundLight = rgb(0x86D160),
        hillFar = rgb(0x7FAFB0), forest = rgb(0x3F8F5A),
        bambooStalk = rgb(0x6CC24A), bambooLight = rgb(0xA6E36A), bambooNode = rgb(0x3F8A30),
        bambooLeaf = rgb(0x5CC04F), bambooLeafDark = rgb(0x2F8E3A),
        tree = rgb(0x3FA04A), treeLight = rgb(0x74C95A), treeDark = rgb(0x2B7A38),
        tree2 = rgb(0x4DAE50), tree2Light = rgb(0x86D466), tree2Dark = rgb(0x32833C),
        particle = rgb(0x8ED66A), particle2 = rgb(0xC6F08A),
    )
    val Autumn = Flora(
        ground = rgb(0xA89A55), groundDark = rgb(0x8A7A42), groundLight = rgb(0xC9B46A),
        hillFar = rgb(0xB09A8A), forest = rgb(0x9A5E3E),
        bambooStalk = rgb(0x9CB85A), bambooLight = rgb(0xC4D57A), bambooNode = rgb(0x6E8A3A),
        bambooLeaf = rgb(0x8FA04E), bambooLeafDark = rgb(0x6A7E3A),
        tree = rgb(0xD2412F), treeLight = rgb(0xF06A3E), treeDark = rgb(0x9E2B25),       // momiji
        tree2 = rgb(0xF2B632), tree2Light = rgb(0xFFD95A), tree2Dark = rgb(0xC98E1E),    // ginkgo
        particle = rgb(0xE5503A), particle2 = rgb(0xFFC940),
    )
    val Winter = Flora(
        ground = rgb(0xEEF2F8), groundDark = rgb(0xC6D0E0), groundLight = rgb(0xFFFFFF),
        hillFar = rgb(0xB4C2D6), forest = rgb(0x6E8298),
        bambooStalk = rgb(0x6F9E6A), bambooLight = rgb(0x93BC84), bambooNode = rgb(0x4A7248),
        bambooLeaf = rgb(0x5E8A64), bambooLeafDark = rgb(0x456C4C),
        tree = rgb(0xF4F7FB), treeLight = rgb(0xFFFFFF), treeDark = rgb(0xCBD6E6),       // snow on bare branches
        tree2 = rgb(0xF4F7FB), tree2Light = rgb(0xFFFFFF), tree2Dark = rgb(0xCBD6E6),
        particle = rgb(0xFFFFFF), particle2 = rgb(0xDDE6F2),
    )
}
