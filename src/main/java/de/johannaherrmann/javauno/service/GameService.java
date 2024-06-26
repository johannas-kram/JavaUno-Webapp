package de.johannaherrmann.javauno.service;

import de.johannaherrmann.javauno.data.fixed.Card;
import de.johannaherrmann.javauno.data.fixed.Deck;
import de.johannaherrmann.javauno.data.state.UnoState;
import de.johannaherrmann.javauno.data.state.component.*;
import de.johannaherrmann.javauno.exceptions.EmptyArgumentException;
import de.johannaherrmann.javauno.exceptions.ExceptionMessage;
import de.johannaherrmann.javauno.exceptions.IllegalArgumentException;
import de.johannaherrmann.javauno.exceptions.IllegalStateException;

import de.johannaherrmann.javauno.helper.UnoRandom;
import de.johannaherrmann.javauno.service.push.PushMessage;
import de.johannaherrmann.javauno.service.push.PushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.Stack;

@Service
public class GameService {

    private final FinalizeTurnService finalizeTurnService;
    private final GlobalStateService globalStateService;
    private final PushService pushService;
    private final TokenService tokenService;

    private static final Logger LOGGER = LoggerFactory.getLogger(GameService.class);

    @Autowired
    public GameService(FinalizeTurnService finalizeTurnService, GlobalStateService globalStateService, PushService pushService,
                       TokenService tokenService) {
        this.finalizeTurnService = finalizeTurnService;
        this.globalStateService = globalStateService;
        this.pushService = pushService;
        this.tokenService = tokenService;
    }

    public String createGame(String token) {
        globalStateService.removeOldGames();
        tokenService.checkForTokenizedGameCreate(token);
        Game game = new Game();
        UnoState.putGame(game);
        LOGGER.info("Created new game with uuid {}", game.getUuid());
        globalStateService.saveGame(game);
        return game.getUuid();
    }

    public boolean isTokenizedGameCreateFeatureEnabled(){
        return tokenService.isFeatureEnabled();
    }

    public void startGame(String gameUuid) throws IllegalArgumentException, IllegalStateException {
        Game game = getGame(gameUuid);
        synchronized (game) {
            if (isGameInLifecycle(game, GameLifecycle.RUNNING)) {
                LOGGER.error("Current round is not finished. New round can not be started yet. Game: {}", gameUuid);
                throw new IllegalStateException(ExceptionMessage.INVALID_STATE_GAME.getValue());
            }
            if (game.getPlayers().size() < 2) {
                LOGGER.error("There are not enough players in the game. Game: {}", gameUuid);
                throw new IllegalStateException(ExceptionMessage.NOT_ENOUGH_PLAYERS.getValue());
            }
            resetGame(game);
            Stack<Card> deck = Deck.getShuffled();
            game.getDiscardPile().push(deck.pop());
            giveCards(game, deck);
            game.getDrawPile().addAll(deck);
            game.setGameLifecycle(GameLifecycle.RUNNING);
            game.nextParty();
            LOGGER.info("Started new round. Game: {}", game.getUuid());
            globalStateService.saveGame(game);
            pushService.push(PushMessage.STARTED_GAME, game);
        }
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndex());
        finalizeTurnService.handleBotTurn(game, currentPlayer);
    }

    public void addMessage(String gameUuid, String playerUuid, String content){
        if(content == null || content.trim().isEmpty()){
            throw new EmptyArgumentException(ExceptionMessage.EMPTY_CHAT_MESSAGE.getValue());
        }
        content = content.trim();
        Game game = getGame(gameUuid);
        synchronized (game) {
            Player player = PlayerService.getPlayerStatic(playerUuid, game);
            String publicUuid = player.getPublicUuid();
            Message message = new Message(content, publicUuid, System.currentTimeMillis());
            game.addMessage(message);
            pushService.pushDirectly(game.getUuid(), "chat-message", publicUuid, ""+ message.getTime() ,content);
            LOGGER.info("Successfully added message. Game: {}; Player: {}; Message: {}", gameUuid, playerUuid, content);
            globalStateService.saveGame(game);
        }
    }

    public Game getGame(String gameUuid) throws IllegalArgumentException {
        return UnoState.getGame(gameUuid);
    }

    boolean isGameInLifecycle(Game game, GameLifecycle gameLifecycle){
        return game.getGameLifecycle().equals(gameLifecycle);
    }

    void stopParty(Game game){
        game.getHumans().values().forEach(e->e.setStopPartyRequested(false));
        game.resetStopPartyRequested();
        game.setGameLifecycle(GameLifecycle.SET_PLAYERS);
        game.setTurnState(TurnState.FINAL_COUNTDOWN);
    }

    void giveCards(Game game, Stack<Card> deck){
        List<Player> playerList = game.getPlayers();
        int firstCardReceiver = handleAndGetFirstCardReceiver(game, playerList);
        for(int cards = 0; cards < 7; cards++){
            for(int index = firstCardReceiver; playerList.get(index).getCards().size() == cards; index=(index+1)%playerList.size()){
                Player player = playerList.get(index);
                player.addCard(deck.pop());
            }
        }
    }

    private void resetGame(Game game){
        game.setCurrentPlayerIndex(setAndGetCurrentPlayerIndex(game));
        game.getHumans().values().forEach(e->e.setStopPartyRequested(false));
        game.resetStopPartyRequested();
        for(Player player : game.getPlayers()){
            player.clearCards();
        }
        game.getDrawPile().clear();
        game.getDiscardPile().clear();
        game.setDesiredColor(null);
        game.setSkip(false);
        game.setDrawDuties(0);
        resetPlayers(game);
        game.setTurnState(TurnState.PUT_OR_DRAW);
        if(game.isReversed()){
            game.toggleReversed();
        }
    }

    private int setAndGetCurrentPlayerIndex(Game game){
        int lastWinner = game.getLastWinner();
        if(lastWinner >= 0){
            return lastWinner;
        }
        int players = game.getPlayers().size();
        return UnoRandom.getRandom().nextInt(players);
    }

    private void resetPlayers(Game game){
        for(Player player : game.getPlayers()){
            player.setUnoSaid(false);
            player.setDrawPenalties(0);
        }
    }

    private int handleAndGetFirstCardReceiver(Game game, List<Player> playerList){
        Random random = UnoRandom.getRandom();
        int firstCardReceiver;
        String uuid;
        do {
            firstCardReceiver = random.nextInt(playerList.size());
            uuid = playerList.get(firstCardReceiver).getUuid();
        }while(game.wasAlreadyFirstCardReceiver(uuid));
        game.addPreviousFirstCardReceiver(uuid);
        return firstCardReceiver;
    }
}
