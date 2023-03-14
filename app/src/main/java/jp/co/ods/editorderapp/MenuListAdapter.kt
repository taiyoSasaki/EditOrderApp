package jp.co.ods.editorderapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.list_menu.view.*

class MenuListAdapter(context: Context) :BaseAdapter() {
    private var mLayoutInflater: LayoutInflater
    private var mMenuList = ArrayList<Menu>()

    init {
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getCount(): Int {
        return mMenuList.size
    }

    override fun getItem(position: Int): Any {
        return mMenuList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup?): View {
        var convertView = view

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.list_menu, parent, false)!!
        }

        val imageString = mMenuList[position].imageString
        if (imageString.isNotEmpty()) {
            val bytes = Base64.decode(imageString, Base64.DEFAULT)
            val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).copy(Bitmap.Config.ARGB_8888, true)
            val imageView = convertView.image_view as ImageView
            imageView.setImageBitmap(image)
        }

        val menuName = convertView.menu_name as TextView
        menuName.text = mMenuList[position].name

        val priceLabel = convertView.price_label as TextView
        priceLabel.text = formatPriceString(mMenuList[position].price)

        return convertView
    }

    fun setMenuList(menuList: ArrayList<Menu>) {
        mMenuList = menuList
    }

    private fun formatPriceString(price :Int) :String {
        var format = "%,3d"
        return "￥" + format.format(price)
    }

}