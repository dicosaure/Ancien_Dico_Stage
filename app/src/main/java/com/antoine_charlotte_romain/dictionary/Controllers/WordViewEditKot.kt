package com.antoine_charlotte_romain.dictionary.Controllers

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.*
import com.antoine_charlotte_romain.dictionary.Controllers.activities.MainActivityKot
import com.antoine_charlotte_romain.dictionary.R
import com.antoine_charlotte_romain.dictionary.business.dictionary.Dictionary
import com.antoine_charlotte_romain.dictionary.business.word.Word
import com.antoine_charlotte_romain.dictionary.business.word.WordSQLITE
import com.dicosaure.Business.Translate.TranslateSQLITE
import org.jetbrains.anko.ctx
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 * Created by dineen on 12/07/2016.
 */
class WordViewEditKot : AppCompatActivity() {

    var mRecorder : MediaRecorder? = null
    var soundSet : Boolean = false
    var imgWord : Bitmap? = null
    var word : WordSQLITE? = null
    var translations : ArrayList<Word>? = null
    var headwordField: TextView? = null
    var noteField : TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(R.layout.view_word_edit)

        //Set the toolbar on the view
        var toolbar = super.findViewById(R.id.tool_bar) as Toolbar
        super.setSupportActionBar(toolbar)
        this.supportActionBar!!.setTitle(R.string.details)
        this.supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        //Set the word and dictionary, come from the segue
        var wordIntent = this.intent.getSerializableExtra(MainActivityKot.EXTRA_WORD) as Word
        this.word = WordSQLITE(this.ctx, wordIntent.idWord, wordIntent.note,
                wordIntent.image, wordIntent.sound, wordIntent.headword, wordIntent.dateView,
                wordIntent.idDictionary)
        var dictionary = this.intent.getSerializableExtra(MainActivityKot.EXTRA_DICTIONARY) as Dictionary

        //Set fields
        (super.findViewById(R.id.edit_dictionary) as TextView).text = dictionary.getNameDictionary()
        this.headwordField = super.findViewById(R.id.edit_word) as TextView
        this.headwordField!!.text = this.word!!.headword
        this.noteField = super.findViewById(R.id.edit_note) as TextView
        if (this.word!!.note == null) {
            this.noteField!!.text = this.resources.getString(R.string.no_note)
        }
        else {
            this.noteField!!.text = this.word!!.note
        }
        this.initTranslationsList()
        if (this.word!!.image != null) {
            this.imgWord = BitmapFactory.decodeByteArray(this.word!!.image, 0, this.word!!.image!!.size)
            (super.findViewById(R.id.image_word) as ImageView).setImageBitmap(this.imgWord)
            (super.findViewById(R.id.text_image) as TextView).text = this.resources.getString(R.string.modify_image)
        }
        if (this.word!!.sound != null) {
            this.initMediaPlayerWithByteArrray(this.word!!.sound!!)
            (super.findViewById(R.id.play_button) as Button).isEnabled = true
        }
        else {
            (super.findViewById(R.id.play_button) as Button).isEnabled = false
        }
    }

    fun initMediaPlayerWithByteArrray(ba : ByteArray) {
        val soundFile = File("""${this.cacheDir}/audiorecord.3gp""")
        //soundFile.deleteOnExit()
        val fos = FileOutputStream(soundFile)
        fos.write(this.word!!.sound)
        fos.close()
//
//        // Tried reusing instance of media player
//        // but that resulted in system crashes...
//        MediaPlayer mediaPlayer = new MediaPlayer();
//
//        // Tried passing path directly, but kept getting
//        // "Prepare failed.: status=0x1"
//        // so using file descriptor instead
//        FileInputStream fis = new FileInputStream(tempMp3);
//        mediaPlayer.setDataSource(fis.getFD());
//
//        mediaPlayer.prepare();
//        mediaPlayer.start();
    }

    fun initTranslationsList() {
        this.translations = ArrayList(this.word!!.selectAllTranslations())
        var translationField = super.findViewById(R.id.edit_translation) as TextView
        if (this.translations!!.count() > 0) {
            var strTranslations = ""
            for (tr in this.translations!!) {
                strTranslations = strTranslations.plus("- " + tr.headword + "\n")
            }
            translationField.text = strTranslations
        }
        else {
            translationField.text = ""
            val btn = super.findViewById(R.id.delete_translation) as Button
            btn.isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        super.getWindow().setSoftInputMode(WindowManager.
                LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    fun add_translation(view: View) {
        //Creating the dialog builder
        val builder = AlertDialog.Builder(this)
        val layout = LayoutInflater.from(ctx).inflate(R.layout.add_translation, null)
        val field = layout.findViewById(R.id.edit_translation) as EditText

        //Adding the layout to the dialog
        builder.setView(layout)
        builder.setPositiveButton(R.string.add) { dialog, which ->
            if (!field.text.toString().isEmpty()) {
                val translateTxt = field.text.toString().trim { it <= ' ' }
                val wordFrom = WordSQLITE(this.ctx, headword = translateTxt, idDictionary = "0", note = "")
                wordFrom.save()
                val translate = TranslateSQLITE(this.ctx, this.word, wordFrom)

                if (translate.save() > 0) {
                    Toast.makeText(this.ctx, R.string.translation_success, 10000).show()
                    if (this.translations!!.size == 0) {
                        val btn = super.findViewById(R.id.delete_translation) as Button
                        btn.isEnabled = true
                    }
                    this.initTranslationsList()
                }
                else {
                    Toast.makeText(this.ctx, R.string.translation_error, 10000).show()
                }
            }
            dialog.cancel()
        }
        builder.create()
        builder.show()
    }

    fun remove_translation(view: View) {
        //Creating the dialog builder
        val builder = AlertDialog.Builder(this)

        var layout = LayoutInflater.from(ctx).inflate(R.layout.remove_translation, null).findViewById(R.id.global_layout) as RelativeLayout
        var gridLayout = layout.findViewById(R.id.popup_layout) as GridLayout
        val txtTitle = gridLayout.findViewById(R.id.title_translation)
        gridLayout.removeAllViews()

        gridLayout.rowCount = this.translations!!.size + 1
        gridLayout.addView(txtTitle)

        //var tmp = gridLayout.findViewById(R.id.check_translation) as CheckBox
        var checkbox : CheckBox

        //gridLayout.removeView(tmp)
        var listCheckBox = ArrayList<CheckBox>()

        for (tr in this.translations!!) {
            checkbox = CheckBox(this.ctx)
            checkbox.isChecked = false
            checkbox.text = tr.headword
            checkbox.tag = tr

            gridLayout.addView(checkbox)

            listCheckBox.add(checkbox)
        }

        //Adding the layout to the dialog
        builder.setView(layout)
        builder.setPositiveButton(R.string.delete) { dialog, which ->
            for (cb in listCheckBox) {
                if (cb.isChecked) {
                    this.translations!!.remove(cb.tag)
                    val tr = TranslateSQLITE(this.ctx, this.word, cb.tag as Word)
                    tr.delete()
                }
            }
            this.initTranslationsList()
            dialog.cancel()
        }
        builder.create()
        builder.show()
    }

    fun loadImagefromGallery(view: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("NTM")
        builder.setPositiveButton(R.string.add) { dialog, which ->
            // Create intent to Open Image applications like Gallery, Google Photos
            val galleryIntent = Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            // Start the Intent
            startActivityForResult(galleryIntent, RESULT_LOAD_IMG)
            dialog.cancel()
        }
        builder.setNegativeButton(R.string.cancel) {
            dialog, which -> dialog.cancel()
        }
        builder.setNeutralButton(R.string.delete) {
            dialog, which ->
            (super.findViewById(R.id.image_word) as ImageView).setImageBitmap(null)
            (super.findViewById(R.id.text_image) as TextView).text = this.resources.getString(R.string.add_txt_img)
            this.imgWord = null
            dialog.cancel()
        }
        builder.create()
        builder.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == Activity.RESULT_OK && null != data) {
                // Get the Image from data
                val selectedImage = data.data
                val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)

                if (Build.VERSION.SDK_INT >= 23) {
                    // Here, thisActivity is the current activity
                    if (ContextCompat.checkSelfPermission(applicationContext,
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                        // Should we show an explanation?
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.READ_EXTERNAL_STORAGE)) {

                            // Show an expanation to the user *asynchronously* -- don't block
                            // this thread waiting for the user's response! After the user
                            // sees the explanation, try again to request the permission.

                        }
                        else {
                            // No explanation needed, we can request the permission.
                            ActivityCompat.requestPermissions(this,
                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)

                            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                            // app-defined int constant. The callback method gets the
                            // result of the request.
                        }
                    }
                }
                // Get the cursor
                val cursor = contentResolver.query(selectedImage,
                        filePathColumn, null, null, null)
                //val filePath = cursor.getString(cursor.getColumnIndex(filePathColumn[0]))
                // Move to first row
                cursor!!.moveToFirst()

                val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                var imgDecodableString = cursor.getString(columnIndex)
                cursor.close()

                val imgView = findViewById(R.id.image_word) as ImageView
                var img = BitmapFactory.decodeFile(imgDecodableString)

                val exif = ExifInterface(imgDecodableString)
                if (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) == ExifInterface.ORIENTATION_ROTATE_90) {
                    val matrix = Matrix();
                    matrix.postRotate(90f);
                    img = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true);
                }
                // Set the Image in ImageView after decoding the String
                imgView.setImageBitmap(img)
                this.imgWord = img
                (super.findViewById(R.id.text_image) as TextView).text = this.resources.getString(R.string.modify_image)
            }
            else {
                Toast.makeText(this, this.resources.getString(R.string.error_picked_image), Toast.LENGTH_LONG).show()
            }
        }
        catch (e: Exception) {
            Toast.makeText(this, this.resources.getString(R.string.permission_error), Toast.LENGTH_SHORT).show()
        }
    }

    fun startRecording(view: View) {
        var btnRecord = (super.findViewById(R.id.start_recording) as Button)
        if (btnRecord.text == this.resources.getString(R.string.record)) {
            try {
                val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1
                if (Build.VERSION.SDK_INT >= 23) {
                    // Here, thisActivity is the current activity
                    if (ContextCompat.checkSelfPermission(applicationContext,
                            Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                        // Should we show an explanation?
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.RECORD_AUDIO)) {

                            // Show an expanation to the user *asynchronously* -- don't block
                            // this thread waiting for the user's response! After the user
                            // sees the explanation, try again to request the permission.

                        } else {

                            // No explanation needed, we can request the permission.

                            ActivityCompat.requestPermissions(this,
                                    arrayOf(Manifest.permission.RECORD_AUDIO),
                                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO)

                            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                            // app-defined int constant. The callback method gets the
                            // result of the request.
                        }
                    }
                }
                //create file audio
                btnRecord.text = this.resources.getString(R.string.recording)
                //val dir = this.cacheDir
                //dir.mkdirs() //create folders where write files
//                var fileAudioPath = this.cacheDir.path//dir.getPath() //Environment.getExternalStorageDirectory().getAbsolutePath();
//                fileAudioPath += """/${FILE_NAME_SOUND}.${FILE_NAME_EXTENSION}"""
                var soundFile = File("""${this.cacheDir}/audiorecord.3gp""")

                this.mRecorder = MediaRecorder()
                this.mRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
                this.mRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                this.mRecorder!!.setMaxDuration(10000)
                this.mRecorder!!.setOutputFile(soundFile.path)
                this.mRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

                try {
                    this.mRecorder!!.prepare()
                } catch (e: IOException) {
                    Toast.makeText(this, this.resources.getString(R.string.permission_error), Toast.LENGTH_SHORT).show();
                }
                this.mRecorder!!.start()
                this.soundSet = true
            } catch (e: Exception) {
                Toast.makeText(this, this.resources.getString(R.string.permission_error), Toast.LENGTH_SHORT).show();
            }
        }
        else {
            this.mRecorder!!.stop()
            this.mRecorder!!.release()
            btnRecord.text = this.resources.getString(R.string.record)
            (super.findViewById(R.id.play_button) as Button).isEnabled = true
        }
    }

    fun playRecord(view: View) {
        var btnPlay = super.findViewById(R.id.play_button) as Button
        if (btnPlay.isEnabled) {
            var mPlayer = MediaPlayer()
            try {
                val audioFile = this.cacheDir.list { file, s ->
                    if (s.contains(FILE_NAME_EXTENSION)) {
                        true
                    }
                    else {
                        false
                    }}
                mPlayer.setDataSource("""${this.cacheDir}/${audioFile[0]}""")
                mPlayer.prepare()
                mPlayer.start()
            }
            catch (e: IOException) {
                Toast.makeText(this, this.resources.getString(R.string.permission_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    fun audioFileIntoByte() : ByteArray {
        val fis = File("""${this.cacheDir}/audiorecord.3gp""").inputStream()
        val bos = ByteArrayOutputStream()
        val b : ByteArray = ByteArray(1024)
        var bytesRead = fis.read(b)
        while (bytesRead != -1) {
            bos.write(b, 0, bytesRead);
            bytesRead = fis.read(b)
        }
        return bos.toByteArray();
    }

    fun imageIntoByte() : ByteArray {
        var bos: ByteArrayOutputStream? = ByteArrayOutputStream();
        this.imgWord!!.compress(Bitmap.CompressFormat.JPEG, 10, bos);
        return bos!!.toByteArray()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.menu_new_word, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId == R.id.action_add_word) {
            this.word!!.headword = this.headwordField!!.text.toString()
            this.word!!.sound = if (this.soundSet != true ) null else this.audioFileIntoByte()
            this.word!!.image = if (this.imgWord == null) null else this.imageIntoByte()
            this.word!!.note = this.noteField!!.text.toString()
            //println(this.word!!)
            if (this.word!!.update() > 0) {
                println("OK")
            }
            else {
                println("KO")
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private val RESULT_LOAD_IMG = 1
        private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2
        private val FILE_NAME_SOUND = "audiorecord"
        private val FILE_NAME_EXTENSION = "3gp"
    }
}