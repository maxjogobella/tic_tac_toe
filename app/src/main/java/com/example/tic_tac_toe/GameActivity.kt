package com.example.tic_tac_toe

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.SystemClock
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tic_tac_toe.util.Constants.PREF_LVL
import com.example.tic_tac_toe.util.Constants.PREF_RULES
import com.example.tic_tac_toe.util.Constants.PREF_SOUND_VALUE
import com.example.tic_tac_toe.MainActivity.Companion.EXTRA_GAME_FIELD
import com.example.tic_tac_toe.MainActivity.Companion.EXTRA_TIME
import com.example.tic_tac_toe.databinding.ActivityGameBinding
import com.example.tic_tac_toe.model.CellGameField
import com.example.tic_tac_toe.model.SettingsInfo
import com.example.tic_tac_toe.util.Constants


class GameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameBinding
    private lateinit var gameField: Array<Array<String>>
    private lateinit var mediaPlayer: MediaPlayer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGameBinding.inflate(layoutInflater)

        binding.close.setOnClickListener {
            onBackPressed()
        }

        binding.menu.setOnClickListener {
            showPopUpMenu()
        }

        binding.cell00.setOnClickListener {
            makeStepToUser(0, 0)
        }

        binding.cell01.setOnClickListener {
            makeStepToUser(0, 1)
        }

        binding.cell02.setOnClickListener {
            makeStepToUser(0, 2)
        }

        binding.cell10.setOnClickListener {
            makeStepToUser(1, 0)
        }

        binding.cell11.setOnClickListener {
            makeStepToUser(1, 1)
        }

        binding.cell12.setOnClickListener {
            makeStepToUser(1, 2)
        }

        binding.cell20.setOnClickListener {
            makeStepToUser(2, 0)
        }

        binding.cell21.setOnClickListener {
            makeStepToUser(2, 1)
        }

        binding.cell22.setOnClickListener {
            makeStepToUser(2, 2)
        }

        setContentView(binding.root)

        val time = intent.getLongExtra(EXTRA_TIME, 0L)
        val gameField = intent.getStringExtra(EXTRA_GAME_FIELD)

        if (gameField != null && time != 0L && gameField != "") {
            restartGame(time, gameField)
        } else {
            initGameField()
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.music)
        mediaPlayer.isLooping = true
        val settingsInfo = getCurrentSettings()
        setVolumeMediaPlayer(settingsInfo.soundValue)

        binding.chronometer.start()
        mediaPlayer.start()
    }


    private fun setVolumeMediaPlayer(soundValue: Int) {
        val volume = soundValue / 100.0
        mediaPlayer.setVolume(volume.toFloat(), volume.toFloat())
    }

    private fun restartGame(time: Long, gameField: String) {
        binding.chronometer.base = SystemClock.elapsedRealtime() - time

        this.gameField = arrayOf()

        val rows = gameField.split("\n")

        for (row in rows) {
            val columns = row.split(";")
            this.gameField += columns.toTypedArray()
        }

        this.gameField.forEachIndexed { indexRow, columns ->
            columns.forEachIndexed { indexColumn, cell ->
                makeGameFieldUI("$indexRow$indexColumn", cell)
            }
        }
    }

    private fun convertGameFieldToString(): String {
        val tmpArray = arrayListOf<String>()
        gameField.forEach { tmpArray.add(it.joinToString(separator = ";")) }
        return tmpArray.joinToString(separator = "\n")
    }

    private fun saveGame(time: Long, gameField: String) {
        getSharedPreferences("game", MODE_PRIVATE).edit().apply {
            putLong(PREF_TIME, time)
            putString(PREF_GAME_FIELD, gameField)
            apply()
        }
    }

    private fun initGameField() {
        gameField = arrayOf()

        for (i in 0..2) {
            var array = arrayOf<String>()
            for (j in 0..2) {
                array += " "
            }
            gameField += array
        }
    }

    private fun makeStepToUser(row: Int, column: Int) {
        if (isEmptyField(row, column)) {
            makeStep(row, column, PLAYER_SYMBOL)

            if (checkGameField(row, column, PLAYER_SYMBOL)) {
                showGameStatus(STATUS_WIN_PLAYER)
            } else if (!isFilledGameField()) {
                val stepOfAI = makeStepToAI()

                if (checkGameField(stepOfAI.row, stepOfAI.column, BOT_SYMBOL)) {
                    showGameStatus(STATUS_WIN_BOT)
                } else if (isFilledGameField()) {
                    showGameStatus(STATUS_DRAW)
                }
            } else {
                showGameStatus(STATUS_DRAW)
            }
        } else {
            Toast.makeText(this, "Поле уже заполнено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeStepToAI(): CellGameField {
        val settingsInfo = getCurrentSettings()
        return when (settingsInfo.lvl) {
            0 -> makeStepOfAIEasyLvl()
            1 -> makeStepOfAIMediumLvl()
            2 -> makeStepOfAIHardLvl()
            else -> CellGameField(0, 0)
        }
    }

    private fun makeStepOfAIEasyLvl(): CellGameField {
        var randomRow = 0
        var randomColumn = 0

        do {
            randomRow = (0..2).random()
            randomColumn = (0..2).random()
        } while (!isEmptyField(randomRow, randomColumn))

        makeStep(randomRow, randomColumn, BOT_SYMBOL)

        return CellGameField(randomRow, randomColumn)
    }

    private fun makeStepOfAIMediumLvl(): CellGameField {
        var bestScore = Double.NEGATIVE_INFINITY
        var moveCell = CellGameField(0, 0)

        var board = gameField.map { it.clone() }.toTypedArray()

        board.forEachIndexed { indexRow, columns ->
            columns.forEachIndexed { indexColumn, cell ->
                if (board[indexRow][indexColumn] == " ") {
                    board[indexRow][indexColumn] = BOT_SYMBOL
                    val score = minimax(board, false)
                    board[indexRow][indexColumn] = " "
                    if (score > bestScore) {
                        bestScore = score
                        moveCell = CellGameField(indexRow, indexColumn)
                    }
                }
            }
        }

        makeStep(moveCell.row, moveCell.column, BOT_SYMBOL)

        return moveCell
    }

    private fun makeStepOfAIHardLvl(): CellGameField {
        var bestScore = Double.NEGATIVE_INFINITY
        var moveCell = CellGameField(0, 0)

        var board = gameField.map { it.clone() }.toTypedArray()

        board.forEachIndexed { indexRow, columns ->
            columns.forEachIndexed { indexColumn, cell ->
                if (board[indexRow][indexColumn] == " ") {
                    board[indexRow][indexColumn] = BOT_SYMBOL
                    val score = minimax(board, false)
                    board[indexRow][indexColumn] = " "
                    if (score > bestScore) {
                        bestScore = score
                        moveCell = CellGameField(indexRow, indexColumn)
                    }
                }
            }
        }

        makeStep(moveCell.row, moveCell.column, BOT_SYMBOL)

        return moveCell
    }

    private fun minimax(board: Array<Array<String>>, isMaximizing: Boolean): Double {
        val result = checkWinner(board)
        result?.let {
            return scores[result]!!
        }

        if (isMaximizing) {
            var bestScore = Double.NEGATIVE_INFINITY
            board.forEachIndexed { indexRow, columns ->
                columns.forEachIndexed { indexColumn, cell ->
                    if (board[indexRow][indexColumn] == " ") {
                        board[indexRow][indexColumn] = BOT_SYMBOL
                        val score = minimax(board, false)
                        board[indexRow][indexColumn] = " "
                        if (score > bestScore) {
                            bestScore = score
                        }
                    }
                }
            }
            return bestScore
        } else {
            var bestScore = Double.POSITIVE_INFINITY
            board.forEachIndexed { indexRow, columns ->
                columns.forEachIndexed { indexColumn, cell ->
                    if (board[indexRow][indexColumn] == " ") {
                        board[indexRow][indexColumn] = PLAYER_SYMBOL
                        val score = minimax(board, true)
                        board[indexRow][indexColumn] = " "
                        if (score < bestScore) {
                            bestScore = score
                        }
                    }
                }
            }
            return bestScore
        }
    }

    private fun checkWinner(board: Array<Array<String>>): Int? {
        var countRowsUser = 0
        var countRowsAI = 0
        var countLeftDiagonalUser = 0
        var countLeftDiagonalAL = 0
        var countRightDiagonalUser = 0
        var countRightDiagonalAI = 0

        board.forEachIndexed { indexRow, columns ->
            if (columns.all { it == PLAYER_SYMBOL })
                return STATUS_WIN_PLAYER
            else if (columns.all { it == BOT_SYMBOL })
                return STATUS_WIN_BOT

            countRowsUser = 0
            countRowsAI = 0

            columns.forEachIndexed { indexColumn, cell ->
                if (board[indexColumn][indexRow] == PLAYER_SYMBOL)
                    countRowsUser++
                else if (board[indexColumn][indexRow] == BOT_SYMBOL)
                    countRowsAI++

                if (indexRow == indexColumn && board[indexRow][indexColumn] == PLAYER_SYMBOL)
                    countLeftDiagonalUser++
                else if (indexRow == indexColumn && board[indexRow][indexColumn] == BOT_SYMBOL)
                    countLeftDiagonalAL++

                if (indexRow == 2 - indexColumn && board[indexRow][indexColumn] == PLAYER_SYMBOL)
                    countRightDiagonalUser++
                else if (indexRow == 2 - indexColumn && board[indexRow][indexColumn] == BOT_SYMBOL)
                    countRightDiagonalAI++
            }

            if (countRowsUser == 3 || countLeftDiagonalUser == 3 || countRightDiagonalUser == 3)
                return STATUS_WIN_PLAYER
            else if (countRowsAI == 3 || countLeftDiagonalAL == 3 || countRightDiagonalAI == 3)
                return STATUS_WIN_BOT
        }

        board.forEach {
            if (it.find { it == " " } != null)
                return null
        }

        return STATUS_DRAW
    }

    private fun getCurrentSettings() : SettingsInfo {
        this.getSharedPreferences("game", MODE_PRIVATE).apply {

            val sound = getInt(PREF_SOUND_VALUE, 100)
            val level = getInt(PREF_LVL, 1)
            val rules = getInt(PREF_RULES, 7)

            return SettingsInfo(sound, level, rules)
        }
    }

    private fun isEmptyField(row: Int, column: Int): Boolean {
        return gameField[row][column] == " "
    }

    private fun makeStep(row: Int, column: Int, symbol: String) {
        gameField[row][column] = symbol

        makeGameFieldUI("$row$column", symbol)
    }

    private fun makeGameFieldUI(position: String, symbol: String) {
        val drawable = when (symbol) {
            PLAYER_SYMBOL -> R.drawable.cross
            BOT_SYMBOL -> R.drawable.zero
            else -> return
        }

        when (position) {
            "00" -> binding.cell00.setImageResource(drawable)
            "01" -> binding.cell01.setImageResource(drawable)
            "02" -> binding.cell02.setImageResource(drawable)
            "10" -> binding.cell10.setImageResource(drawable)
            "11" -> binding.cell11.setImageResource(drawable)
            "12" -> binding.cell12.setImageResource(drawable)
            "20" -> binding.cell20.setImageResource(drawable)
            "21" -> binding.cell21.setImageResource(drawable)
            "22" -> binding.cell22.setImageResource(drawable)
        }
    }

    private fun checkGameField(x: Int, y: Int, symbol: String): Boolean {
        val n = gameField.size
        val counts = intArrayOf(0, 0, 0, 0)

        for (i in 0 until n) {
            if (gameField[x][i] == symbol) counts[0]++
            if (gameField[i][y] == symbol) counts[1]++
            if (gameField[i][i] == symbol) counts[2]++
            if (gameField[i][n - i - 1] == symbol) counts[3]++
        }

        val settings = getCurrentSettings()
        return when (settings.rules) {
            1 -> counts[0] == n
            2 -> counts[1] == n
            3 -> counts[0] == n || counts[1] == n
            4 -> counts[2] == n || counts[3] == n
            5 -> counts[0] == n || counts[2] == n || counts[3] == n
            6 -> counts[1] == n || counts[2] == n || counts[3] == n
            7 -> counts.any { it == n }
            else -> false
        }
    }

    private fun isFilledGameField(): Boolean {
        gameField.forEach { strings ->
            if (strings.find { it == " " } != null)
                return false
        }
        return true
    }

    private fun showGameStatus(status: Int) {
        val dialog = Dialog(this, R.style.Theme_TicTacToe)
        with(dialog) {
            window?.setBackgroundDrawable(ColorDrawable(Color.argb(50, 0, 0, 0)))
            setContentView(R.layout.dialog_popup_status_game)
            setCancelable(true)
        }

        val image = dialog.findViewById<ImageView>(R.id.dialogImageStatus)
        val text = dialog.findViewById<TextView>(R.id.dialogTextStatus)
        val button = dialog.findViewById<TextView>(R.id.dialogBack)

        button.setOnClickListener {
            finish()
        }

        when (status) {
            GameActivity.STATUS_WIN_BOT -> {
                image.setImageResource(R.drawable.lose)
                text.text = getString(R.string.lose)
            }

            GameActivity.STATUS_DRAW -> {
                image.setImageResource(R.drawable.draw)
                text.text = getString(R.string.draw)
            }

            GameActivity.STATUS_WIN_PLAYER -> {
                image.setImageResource(R.drawable.win_svgrepo_com)
                text.text = getString(R.string.win)
            }
        }

        dialog.show()
    }

    private fun showPopUpMenu() {
        val elapsedMils = SystemClock.elapsedRealtime() - binding.chronometer.base
        val dialog = Dialog(this, R.style.Theme_TicTacToe)
        with(dialog) {
            window?.setBackgroundDrawable(ColorDrawable(Color.argb(30, 0, 0, 0)))
            setContentView(R.layout.dialog_popup_menu)
            setCancelable(true)
        }

        val toContinue = dialog.findViewById<TextView>(R.id.dialogContinue)
        val toSettings = dialog.findViewById<TextView>(R.id.dialogSettings)
        val toExit = dialog.findViewById<TextView>(R.id.dialogSaveAndExit)

        toContinue.setOnClickListener {
            dialog.hide()
        }

        toSettings.setOnClickListener {
            dialog.hide()
            val intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, Constants.REQUEST_POP_UP_MENU)
            finish()

            val settingsInfo = getCurrentSettings()
            setVolumeMediaPlayer(settingsInfo.soundValue)
        }

        toExit.setOnClickListener {
            saveGame(elapsedMils, convertGameFieldToString())
            dialog.hide()
            finish()
        }

        dialog.show()
    }

    companion object {
        const val PREF_TIME = "extra_time"
        const val PREF_GAME_FIELD = "exta_gameFied"
        const val STATUS_WIN_PLAYER = 1
        const val STATUS_WIN_BOT = 2
        const val STATUS_DRAW = 3

        val scores = hashMapOf(
            Pair(STATUS_WIN_PLAYER, -1.0), Pair(STATUS_WIN_BOT, 1.0), Pair(STATUS_DRAW, 0.0)
        )

        const val PLAYER_SYMBOL = "X"
        const val BOT_SYMBOL = "0"

        fun newIntent(context : Context) : Intent = Intent(context, GameActivity::class.java)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.release()
    }
}



