package de.markherrmann.javauno.data.state.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.markherrmann.javauno.data.fixed.Card;
import de.markherrmann.javauno.data.fixed.Color;

import java.util.*;

public class Game{
	
	private String uuid;
	private Map<String, Player> humans = new HashMap<>();
	private Map<String, Player> bots = new HashMap<>();
	private List<Player> players = new ArrayList<>();
	private Stack<Card> layStack = new Stack<>();
	private Stack<Card> takeStack = new Stack<>();
	private boolean reversed;
	private String desiredColor;
	private int currentPlayerIndex;
	private GameLifecycle gameLifecycle = GameLifecycle.SET_PLAYERS;
	private Card topCard;
	private long lastAction;
    private TurnState turnState;
    private int take;
    private boolean skip;

	public Game(){
	    this.uuid = UUID.randomUUID().toString();
    }

    @JsonIgnore
	public String getUuid(){
		return uuid;
	}

	@JsonIgnore
	public Map<String, Player> getHumans(){
		return humans;
	}

	public void putHuman(Player player){
	    humans.put(player.getUuid(), player);
	    addPlayer(player);
    }

    public void removeHuman(Player player){
        humans.remove(player.getUuid());
        removePlayer(player);
    }

    @JsonIgnore
    public Map<String, Player> getBots(){
        return bots;
    }

    public void putBot(Player player){
        bots.put(player.getBotUuid(), player);
        addPlayer(player);
    }

    public void removeBot(Player player){
        bots.remove(player.getBotUuid());
        removePlayer(player);
    }

	@JsonIgnore
    public List<Player> getPlayers() {
        return players;
    }

	@JsonIgnore
    public Stack<Card> getLayStack(){
		return layStack;
	}

	@JsonIgnore
	public Stack<Card> getTakeStack(){
		return takeStack;
	}

    public boolean isReversed() {
        return reversed;
    }

    public String getDesiredColor(){
		return desiredColor;
	}

    public void toggleReversed(){
	    reversed = !reversed;
    }

    public void setDesiredColor(String desiredColor) {
        this.desiredColor = desiredColor;
    }

    @JsonIgnore
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

	public void setCurrentPlayerIndex(int currentPlayerIndex) {
		this.currentPlayerIndex = currentPlayerIndex;
	}

	public GameLifecycle getGameLifecycle() {
		return gameLifecycle;
	}

	public void setGameLifecycle(GameLifecycle gameLifecycle) {
		this.gameLifecycle = gameLifecycle;
	}

    public void setLastAction(long lastAction) {
        this.lastAction = lastAction;
    }

    public long getLastAction(){
	    return lastAction;
    }

	public Card getTopCard(){
		if(layStack.isEmpty()){
			return null;
		}
		return layStack.peek();
	}

	private void addPlayer(Player player){
	    players.add(player);
    }

    private void removePlayer(Player player){
        players.remove(player);
    }


    @JsonIgnore
    public TurnState getTurnState() {
        return turnState;
    }

    public void setTurnState(TurnState turnState) {
        this.turnState = turnState;
    }

	public int getTake() {
		return take;
	}

	public void setTake(int take) {
		this.take = take;
	}

	public boolean isSkip() {
		return skip;
	}

	public void setSkip(boolean skip) {
		this.skip = skip;
	}
}
