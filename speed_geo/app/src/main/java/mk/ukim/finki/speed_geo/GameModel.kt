package mk.ukim.finki.speed_geo

data class GameModel (
    var gameId : String = "-1",
    val letter: String = "",
    val timeInSeconds: Int = 180,
    val fields: List<String> = listOf("country", "city", "river", "sea", "mountain", "plant", "animal"),
    var player1Id: String = "Player 1",
    var player2Id: String = "",
    var player1Fields: Map<String, String> = emptyMap(),
    var player2Fields: Map<String, String> = emptyMap(),
    var player1Score: Int = 0,
    var player2Score: Int = 0,
    var gameStatus : GameStatus = GameStatus.CREATED,
    var winner : String = ""
)

enum class GameStatus {
    CREATED,
    JOINED,
    InPROGRESS,
    PLAYER1DATA,
    PLAYER2DATA,
    CALCULATING,
    CheckWINNER,
    FINISHED
}