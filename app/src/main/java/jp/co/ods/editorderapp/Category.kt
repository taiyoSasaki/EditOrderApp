package jp.co.ods.editorderapp

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class Category(val key: String, var categoryName: String, val menuList: ArrayList<Menu>) :Serializable {

}