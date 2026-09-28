package com.pasquale.notificationblocker.ui.theme

/**
 * Fixed palette of the zen forest illustration (ui/zen), as opaque ARGB ints so the scene logic stays
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

    // Nature: stone, wood, water birds
    val Stone = rgb(0xA3A3AE)
    val StoneLight = rgb(0xC4C4CE)
    val StoneDark = rgb(0x6B6B78)
    val LanternWindow = rgb(0x3A302C)
    val LanternLight = rgb(0xFFD27F)
    val LanternFlicker = rgb(0xFFB347)
    val Firefly = rgb(0xE8FF8A)
    val HeronWhite = rgb(0xF7F7F2)
    val HeronShade = rgb(0xD5D9E0)
    val HeronBeak = rgb(0xE8B040)
    val HeronLeg = rgb(0x3A3A40)
    val Trunk = rgb(0x5B3A2E)
    val TrunkLight = rgb(0x7A5040)
    val Snow = rgb(0xF4F7FB)
    val SnowShade = rgb(0xCBD6E6)

    // Desk and city (working hours)
    val IndoorLight = rgb(0xFFEFD9)   // warm lamp light once it is dark outside
    val Wall = rgb(0xE4DDD0)
    val WallShade = rgb(0xCFC5B4)
    val WindowFrame = rgb(0xF6F3ED)
    val WindowFrameShade = rgb(0xD6CFC2)
    val BuildingFar = rgb(0x9DAAC0)
    val BuildingNear = rgb(0x6F7E98)
    val BuildingShade = rgb(0x5A6780)
    val Plane = rgb(0xF2F4F8)
    val PlaneLight = rgb(0xFF5A5A)
    val Desk = rgb(0xBE8F62)
    val DeskLight = rgb(0xD6A878)
    val DeskEdge = rgb(0x8E6440)
    val LaptopBody = rgb(0x3A3F4B)
    val LaptopBase = rgb(0xC7CCD5)
    val ScreenBg = rgb(0xF4F6FA)
    val ScreenSide = rgb(0xDFE4EE)
    val ScreenHeader = rgb(0x5C4E8C)  // brand primary
    val ScreenLine = rgb(0xBAC2CE)
    val ScreenBar = rgb(0xB8A6FF)     // brand primary (dark theme)
    val ScreenAccent = rgb(0xFFC56B)  // brand secondary
    val Mug = rgb(0xEEE8F8)
    val MugShade = rgb(0xCFC6E3)
    val Coffee = rgb(0x6B4430)
    val Steam = rgb(0xFFFFFF)
    val Paper = rgb(0xFBF8F1)
    val PaperLine = rgb(0xC9D3E0)
    val Pen = rgb(0x2B2D42)
    val Pot = rgb(0xC56A45)
    val PotShade = rgb(0x9E4F33)
    val Plant = rgb(0x5AA466)
    val PlantDark = rgb(0x3C7F4A)

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
