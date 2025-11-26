package uk.ac.tees.mad.recycleright.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApiService {



    // we have to give the barcode to it and it will return the Response
    @GET("api/v2/product/{barcode}")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<OpenFoodFactsResponse>

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.net/"
    }
}