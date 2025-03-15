package com.example.dressit.ui.post

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.dressit.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DateRangePickerDialog(
    context: Context,
    private val post: com.example.dressit.data.model.Post,
    private val onDateRangeSelected: (Long, Long, String) -> Unit
) : Dialog(context) {

    private lateinit var calendarView: CalendarView
    private lateinit var startDateText: TextView
    private lateinit var endDateText: TextView
    private lateinit var notesInput: TextView
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button

    private var startDate: Long = 0
    private var endDate: Long = 0
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    // משתנה שיציין האם נבחר כבר תאריך התחלה
    private var isStartDateSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_date_range_picker, null)
        setContentView(view)
        
        // הדפסת לוג למיקום הפוסט
        Log.d("DateRangePickerDialog", "Post ${post.id} location: Lat=${post.latitude}, Long=${post.longitude}")
        
        // הגדרת כותרת הדיאלוג
        val titleText = view.findViewById<TextView>(R.id.titleText)
        titleText.text = "בחר תאריכי השכרה עבור: ${post.title}"
        
        // הגדרת מחיר השכרה
        val priceText = view.findViewById<TextView>(R.id.priceText)
        priceText.text = "מחיר השכרה: ${post.rentalPrice} ${post.currency}"
        
        // אתחול רכיבי הממשק
        calendarView = view.findViewById(R.id.calendarView)
        startDateText = view.findViewById(R.id.startDateText)
        endDateText = view.findViewById(R.id.endDateText)
        notesInput = view.findViewById(R.id.notesInput)
        confirmButton = view.findViewById(R.id.confirmButton)
        cancelButton = view.findViewById(R.id.cancelButton)
        
        // איפוס תאריכי התחלה וסיום
        startDate = 0
        endDate = 0
        isStartDateSelected = false
        updateStartDateText()
        updateEndDateText()
        
        // הגדרת מאזין ללחיצה על לוח השנה
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth)
            val selectedDate = selectedCalendar.timeInMillis
            
            // בדיקה שהתאריך לא בעבר
            if (selectedDate < System.currentTimeMillis() - 24 * 60 * 60 * 1000) {
                Toast.makeText(context, "לא ניתן לבחור תאריך בעבר", Toast.LENGTH_SHORT).show()
                return@setOnDateChangeListener
            }
            
            // אם תאריך התחלה לא נבחר עדיין, בחר תאריך התחלה
            if (!isStartDateSelected) {
                startDate = selectedDate
                isStartDateSelected = true
                updateStartDateText()
                Toast.makeText(context, "תאריך התחלה נבחר. כעת בחר תאריך סיום", Toast.LENGTH_SHORT).show()
            } 
            // אחרת, בחר תאריך סיום
            else {
                // וודא שתאריך הסיום לא מוקדם מתאריך ההתחלה
                if (selectedDate < startDate) {
                    Toast.makeText(context, "תאריך הסיום לא יכול להיות לפני תאריך ההתחלה", Toast.LENGTH_SHORT).show()
                    return@setOnDateChangeListener
                }
                
                endDate = selectedDate
                updateEndDateText()
            }
        }
        
        // הגדרת מאזין ללחיצה על כפתור האישור
        confirmButton.setOnClickListener {
            if (startDate == 0L || endDate == 0L) {
                Toast.makeText(context, "יש לבחור תאריך התחלה ותאריך סיום", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val notes = notesInput.text.toString().trim()
            onDateRangeSelected(startDate, endDate, notes)
            dismiss()
        }
        
        // הגדרת מאזין ללחיצה על כפתור הביטול
        cancelButton.setOnClickListener {
            dismiss()
        }
    }
    
    private fun updateStartDateText() {
        if (startDate != 0L) {
            startDateText.text = "תאריך התחלה: ${dateFormat.format(Date(startDate))}"
            startDateText.setOnClickListener {
                // איפוס הבחירה והתחלה מחדש
                startDate = 0
                endDate = 0
                isStartDateSelected = false
                updateStartDateText()
                updateEndDateText()
                Toast.makeText(context, "הבחירה אופסה. בחר תאריך התחלה מחדש", Toast.LENGTH_SHORT).show()
            }
        } else {
            startDateText.text = "תאריך התחלה: לא נבחר"
            startDateText.setOnClickListener(null)
        }
    }
    
    private fun updateEndDateText() {
        if (endDate != 0L) {
            endDateText.text = "תאריך סיום: ${dateFormat.format(Date(endDate))}"
            endDateText.setOnClickListener {
                // איפוס רק של תאריך הסיום
                endDate = 0
                isStartDateSelected = true  // משאיר את מצב הבחירה בתאריך התחלה
                updateEndDateText()
                Toast.makeText(context, "תאריך סיום אופס. בחר תאריך סיום חדש", Toast.LENGTH_SHORT).show()
            }
        } else {
            endDateText.text = "תאריך סיום: לא נבחר"
            endDateText.setOnClickListener(null)
        }
    }
} 