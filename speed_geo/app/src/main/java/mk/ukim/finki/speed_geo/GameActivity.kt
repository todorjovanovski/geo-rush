package mk.ukim.finki.speed_geo

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mk.ukim.finki.speed_geo.data.FieldsDao
import mk.ukim.finki.speed_geo.data.FieldsDatabase
import mk.ukim.finki.speed_geo.databinding.ActivityGameBinding
import mk.ukim.finki.speed_geo.domain.model.fields.City
import okhttp3.internal.wait
import java.util.Locale

class GameActivity : AppCompatActivity() {

    private lateinit var fieldsDao: FieldsDao

    private lateinit var binding: ActivityGameBinding

    private var gameModel: GameModel? = null

    private lateinit var timer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GameData.fetchGameModel()

        val db = FieldsDatabase.getDatabase(this)
        fieldsDao = db.fieldsDao()


        binding.startGameBtn.setOnClickListener {
            startGame()
        }

        binding.finishGameBtn.setOnClickListener {
            finishGame()
        }

        GameData.gameModel.observe(this) {
            gameModel = it
            setUI()
        }
    }

    fun setUI() {
        gameModel?.apply {

            binding.startGameBtn.visibility = View.VISIBLE
            binding.finishGameBtn.visibility = View.INVISIBLE

            binding.gameStatusTv.text =
                when (gameStatus) {
                    GameStatus.CREATED -> {
                        binding.startGameBtn.visibility = View.INVISIBLE
                        "Game ID: $gameId"
                    }

                    GameStatus.JOINED -> {
                        "Click on start game"
                    }

                    GameStatus.InPROGRESS -> {
                        binding.startGameBtn.visibility = View.INVISIBLE
                        binding.finishGameBtn.visibility = View.VISIBLE
                        binding.letterTv.text = gameModel?.letter.toString()
                        gameModel?.timeInSeconds?.let { startTimer(it) }
                        "Game is in progress"
                    }

                    GameStatus.PLAYER1DATA -> {
                        if (player1Id == GameData.myID) {
                            setData(player1Id)
                            timer.cancel()
                        }
                        "And the winner is..."
                    }

                    GameStatus.PLAYER2DATA -> {
                        if (player2Id == GameData.myID) {
                            timer.cancel()
                            setData(player2Id)
                        }
                        "And the winner is..."
                    }

                    GameStatus.CALCULATING -> {
                        if (player1Id == GameData.myID) {
                            setScores()
                        }
                        "And the winner is..."
                    }

                    GameStatus.CheckWINNER -> {
                        if (player1Id == GameData.myID) {
                            setWinner()
                        }
                        "And the winner is..."
                    }

                    GameStatus.FINISHED -> {
                        if (gameId != "-1") {
                            if (winner.isNotEmpty()) "$winner WON"
                            else "DRAW"
                        } else "You won $player1Score points"
                    }
                }
        }
    }

    fun startGame() {
        gameModel?.apply {
            updateGameData(
                GameModel(
                    gameId = gameId,
                    gameStatus = GameStatus.InPROGRESS,
                    letter = getRandomLetter().toString(),
                    player1Id = player1Id,
                    player2Id = player2Id
                )
            )
        }
//        binding.letterTv.text = gameModel?.letter.toString()
//        gameModel?.timeInSeconds?.let { startTimer(it) }
    }

    fun updateGameData(model: GameModel) {
        GameData.saveGameModel(model)
    }

    fun finishGame() {
//        if (gameModel!!.gameId == "-1") {
//            gameModel?.apply {
//                updateGameData(
//                    GameModel(
//                        gameId = gameId,
//                        gameStatus = GameStatus.FINISHED,
//                        letter = letter,
//                        player1Score = calculateScore(getBindings())
//                    )
//                )
//            }
//            timer.cancel()
//            return
//        }
        gameModel?.apply {
            updateGameData(
                GameModel(
                    gameId = gameId,
                    gameStatus = GameStatus.PLAYER1DATA,
                    letter = letter,
                    player1Fields = player1Fields,
                    player2Fields = player2Fields,
                    player1Id = player1Id,
                    player2Id = player2Id
                )
            )
        }
    }

    fun getRandomLetter(): Char {
        val alphabet = ('A'..'Z').toList()
        return alphabet.shuffled().first()
    }

    fun startTimer(timeInSeconds: Int) {
        timer = object : CountDownTimer(18 * 1000L, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                val minutes = ((millisUntilFinished / 1000) % 3600) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeFormatted =
                    String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                binding.timerTv.text = timeFormatted
            }

            @SuppressLint("SetTextI18n")
            override fun onFinish() {
                binding.timerTv.text = "Time is up!"
                finishGame()
            }
        }.start()
    }

    suspend fun calculateScore(fieldsMap: Map<String, String>): Int {
        var score = 0
        for (field in gameModel?.fields!!) {
            val player1Field = fieldsMap[field]
            var value = false

            value = when (field) {
                "country" -> withContext(Dispatchers.IO) {
                    fieldsDao.getCountryByName(player1Field!!) != null
                }
                "city" -> withContext(Dispatchers.IO) {
                    fieldsDao.getCityByName(player1Field!!) != null
                }
                "river" -> withContext(Dispatchers.IO) {
                    fieldsDao.getRiverByName(player1Field!!) != null
                }
                "sea" -> withContext(Dispatchers.IO) {
                    fieldsDao.getSeaByName(player1Field!!) != null
                }
                "mountain" -> withContext(Dispatchers.IO) {
                    fieldsDao.getMountainByName(player1Field!!) != null
                }
                "plant" -> withContext(Dispatchers.IO) {
                    fieldsDao.getPlantByName(player1Field!!) != null
                }
                "animal" -> withContext(Dispatchers.IO) {
                    fieldsDao.getAnimalByName(player1Field!!) != null
                }
                else -> false
            }

            if (value)
                score += 10
        }
        return score
    }


//    fun calculateScoreTEST(): List<Int> {
//        val player1Fields = gameModel!!.player1Fields
//        val player2Fields = gameModel!!.player2Fields
//        var player1Score = 0
//        var player2Score = 0
//
//        for (field in gameModel!!.fields) {
//            val player1Field = player1Fields[field]
//            val player2Field = player2Fields[field]
//            val isPlayer1FieldValid = checkFieldValidity(field, player1Field)
//            val isPlayer2FieldValid = checkFieldValidity(field, player2Field)
//
//            when {
//                isPlayer1FieldValid && isPlayer2FieldValid -> {
//                    player1Score += 10
//                    player2Score += 10
//                }
//                isPlayer1FieldValid && !isPlayer2FieldValid -> {
//                    player1Score += 20
//                }
//                !isPlayer1FieldValid && isPlayer2FieldValid -> {
//                    player2Score += 20
//                }
//            }
//        }
//        return listOf(player1Score, player2Score)
//    }

//    private fun checkFieldValidity(fieldType: String, fieldValue: String?): Boolean {
//        // Implement the logic to check if the field value is valid
//        var bool = false
//        lifecycleScope.launch(Dispatchers.IO) {
//            if (fieldType == "country" && fieldsDao.getCountryByName(fieldValue ?: "") != null)
//                bool = true
//            if (fieldType == "city" && fieldsDao.getCityByName(fieldValue ?: "") != null)
//                bool = true
//            if (fieldType == "river" && fieldsDao.getRiverByName(fieldValue ?: "") != null)
//                bool = true
//            if (fieldType == "sea" && fieldsDao.getSeaByName(fieldValue ?: "") != null)
//                bool = true
//            if (fieldType == "mountain" && fieldsDao.getMountainByName(fieldValue?: "") != null)
//                bool = true
//            if (fieldType == "plant" && fieldsDao.getPlantByName(fieldValue ?: "") != null)
//                bool = true
//            if (fieldType == "animal" && fieldsDao.getAnimalByName(fieldValue ?: "") != null)
//                bool = true
//        }
//        return bool
//    }

    fun checkWinner(model: GameModel): String {
        if (model.player1Score > model.player2Score)
            return model.player1Id
        else if (model.player1Score < model.player2Score)
            return model.player2Id
        return ""
    }

    fun setData(playerId : String) {
        val inputs = getBindings()
        gameModel?.apply {
            updateGameData(
                GameModel(
                    gameId = gameId,
                    gameStatus = if (playerId == player1Id) GameStatus.PLAYER2DATA else GameStatus.CALCULATING,
                    letter = letter,
                    player1Fields = if (playerId == player1Id) inputs else player1Fields,
                    player2Fields = if (playerId == player2Id) inputs else player2Fields,
                    player1Id = player1Id,
                    player2Id = player2Id
                )
            )
        }

    }

    fun setScores() {
        lifecycleScope.launch {
            val player1Score = calculateScore(gameModel!!.player1Fields)
            val player2Score = calculateScore(gameModel!!.player2Fields)

            gameModel?.apply {
                updateGameData(
                    GameModel(
                        gameId = gameId,
                        gameStatus = GameStatus.CheckWINNER,
                        letter = letter,
                        player1Fields = player1Fields,
                        player2Fields = player2Fields,
                        player1Score = player1Score,
                        player2Score = player2Score,
                        player1Id = player1Id,
                        player2Id = player2Id,
                    )
                )
            }
        }
    }


    fun setWinner() {
        gameModel?.apply {
            updateGameData(
                GameModel(
                    gameId = gameId,
                    gameStatus = GameStatus.FINISHED,
                    letter = letter,
                    player1Fields = player1Fields,
                    player2Fields = player2Fields,
                    player1Score = player1Score,
                    player2Score = player2Score,
                    player1Id = player1Id,
                    player2Id = player2Id,
                    winner = checkWinner(this)
                )
            )
        }
    }

    fun getBindings() : Map<String, String> {
        return mapOf(
            "country" to binding.countryInput.text.toString(),
            "city" to binding.cityInput.text.toString(),
            "river" to binding.riverInput.text.toString(),
            "sea" to binding.seaInput.text.toString(),
            "mountain" to binding.mountainInput.text.toString(),
            "plant" to binding.plantInput.text.toString(),
            "animal" to binding.animalInput.text.toString()
        )
    }
}