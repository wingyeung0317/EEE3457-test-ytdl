package hk.edu.vtc.stu.eee3457_test_230251805

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import androidx.navigation.ui.AppBarConfiguration
import hk.edu.vtc.stu.eee3457_test_230251805.databinding.ActivityInchToCmBinding

class InchToCmActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityInchToCmBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInchToCmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val return_to_main = findViewById<ImageView>(R.id.return_to_main)
        return_to_main.setOnClickListener {
            startActivity(Intent(this@InchToCmActivity, MainActivity::class.java))
        }

        val edit1 = findViewById<EditText>(R.id.editCm)
        val edit2 = findViewById<EditText>(R.id.editInch)
        val checkf = findViewById<CheckBox>(R.id.checkBox)


        fun floatFormat(bool: Boolean, value: Double): String{
            Log.d("TAG", "floatFormat: $bool")
            if(bool == true){
                return String.format("%.1f", value)
            }else{
                return value.toString()
            }
        }

        fun cm_to_inch(value:Float){
            var inch = value / 2.54
            edit2.setText(floatFormat(checkf.isChecked, inch))
        }

        fun inch_to_cm(value:Float){
            var cm = value * 2.54
            edit1.setText(floatFormat(checkf.isChecked, cm))
        }

        fun onInput(type:String, value:Float){
            when(type){
                "cm" -> cm_to_inch(value)
                "inch" -> inch_to_cm(value)
            }
        }

        edit1.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) {
                try {
                    onInput("cm", edit1.text.toString().toFloat())
                }catch (e:Exception){
                    edit2.setText("")
                }
                return@OnKeyListener true
            }
            false
        })
        edit2.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) {
                try {
                    onInput("inch", edit2.text.toString().toFloat())
                }catch (e:Exception){
                    edit1.setText("")
                }
                return@OnKeyListener true
            }
            false
        })
    }
}