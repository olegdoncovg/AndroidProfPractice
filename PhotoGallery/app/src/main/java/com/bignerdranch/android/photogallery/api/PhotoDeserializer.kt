package com.bignerdranch.android.photogallery.api

import com.bignerdranch.android.photogallery.GalleryItem
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class PhotoDeserializer : JsonDeserializer<PhotoResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): PhotoResponse {
        val jsonObj = json.asJsonObject

        val photosJson: JsonElement = jsonObj.get("photo")
        val photosList: ArrayList<GalleryItem> = ArrayList()
        val jsonArr = photosJson.asJsonArray
        if (null != jsonArr) {
            val size = jsonArr.size()
            for (i in 0 until size) {
                val photoJson: JsonElement = jsonArr[i]
                val photoObj = photoJson.asJsonObject

                photosList.add(
                    GalleryItem(
                        "123" + photoObj.get("title").asString,
                        photoObj.get("id").asString,
                        photoObj.get("url_s").asString
                    )
                )
            }
        }
        return PhotoResponse(photosList)
    }
}