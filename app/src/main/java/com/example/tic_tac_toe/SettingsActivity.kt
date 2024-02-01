package com.example.tic_tac_toe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import com.example.tic_tac_toe.util.Constants.PREF_LVL
import com.example.tic_tac_toe.util.Constants.PREF_RULES
import com.example.tic_tac_toe.util.Constants.PREF_SOUND_VALUE
import com.example.tic_tac_toe.util.Constants.REQUEST_POP_UP_MENU
import com.example.tic_tac_toe.util.Constants.SHARED_PREF_NAME
import com.example.tic_tac_toe.databinding.ActivitySettingsBinding
import com.example.tic_tac_toe.model.SettingsInfo

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private var currentSound = 0
    private var currentLvl = 0
    private var currentRules = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }

        val data = getSettingsInfo()
        currentSound = data.soundValue
        currentLvl = data.lvl
        currentRules = data.rules

        setSwitches()
        setVisibility()
        setInfoLevel()
        binding.seekBar.progress = currentSound

        updateSwitchListener()
        setAndUpdateSeekBar()

        binding.toBack.setOnClickListener {
            updateSoundValue(currentSound)()
            updateLvl(currentLvl)()
            updateRules(currentRules)()
            onBackPressed()
            setResult(RESULT_OK)
        }

        binding.next.setOnClickListener { updateLevel(1) }
        binding.previous.setOnClickListener { updateLevel(-1) }
    }

    private fun setSwitches() {
        binding.switchVert.isChecked = currentRules and 1 != 0
        binding.switchHor.isChecked = currentRules and 2 != 0
        binding.switchDia.isChecked = currentRules and 4 != 0
    }

    private fun setVisibility() {
        binding.previous.visibility = if (currentLvl == 0) View.INVISIBLE else View.VISIBLE
        binding.next.visibility = if (currentLvl == 2) View.INVISIBLE else View.VISIBLE
    }

    private fun setInfoLevel() {
        binding.infoLevel.text = resources.getStringArray(R.array.game_level)[currentLvl]
    }

    private fun setAndUpdateSeekBar() {
        binding.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                currentSound = value
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { updateSoundValue(currentSound) }
        })
    }

    private fun updateSwitchListener() {
        binding.switchVert.setOnCheckedChangeListener { _, isChecked -> updateRules(1, isChecked) }
        binding.switchHor.setOnCheckedChangeListener { _, isChecked -> updateRules(2, isChecked) }
        binding.switchDia.setOnCheckedChangeListener { _, isChecked -> updateRules(4, isChecked) }
    }

    private fun updateRules(value: Int, isChecked: Boolean) {
        currentRules = if (isChecked) currentRules or value else currentRules and value.inv()
        updateRules(currentRules)
    }

    private fun updateLevel(delta: Int) {
        currentLvl += delta
        setVisibility()
        setInfoLevel()
        updateLvl(currentLvl)
    }

    private fun updatePreference(key: String, value: Int) {
        with(getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE).edit()) {
            putInt(key, value)
            apply()
        }
    }

    private fun getPreference(key: String, defaultValue: Int): Int {
        return getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE).getInt(key, defaultValue)
    }

    private fun updateSoundValue(value: Int): () -> Unit = {
        updatePreference(PREF_SOUND_VALUE, value)
        setResult(REQUEST_POP_UP_MENU)
    }

    private fun updateLvl(lvl: Int) : () -> Unit = {
        updatePreference(PREF_LVL, lvl)
        setResult(REQUEST_POP_UP_MENU)
    }
    private fun updateRules(rules: Int) : () -> Unit = {
        updatePreference(PREF_RULES, rules)
        setResult(REQUEST_POP_UP_MENU)
    }

    private fun getSettingsInfo(): SettingsInfo {
        val soundValue = getPreference(PREF_SOUND_VALUE, 100)
        val lvl = getPreference(PREF_LVL, 1)
        val rules = getPreference(PREF_RULES, 7)

        return SettingsInfo(soundValue, lvl, rules)
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, SettingsActivity::class.java)
    }
}