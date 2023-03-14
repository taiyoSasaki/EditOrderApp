package jp.co.ods.editorderapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.preference.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mCategoryList: ArrayList<Category>
    private lateinit var mAdapter: CategoryListAdapter

    private var mStoreRef: DatabaseReference? = null

    private val mEventListener = object :ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val map = snapshot.value as Map<String, Any>
            val key = snapshot.key ?: ""
            val categoryName = map["category"].toString()

            val mMenuList = ArrayList<jp.co.ods.editorderapp.Menu>()
            val menuMap = map["menu"] as Map<String, Any>?
            if (menuMap != null) {
                for (mapKey in menuMap.keys) {
                    val temp = menuMap[mapKey] as Map<String, String>
                    val menuName = temp["name"] ?: ""
                    val menuPrice = temp["price"].toString()
                    val menuImage = temp["image"] ?: ""
                    val menuExplain = temp["explain"] ?: ""
                    val menu = Menu(mapKey, menuName, menuPrice.toInt(), menuImage, menuExplain)
                    mMenuList.add(menu)
                }
            }

            val category = Category(key, categoryName, mMenuList)
            mCategoryList.add(category)

            //アダプターへ通知
            mAdapter.notifyDataSetChanged()
        }
        override fun onCancelled(error: DatabaseError) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            val map = snapshot.value as Map<String, Any>
            val key = snapshot.key ?: ""
            val categoryName = map["category"].toString()

            for (i in 0..mCategoryList.size-1) {
                if (key == mCategoryList[i].key) {
                    mCategoryList[i].categoryName = categoryName
                }
            }
            //アダプターへ通知
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            val map = snapshot.value as Map<String, Any>
            val key = snapshot.key ?: ""
            val categoryName = map["category"].toString()

            val mMenuList = ArrayList<jp.co.ods.editorderapp.Menu>()
            val menuMap = map["menu"] as Map<String, Any>?
            if (menuMap != null) {
                for (mapKey in menuMap.keys) {
                    val temp = menuMap[mapKey] as Map<String, String>
                    val menuName = temp["name"] ?: ""
                    val menuPrice = temp["price"].toString()
                    val menuImage = temp["image"] ?: ""
                    val menuExplain = temp["explain"] ?: ""
                    val menu = Menu(mapKey, menuName, menuPrice.toInt(), menuImage, menuExplain)
                    mMenuList.add(menu)
                }
            }

            val category = Category(key, categoryName, mMenuList)
            mCategoryList.remove(category)
            //アダプターへ通知
            mAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ～～ ここから
        // idがtoolbarがインポート宣言により取得されているので
        // id名でActionBarのサポートを依頼
        setSupportActionBar(toolbar)

        //preferenceから表示名を取得してタイトルに反映させる
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val storeName = sp.getString(StoreNameKEY, "")

        //UI設定
        title = storeName

        // fabにClickリスナーを登録
        fab.setOnClickListener { _ ->
            showCategoryAddDialog()
        }

        //Firebase用
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        //ListViewの準備
        mAdapter = CategoryListAdapter(this)
        mCategoryList = ArrayList<Category>()
        mAdapter.notifyDataSetChanged()

        listView.setOnItemClickListener {parent, view, position, id ->
            if (mStoreRef != null) {
                mStoreRef!!.removeEventListener(mEventListener)
            }

            //Categoryを渡してメニュー一覧画面を起動する
            val intent = Intent(applicationContext, MenuListActivity::class.java)
            intent.putExtra("category", mCategoryList[position])
            startActivity(intent)
        }

        listView.setOnItemLongClickListener { parent, view, position, id ->
            //長押し
            //削除、編集へのアクセス
            showRemoveCategoryAddDialog(mCategoryList[position])

            true
        }

    }

    override fun onResume() {
        super.onResume()
        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser
        // ログインしていなければログイン画面に遷移させる
        if (user == null) {
            val intent = Intent(applicationContext, LoginActivity::class.java)
            startActivity(intent)
        } else {
            mCategoryList.clear()
            mAdapter.setCategoryList(mCategoryList)
            listView.adapter = mAdapter

            //ログインしている店の階層にリスナー登録
            if (mStoreRef != null) {
                mStoreRef!!.removeEventListener(mEventListener)
            }
            mStoreRef = mDatabaseReference.child(user.uid)
            mStoreRef!!.addChildEventListener(mEventListener)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }


    private fun showCategoryAddDialog() {
        // ダイアログ専用のレイアウトを読み込む
        val dialogLayout = LayoutInflater.from(this).inflate(R.layout.edit_text_dialog, null)
        val editText = dialogLayout.findViewById<AppCompatEditText>(R.id.editTextDialog)
        editText.hint = getString(R.string.category_add_hint)

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.category_add_title))
            .setMessage(getString(R.string.category_add_message))
            .setView(dialogLayout)
            .setPositiveButton(getString(R.string.dialog_positive)) { _, _ ->
                //OKボタンを押したとき
                val categoryString = editText.text.toString()
                //Firebaseに保存
                mStoreRef!!.push().setValue(mapOf("category" to categoryString))
            }
            .setNegativeButton(getString(R.string.dialog_negative), null)
            .create()

        dialog.show()

        // ダイアログのボタンを取得し、デフォルトの色を設定
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false //デフォルトでオフにしておく
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.gray))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.hotPink))

        // AppCompatEditTextにTextChangedListenerをセット
        editText.addTextChangedListener( object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // 1~10文字の時だけOKボタンを有効化する
                if (s.isNullOrEmpty() || s.length > 10) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.gray))
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.hotPink))
                }
            }
        })
    }

    private fun showRemoveCategoryAddDialog(category: Category) {
        // ダイアログ専用のレイアウトを読み込む
        val dialogLayout = LayoutInflater.from(this).inflate(R.layout.edit_text_dialog, null)
        val editText = dialogLayout.findViewById<AppCompatEditText>(R.id.editTextDialog)
        editText.hint = getString(R.string.category_add_hint)

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.category_change_title))
            .setMessage(getString(R.string.category_add_message))
            .setView(dialogLayout)
            .setPositiveButton(getString(R.string.dialog_positive)) { _, _ ->
                //OKボタンを押したとき
                val categoryString = editText.text.toString()
                //Firebaseに保存
                mStoreRef!!.child(category.key).updateChildren(mapOf("category" to categoryString))
            }
            .setNegativeButton(getString(R.string.dialog_negative), null)
            .setNeutralButton(getString(R.string.category_remove)) { _, _ ->
                //Firebaseに保存
                mStoreRef!!.updateChildren(mapOf(category.key to null))
            }
            .create()

        dialog.show()

        // ダイアログのボタンを取得し、デフォルトの色を設定
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false //デフォルトでオフにしておく
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.gray))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.hotPink))

        // AppCompatEditTextにTextChangedListenerをセット
        editText.addTextChangedListener( object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // 1~10文字の時だけOKボタンを有効化する
                if (s.isNullOrEmpty() || s.length > 10) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.gray))
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.hotPink))
                }
            }
        })
    }
}
