package uk.ac.tees.mad.recycleright.data.mapper

import uk.ac.tees.mad.recycleright.data.model.RecyclableItem
import uk.ac.tees.mad.recycleright.data.model.RecycleCategory
import uk.ac.tees.mad.recycleright.data.remote.OpenFoodFactsResponse
import java.util.UUID

object RecyclabilityMapper {

    /**
     * Converts OpenFoodFacts API response to RecyclableItem entity
     * Uses barcode-based deterministic ID to prevent duplicates
     */
    fun mapToRecyclableItem(
        response: OpenFoodFactsResponse,
        barcode: String
    ): RecyclableItem? {
        val product = response.product ?: return null
        val productName = product.productName ?: "Unknown Product"

        // Determine recyclability based on OpenFoodFacts data
        val category = determineRecycleCategory(response)
        val tips = generateRecyclingTips(response, category)
        val description = generateDescription(response, category)

        return RecyclableItem(
            // Use barcode as ID to prevent duplicates when scanning same item
            id = barcode,
            name = productName,
            category = category,
            description = description,
            tips = tips,
            barcode = barcode,
            imageUrl = product.imageUrl,
            isFavorite = false
        )
    }

    /**
     * Determines recycle category based on UK recycling guidelines
     * Priority: Compostable > Recyclable > General Waste
     */
    private fun determineRecycleCategory(response: OpenFoodFactsResponse): RecycleCategory {
        val product = response.product ?: return RecycleCategory.GENERAL_WASTE

        // Get all available materials from both sources
        val materials = getAllMaterials(response)

        // If no materials data, try eco-score
        if (materials.isEmpty()) {
            return determineFromEcoScore(response)
        }

        // Priority 1: Check if compostable (food waste, organic materials)
        if (isCompostable(materials)) {
            return RecycleCategory.COMPOSTABLE
        }

        // Priority 2: Check UK-accepted recyclables
        if (isRecyclableInUK(materials)) {
            return RecycleCategory.RECYCLABLE
        }

        // Priority 3: Check if explicitly non-recyclable
        if (isNonRecyclableInUK(materials)) {
            return RecycleCategory.GENERAL_WASTE
        }

        // Fallback: Use eco-score data
        return determineFromEcoScore(response)
    }

    /**
     * Extracts all material information from OpenFoodFacts response
     * Combines ecoscore and packaging data for comprehensive analysis
     */
    private fun getAllMaterials(response: OpenFoodFactsResponse): List<String> {
        val product = response.product ?: return emptyList()

        // Source 1: Ecoscore packaging materials
        val ecoscoreMaterials = product.ecoscoreData?.adjustments?.packaging?.packagings
            ?.mapNotNull { it.material } ?: emptyList()

        // Source 2: Product packaging materials
        val productMaterials = product.packagings
            ?.mapNotNull { it.material } ?: emptyList()

        // Combine and remove duplicates
        return (ecoscoreMaterials + productMaterials)
            .map { it.lowercase() }
            .distinct()
    }

    /**
     * Checks if materials are compostable/biodegradable
     */
    private fun isCompostable(materials: List<String>): Boolean {
        val compostableKeywords = listOf(
            "compostable",
            "biodegradable",
            "organic",
            "bio-based"
        )

        return materials.any { material ->
            compostableKeywords.any { keyword ->
                material.contains(keyword, ignoreCase = true)
            }
        }
    }

    /**
     * Checks if materials are recyclable according to UK guidelines
     * Most UK councils accept: Glass, Metal, Paper, Cardboard, PET, HDPE, PP
     */
    private fun isRecyclableInUK(materials: List<String>): Boolean {
        val ukRecyclableMaterials = listOf(
            // Glass
            "glass",

            // Metals
            "metal", "aluminium", "aluminum", "steel", "tin",

            // Paper products
            "paper", "cardboard", "card",

            // Recyclable plastics (widely accepted in UK)
            "pet", "hdpe", "pp", "polypropylene",
            "1-pet", "2-hdpe", "5-pp"  // Recycling codes
        )

        return materials.any { material ->
            ukRecyclableMaterials.any { recyclable ->
                material.contains(recyclable, ignoreCase = true)
            }
        }
    }

    /**
     * Checks if materials are explicitly non-recyclable in UK
     * Most UK councils reject: Polystyrene, PVC, LDPE, Black plastic
     */
    private fun isNonRecyclableInUK(materials: List<String>): Boolean {
        val ukNonRecyclableMaterials = listOf(
            "polystyrene", "styrofoam", "eps",
            "pvc", "polyvinyl",
            "ldpe", "4-ldpe",  // Plastic bags/film
            "black plastic",
            "mixed material",
            "composite",
            "laminate"
        )

        return materials.any { material ->
            ukNonRecyclableMaterials.any { nonRecyclable ->
                material.contains(nonRecyclable, ignoreCase = true)
            }
        }
    }

    /**
     * Determines category from eco-score when materials data unavailable
     */
    private fun determineFromEcoScore(response: OpenFoodFactsResponse): RecycleCategory {
        val product = response.product ?: return RecycleCategory.GENERAL_WASTE
        val packagingAdjustment = product.ecoscoreData?.adjustments?.packaging

        val nonRecyclableMaterials = packagingAdjustment?.nonRecyclableAndNonBiodegradableMaterials
        val environmentalScore = packagingAdjustment?.score ?: -100

        return when {
            // Confirmed recyclable
            nonRecyclableMaterials == 0 && environmentalScore >= 50 -> {
                RecycleCategory.RECYCLABLE
            }
            // Confirmed non-recyclable
            nonRecyclableMaterials != null && nonRecyclableMaterials > 0 -> {
                RecycleCategory.GENERAL_WASTE
            }
            // Unknown - default to general waste with informative message
            else -> RecycleCategory.GENERAL_WASTE
        }
    }

    /**
     * Generates user-friendly description based on category and available data
     */
    private fun generateDescription(
        response: OpenFoodFactsResponse,
        category: RecycleCategory
    ): String {
        val product = response.product ?: return "No product information available"
        val materials = getAllMaterials(response)

        return when (category) {
            RecycleCategory.RECYCLABLE -> {
                if (materials.isNotEmpty()) {
                    "This product's ${materials.joinToString(", ")} packaging is recyclable in most UK councils."
                } else {
                    "This product uses recyclable packaging materials. Please recycle responsibly."
                }
            }
            RecycleCategory.COMPOSTABLE -> {
                "This product is compostable or biodegradable. Add to food waste or compost bin."
            }
            RecycleCategory.GENERAL_WASTE -> {
                if (materials.isNotEmpty()) {
                    "This product's packaging contains non-recyclable materials. Dispose in general waste."
                } else {
                    "Packaging information limited. Check local recycling guidelines or dispose in general waste."
                }
            }
        }
    }

    /**
     * Generates specific recycling tips based on materials present
     */
    private fun generateRecyclingTips(
        response: OpenFoodFactsResponse,
        category: RecycleCategory
    ): String {
        if (category == RecycleCategory.GENERAL_WASTE) {
            return "Not recyclable in most UK areas. Dispose in general waste bin."
        }

        if (category == RecycleCategory.COMPOSTABLE) {
            return "Add to food waste caddy or home compost bin. Breaks down naturally."
        }

        val materials = getAllMaterials(response)
        val tips = mutableListOf<String>()

        materials.forEach { material ->
            when {
                material.contains("glass") -> {
                    tips.add("Remove lids and rinse glass containers")
                }
                material.contains("pet") || material.contains("hdpe") || material.contains("pp") -> {
                    tips.add("Remove caps, rinse bottles, and crush to save space")
                }
                material.contains("plastic") && !tips.any { it.contains("plastic") } -> {
                    tips.add("Check recycling symbol - rinse and remove caps")
                }
                material.contains("paper") || material.contains("cardboard") -> {
                    tips.add("Flatten boxes and keep paper dry - no greasy cardboard")
                }
                material.contains("metal") || material.contains("aluminium") || material.contains("steel") -> {
                    tips.add("Rinse metal containers and crush cans to save space")
                }
            }
        }

        return if (tips.isNotEmpty()) {
            tips.distinct().joinToString(". ") + "."
        } else {
            "Rinse packaging and check with your local UK council for specific guidelines."
        }
    }

    /**
     * Gets human-readable materials description for display
     */
    fun getMaterialsDescription(response: OpenFoodFactsResponse): String {
        val materials = getAllMaterials(response)

        if (materials.isEmpty()) {
            return "Packaging materials not specified"
        }

        val product = response.product
        val packagingDetails = product?.ecoscoreData?.adjustments?.packaging?.packagings

        if (packagingDetails.isNullOrEmpty()) {
            return materials.joinToString(", ") { it.capitalize() }
        }

        val descriptions = packagingDetails.mapNotNull { detail ->
            val material = detail.material?.removePrefix("en:")?.replace("-", " ")?.capitalize()
            val shape = detail.shape?.removePrefix("en:")?.replace("-", " ")?.capitalize()

            when {
                material != null && shape != null -> "$material $shape"
                material != null -> material
                shape != null -> shape
                else -> null
            }
        }

        return if (descriptions.isNotEmpty()) {
            descriptions.joinToString(", ")
        } else {
            "Mixed packaging materials"
        }
    }
}

private fun String.capitalize(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}