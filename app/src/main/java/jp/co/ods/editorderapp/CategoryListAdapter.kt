package jp.co.ods.editorderapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.list_categorys.view.*

class CategoryListAdapter(context: Context) :BaseAdapter() {
    private var mLayoutInflater: LayoutInflater
    private var mCategoryList = ArrayList<Category>()

    init {
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getCount(): Int {
        return mCategoryList.size
    }

    override fun getItem(position: Int): Any {
        return mCategoryList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup?): View {
        var convertView = view

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.list_categorys, parent, false)
        }

        val category_name = convertView!!.category_name as TextView
        category_name.text = mCategoryList[position].categoryName

        return convertView
    }

    fun setCategoryList(categoryList: ArrayList<Category>) {
        mCategoryList = categoryList
    }

}