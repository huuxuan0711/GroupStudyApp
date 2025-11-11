package com.xmobile.project1groupstudyappnew.api_service

import com.xmobile.project1groupstudyappnew.model.obj.CloudinaryUploadResponse
import com.xmobile.project1groupstudyappnew.model.obj.DeleteResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface CloudinaryApi {
    @Multipart
    @POST("auto/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("upload_preset") uploadPreset: RequestBody
    ): CloudinaryUploadResponse
}
