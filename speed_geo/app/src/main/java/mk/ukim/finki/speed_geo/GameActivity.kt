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

    @SuppressLint("SetTextI18n")
    fun setUI() {
        gameModel?.apply {
            disableInputs()
            binding.gameStatusTv.text =
                when (gameStatus) {
                    GameStatus.CREATED -> {
                        binding.player1ScoreTv.text = "$player1Id ready"
                        binding.startGameBtn.visibility = View.INVISIBLE
                        "Game ID: $gameId"
                    }

                    GameStatus.JOINED -> {
                        clearUi()
                        binding.startGameBtn.visibility = View.VISIBLE
                        if (gameId != "-1") {
                            binding.player1ScoreTv.text = "$player1Id ready"
                            binding.player2ScoreTv.text = "$player2Id ready"
                            "You are ${GameData.myID}"
                        } else {
                            "Click on start game"
                        }
                    }

                    GameStatus.InPROGRESS -> {
                        if (gameId != "-1") {
                            binding.player1ScoreTv.text = player1Id
                            binding.player2ScoreTv.text = player2Id
                        }
                        clearUi()
                        enableInputs()
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
                        if (GameData.myID == gameModel!!.player1Id) {
                            markFields(player1FieldsValidity)
                        } else if (GameData.myID == gameModel!!.player2Id) {
                            markFields(player2FieldsValidity)
                        }
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
                            binding.player1ScoreTv.text = "$player1Id: $player1Score pts."
                            binding.player2ScoreTv.text = "$player2Id: $player2Score pts."
                            if (winner.isNotEmpty()) {
                                resultMessage += if (GameData.myID != winner) {
                                    "$winner WON"
                                } else {
                                    "YOU WON"
                                }
                            } else {
                                resultMessage = "DRAW"
                            }
                        } else resultMessage = "You won $player1Score points"
                        resultMessage
                    }
                }
        }
    }

    private fun startGame() {
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

    private fun updateGameData(model: GameModel) {
        GameData.saveGameModel(model)
    }

    fun finishGame() {
        if (gameModel!!.gameId == "-1") {
            lifecycleScope.launch {
                val (score, validity) = calculateScore(getBindings())
                gameModel?.apply {
                    updateGameData(
                        GameModel(
                            gameId = gameId,
                            gameStatus = GameStatus.FINISHED,
                            letter = letter,
                            player1Score = score
                        )
                    )
                }
                markFields(validity)
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

    private fun getRandomLetter(): Char {
        val alphabet = ('A'..'Z').toList()
        return alphabet.shuffled().first()
    }

    private fun startTimer(timeInSeconds: Int) {
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

    private fun cancelTimer() {
        if (::timer.isInitialized) {
            timer.cancel()
        }
        isTimerRunning = false
    }

    private suspend fun calculateScore(fieldsMap: Map<String, String>): Pair<Int, Map<String, Boolean>> {
        var score = 0
        val validityMap = mutableMapOf<String, Boolean>()

        for (field in gameModel!!.fields) {
            val player1Field = fieldsMap[field]

            val isValid = when (field) {
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

            if (isValid) {
                score += 10
            }
            validityMap[field] = isValid
        }
        return Pair(score, validityMap)
    }


    private fun checkWinner(model: GameModel): String {
        return when {
            model.player1Score > model.player2Score -> model.player1Id
            model.player1Score < model.player2Score -> model.player2Id
            else -> ""
        }
    }

    private fun setData(playerId: String) {
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

    private fun setScores() {
        lifecycleScope.launch {
            val (player1Score, player1Validity) = calculateScore(gameModel!!.player1Fields)
            val (player2Score, player2Validity) = calculateScore(gameModel!!.player2Fields)

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
                        player1FieldsValidity = player1Validity,
                        player2FieldsValidity = player2Validity
                    )
                )
            }
        }
    }


    private fun setWinner() {
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

    private fun getBindings(): Map<String, String> {
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

    private fun markFields(validityMap: Map<String, Boolean>) {
        validityMap.forEach { (field, isValid) ->
            when (field) {
                "country" -> binding.countryCheck.setBackgroundResource(
                    if (isValid) R.drawable.check_circle_24dp else R.drawable.cancel_24dp
                )
                "city" -> binding.cityCheck.setBackgroundResource(
                    if (isValid) R.drawable.check_circle_24dp else R.drawable.cancel_24dp
                )
                "river" -> binding.riverCheck.setBackgroundResource(
                    if (isValid) R.drawable.check_circle_24dp else R.drawable.cancel_24dp
                )
                "sea" -> binding.seaCheck.setBackgroundResource(
                    if (isValid) R.drawable.check_circle_24dp else R.drawable.cancel_24dp
                )
                "mountain" -> binding.mountainCheck.setBackgroundResource(
                    if (isValid) R.drawable.check_circle_24dp else R.drawable.cancel_24dp
                )
                "plant" -> binding.plantCheck.setBackgroundResource(
                    if (isValid) R.drawable.check_circle_24dp else R.drawable.cancel_24dp
                )
                "animal" -> binding.animalCheck.setBackgroundResource(
                    if (isValid) R.drawable.check_circle_24dp else R.drawable.cancel_24dp
                )
            }
        }
    }


    private fun clearUi() {
        binding.countryInput.text.clear()
        binding.cityInput.text.clear()
        binding.riverInput.text.clear()
        binding.seaInput.text.clear()
        binding.mountainInput.text.clear()
        binding.plantInput.text.clear()
        binding.animalInput.text.clear()

        binding.countryCheck.setBackgroundResource(0)
        binding.cityCheck.setBackgroundResource(0)
        binding.riverCheck.setBackgroundResource(0)
        binding.seaCheck.setBackgroundResource(0)
        binding.mountainCheck.setBackgroundResource(0)
        binding.plantCheck.setBackgroundResource(0)
        binding.animalCheck.setBackgroundResource(0)
    }

    private fun enableInputs() {
        binding.countryInput.isEnabled = true
        binding.cityInput.isEnabled = true
        binding.riverInput.isEnabled = true
        binding.seaInput.isEnabled = true
        binding.mountainInput.isEnabled = true
        binding.plantInput.isEnabled = true
        binding.animalInput.isEnabled = true
    }

    private fun disableInputs() {
        binding.countryInput.isEnabled = false
        binding.cityInput.isEnabled = false
        binding.riverInput.isEnabled = false
        binding.seaInput.isEnabled = false
        binding.mountainInput.isEnabled = false
        binding.plantInput.isEnabled = false
        binding.animalInput.isEnabled = false
    }

}
