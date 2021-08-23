package com.bignerdranch.android.geomain.geoquiz

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

private const val TAG = "CheatActivity"

private const val EXTRA_ANSWER_IS_TRUE =
    "com.bignerdranch.android.geoquiz.answer_is_true"
internal const val EXTRA_ANSWER_SHOWN =
    "com.bignerdranch.android.geoquiz.answer_is_shown"

class CheatActivity : AppCompatActivity() {

    private lateinit var answerTextView: TextView
    private lateinit var apiTextView: TextView
    private lateinit var showAnswerButton: Button
    private var answerIsTrue = false
    private var isAnswerShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cheat)

        answerIsTrue = intent.getBooleanExtra(EXTRA_ANSWER_IS_TRUE, false)
        answerTextView = findViewById(R.id.answer_text_view)
        apiTextView = findViewById(R.id.api_level_text_view)
        apiTextView.text = getString(R.string.api_level, Build.VERSION.SDK_INT)
        showAnswerButton = findViewById(R.id.show_answer_button)
        showAnswerButton.setOnClickListener {
            val answerText = when {
                answerIsTrue -> R.string.true_button
                else -> R.string.false_button
            }
            answerTextView.setText(answerText)
            setAnswerShownResult(true)
            Log.d(TAG, "OnClickListener: isAnswerShown=$isAnswerShown");
        }
        if (savedInstanceState != null) {
            setAnswerShownResult(savedInstanceState.getBoolean(EXTRA_ANSWER_SHOWN, false))
            Log.d(TAG, "onCreate: isAnswerShown=$isAnswerShown")
        }
    }

    private fun setAnswerShownResult(isAnswerShown: Boolean) {
        this.isAnswerShown = isAnswerShown
        val data = Intent().apply {
            putExtra(EXTRA_ANSWER_SHOWN, isAnswerShown)
        }
        setResult(Activity.RESULT_OK, data)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXTRA_ANSWER_SHOWN, isAnswerShown)
    }

    companion object {
        fun newIntent(packageContext: Context, answerIsTrue: Boolean): Intent {
            return Intent(packageContext, CheatActivity::class.java).apply {
                putExtra(EXTRA_ANSWER_IS_TRUE, answerIsTrue)
            }
        }
    }
}