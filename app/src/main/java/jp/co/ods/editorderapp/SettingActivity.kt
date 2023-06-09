package jp.co.ods.editorderapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_setting.*

class SettingActivity : AppCompatActivity() {

    private lateinit var mDataBaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        //preferenceから表示名を取得してEditTextに反映させる
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val name = sp.getString(StoreNameKEY, "")
        store_name.setText(name)

        mDataBaseReference = FirebaseDatabase.getInstance().reference

        //UIの初期化
        title = getString(R.string.settings_title)

        logoutButton.setOnClickListener { v ->
            //ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                //ログインしていない場合は何もしない
                Snackbar.make(v, getString(R.string.no_login_user), Snackbar.LENGTH_LONG).show()
            } else {
                FirebaseAuth.getInstance().signOut()  //ログアウト
                finish()
            }
        }

        change_name.setOnClickListener { v ->
            //キーボードが出ていたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            //ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            //変更した表示名をFirebaseに保存する
            val name2 = store_name.text.toString()
            val userRef = mDataBaseReference.child(StoresPATH).child(user!!.uid)
            val data = HashMap<String, String>()
            data["StoreName"] = name2
            userRef.setValue(data)

            //変更した表示名をpreferenceに保存する
            val sp2 = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val editor = sp2.edit()
            editor.putString(StoreNameKEY, name2)
            editor.commit()

            Snackbar.make(v, getString(R.string.change_disp_name), Snackbar.LENGTH_LONG).show()
        }


    }
}