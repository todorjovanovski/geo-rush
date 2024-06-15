package mk.ukim.finki.speed_geo

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import mk.ukim.finki.speed_geo.databinding.ActivityGameBinding
import java.util.Locale

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding

    private var gameModel: GameModel? = null

    private lateinit var timer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GameData.fetchGameModel()

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
        if (gameModel!!.gameId == "-1") {
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
            timer.cancel()
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

    fun calculateScore(fieldsMap: Map<String, String>): Int {
        var score = 0
        for (field in gameModel?.fields!!) {
            val player1Field = fieldsMap[field]

            if (field == "country" && player1Field == "test")
                score += 10;
            if (field == "city" && player1Field == "test")
                score += 5;
        }
        return score;
    }

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
        gameModel?.apply {
            updateGameData(
                GameModel(
                    gameId = gameId,
                    gameStatus = GameStatus.CheckWINNER,
                    letter = letter,
                    player1Fields = player1Fields,
                    player2Fields = player2Fields,
                    player1Score = calculateScore(player1Fields),
                    player2Score = calculateScore(player2Fields),
                    player1Id = player1Id,
                    player2Id = player2Id,
                )
            )
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