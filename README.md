# GeoRush - Android speed geography game project

### Description
A fast-paced and educational Android game developed in Kotlin. This game challenges players to quickly name geographical entities starting with a given letter, competing either solo in practice mode or against others in real-time.
  
#### Practice Mode
In Practice Mode, players can test their geography knowledge at their own pace. A random letter is presented, and players must enter names for the following categories:

- Country
- City
- River
- Sea
- Mountain
- Plant
- Animal

Players have 3 minutes to complete all the fields. Each correct entry earns the player 10 points. Practice mode is perfect for sharpening your geography skills and preparing for online competition.

#### Online Mode
In Online Mode, players can compete against each other in real-time. Here's how it works:

- Create a Game: A user creates a game, generating a unique game ID.
- Join a Game: Another user can join the game by entering the game ID.
- Start Game: Both players are presented with the same random letter. They must fill in the fields as quickly as possible.
- Finish Game: Game is finished when timer ends or any player clicks finish.

### Features
- Room Local Database
- Firebase Firestore real-time connection
- Observable Live Data
- Game Model & Object

### Differentiation
The Speed Geography Game stands out as a real-time multiplayer game where players guess country, city, river, sea, mountain, plant, and animal names in English. This feature makes it accessible to a broader audience compared to other geography games of this type. Whether practicing offline or challenging opponents online, players can test their geographic knowledge swiftly and effectively with minimal latency.
