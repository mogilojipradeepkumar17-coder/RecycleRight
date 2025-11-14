package uk.ac.tees.mad.recycleright.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApiService {

    @GET("api/v2/product/{barcode}")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<OpenFoodFactsResponse>

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.net/"
    }
}