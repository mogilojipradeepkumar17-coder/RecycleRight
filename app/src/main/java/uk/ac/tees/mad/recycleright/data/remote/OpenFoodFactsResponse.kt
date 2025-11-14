package uk.ac.tees.mad.recycleright.data.remote


import com.google.gson.annotations.SerializedName

data class OpenFoodFactsResponse(
    @SerializedName("code")
    val code: String?,
    @SerializedName("product")
    val product: Product?,
    @SerializedName("status")
    val status: Int?,
    @SerializedName("status_verbose")
    val statusVerbose: String?
)

data class Product(
    @SerializedName("product_name")
    val productName: String?,
    @SerializedName("brands")
    val brands: String?,
    @SerializedName("categories")
    val categories: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("ecoscore_data")
    val ecoscoreData: EcoscoreData?,
    @SerializedName("packagings")
    val packagings: List<PackagingItem>?
)

data class EcoscoreData(
    @SerializedName("adjustments")
    val adjustments: Adjustments?
)

data class Adjustments(
    @SerializedName("packaging")
    val packaging: PackagingAdjustment?
)

data class PackagingAdjustment(
    @SerializedName("non_recyclable_and_non_biodegradable_materials")
    val nonRecyclableAndNonBiodegradableMaterials: Int?,
    @SerializedName("packagings")
    val packagings: List<PackagingDetail>?,
    @SerializedName("score")
    val score: Int?
)

data class PackagingDetail(
    @SerializedName("material")
    val material: String?,
    @SerializedName("shape")
    val shape: String?,
    @SerializedName("environmental_score_material_score")
    val environmentalScoreMaterialScore: Int?
)

data class PackagingItem(
    @SerializedName("material")
    val material: String?,
    @SerializedName("shape")
    val shape: String?
)