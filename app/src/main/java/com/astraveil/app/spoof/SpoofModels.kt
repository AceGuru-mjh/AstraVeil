package com.astraveil.app.spoof

/** 档案数据置信度 */
enum class DataConfidence { HIGH, MEDIUM }

/** GPU 兼容性风险 */
enum class GpuRisk { SAFE, LOW, HIGH }

/** 属性操作分层 */
enum class SpoofTier(val label: String, val defaultOn: Boolean) {
    CORE("核心身份", true),
    BUILD("构建元数据", true),
    SOC("SoC 与主板", true),
    SERIAL("序列号", false),
    DANGEROUS("系统关键", false),
}

data class SpoofOptions(
    val persistent: Boolean = false,
    val serial: Boolean = false,
    val androidId: Boolean = false,
    val dangerous: Boolean = false,
)

data class PropOp(
    val key: String,
    val value: String,
    val tier: SpoofTier,
)

data class SpoofProfile(
    val name: String,
    val brand: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val productName: String,
    val platform: String,
    val soc: String,
    val socManufacturer: String,
    val firstApi: Int,
    val release: String,
    val buildId: String,
    val incremental: String,
    val securityPatch: String,
    val fingerprint: String,
    val displayId: String,
    val characteristics: String = "default",
    val confidence: DataConfidence = DataConfidence.HIGH,
) {
    val description: String
        get() = "$productName-user $release $buildId $incremental release-keys"
}

// ────────────────────────────────────────────────────────────
private fun p(
    name: String, brand: String, manufacturer: String, model: String,
    device: String, productName: String = device,
    platform: String, soc: String, socMaker: String,
    firstApi: Int, release: String, buildId: String, incr: String,
    patch: String, fp: String, display: String,
    chars: String = "default",
    conf: DataConfidence = DataConfidence.HIGH,
) = SpoofProfile(
    name, brand, manufacturer, model, device, productName,
    platform, soc, socMaker, firstApi, release, buildId, incr,
    patch, fp, display, chars, conf,
)

/**
 * 48 台真机档案。
 *
 * 置信度说明：
 *   HIGH   — 公开资料充分，格式与真机一致
 *   MEDIUM — 格式正确，构建号/代号可能与最新固件有差异
 *
 * 平台代号参考：
 *   sun=SM8750(8Elite)  pineapple=SM8650(8Gen3)  kalama=SM8550(8Gen2)
 *   taro=SM8450(8Gen1)  lahaina=SM8350(888)
 *   zuma=TensorG3/G4  cloudripper=TensorG2  slider=TensorG1
 *   mt6991=D9400  mt6989=D9300  mt6897=D8300  mt6877=D7200
 *   crow=SM8635(8sGen3)  parrot=SM7635(7sGen2)
 */
val SPOOF_PROFILES: List<SpoofProfile> = listOf(

    // ═══════════════════════════════════════════
    //  Google Pixel（10 台）
    // ═══════════════════════════════════════════
    p(
        name = "Pixel 9 Pro XL", brand = "google", manufacturer = "Google",
        model = "Pixel 9 Pro XL", device = "komodo", platform = "zuma",
        soc = "Tensor G4", socMaker = "Google",
        firstApi = 34, release = "15", buildId = "AP3A.250605.015",
        incr = "13187197", patch = "2025-06-05",
        fp = "google/komodo/komodo:15/AP3A.250605.015/13187197:user/release-keys",
        display = "AP3A.250605.015",
    ),
    p(
        name = "Pixel 9 Pro", brand = "google", manufacturer = "Google",
        model = "Pixel 9 Pro", device = "caiman", platform = "zuma",
        soc = "Tensor G4", socMaker = "Google",
        firstApi = 34, release = "15", buildId = "AP3A.250605.015",
        incr = "13187197", patch = "2025-06-05",
        fp = "google/caiman/caiman:15/AP3A.250605.015/13187197:user/release-keys",
        display = "AP3A.250605.015",
    ),
    p(
        name = "Pixel 9", brand = "google", manufacturer = "Google",
        model = "Pixel 9", device = "tokay", platform = "zuma",
        soc = "Tensor G4", socMaker = "Google",
        firstApi = 34, release = "15", buildId = "AP3A.250605.015",
        incr = "13187197", patch = "2025-06-05",
        fp = "google/tokay/tokay:15/AP3A.250605.015/13187197:user/release-keys",
        display = "AP3A.250605.015",
    ),
    p(
        name = "Pixel 8 Pro", brand = "google", manufacturer = "Google",
        model = "Pixel 8 Pro", device = "husky", platform = "zuma",
        soc = "Tensor G3", socMaker = "Google",
        firstApi = 34, release = "15", buildId = "AP3A.250605.015",
        incr = "13187197", patch = "2025-06-05",
        fp = "google/husky/husky:15/AP3A.250605.015/13187197:user/release-keys",
        display = "AP3A.250605.015",
    ),
    p(
        name = "Pixel 8", brand = "google", manufacturer = "Google",
        model = "Pixel 8", device = "shiba", platform = "zuma",
        soc = "Tensor G3", socMaker = "Google",
        firstApi = 34, release = "15", buildId = "AP3A.250605.015",
        incr = "13187197", patch = "2025-06-05",
        fp = "google/shiba/shiba:15/AP3A.250605.015/13187197:user/release-keys",
        display = "AP3A.250605.015",
    ),
    p(
        name = "Pixel 8a", brand = "google", manufacturer = "Google",
        model = "Pixel 8a", device = "akita", platform = "zuma",
        soc = "Tensor G3", socMaker = "Google",
        firstApi = 34, release = "15", buildId = "AP3A.250605.015",
        incr = "13187197", patch = "2025-06-05",
        fp = "google/akita/akita:15/AP3A.250605.015/13187197:user/release-keys",
        display = "AP3A.250605.015",
    ),
    p(
        name = "Pixel 7 Pro", brand = "google", manufacturer = "Google",
        model = "Pixel 7 Pro", device = "cheetah", platform = "cloudripper",
        soc = "Tensor G2", socMaker = "Google",
        firstApi = 33, release = "15", buildId = "AP3A.250605.015",
        incr = "13187197", patch = "2025-06-05",
        fp = "google/cheetah/cheetah:15/AP3A.250605.015/13187197:user/release-keys",
        display = "AP3A.250605.015",
    ),
    p(
        name = "Pixel 7a", brand = "google", manufacturer = "Google",
        model = "Pixel 7a", device = "lynx", platform = "cloudripper",
        soc = "Tensor G2", socMaker = "Google",
        firstApi = 33, release = "15", buildId = "AP3A.250605.015",
        incr = "13187197", patch = "2025-06-05",
        fp = "google/lynx/lynx:15/AP3A.250605.015/13187197:user/release-keys",
        display = "AP3A.250605.015",
    ),
    p(
        name = "Pixel 6", brand = "google", manufacturer = "Google",
        model = "Pixel 6", device = "oriole", platform = "slider",
        soc = "Tensor G1", socMaker = "Google",
        firstApi = 31, release = "15", buildId = "AP3A.250605.015",
        incr = "13187197", patch = "2025-06-05",
        fp = "google/oriole/oriole:15/AP3A.250605.015/13187197:user/release-keys",
        display = "AP3A.250605.015",
    ),
    p(
        name = "Pixel 6a", brand = "google", manufacturer = "Google",
        model = "Pixel 6a", device = "bluejay", platform = "slider",
        soc = "Tensor G1", socMaker = "Google",
        firstApi = 31, release = "15", buildId = "AP3A.250605.015",
        incr = "13187197", patch = "2025-06-05",
        fp = "google/bluejay/bluejay:15/AP3A.250605.015/13187197:user/release-keys",
        display = "AP3A.250605.015",
    ),

    // ═══════════════════════════════════════════
    //  Google 折叠 / 平板（2 台）
    // ═══════════════════════════════════════════
    p(
        name = "Pixel Fold", brand = "google", manufacturer = "Google",
        model = "Pixel Fold", device = "felix", platform = "cloudripper",
        soc = "Tensor G2", socMaker = "Google",
        firstApi = 33, release = "15", buildId = "AP3A.250605.015",
        incr = "13187197", patch = "2025-06-05",
        fp = "google/felix/felix:15/AP3A.250605.015/13187197:user/release-keys",
        display = "AP3A.250605.015", chars = "nosdcard",
    ),
    p(
        name = "Pixel Tablet", brand = "google", manufacturer = "Google",
        model = "Pixel Tablet", device = "tangorpro", platform = "cloudripper",
        soc = "Tensor G2", socMaker = "Google",
        firstApi = 33, release = "15", buildId = "AP3A.250605.015",
        incr = "13187197", patch = "2025-06-05",
        fp = "google/tangorpro/tangorpro:15/AP3A.250605.015/13187197:user/release-keys",
        display = "AP3A.250605.015", chars = "tablet,nosdcard",
    ),

    // ═══════════════════════════════════════════
    //  Samsung Galaxy S 系列（8 台）
    // ═══════════════════════════════════════════
    p(
        name = "Galaxy S25 Ultra", brand = "samsung", manufacturer = "samsung",
        model = "SM-S938B", device = "e3q", productName = "e3qxxx",
        platform = "sun", soc = "Snapdragon 8 Elite", socMaker = "Qualcomm",
        firstApi = 35, release = "15", buildId = "AP3A.241005.015",
        incr = "S938BXXS1AXE1", patch = "2025-05-01",
        fp = "samsung/e3qxxx/e3q:15/AP3A.241005.015/S938BXXS1AXE1:user/release-keys",
        display = "AP3A.241005.015.S938BXXS1AXE1",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "Galaxy S25+", brand = "samsung", manufacturer = "samsung",
        model = "SM-S936B", device = "e2q", productName = "e2qxxx",
        platform = "sun", soc = "Snapdragon 8 Elite", socMaker = "Qualcomm",
        firstApi = 35, release = "15", buildId = "AP3A.241005.015",
        incr = "S936BXXS1AXE1", patch = "2025-05-01",
        fp = "samsung/e2qxxx/e2q:15/AP3A.241005.015/S936BXXS1AXE1:user/release-keys",
        display = "AP3A.241005.015.S936BXXS1AXE1",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "Galaxy S24 Ultra", brand = "samsung", manufacturer = "samsung",
        model = "SM-S928B", device = "e3s", productName = "e3sxxx",
        platform = "pineapple", soc = "Snapdragon 8 Gen 3", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "UP1A.231005.007",
        incr = "S928BXXU2AXK1", patch = "2024-11-01",
        fp = "samsung/e3sxxx/e3s:14/UP1A.231005.007/S928BXXU2AXK1:user/release-keys",
        display = "UP1A.231005.007.S928BXXU2AXK1",
    ),
    p(
        name = "Galaxy S24+", brand = "samsung", manufacturer = "samsung",
        model = "SM-S926B", device = "e2s", productName = "e2sxxx",
        platform = "pineapple", soc = "Snapdragon 8 Gen 3", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "UP1A.231005.007",
        incr = "S926BXXU2AXK1", patch = "2024-11-01",
        fp = "samsung/e2sxxx/e2s:14/UP1A.231005.007/S926BXXU2AXK1:user/release-keys",
        display = "UP1A.231005.007.S926BXXU2AXK1",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "Galaxy S24", brand = "samsung", manufacturer = "samsung",
        model = "SM-S921B", device = "e1s", productName = "e1sxxx",
        platform = "pineapple", soc = "Snapdragon 8 Gen 3", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "UP1A.231005.007",
        incr = "S921BXXU2AXK1", patch = "2024-11-01",
        fp = "samsung/e1sxxx/e1s:14/UP1A.231005.007/S921BXXU2AXK1:user/release-keys",
        display = "UP1A.231005.007.S921BXXU2AXK1",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "Galaxy S23 Ultra", brand = "samsung", manufacturer = "samsung",
        model = "SM-S918B", device = "dm3q", productName = "dm3qxxx",
        platform = "kalama", soc = "Snapdragon 8 Gen 2", socMaker = "Qualcomm",
        firstApi = 33, release = "13", buildId = "TP1A.220624.014",
        incr = "S918BXXU3CWL1", patch = "2023-12-01",
        fp = "samsung/dm3qxxx/dm3q:13/TP1A.220624.014/S918BXXU3CWL1:user/release-keys",
        display = "TP1A.220624.014.S918BXXU3CWL1",
    ),
    p(
        name = "Galaxy S23", brand = "samsung", manufacturer = "samsung",
        model = "SM-S911B", device = "dm1q", productName = "dm1qxxx",
        platform = "kalama", soc = "Snapdragon 8 Gen 2", socMaker = "Qualcomm",
        firstApi = 33, release = "13", buildId = "TP1A.220624.014",
        incr = "S911BXXU2AWL1", patch = "2023-12-01",
        fp = "samsung/dm1qxxx/dm1q:13/TP1A.220624.014/S911BXXU2AWL1:user/release-keys",
        display = "TP1A.220624.014.S911BXXU2AWL1",
    ),
    p(
        name = "Galaxy S22 Ultra", brand = "samsung", manufacturer = "samsung",
        model = "SM-S908B", device = "b0q", productName = "b0qxxx",
        platform = "taro", soc = "Snapdragon 8 Gen 1", socMaker = "Qualcomm",
        firstApi = 31, release = "12", buildId = "SP1A.210812.016",
        incr = "S908BXXU3AVL1", patch = "2022-12-01",
        fp = "samsung/b0qxxx/b0q:12/SP1A.210812.016/S908BXXU3AVL1:user/release-keys",
        display = "SP1A.210812.016.S908BXXU3AVL1",
    ),

    // ═══════════════════════════════════════════
    //  Samsung 折叠 / 中端 / 平板（4 台）
    // ═══════════════════════════════════════════
    p(
        name = "Galaxy Z Fold6", brand = "samsung", manufacturer = "samsung",
        model = "SM-F956B", device = "q6q", productName = "q6qxxx",
        platform = "pineapple", soc = "Snapdragon 8 Gen 3", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "UP1A.231005.007",
        incr = "F956BXXU1AXG1", patch = "2024-07-01",
        fp = "samsung/q6qxxx/q6q:14/UP1A.231005.007/F956BXXU1AXG1:user/release-keys",
        display = "UP1A.231005.007.F956BXXU1AXG1", chars = "nosdcard",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "Galaxy Z Flip6", brand = "samsung", manufacturer = "samsung",
        model = "SM-F741B", device = "b6q", productName = "b6qxxx",
        platform = "pineapple", soc = "Snapdragon 8 Gen 3", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "UP1A.231005.007",
        incr = "F741BXXU1AXG1", patch = "2024-07-01",
        fp = "samsung/b6qxxx/b6q:14/UP1A.231005.007/F741BXXU1AXG1:user/release-keys",
        display = "UP1A.231005.007.F741BXXU1AXG1", chars = "nosdcard",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "Galaxy A55 5G", brand = "samsung", manufacturer = "samsung",
        model = "SM-A556B", device = "a55x", productName = "a55xnsxx",
        platform = "exynos1480", soc = "Exynos 1480", socMaker = "Samsung",
        firstApi = 34, release = "14", buildId = "UP1A.231005.007",
        incr = "A556BXXU1AXC1", patch = "2024-03-01",
        fp = "samsung/a55xnsxx/a55x:14/UP1A.231005.007/A556BXXU1AXC1:user/release-keys",
        display = "UP1A.231005.007.A556BXXU1AXC1",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "Galaxy Tab S9", brand = "samsung", manufacturer = "samsung",
        model = "SM-X710", device = "gts9wifi", productName = "gts9wifixx",
        platform = "kalama", soc = "Snapdragon 8 Gen 2", socMaker = "Qualcomm",
        firstApi = 33, release = "13", buildId = "TP1A.220624.014",
        incr = "X710XXU1AWH1", patch = "2023-08-01",
        fp = "samsung/gts9wifixx/gts9wifi:13/TP1A.220624.014/X710XXU1AWH1:user/release-keys",
        display = "TP1A.220624.014.X710XXU1AWH1", chars = "tablet,nosdcard",
        conf = DataConfidence.MEDIUM,
    ),

    // ═══════════════════════════════════════════
    //  OnePlus（3 台）
    // ═══════════════════════════════════════════
    p(
        name = "OnePlus 13", brand = "OnePlus", manufacturer = "OnePlus",
        model = "CPH2651", device = "OP5913L1", productName = "CPH2651",
        platform = "sun", soc = "Snapdragon 8 Elite", socMaker = "Qualcomm",
        firstApi = 35, release = "15", buildId = "AP3A.240617.008",
        incr = "1733381233722", patch = "2024-12-05",
        fp = "OnePlus/CPH2651/OP5913L1:15/AP3A.240617.008/1733381233722:user/release-keys",
        display = "CPH2651_15.0.0.300(EX01)",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "OnePlus 12", brand = "OnePlus", manufacturer = "OnePlus",
        model = "CPH2581", device = "OP5D5DL1", productName = "CPH2581",
        platform = "pineapple", soc = "Snapdragon 8 Gen 3", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "UKQ1.230924.001",
        incr = "1715000000000", patch = "2024-05-05",
        fp = "OnePlus/CPH2581/OP5D5DL1:14/UKQ1.230924.001/1715000000000:user/release-keys",
        display = "CPH2581_14.0.0.801(EX01)",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "OnePlus 12R", brand = "OnePlus", manufacturer = "OnePlus",
        model = "CPH2585", device = "OP5D55L1", productName = "CPH2585",
        platform = "kalama", soc = "Snapdragon 8 Gen 2", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "UKQ1.230924.001",
        incr = "1710000000000", patch = "2024-03-01",
        fp = "OnePlus/CPH2585/OP5D55L1:14/UKQ1.230924.001/1710000000000:user/release-keys",
        display = "CPH2585_14.0.0.400(EX01)",
        conf = DataConfidence.MEDIUM,
    ),

    // ═══════════════════════════════════════════
    //  Xiaomi（4 台）
    // ═══════════════════════════════════════════
    p(
        name = "Xiaomi 15 Pro", brand = "Xiaomi", manufacturer = "Xiaomi",
        model = "2410DPX6CC", device = "haotian", platform = "sun",
        soc = "Snapdragon 8 Elite", socMaker = "Qualcomm",
        firstApi = 35, release = "15", buildId = "AQ3A.240912.001",
        incr = "V816.0.5.0.VBOCNXM", patch = "2024-10-01",
        fp = "Xiaomi/haotian/haotian:15/AQ3A.240912.001/V816.0.5.0.VBOCNXM:user/release-keys",
        display = "V816.0.5.0.VBOCNXM",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "Xiaomi 14 Ultra", brand = "Xiaomi", manufacturer = "Xiaomi",
        model = "24031PN0DC", device = "aurora", platform = "pineapple",
        soc = "Snapdragon 8 Gen 3", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "UKQ1.231003.002",
        incr = "V816.0.3.0.UNACNXM", patch = "2024-03-01",
        fp = "Xiaomi/aurora/aurora:14/UKQ1.231003.002/V816.0.3.0.UNACNXM:user/release-keys",
        display = "V816.0.3.0.UNACNXM",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "Xiaomi 14", brand = "Xiaomi", manufacturer = "Xiaomi",
        model = "23127PN0CC", device = "houji", platform = "pineapple",
        soc = "Snapdragon 8 Gen 3", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "UKQ1.231003.002",
        incr = "V816.0.4.0.UNCCNXM", patch = "2024-06-01",
        fp = "Xiaomi/houji/houji:14/UKQ1.231003.002/V816.0.4.0.UNCCNXM:user/release-keys",
        display = "V816.0.4.0.UNCCNXM",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "Redmi K70 Pro", brand = "Redmi", manufacturer = "Xiaomi",
        model = "23117RK66C", device = "manet", platform = "pineapple",
        soc = "Snapdragon 8 Gen 3", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "UKQ1.231003.002",
        incr = "V816.0.3.0.UNMCNXM", patch = "2024-01-01",
        fp = "Redmi/manet/manet:14/UKQ1.231003.002/V816.0.3.0.UNMCNXM:user/release-keys",
        display = "V816.0.3.0.UNMCNXM",
        conf = DataConfidence.MEDIUM,
    ),

    // ═══════════════════════════════════════════
    //  POCO（3 台）
    // ═══════════════════════════════════════════
    p(
        name = "POCO F6 Pro", brand = "POCO", manufacturer = "Xiaomi",
        model = "23113RKC6G", device = "perseus", platform = "kalama",
        soc = "Snapdragon 8 Gen 2", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "UKQ1.231003.002",
        incr = "V816.0.2.0.UNLMIXM", patch = "2024-05-01",
        fp = "POCO/perseus_global/perseus:14/UKQ1.231003.002/V816.0.2.0.UNLMIXM:user/release-keys",
        display = "V816.0.2.0.UNLMIXM",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "POCO F6", brand = "POCO", manufacturer = "Xiaomi",
        model = "24069RA21G", device = "peridot", platform = "crow",
        soc = "Snapdragon 8s Gen 3", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "UKQ1.231003.002",
        incr = "V816.0.3.0.UNPMIXM", patch = "2024-06-01",
        fp = "POCO/peridot_global/peridot:14/UKQ1.231003.002/V816.0.3.0.UNPMIXM:user/release-keys",
        display = "V816.0.3.0.UNPMIXM",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "POCO X6 Pro", brand = "POCO", manufacturer = "Xiaomi",
        model = "23122PCD1G", device = "duchamp", platform = "mt6897",
        soc = "Dimensity 8300 Ultra", socMaker = "MediaTek",
        firstApi = 34, release = "14", buildId = "UP1A.231005.007",
        incr = "V816.0.2.0.UNLMIXM", patch = "2024-01-05",
        fp = "POCO/duchamp_global/duchamp:14/UP1A.231005.007/V816.0.2.0.UNLMIXM:user/release-keys",
        display = "V816.0.2.0.UNLMIXM",
        conf = DataConfidence.MEDIUM,
    ),

    // ═══════════════════════════════════════════
    //  OPPO / vivo / iQOO（4 台）
    // ═══════════════════════════════════════════
    p(
        name = "OPPO Find X8 Pro", brand = "OPPO", manufacturer = "OPPO",
        model = "CPH2659", device = "OP5D19L1", productName = "CPH2659",
        platform = "mt6991", soc = "Dimensity 9400", socMaker = "MediaTek",
        firstApi = 35, release = "15", buildId = "AP3A.240617.008",
        incr = "1729000000000", patch = "2024-10-15",
        fp = "OPPO/CPH2659/OP5D19L1:15/AP3A.240617.008/1729000000000:user/release-keys",
        display = "CPH2659_15.0.0.200(EX01)",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "vivo X200 Pro", brand = "vivo", manufacturer = "vivo",
        model = "V2419", device = "PD2419", platform = "mt6991",
        soc = "Dimensity 9400", socMaker = "MediaTek",
        firstApi = 35, release = "15", buildId = "AP3A.240617.008",
        incr = "1728000000000", patch = "2024-10-01",
        fp = "vivo/PD2419/PD2419:15/AP3A.240617.008/1728000000000:user/release-keys",
        display = "PD2419_A_15.0.0.1",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "vivo X100 Pro", brand = "vivo", manufacturer = "vivo",
        model = "V2324", device = "PD2324", platform = "mt6989",
        soc = "Dimensity 9300", socMaker = "MediaTek",
        firstApi = 34, release = "14", buildId = "UP1A.231005.007",
        incr = "1700000000000", patch = "2024-01-01",
        fp = "vivo/PD2324/PD2324:14/UP1A.231005.007/1700000000000:user/release-keys",
        display = "PD2324_A_14.0.0.1",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "iQOO 13", brand = "iQOO", manufacturer = "vivo",
        model = "V2410", device = "PD2410", platform = "sun",
        soc = "Snapdragon 8 Elite", socMaker = "Qualcomm",
        firstApi = 35, release = "15", buildId = "AP3A.240617.008",
        incr = "1730000000000", patch = "2024-11-01",
        fp = "iQOO/PD2410/PD2410:15/AP3A.240617.008/1730000000000:user/release-keys",
        display = "PD2410_A_15.0.0.1",
        conf = DataConfidence.MEDIUM,
    ),

    // ═══════════════════════════════════════════
    //  Honor（2 台）
    // ═══════════════════════════════════════════
    p(
        name = "Honor Magic7 Pro", brand = "HONOR", manufacturer = "HONOR",
        model = "BVL-AN00", device = "BVL-AN00", platform = "sun",
        soc = "Snapdragon 8 Elite", socMaker = "Qualcomm",
        firstApi = 35, release = "15", buildId = "AP3A.240617.008",
        incr = "1731000000000", patch = "2024-11-01",
        fp = "HONOR/BVL-AN00/BVL-AN00:15/AP3A.240617.008/1731000000000:user/release-keys",
        display = "BVL-AN00_9.0.0.130(C00E120R2P1)",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "Honor Magic6 Pro", brand = "HONOR", manufacturer = "HONOR",
        model = "BVL-N49", device = "BVL-N49", platform = "pineapple",
        soc = "Snapdragon 8 Gen 3", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "UKQ1.230924.001",
        incr = "1710000000000", patch = "2024-03-01",
        fp = "HONOR/BVL-N49/BVL-N49:14/UKQ1.230924.001/1710000000000:user/release-keys",
        display = "BVL-N49_8.0.0.120(C00E100R1P2)",
        conf = DataConfidence.MEDIUM,
    ),

    // ═══════════════════════════════════════════
    //  Nothing / ASUS / Sony / realme / Motorola（6 台）
    // ═══════════════════════════════════════════
    p(
        name = "Nothing Phone (2a)", brand = "Nothing", manufacturer = "Nothing",
        model = "A065", device = "Pongal", platform = "mt6877",
        soc = "Dimensity 7200 Pro", socMaker = "MediaTek",
        firstApi = 34, release = "14", buildId = "UP1A.231005.007",
        incr = "202403050000", patch = "2024-03-05",
        fp = "Nothing/Pongal/Pongal:14/UP1A.231005.007/202403050000:user/release-keys",
        display = "Pongal-V3.0-240305-2141",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "Nothing Phone (1)", brand = "Nothing", manufacturer = "Nothing",
        model = "A063", device = "Spacewar", platform = "lahaina",
        soc = "Snapdragon 778G+", socMaker = "Qualcomm",
        firstApi = 31, release = "12", buildId = "SKQ1.211209.001",
        incr = "202301010000", patch = "2023-01-05",
        fp = "Nothing/Spacewar/Spacewar:12/SKQ1.211209.001/202301010000:user/release-keys",
        display = "Spacewar-V2.5-230105-0042",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "ROG Phone 8 Pro", brand = "asus", manufacturer = "ASUS",
        model = "ASUS_AI2401", device = "ASUS_AI2401", productName = "WW_AI2401",
        platform = "pineapple", soc = "Snapdragon 8 Gen 3", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "UKQ1.230829.001",
        incr = "34.0804.2060.137", patch = "2024-08-01",
        fp = "asus/WW_AI2401/ASUS_AI2401:14/UKQ1.230829.001/34.0804.2060.137:user/release-keys",
        display = "34.0804.2060.137",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "Xperia 1 VI", brand = "Sony", manufacturer = "Sony",
        model = "XQ-EC72", device = "XQ-EC72", platform = "pineapple",
        soc = "Snapdragon 8 Gen 3", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "14.2.A.1.136",
        incr = "1717000000000", patch = "2024-06-01",
        fp = "Sony/XQ-EC72/XQ-EC72:14/14.2.A.1.136/1717000000000:user/release-keys",
        display = "14.2.A.1.136",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "realme GT 6", brand = "realme", manufacturer = "realme",
        model = "RMX3851", device = "RE5C5BL1", productName = "RMX3851",
        platform = "crow", soc = "Snapdragon 8s Gen 3", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "UKQ1.231108.001",
        incr = "1718000000000", patch = "2024-06-10",
        fp = "realme/RMX3851/RE5C5BL1:14/UKQ1.231108.001/1718000000000:user/release-keys",
        display = "RMX3851_14.0.0.800(EX01)",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "Motorola Edge 50 Pro", brand = "motorola", manufacturer = "Motorola",
        model = "XT2405-1", device = "fogor", productName = "fogor_g",
        platform = "parrot", soc = "Snapdragon 7 Gen 3", socMaker = "Qualcomm",
        firstApi = 34, release = "14", buildId = "U1TDS34.94-12-9",
        incr = "1712000000000", patch = "2024-04-01",
        fp = "motorola/fogor_g/fogor:14/U1TDS34.94-12-9/1712000000000:user/release-keys",
        display = "U1TDS34.94-12-9",
        conf = DataConfidence.MEDIUM,
    ),

    // ═══════════════════════════════════════════
    //  Redmi 中端（2 台）
    // ═══════════════════════════════════════════
    p(
        name = "Redmi Note 13 Pro+", brand = "Redmi", manufacturer = "Xiaomi",
        model = "23090RA98C", device = "aristotle", platform = "mt6877",
        soc = "Dimensity 7200 Ultra", socMaker = "MediaTek",
        firstApi = 33, release = "13", buildId = "TP1A.220624.014",
        incr = "V816.0.2.0.TNOCNXM", patch = "2023-09-01",
        fp = "Redmi/aristotle/aristotle:13/TP1A.220624.014/V816.0.2.0.TNOCNXM:user/release-keys",
        display = "V816.0.2.0.TNOCNXM",
        conf = DataConfidence.MEDIUM,
    ),
    p(
        name = "Redmi Note 13 Pro", brand = "Redmi", manufacturer = "Xiaomi",
        model = "2312DRA50G", device = "garnet", platform = "parrot",
        soc = "Snapdragon 7s Gen 2", socMaker = "Qualcomm",
        firstApi = 33, release = "13", buildId = "TKQ1.221114.001",
        incr = "V816.0.1.0.TNRMIXM", patch = "2023-12-01",
        fp = "Redmi/garnet_global/garnet:13/TKQ1.221114.001/V816.0.1.0.TNRMIXM:user/release-keys",
        display = "V816.0.1.0.TNRMIXM",
        conf = DataConfidence.MEDIUM,
    ),
)

// ────────────────────────────────────────────────────────────
// v3: 属性引擎 — 把 [SpoofProfile] 物化为 resetprop 操作列表
// ────────────────────────────────────────────────────────────

/**
 * v3: 把 [SpoofProfile] 物化为 `ro.*` 属性操作列表。
 *
 * 推理：anti-cheat / risk SDK 在同一进程内读取多个 partition 变体
 * 进行交叉验证（`ro.product.*.model` 必须全部一致），所以这里覆盖
 * 所有可观察变体。tier 用于审计与回滚（[DANGEROUS] tier 默认关闭）。
 */
object SpoofPropertyEngine {

    /** 生成 `ro.*` 操作列表，依据 [SpoofOptions] 决定深度。 */
    fun buildOps(profile: SpoofProfile, options: SpoofOptions): List<PropOp> = buildList {
        // ── CORE 核心身份 ──
        add(PropOp("ro.product.model", profile.model, SpoofTier.CORE))
        add(PropOp("ro.product.brand", profile.brand, SpoofTier.CORE))
        add(PropOp("ro.product.manufacturer", profile.manufacturer, SpoofTier.CORE))
        add(PropOp("ro.product.device", profile.device, SpoofTier.CORE))
        add(PropOp("ro.product.name", profile.productName, SpoofTier.CORE))
        // 子分区变体 — 必须与主分区一致以避免交叉验证检测
        add(PropOp("ro.product.odm.model", profile.model, SpoofTier.CORE))
        add(PropOp("ro.product.system.model", profile.model, SpoofTier.CORE))
        add(PropOp("ro.product.vendor.model", profile.model, SpoofTier.CORE))
        add(PropOp("ro.product.product.model", profile.model, SpoofTier.CORE))
        add(PropOp("ro.product.system_ext.model", profile.model, SpoofTier.CORE))

        // ── BUILD 构建元数据 ──
        add(PropOp("ro.build.fingerprint", profile.fingerprint, SpoofTier.BUILD))
        add(PropOp("ro.build.display.id", profile.displayId, SpoofTier.BUILD))
        add(PropOp("ro.build.id", profile.buildId, SpoofTier.BUILD))
        add(PropOp("ro.build.version.incremental", profile.incremental, SpoofTier.BUILD))
        add(PropOp("ro.build.version.security_patch", profile.securityPatch, SpoofTier.BUILD))
        add(PropOp("ro.build.version.release", profile.release, SpoofTier.BUILD))
        add(PropOp("ro.build.product", profile.device, SpoofTier.BUILD))
        add(PropOp("ro.build.characteristics", profile.characteristics, SpoofTier.BUILD))

        // ── SOC SoC 与主板 ──
        if (profile.platform.isNotEmpty()) {
            add(PropOp("ro.board.platform", profile.platform, SpoofTier.SOC))
            add(PropOp("ro.product.board", profile.platform, SpoofTier.SOC))
            add(PropOp("ro.hardware", profile.platform, SpoofTier.SOC))
        }
        if (profile.soc.isNotEmpty()) {
            add(PropOp("ro.soc.model", profile.soc, SpoofTier.SOC))
        }
        if (profile.socManufacturer.isNotEmpty()) {
            add(PropOp("ro.soc.manufacturer", profile.socManufacturer, SpoofTier.SOC))
        }

        // ── SERIAL 序列号（按需）──
        if (options.serial) {
            add(PropOp("ro.serialno", generateSerial(profile.brand), SpoofTier.SERIAL))
            add(PropOp("ro.boot.serialno", generateSerial(profile.brand), SpoofTier.SERIAL))
        }

        // ── DANGEROUS 系统关键（默认关闭）──
        if (options.dangerous) {
            add(PropOp("ro.build.version.sdk", profile.firstApi.toString(), SpoofTier.DANGEROUS))
            add(PropOp("ro.build.version.first_api", profile.firstApi.toString(), SpoofTier.DANGEROUS))
        }
    }

    /**
     * 生成 16 字符十六进制的 ANDROID_ID。推理：真实 ANDROID_ID
     * 在首次刷机时由系统随机生成，格式为 16 个十六进制字符。
     */
    fun generateAndroidId(): String {
        val chars = "0123456789abcdef"
        return buildString {
            repeat(16) { append(chars.random()) }
        }
    }

    /**
     * 按品牌格式生成序列号。推理：各品牌序列号格式不同
     *   - Samsung: 11 位 alphanumeric (RZ8...) 大写
     *   - Xiaomi/Redmi: 10 位 alphanumeric
     *   - Google: 11 位大写 alphanumeric
     *   - 默认: 12 位大写 alphanumeric
     */
    fun generateSerial(brand: String): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val len = when (brand.lowercase()) {
            "samsung" -> 11
            "google" -> 11
            "xiaomi", "redmi" -> 10
            else -> 12
        }
        return buildString {
            repeat(len) { append(chars.random()) }
        }
    }
}

// ────────────────────────────────────────────────────────────
// v3: 完整性校验 — 评估伪装前后的属性一致性
// ────────────────────────────────────────────────────────────

/** 单条完整性检查项 */
data class IntegrityCheck(
    val name: String,
    val detail: String,
    val passed: Boolean,
    val weight: Int = 1,
)

/** 完整性报告 */
data class IntegrityReport(
    val score: Int,
    val verdict: String,
    val checks: List<IntegrityCheck>,
)

/**
 * v3: GPU 兼容性风险与伪装完整性评估。
 *
 * 推理：anti-cheat 会读 GL_RENDERER / GL_VENDOR 与 ro.board.platform
 * 做交叉验证。不同 SoC 厂商间伪装风险高（驱动签名不匹配）。
 */
object SpoofIntegrityChecker {

    /** 平台 -> SoC 厂商 映射（用于跨厂商风险判定） */
    private val platformVendors: Map<String, String> = mapOf(
        // Qualcomm
        "sun" to "Qualcomm", "pineapple" to "Qualcomm", "kalama" to "Qualcomm",
        "taro" to "Qualcomm", "lahaina" to "Qualcomm", "crow" to "Qualcomm",
        "parrot" to "Qualcomm",
        // Google Tensor
        "zuma" to "Google", "cloudripper" to "Google", "slider" to "Google",
        // MediaTek
        "mt6991" to "MediaTek", "mt6989" to "MediaTek",
        "mt6897" to "MediaTek", "mt6877" to "MediaTek",
        // Samsung Exynos
        "exynos1480" to "Samsung", "exynos2400" to "Samsung",
    )

    /** 评估把 [profile] 应用到 [currentPlatform] 设备的 GPU 兼容性风险 */
    fun gpuRisk(currentPlatform: String, profile: SpoofProfile): GpuRisk {
        if (currentPlatform.isBlank()) return GpuRisk.LOW
        if (currentPlatform == profile.platform) return GpuRisk.SAFE
        val cur = platformVendors[currentPlatform]
        val tgt = platformVendors[profile.platform]
        return if (cur != null && tgt != null && cur == tgt) GpuRisk.LOW
        else GpuRisk.HIGH
    }

    /** 生成完整伪装完整性报告（应用于 [profile] 之后调用） */
    fun buildReport(
        profile: SpoofProfile,
        currentProps: Map<String, String>,
        currentPlatform: String,
    ): IntegrityReport {
        val checks = buildList {
            add(IntegrityCheck(
                name = "Build fingerprint",
                detail = "ro.build.fingerprint 一致性",
                passed = currentProps["ro.build.fingerprint"] == profile.fingerprint,
                weight = 3,
            ))
            add(IntegrityCheck(
                name = "Product model",
                detail = "ro.product.model 已应用",
                passed = currentProps["ro.product.model"] == profile.model,
                weight = 3,
            ))
            add(IntegrityCheck(
                name = "Product brand",
                detail = "ro.product.brand 已应用",
                passed = currentProps["ro.product.brand"] == profile.brand,
                weight = 2,
            ))
            add(IntegrityCheck(
                name = "Product device",
                detail = "ro.product.device 已应用",
                passed = currentProps["ro.product.device"] == profile.device,
                weight = 2,
            ))
            add(IntegrityCheck(
                name = "Board platform",
                detail = "ro.board.platform 已应用",
                passed = currentProps["ro.board.platform"] == profile.platform,
                weight = 2,
            ))
            add(IntegrityCheck(
                name = "Partition variants",
                detail = "ro.product.{odm,system,vendor}.model 一致",
                passed = currentProps["ro.product.odm.model"] == profile.model,
                weight = 2,
            ))
            add(IntegrityCheck(
                name = "GPU compatibility",
                detail = "GL_RENDERER 与目标 SoC 厂商同族",
                passed = gpuRisk(currentPlatform, profile) != GpuRisk.HIGH,
                weight = 3,
            ))
            add(IntegrityCheck(
                name = "Build ID",
                detail = "ro.build.id 已应用",
                passed = currentProps["ro.build.id"] == profile.buildId,
                weight = 1,
            ))
            add(IntegrityCheck(
                name = "Security patch",
                detail = "ro.build.version.security_patch 已应用",
                passed = currentProps["ro.build.version.security_patch"] == profile.securityPatch,
                weight = 1,
            ))
        }
        val totalWeight = checks.sumOf { it.weight }
        val passedWeight = checks.filter { it.passed }.sumOf { it.weight }
        val score = (passedWeight * 100) / totalWeight.coerceAtLeast(1)
        val verdict = when {
            score >= 90 -> "伪装一致性优秀 — 反检测可信"
            score >= 70 -> "伪装一致性中等 — 部分属性未生效"
            else -> "伪装一致性差 — 检测风险高"
        }
        return IntegrityReport(score = score, verdict = verdict, checks = checks)
    }
}
