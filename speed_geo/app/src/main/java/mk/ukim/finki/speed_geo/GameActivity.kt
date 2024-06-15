package mk.ukim.finki.speed_geo

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import mk.ukim.finki.speed_geo.databinding.ActivityGameBinding
import java.util.Locale

class GameActivity : AppCompatActivity() {

    private lateinit var binding : ActivityGameBinding

    private var gameModel : GameModel? = null

    private lateinit var timer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startGameBtn.setOnClickListener {
            startGame()
        }

        binding.finishGameBtn.setOnClickListener {
            timer.cancel()
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
                when(gameStatus) {
                    GameStatus.CREATED -> {
                        binding.startGameBtn.visibility = View.INVISIBLE
                        "Game ID :" + gameId
                    }
                    GameStatus.JOINED -> {
                        "Click on start game"
                    }
                    GameStatus.InPROGRESS -> {
                        binding.startGameBtn.visibility = View.INVISIBLE
                        binding.finishGameBtn.visibility = View.VISIBLE
                        "Game is in progress"
                    }
                    GameStatus.FINISHED -> {
                        if (gameId != "-1") {
                            if(winner.isNotEmpty()) winner + " WON"
                            else "DRAW"
                        }
                        else "You won " + player1Score + " points"
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
                    letter = getRandomLetter().toString()
                )
            )
        }
        binding.letterTv.text = gameModel?.letter.toString()
        gameModel?.timeInSeconds?.let { startTimer(it) }
    }

    fun updateGameData(model: GameModel) {
        GameData.saveGameModel(model)
    }

    fun finishGame() {
        val inputs = mapOf(
            "country" to binding.countryInput.text.toString(),
            "city" to binding.cityInput.text.toString(),
            "river" to binding.riverInput.text.toString(),
            "sea" to binding.seaInput.text.toString(),
            "mountain" to binding.mountainInput.text.toString(),
            "plant" to binding.plantInput.text.toString(),
            "animal" to binding.animalInput.text.toString()
        )
        gameModel?.apply {
            updateGameData(
                GameModel(
                    gameId = gameId,
                    gameStatus = GameStatus.FINISHED,
                    letter = letter,
                    player1Fields = inputs,
                    player1Score = calculateScore(inputs)
                )
            )
        }
    }

    fun getRandomLetter() : Char {
        val alphabet = ('A'..'Z').toList()
        return alphabet.shuffled().first()
    }

    fun startTimer(timeInSeconds : Int) {
        timer = object : CountDownTimer(timeInSeconds * 1000L, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                val minutes = ((millisUntilFinished / 1000) % 3600) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                binding.timerTv.text = timeFormatted
            }

            @SuppressLint("SetTextI18n")
            override fun onFinish() {
                binding.timerTv.text = "Time is up!"
                finishGame()
            }
        }.start()
    }

    fun calculateScore(fieldsMap : Map<String, String>) : Int {
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
}