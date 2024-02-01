package com.example.tic_tac_toe

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tic_tac_toe.GameActivity.Companion.PREF_GAME_FIELD
import com.example.tic_tac_toe.GameActivity.Companion.PREF_TIME
import com.example.tic_tac_toe.databinding.ActivityMainBinding
import com.example.tic_tac_toe.model.InfoGame

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }

        launchSettingsActivity()
        launchGameActivity()
        launchContinueActivity()

    }

    private fun launchContinueActivity() {
        binding.buttonContinue.setOnClickListener {
            val data = getInfoGame()
            val intent = Intent(this, GameActivity::class.java).apply {
                putExtra(EXTRA_TIME, data.time)
                putExtra(EXTRA_GAME_FIELD, data.gameField)
            }
            startActivity(intent)
        }
    }

    private fun launchSettingsActivity() {
        binding.buttonSettings.setOnClickListener {
            val intent = SettingsActivity.newIntent(this)
            startActivity(intent)
        }
    }

    private fun launchGameActivity() {
        binding.buttonNewGame.setOnClickListener {
            val intent = GameActivity.newIntent(this)
            startActivity(intent)
        }

    }

    private fun getInfoGame() : InfoGame {
        with(getSharedPreferences("game", MODE_PRIVATE)) {
            val time = getLong(PREF_TIME, 0L)
            val gameField = getString(PREF_GAME_FIELD, "")

            return if (gameField != null) {
                InfoGame(time, gameField)
            } else {
                InfoGame(0L, "")
            }
        }
    }
    companion object {
        const val EXTRA_TIME = "game.TIME"
        const val EXTRA_GAME_FIELD = "game.GAME_FIELD"
    }
}