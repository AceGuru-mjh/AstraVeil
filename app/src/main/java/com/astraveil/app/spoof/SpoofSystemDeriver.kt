package com.astraveil.app.spoof

/**
 * 系统环境属性派生引擎。
 *
 * 48 台设备 × 15 个新字段 = 720 个数据点，手动填写不现实。
 * 绝大多数系统属性可以从已知字段推导：
 *   brand → buildHost, buildUser
 *   platform → openGlesVersion, cpuAbi, vndkVersion
 *   release → sdk, codename
 *   buildId → bootloader
 */
object SpoofSystemDeriver {

    data class DerivedProps(
        val sdk: String,
        val codename: String,
        val previewSdk: String,
        val allCodenames: String,
        val buildHost: String,
        val buildUser: String,
        val buildDateUtc: String,
        val bootloader: String,
        val baseband: String,
        val hardwareName: String,
        val hardwareRevision: String,
        val openGlesVersion: String,
        val eglImpl: String,
        val vulkanImpl: String,
        val cpuAbiList: String,
        val cpuAbiList64: String,
        val cpuAbiList32: String,
        val bionicArch: String,
        val bionicCpuVariant: String,
        val vbmetaState: String,
        val verifiedBootState: String,
        val flashLocked: String,
        val verityMode: String,
        val trebleEnabled: String,
        val vndkVersion: String,
        val debuggable: String,
        val secure: String,
        val adbSecure: String,
        val kernelVersion: String,
        val simOperatorNumeric: String,
        val simOperatorAlpha: String,
    )

    fun derive(p: SpoofProfile): DerivedProps {
        val sdk = sdkFromRelease(p.release)
        val brand = p.brand.lowercase()

        return DerivedProps(
            sdk = sdk,
            codename = "REL",
            previewSdk = "0",
            allCodenames = "REL",
            buildHost = p.buildHost.ifEmpty { buildHostFor(brand) },
            buildUser = p.buildUser.ifEmpty { buildUserFor(brand) },
            buildDateUtc = patchToUtc(p.securityPatch),
            bootloader = p.bootloader.ifEmpty { bootloaderFor(p) },
            baseband = p.baseband.ifEmpty { basebandFor(p) },
            hardwareName = p.hardwareName.ifEmpty { hardwareFor(p.platform) },
            hardwareRevision = p.hardwareRevision.ifEmpty { "MP1.0" },
            openGlesVersion = p.openGlesVersion.ifEmpty { "196610" },
            eglImpl = eglFor(p.platform),
            vulkanImpl = vulkanFor(p.platform),
            cpuAbiList = p.cpuAbiList.ifEmpty { "arm64-v8a,armeabi-v7a,armeabi" },
            cpuAbiList64 = p.cpuAbiList64.ifEmpty { "arm64-v8a" },
            cpuAbiList32 = "armeabi-v7a,armeabi",
            bionicArch = "arm64",
            bionicCpuVariant = bionicVariantFor(p.platform),
            vbmetaState = p.vbmetaState.ifEmpty { "green" },
            verifiedBootState = p.verifiedBootState.ifEmpty { "green" },
            flashLocked = p.flashLocked.ifEmpty { "1" },
            verityMode = p.verityMode.ifEmpty { "enforcing" },
            trebleEnabled = "true",
            vndkVersion = p.vndkVersion.ifEmpty { sdk },
            debuggable = "0",
            secure = "1",
            adbSecure = "1",
            kernelVersion = p.kernelVersion.ifEmpty { kernelFor(p) },
            simOperatorNumeric = "310260",
            simOperatorAlpha = "T-Mobile",
        )
    }

    private fun sdkFromRelease(release: String): String = when (release) {
        "16" -> "36"; "15" -> "35"; "14" -> "34"; "13" -> "33"
        "12.1", "12L" -> "32"; "12" -> "31"; "11" -> "30"; else -> "35"
    }

    private fun buildHostFor(brand: String): String = when (brand) {
        "google" -> "abfarm-release-rbe-00072"
        "samsung" -> "21DPEB22"
        "xiaomi", "redmi", "poco" -> "xiaomi.com"
        "oneplus" -> "oneplus-mobile-build-server"
        "oppo" -> "oppo-build-server"
        "vivo", "iqoo" -> "vivo-build-server"
        "honor" -> "honor-build-server"
        "nothing" -> "nothing-build-server"
        "asus" -> "asus-build-server"
        "sony" -> "sony-build-server"
        "realme" -> "realme-build-server"
        "motorola" -> "motorola-build-server"
        else -> "build-server"
    }

    private fun buildUserFor(brand: String): String = when (brand) {
        "google" -> "android-build"
        "samsung" -> "dpi"
        "xiaomi", "redmi", "poco" -> "builder"
        else -> "builder"
    }

    private fun bootloaderFor(p: SpoofProfile): String = when {
        p.brand.equals("samsung", true) -> p.incremental
        p.brand.equals("google", true) -> "${p.device}-${p.buildId.takeLast(4)}"
        else -> p.incremental
    }

    private fun basebandFor(p: SpoofProfile): String = when {
        p.brand.equals("google", true) ->
            "g5300i-${p.securityPatch.replace("-", "").take(6)}-${p.incremental.takeLast(8)}"
        p.brand.equals("samsung", true) -> p.incremental
        else -> p.incremental
    }

    private fun hardwareFor(platform: String): String = when {
        platform in listOf("sun", "pineapple", "kalama", "taro", "lahaina",
            "crow", "parrot") -> "qcom"
        platform.startsWith("mt") -> platform
        platform.startsWith("exynos") -> platform
        platform in listOf("zuma", "cloudripper", "slider") -> platform
        else -> platform
    }

    private fun eglFor(platform: String): String = when (platform) {
        "sun", "pineapple", "kalama", "taro", "lahaina",
        "crow", "parrot" -> "adreno"
        "zuma", "cloudripper", "slider",
        "mt6991", "mt6989", "mt6897", "mt6877" -> "mali"
        "exynos1480" -> "samsung"
        else -> "mali"
    }

    private fun vulkanFor(platform: String): String = when (platform) {
        "sun", "pineapple", "kalama", "taro", "lahaina",
        "crow", "parrot" -> "adreno"
        else -> "mali"
    }

    private fun bionicVariantFor(platform: String): String = when (platform) {
        "sun" -> "cortex-x925"
        "pineapple" -> "cortex-x4"
        "kalama" -> "cortex-x3"
        "taro" -> "cortex-x2"
        "lahaina" -> "cortex-x1"
        "zuma" -> "cortex-x3"
        "cloudripper" -> "cortex-x2"
        "slider" -> "cortex-x1"
        else -> "generic"
    }

    private fun kernelFor(p: SpoofProfile): String {
        val major = when (p.release) {
            "15" -> "6.1"
            "14" -> "5.15"
            "13" -> "5.10"
            "12" -> "5.10"
            else -> "6.1"
        }
        val androidTag = when (p.release) {
            "15" -> "android15"
            "14" -> "android14"
            "13" -> "android13"
            else -> "android14"
        }
        return "$major.99-$androidTag-11-g${p.incremental.takeLast(12).padEnd(12, '0')}"
    }

    private fun patchToUtc(patch: String): String {
        return try {
            val date = java.time.LocalDate.parse(patch)
            date.atStartOfDay(java.time.ZoneOffset.UTC)
                .toEpochSecond().toString()
        } catch (_: Exception) {
            "1717200000"
        }
    }
}
