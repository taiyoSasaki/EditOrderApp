package jp.co.ods.editorderapp

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class Menu (val key: String, val name: String, val price: Int, val imageString: String, val explain: String) :Serializable{

    @Exclude
    fun toMap() :Map<String, Any> {
        return mapOf(
            "name" to name,
            "price" to price,
            "image" to imageString,
            "explain" to explain
        )
    }
}