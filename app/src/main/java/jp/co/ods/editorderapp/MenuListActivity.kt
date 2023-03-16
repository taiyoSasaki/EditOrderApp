package jp.co.ods.editorderapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_menu_list.*
import kotlinx.android.synthetic.main.content_main.listView

class MenuListActivity : AppCompatActivity() {

    private lateinit var mCategory :Category
    private lateinit var mAdapter :MenuListAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_list)

        //preferenceから表示名を取得してタイトルに反映させる
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val storeName = sp.getString(StoreNameKEY, "")

        //渡ってきたMenuリストのオブジェクトを保存する
        val extras = intent.extras
        mCategory = extras!!.get("category") as Category

        //UI設定
        title = "$storeName > ${mCategory.categoryName}"

        //ListViewの準備
        mAdapter = MenuListAdapter(this)
        mAdapter.setMenuList(mCategory.menuList)
        mAdapter.notifyDataSetChanged()
        listView.adapter = mAdapter

        //アイテムをタップしたとき
        listView.setOnItemClickListener { parent, view, position, id ->
            val intent = Intent(applicationContext, MenuAddActivity::class.java)
            intent.putExtra("categoryKey", mCategory.key)
            intent.putExtra("categoryName", mCategory.categoryName)
            intent.putExtra( "menu", mCategory.menuList[position])
            startActivity(intent)
            finish()
        }

        //アイテムを長押ししたとき
        listView.setOnItemLongClickListener { parent, view, position, id ->
            //メニュー削除メソッド
            //ダイアログを表示する
            val builder = AlertDialog.Builder(this)
                .setTitle(getString(R.string.menu_remove_title))
                .setMessage(getString(R.string.menu_remove_message))
                .setPositiveButton(getString(R.string.menu_remove)) { _, _ ->
                    val user = FirebaseAuth.getInstance().currentUser
                    val dataBaseReference = FirebaseDatabase.getInstance().reference
                    val menuRef = dataBaseReference.child(user!!.uid).child(CategoryPath).child(mCategory.key).child(MenuPath)
                    val menuKey = mCategory.menuList[position].key
                    menuRef.child(menuKey).setValue(null)
                    finish()
                }
                .setNegativeButton(getString(R.string.menu_cancel_remove), null)
            builder.create()
            builder.show()
            true
        }

        //プラスボタンを押したとき
        fab.setOnClickListener { _ ->
            val intent = Intent(applicationContext, MenuAddActivity::class.java)
            intent.putExtra("categoryKey", mCategory.key)
            intent.putExtra("categoryName", mCategory.categoryName)
            intent.putExtra("menu", Menu("", "", 0, "", ""))
            startActivity(intent)
            finish()
        }

    }

    override fun onResume() {
        super.onResume()
        mAdapter.notifyDataSetChanged()
    }
}
