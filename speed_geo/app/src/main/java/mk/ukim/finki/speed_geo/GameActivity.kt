package mk.ukim.finki.speed_geo

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mk.ukim.finki.speed_geo.data.FieldsDao
import mk.ukim.finki.speed_geo.data.FieldsDatabase
import mk.ukim.finki.speed_geo.databinding.ActivityGameBinding
import java.util.Locale

class GameActivity : AppCompatActivity() {

    private lateinit var fieldsDao: FieldsDao
    private lateinit var binding: ActivityGameBinding
    private var gameModel: GameModel? = null
    private lateinit var timer: CountDownTimer
    private var isTimerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GameData.fetchGameModel()
        val db = FieldsDatabase.getDatabase(this)
        fieldsDao = db.fieldsDao()

        binding.finishGameBtn.visibility = View.INVISIBLE

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
            binding.gameStatusTv.text =
                when (gameStatus) {
                    GameStatus.CREATED -> {
                        binding.startGameBtn.visibility = View.INVISIBLE
                        "Game ID: $gameId"
                    }

                    GameStatus.JOINED -> {
                        binding.startGameBtn.visibility = View.VISIBLE
                        "Click on start game"
                    }

                    GameStatus.InPROGRESS -> {
                        clearUi()
                        binding.startGameBtn.visibility = View.INVISIBLE
                        binding.finishGameBtn.visibility = View.VISIBLE
                        binding.letterTv.text = gameModel?.letter.toString()
                        gameModel?.timeInSeconds?.let { startTimer(it) }
                        "Game is in progress"
                    }

                    GameStatus.PLAYER1DATA -> {
                        cancelTimer()
                        binding.finishGameBtn.visibility = View.INVISIBLE
                        if (player1Id == GameData.myID) {
                            setData(player1Id)
                        }
                        "And the winner is..."
                    }

                    GameStatus.PLAYER2DATA -> {
                        if (player2Id == GameData.myID) {
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
                        binding.finishGameBtn.visibility = View.INVISIBLE
                        binding.startGameBtn.visibility = View.VISIBLE
                        var resultMessage = ""
                        if (gameId != "-1") {
                            if (winner.isNotEmpty()) {
                                if (GameData.myID != winner) {
                                    resultMessage += "$winner WON"
                                    resultMessage += if (GameData.myID == player1Id)
                                        "\nYou got $player1Score points"
                                    else
                                        "\nYou got $player2Score points"
                                } else {
                                    resultMessage += "YOU WON"
                                    resultMessage += if (GameData.myID == player1Id)
                                        "\nYou got $player1Score points"
                                    else
                                        "\nYou got $player2Score points"
                                }
                            } else resultMessage = "DRAW"
                        } else resultMessage = "You won $player1Score points"
                        resultMessage
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
    }

    fun updateGameData(model: GameModel) {
        GameData.saveGameModel(model)
    }

    fun finishGame() {
        if (gameModel!!.gameId == "-1") {
            lifecycleScope.launch {
                gameModel?.apply {
                    updateGameData(
                        GameModel(
                            gameId = gameId,
                            gameStatus = GameStatus.FINISHED,
                            letter = letter,
                            player1Score = calculateScore(getBindings())
                        )
                    )
                }
                cancelTimer()
            }
            return
        }
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
        if (isTimerRunning) {
            cancelTimer()
        }
        isTimerRunning = true
        timer = object : CountDownTimer(timeInSeconds * 1000L, 1000) {
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

    fun cancelTimer() {
        if (::timer.isInitialized) {
            timer.cancel()
        }
        isTimerRunning = false
    }

    suspend fun calculateScore(fieldsMap: Map<String, String>): Int {
        var score = 0
        for (field in gameModel!!.fields) {
            val player1Field = fieldsMap[field]

            val value = when (field) {
                "country" -> withContext(Dispatchers.IO) {
                    val country = fieldsDao.getCountryByName(player1Field!!)
                    country != null && country.countryName.first().toString() == gameModel!!.letter
                }
                "city" -> withContext(Dispatchers.IO) {
                    val city = fieldsDao.getCityByName(player1Field!!)
                    city != null && city.cityName.first().toString() == gameModel!!.letter
                }
                "river" -> withContext(Dispatchers.IO) {
                    val river = fieldsDao.getRiverByName(player1Field!!)
                    river != null && river.riverName.first().toString() == gameModel!!.letter
                }
                "sea" -> withContext(Dispatchers.IO) {
                    val sea = fieldsDao.getSeaByName(player1Field!!)
                    sea != null && sea.seaName.first().toString() == gameModel!!.letter
                }
                "mountain" -> withContext(Dispatchers.IO) {
                    val mountain = fieldsDao.getMountainByName(player1Field!!)
                    mountain != null && mountain.mountainName.first().toString() == gameModel!!.letter
                }
                "plant" -> withContext(Dispatchers.IO) {
                    val plant = fieldsDao.getPlantByName(player1Field!!)
                    plant != null && plant.plantName.first().toString() == gameModel!!.letter
                }
                "animal" -> withContext(Dispatchers.IO) {
                    val animal = fieldsDao.getAnimalByName(player1Field!!)
                    animal != null && animal.animalName.first().toString() == gameModel!!.letter
                }
                else -> false
            }

            if (value) score += 10
        }
        return score
    }

    fun checkWinner(model: GameModel): String {
        return when {
            model.player1Score > model.player2Score -> model.player1Id
            model.player1Score < model.player2Score -> model.player2Id
            else -> ""
        }
    }

    fun setData(playerId: String) {
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

    fun getBindings(): Map<String, String> {
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

    fun clearUi() {
        binding.countryInput.text.clear()
        binding.cityInput.text.clear()
        binding.riverInput.text.clear()
        binding.seaInput.text.clear()
        binding.mountainInput.text.clear()
        binding.plantInput.text.clear()
        binding.animalInput.text.clear()
    }
}
