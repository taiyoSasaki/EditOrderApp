package jp.co.ods.editorderapp

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_menu_add.*
import kotlinx.android.synthetic.main.list_menu.menu_name
import java.io.ByteArrayOutputStream

class MenuAddActivity : AppCompatActivity() {
    companion object {
        private val PERMISSIONS_REQUEST_CODE = 100
        private val CHOOSER_REQUEST_CODE = 100
    }

    private lateinit var mCategoryKey: String
    private lateinit var mCategoryName: String
    private lateinit var mMenu: Menu
    private var mPictureUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_add)

        //preferenceから表示名を取得してタイトルに反映させる
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val storeName = sp.getString(StoreNameKEY, "")

        //渡ってきたMenu型オブジェクトを保存する
        val extras = intent.extras
        mCategoryKey = extras!!.get("categoryKey") as String
        mCategoryName = extras.get("categoryName") as String
        val mMenu = extras.get("menu") as Menu

        //UI設定
        title = "$storeName > $mCategoryName > ${mMenu.name}"

        menu_name.text = mMenu.name
        if (mMenu.imageString.isNotEmpty()) {
            val bytes = Base64.decode(mMenu.imageString, Base64.DEFAULT)
            val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).copy(Bitmap.Config.ARGB_8888, true)
            menu_image.setImageBitmap(image)
        }

        menu_price.setText(mMenu.price.toString())
        menu_explain.setText(mMenu.explain)

        menu_image.setOnClickListener{
            //パーミッションの許可状態を確認する
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    //許可されている
                    showChooser()
                } else {
                    // 許可されていないので許可ダイヤログを表示する
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)

                }
            } else {
                showChooser()
            }
        }

        menu_edit_ok.setOnClickListener{v ->
            //キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)

            val user = FirebaseAuth.getInstance().currentUser
            val dataBaseReference = FirebaseDatabase.getInstance().reference
            val menuRef = dataBaseReference.child(user!!.uid).child(mCategoryKey).child(MenuPath)

            var menuKey: String
            if (mMenu.key == "") {
                menuKey = menuRef.push().key ?: ""
            } else {
                menuKey = mMenu.key
            }
            val menuName = menu_name.text.toString()
            val menuPrice = menu_price.text.toString()

            //nameとpriceが入力されていなければエラー
            if (menuName.isEmpty()) {
                //メニュー名が入力されていない時はエラーを表示するだけ
                Snackbar.make(v, getString(R.string.input_menu_name), Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (menuPrice.isEmpty()) {
                //価格が入力されていない時はエラーを表示するだけ
                Snackbar.make(v, getString(R.string.input_menu_price), Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val menuExplain = menu_explain.text.toString()
            //添付画像を取得する
            val drawable = menu_image.drawable as? BitmapDrawable
            var bitmapString = ""
            //添付画像が設定されていれば画像をとる出し手BASE64エンコードする
            if (drawable != null) {
                val bitmap = drawable.bitmap
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                bitmapString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
            }

            val sendMenu = Menu(menuKey, menuName, menuPrice.toInt(), bitmapString, menuExplain)
            val data = sendMenu.toMap()

            val childUpdates = hashMapOf<String, Any>(menuKey to data)
            //Firebaseを更新
            menuRef.updateChildren(childUpdates)
                .addOnSuccessListener {
                    finish()
                }
        }
        menu_edit_cancel.setOnClickListener { v ->
            //キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)

            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHOOSER_REQUEST_CODE) {

            if (resultCode != Activity.RESULT_OK) {
                if (mPictureUri != null) {
                    contentResolver.delete(mPictureUri!!, null, null)
                    mPictureUri = null
                }
                return
            }

            //画像を取得
            val uri = if (data == null || data.data == null) mPictureUri else data.data

            //URIからBitmapを取得する
            val image: Bitmap
            try {
                val contentResolver = contentResolver
                val inputStream = contentResolver.openInputStream(uri!!)
                image = BitmapFactory.decodeStream(inputStream)
                inputStream!!.close()
            } catch (e :Exception) {
                return
            }

            //取得したBitmap長編を500ピクセルにリサイズする
            val imageWidth = image.width
            val imageHeight = image.height
            val scale = Math.min(500.toFloat() / imageWidth, 500.toFloat() / imageHeight)  //(1)

            val matrix = Matrix()
            matrix.postScale(scale, scale)

            val resizedImage = Bitmap.createBitmap(image, 0, 0, imageWidth, imageHeight, matrix, true)

            //BitmapをImageViewに設定する
            menu_image.setImageBitmap(resizedImage)

            mPictureUri = null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //ユーザーが許可したとき
                    showChooser()
                }
                return
            }
        }
    }

    private fun showChooser() {
        //ギャラリーから選択するIntent
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)

        //カメラで撮影するIntent
        val filename = System.currentTimeMillis().toString() + ".jpg"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        mPictureUri = contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri)

        //ギャラリー選択のIntentを与えてcreateChooserメソッドを呼ぶ
        val chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.get_image))

        //EXTRA_INITIAL_INTENTSにカメラ撮影のintentを追加
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

        startActivityForResult(chooserIntent, CHOOSER_REQUEST_CODE)
    }
}