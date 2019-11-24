package de.markherrmann.javauno.service;

import de.markherrmann.javauno.data.fixed.Card;
import de.markherrmann.javauno.data.fixed.Deck;
import de.markherrmann.javauno.data.state.UnoState;
import de.markherrmann.javauno.data.state.component.Game;
import de.markherrmann.javauno.data.state.component.GameLifecycle;
import de.markherrmann.javauno.data.state.component.Player;
import de.markherrmann.javauno.data.state.component.TurnState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Stack;

@Service
public class GameService {

    private final HousekeepingService housekeepingService;

    private final Logger logger = LoggerFactory.getLogger(GameService.class);

    @Autowired
    public GameService(HousekeepingService housekeepingService) {
        this.housekeepingService = housekeepingService;
    }

    public String createGame(){
        housekeepingService.removeOldGames();
        Game game = new Game();
        UnoState.putGame(game);
        housekeepingService.updateLastAction(game);
        logger.info("Created new game with uuid " + game.getUuid());
        return game.getUuid();
    }

    public void startGame(String gameUuid) throws IllegalArgumentException, IllegalStateException {
        Game game = getGame(gameUuid);
        synchronized (game) {
            if (isGameInLifecycle(game, GameLifecycle.RUNNING)) {
                logger.error("Current round is not finished. New round can not be started yet. Game: " + gameUuid);
                throw new IllegalStateException("Current round is not finished. New round can not be started yet.");
            }
            if (game.getPlayers().size() < 2) {
                logger.error("There are not enough players in the game. Game: " + gameUuid);
                throw new IllegalStateException("There are not enough players in the game.");
            }
            resetGame(game);
            Stack<Card> deck = Deck.getShuffled();
            game.getDiscardPile().push(deck.pop());
            giveCards(game.getPlayers(), deck);
            game.getDrawPile().addAll(deck);
            game.setGameLifecycle(GameLifecycle.RUNNING);
            housekeepingService.updateLastAction(game);
            logger.info("Started new round. Game: " + game.getUuid());
        }
    }

    private void resetGame(Game game){
        for(Player player : game.getPlayers()){
            player.clearCards();
        }
        game.getDrawPile().clear();
        game.getDiscardPile().clear();
        game.setDesiredColor(null);
        resetPlayers(game);
        if(game.isReversed()){
            game.toggleReversed();
        }
        game.setTurnState(TurnState.PUT_OR_DRAW);
    }

    private void resetPlayers(Game game){
        for(Player player : game.getPlayers()){
            player.setUnoSaid(false);
        }
    }

    private void giveCards(List<Player> playerList, Stack<Card> deck){
        for(int cards = 0; cards < 7; cards++){
            for(Player player : playerList){
                player.addCard(deck.pop());
            }
        }
    }

    Game getGame(String gameUuid) throws IllegalArgumentException {
        return UnoState.getGame(gameUuid);
    }

    boolean isGameInLifecycle(Game game, GameLifecycle gameLifecycle){
        return game.getGameLifecycle().equals(gameLifecycle);
    }
}
