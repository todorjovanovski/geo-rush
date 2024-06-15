package mk.ukim.finki.speed_geo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import mk.ukim.finki.speed_geo.databinding.ActivityMainBinding
import kotlin.random.Random
import kotlin.random.nextInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.playOfflineBtn.setOnClickListener {
            createOfflineGame()
        }

        binding.createGameOnlineBtn.setOnClickListener {
            createOnlineGame()
        }

        binding.joinGameOnlineBtn.setOnClickListener {
            joinOnlineGame()
        }
    }

    fun createOfflineGame() {
        GameData.saveGameModel(
            GameModel(
                gameStatus = GameStatus.JOINED
            )
        )
        startGame()
    }

    fun createOnlineGame() {
        GameData.saveGameModel(
            GameModel(
                gameId = Random.nextInt(1000 .. 9999).toString(),
                gameStatus = GameStatus.CREATED
            )
        )
        startGame()
    }

    fun joinOnlineGame() {
        val gameId = binding.gameIdInput.text.toString();
        if (gameId.isEmpty()) {
            binding.gameIdInput.error = "Please enter game ID";
            return
        }

        Firebase.firestore.collection("games")
            .document(gameId)
            .get()
            .addOnSuccessListener {
                val model = it?.toObject(GameModel::class.java)
                if (model == null) {
                    binding.gameIdInput.error = "Please enter a valid game ID"
                } else {
                    model.gameStatus = GameStatus.JOINED
                    model.player2Id = "Player 2"
                    GameData.saveGameModel(model)
                    startGame()
                }
            }
    }

    fun startGame() {
        startActivity(Intent(this, GameActivity::class.java))
    }
}