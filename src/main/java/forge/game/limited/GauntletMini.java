/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.limited;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.List;

import forge.AllZone;
import forge.Singletons;
import forge.deck.Deck;
import forge.game.GameNew;
import forge.game.GameType;
import forge.game.PlayerStartsGame;
import forge.gui.SOverlayUtils;
import forge.gui.match.CMatchUI;

/**
 * <p>
 * GauntletMini class.
 * </p>
 * 
 * @author Forge
 * @version $Id: GauntletMini.java $
 * @since 1.2.xx
 */
public class GauntletMini {

    private int rounds;
    private Deck humanDeck;
    private int currentRound;
    private int wins;
    private int losses;
    private boolean gauntletDraft; // Means: Draft game is in Gauntlet-mode, not a single match
    private GameType gauntletType;
    private List<Deck> aiDecks;

    // private final String humanName;
    /**
     * TODO: Write javadoc for Constructor.
     */
    public void gauntletMini() {
        currentRound = 1;
        gauntletDraft = false;
        wins = 0;
        losses = 0;
        gauntletType = GameType.Sealed; // Assignable in launch();
        aiDecks = null;
    }

    /**
     * 
     * Set the number of rounds in the tournament.
     * 
     * @param gameRounds
     *          the number of rounds in the mini tournament
     */

    private void setRounds(int gameRounds) {
        rounds = gameRounds;
    }

    /**
     * 
     * Chooses the human deck for the tournament.
     * Note: The AI decks are connected to the human deck.
     * 
     * @param hDeck
     *          the human deck for this tournament
     */
    private void setHumanDeck(Deck hDeck) {
        humanDeck = hDeck;
    }

    /**
     * Resets the tournament.
     */
    public void resetCurrentRound() {
        wins = 0;
        losses = 0;
        currentRound = 1;
    }


    /**
     * Advances the tournament to the next round.
     */
    public void nextRound() {

        // System.out.println("Moving from round " + currentRound + " to round " +  currentRound + 1 + " of " + rounds);
        if (currentRound >= rounds) {
            currentRound = rounds;
            return;
        }

        currentRound += 1;

    }

    /**
     * 
     * Setup and launch the gauntlet.
     * Note: The AI decks are connected to the human deck.
     * 
     * @param gameRounds
     *          the number of rounds (opponent decks) in this tournament
     * @param hDeck
     *          the human deck for this tournament
     * @param gType
     *          game type (Sealed, Draft, Constructed...)
     */
    public void launch(int gameRounds, Deck hDeck, final GameType gType) {
        setHumanDeck(hDeck);
        setRounds(gameRounds);
        gauntletType = gType;
        if (gauntletType == GameType.Sealed) {
            aiDecks = Singletons.getModel().getDecks().getSealed().get(humanDeck.getName()).getAiDecks();
        }
        else if (gauntletType == GameType.Draft) {
            gauntletDraft = true;
            aiDecks = Singletons.getModel().getDecks().getDraft().get(humanDeck.getName()).getAiDecks();
        }
        else {
            throw new IllegalStateException("Cannot launch Gauntlet, game mode not implemented.");
        }
        resetCurrentRound();
        start();
    }

    /**
     * Starts the tournament.
     */
    private void start() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

        final SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {
            @Override

            public Object doInBackground() {
                CMatchUI.SINGLETON_INSTANCE.initMatch(null);
                Singletons.getModel().getMatchState().setGameType(gauntletType);
                GameNew.newGame( new PlayerStartsGame(AllZone.getHumanPlayer(), humanDeck),
                                 new PlayerStartsGame(AllZone.getComputerPlayer(), aiDecks.get(currentRound)));
                return null;
            }

            @Override
            public void done() {
                SOverlayUtils.hideOverlay();
            }
        };
        worker.execute();
    }


    /**
     * Returns the total number of rounds in the tournament.
     * @return int, number of rounds in the tournament
     */
    public final int getRounds() {
        return rounds;
    }

    /**
     * Returns the number of the current round in the tournament.
     * @return int, number of rounds in the tournament
     */
    public final int getCurrentRound() {
        return currentRound;
    }

    /**
     * Adds a game win to the tournament statistics.
     */
    public void addWin() {
        wins++;
    }

    /**
     * Adds a game loss to the tournament statistics.
     */
    public void addLoss() {
        losses++;
    }

    /**
     * The total number of won games in this tournament.
     * @return int, number of wins
     */
    public final int getWins() {
        return wins;
    }

    /**
     * The total number of lost games in this tournament.
     * @return int, numer of losses
     */
    public final int getLosses() {
        return losses;
    }

    /**
     * Resets the gauntletDraft value.
     */
    public void resetGauntletDraft() {
        gauntletDraft = false;
    }

    /**
     * Draft mode status.
     * @return boolean, gauntletDraft
     */
    public final boolean getGauntletDraft() {
        return gauntletDraft;
    }

}

