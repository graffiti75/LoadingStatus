package br.android.cericatto.loadingstatus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.android.cericatto.loadingstatus.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {

	private lateinit var binding: ActivityDetailBinding

	private var fileName = ""
	private var status = ""

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityDetailBinding.inflate(layoutInflater)
		setContentView(binding.root)

		initButtonListeners()
		initExtras()
	}

	private fun initButtonListeners() {
		binding.okButton.setOnClickListener {
			finish()
		}
	}

	private fun initExtras() {
		binding.apply {
			fileName = intent.getStringExtra(MainActivity.FILE_NAME).toString()
			status = intent.getStringExtra(MainActivity.STATUS).toString()
			fileNameTextView.text = fileName
			statusTextView.text = status

			if (status == getString(R.string.status_failed)) {
				statusTextView.setTextColor(getColor(R.color.red_500))
			} else {
//				statusTextView.setTextColor(null)
			}
		}
	}
}