package de.johannaherrmann.javauno.controller;

import de.johannaherrmann.javauno.TestHelper;
import de.johannaherrmann.javauno.data.state.UnoState;
import de.johannaherrmann.javauno.data.state.component.Game;
import de.johannaherrmann.javauno.data.state.component.Player;
import de.johannaherrmann.javauno.data.state.component.TurnState;
import de.johannaherrmann.javauno.exceptions.ExceptionMessage;
import de.johannaherrmann.javauno.exceptions.IllegalArgumentException;
import de.johannaherrmann.javauno.exceptions.IllegalStateException;
import de.johannaherrmann.javauno.service.GameService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
public class TurnControllerKeepTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameService gameService;

    private Game game;

    @Before
    public void setup(){
        game = TestHelper.createGame(gameService);
        addPlayers();
        gameService.startGame(game.getUuid());
        game.setTurnState(TurnState.PUT_DRAWN);
    }

    @After
    public void teardown(){
        TestHelper.deleteGames();
    }

    @Test
    public void shouldKeep() throws Exception {
        String gameUuid = game.getUuid();
        Player player = game.getPlayers().get(0);
        String playerUuid = player.getUuid();

        MvcResult mvcResult = this.mockMvc.perform(post("/api/turn/keep/{gameUuid}/{playerUuid}", gameUuid, playerUuid))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(TestHelper.jsonToObject(mvcResult.getResponse().getContentAsString()).getMessage()).isEqualTo("success");
        assertThat(game.getTurnState()).isEqualTo(TurnState.FINAL_COUNTDOWN);
    }

    @Test
    public void shouldFailCausedByNoSuchGame() throws Exception {
        UnoState.removeGame(game.getUuid());
        Exception expectedException = new IllegalArgumentException(ExceptionMessage.NO_SUCH_GAME.getValue());
        shouldFail(expectedException, TurnState.PUT_DRAWN, HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldFailCausedByNoSuchPlayer() throws Exception {
        game.getPlayers().clear();
        game.getPlayers().add(new Player("test", false));
        Exception expectedException = new IllegalArgumentException(ExceptionMessage.NO_SUCH_PLAYER.getValue());
        shouldFail(expectedException, TurnState.PUT_DRAWN, HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldFailCausedByInvalidTurnState() throws Exception {
        game.setTurnState(TurnState.PUT_OR_DRAW);
        Exception expectedException = new IllegalStateException(ExceptionMessage.INVALID_STATE_TURN.getValue());
        shouldFail(expectedException, TurnState.PUT_OR_DRAW, HttpStatus.BAD_REQUEST);
    }

    @Test
    public void shouldFailCausedByAnotherTurn() throws Exception {
        game.setCurrentPlayerIndex(1);
        Exception expectedException = new IllegalStateException(ExceptionMessage.NOT_YOUR_TURN.getValue());
        shouldFail(expectedException, TurnState.PUT_DRAWN, HttpStatus.BAD_REQUEST);
    }

    private void shouldFail(Exception expectedException, TurnState turnState, HttpStatus httpStatus) throws Exception {
        String gameUuid = game.getUuid();
        Player player = game.getPlayers().get(0);
        String playerUuid = player.getUuid();
        String expectedMessage = "failure: " + expectedException;

        MvcResult mvcResult = this.mockMvc.perform(post("/api/turn/keep/{gameUuid}/{playerUuid}", gameUuid, playerUuid))
                .andExpect(status().is(httpStatus.value()))
                .andReturn();

        assertThat(TestHelper.jsonToObject(mvcResult.getResponse().getContentAsString()).getMessage()).isEqualTo(expectedMessage);
        assertThat(game.getTurnState()).isEqualTo(turnState);
    }


    private void addPlayers(){
        Player player = new Player("player name", false);
        Player player2 = new Player("player2 name", false);
        game.putHuman(player);
        game.putHuman(player2);
    }
}
